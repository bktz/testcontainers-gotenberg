package com.digibel.testcontainers.gotenberg.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for Gotenberg Dev Services.
 */
@ConfigMapping(prefix = "quarkus.gotenberg.devservices")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface GotenbergDevServicesConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The container image name to use for the Gotenberg container.
     */
    @WithDefault("gotenberg/gotenberg:8")
    String imageName();

    /**
     * Indicates if the Gotenberg container managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-gotenberg} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-gotenberg} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services looks for a container with the
     * {@code quarkus-dev-service-gotenberg} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-gotenberg} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Gotenberg containers.
     */
    @WithDefault("gotenberg")
    String serviceName();

    /**
     * Optional fixed port the Gotenberg container will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    Optional<Integer> port();
}

