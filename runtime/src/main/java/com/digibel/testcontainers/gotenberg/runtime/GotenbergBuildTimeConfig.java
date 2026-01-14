package com.digibel.testcontainers.gotenberg.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Build time configuration for Gotenberg.
 */
@ConfigMapping(prefix = "quarkus.gotenberg")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface GotenbergBuildTimeConfig {

    /**
     * Dev Services configuration.
     */
    GotenbergDevServicesConfig devservices();
}

