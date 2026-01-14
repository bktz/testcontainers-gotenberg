package com.digibel.testcontainers.gotenberg.runtime;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

/**
 * Testcontainer for Gotenberg.
 */
public class GotenbergContainer extends GenericContainer<GotenbergContainer> {

    public static final int GOTENBERG_PORT = 3000;
    private static final String HEALTH_ENDPOINT = "/health";

    public GotenbergContainer(String dockerImageName) {
        super(dockerImageName);
        withExposedPorts(GOTENBERG_PORT);
        setWaitStrategy(new HttpWaitStrategy()
                .forPort(GOTENBERG_PORT)
                .forPath(HEALTH_ENDPOINT)
                .withStartupTimeout(Duration.ofSeconds(60)));
    }

    /**
     * Get the URL to connect to Gotenberg.
     *
     * @return the Gotenberg URL
     */
    public String getGotenbergUrl() {
        return "http://" + getHost() + ":" + getMappedPort(GOTENBERG_PORT);
    }
}

