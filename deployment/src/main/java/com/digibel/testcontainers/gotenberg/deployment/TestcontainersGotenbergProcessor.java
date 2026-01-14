package com.digibel.testcontainers.gotenberg.deployment;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;

import com.digibel.testcontainers.gotenberg.runtime.GotenbergBuildTimeConfig;
import com.digibel.testcontainers.gotenberg.runtime.GotenbergContainer;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = GlobalDevServicesEnabledCondition.class)
class TestcontainersGotenbergProcessor {

    private static final Logger log = Logger.getLogger(TestcontainersGotenbergProcessor.class);
    private static final String FEATURE = "testcontainers-gotenberg";
    private static final String GOTENBERG_URL_PROPERTY = "quarkus.gotenberg.url";
    private static final int GOTENBERG_PORT = 3000;

    static volatile RunningDevService devService;
    static volatile GotenbergDevServicesConfig capturedDevServicesConfiguration;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public DevServicesResultBuildItem startGotenbergDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            GotenbergBuildTimeConfig gotenbergBuildTimeConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        com.digibel.testcontainers.gotenberg.runtime.GotenbergDevServicesConfig currentDevServicesConfiguration = gotenbergBuildTimeConfig
                .devservices();

        // Check if we need to shut down and restart the service
        if (devService != null) {
            boolean shouldShutdownTheBroker = capturedDevServicesConfiguration != null
                    && !new GotenbergDevServicesConfig(currentDevServicesConfiguration)
                            .equals(capturedDevServicesConfiguration);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownGotenberg();
            capturedDevServicesConfiguration = null;
            devService = null;
        }

        capturedDevServicesConfiguration = new GotenbergDevServicesConfig(currentDevServicesConfiguration);

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Gotenberg Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            devService = startGotenberg(dockerStatusBuildItem, capturedDevServicesConfiguration,
                    launchMode.getLaunchMode());
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownGotenberg();

                    log.info("Dev Services for Gotenberg shut down.");
                }
                first = true;
                devService = null;
                capturedDevServicesConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        if (devService.isOwner()) {
            log.infof("Dev Services for Gotenberg started. Other Quarkus applications in dev mode will find the "
                    + "service and use the running container.");
        }
        return devService.toBuildItem();
    }

    private void shutdownGotenberg() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Gotenberg server", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startGotenberg(DockerStatusBuildItem dockerStatusBuildItem,
            GotenbergDevServicesConfig devServicesConfig,
            LaunchMode launchMode) {

        if (!devServicesConfig.enabled()) {
            log.debug("Not starting Dev Services for Gotenberg, as it has been disabled in the config.");
            return null;
        }

        // Check if gotenberg.url is set
        if (ConfigUtils.isPropertyPresent(GOTENBERG_URL_PROPERTY)) {
            log.debug("Not starting Dev Services for Gotenberg, the quarkus.gotenberg.url is configured.");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn("Docker isn't working, please configure the Gotenberg URL property ("
                    + GOTENBERG_URL_PROPERTY + ").");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = devServicesConfig.shared()
                ? startSharedContainer(dockerStatusBuildItem, devServicesConfig, launchMode)
                : startStandaloneContainer(dockerStatusBuildItem, devServicesConfig);

        if (maybeContainerAddress.isEmpty()) {
            return null;
        }

        ContainerAddress containerAddress = maybeContainerAddress.get();

        Map<String, String> configProperties = Map.of(
                GOTENBERG_URL_PROPERTY, containerAddress.getUrl());

        return new RunningDevService(FEATURE, containerAddress.getId(),
                null, configProperties);
    }

    private Optional<ContainerAddress> startSharedContainer(DockerStatusBuildItem dockerStatusBuildItem,
            GotenbergDevServicesConfig devServicesConfig,
            LaunchMode launchMode) {

        ContainerLocator locator = new ContainerLocator("quarkus-dev-service-gotenberg", GOTENBERG_PORT);
        Optional<ContainerAddress> existingContainer = locator.locateContainer(
                devServicesConfig.serviceName(),
                devServicesConfig.shared(),
                launchMode);

        if (existingContainer.isPresent()) {
            ContainerAddress address = existingContainer.get();
            log.infof("Found existing shared Gotenberg container: %s", address.getId());
            return existingContainer;
        }

        GotenbergContainer container = createContainer(devServicesConfig);

        container.withLabel("quarkus-dev-service-gotenberg", devServicesConfig.serviceName());
        container.start();

        String host = container.getHost();
        int mappedPort = container.getMappedPort(GOTENBERG_PORT);
        String url = "http://" + host + ":" + mappedPort;

        log.infof("Gotenberg Dev Services started on %s (Container ID: %s)",
                url, container.getContainerId());

        return Optional.of(new ContainerAddress(container.getContainerId(), host, mappedPort));
    }

    private Optional<ContainerAddress> startStandaloneContainer(DockerStatusBuildItem dockerStatusBuildItem,
            GotenbergDevServicesConfig devServicesConfig) {

        GotenbergContainer container = createContainer(devServicesConfig);

        container.start();

        String host = container.getHost();
        int mappedPort = container.getMappedPort(GOTENBERG_PORT);
        String url = "http://" + host + ":" + mappedPort;

        log.infof("Gotenberg Dev Services started on %s (Container ID: %s)",
                url, container.getContainerId());

        return Optional.of(new ContainerAddress(container.getContainerId(), host, mappedPort));
    }

    private GotenbergContainer createContainer(GotenbergDevServicesConfig devServicesConfig) {

        DockerImageName dockerImageName = DockerImageName.parse(devServicesConfig.imageName())
                .asCompatibleSubstituteFor("gotenberg/gotenberg");

        GotenbergContainer container = new GotenbergContainer(dockerImageName.toString());

        // Configure port
        if (devServicesConfig.port().isPresent()) {
            Integer port = devServicesConfig.port().get();
            container.setPortBindings(java.util.List.of(port + ":" + GOTENBERG_PORT));
            log.infof("Gotenberg container configured with fixed port: %d", port);
        }

        return container;
    }


    private static final class GotenbergDevServicesConfig {
        private final boolean enabled;
        private final String imageName;
        private final boolean shared;
        private final String serviceName;
        private final Optional<Integer> port;

        public GotenbergDevServicesConfig(
                com.digibel.testcontainers.gotenberg.runtime.GotenbergDevServicesConfig config) {
            this.enabled = config.enabled();
            this.imageName = config.imageName();
            this.shared = config.shared();
            this.serviceName = config.serviceName();
            this.port = config.port();
        }

        public boolean enabled() {
            return enabled;
        }

        public String imageName() {
            return imageName;
        }

        public boolean shared() {
            return shared;
        }

        public String serviceName() {
            return serviceName;
        }

        public Optional<Integer> port() {
            return port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            GotenbergDevServicesConfig that = (GotenbergDevServicesConfig) o;
            return enabled == that.enabled &&
                    shared == that.shared &&
                    Objects.equals(imageName, that.imageName) &&
                    Objects.equals(serviceName, that.serviceName) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, imageName, shared, serviceName, port);
        }
    }

    private static class IsDockerWorking implements BooleanSupplier {

        private final boolean isDockerWorking;

        public IsDockerWorking() {
            this.isDockerWorking = isDockerAvailable();
        }

        @Override
        public boolean getAsBoolean() {
            return isDockerWorking;
        }

        private boolean isDockerAvailable() {
            try {
                Class.forName("org.testcontainers.DockerClientFactory");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
