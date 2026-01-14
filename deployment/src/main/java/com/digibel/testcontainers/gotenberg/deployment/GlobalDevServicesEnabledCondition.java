package com.digibel.testcontainers.gotenberg.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.LaunchMode;

/**
 * Condition to check if Dev Services should be enabled.
 */
public class GlobalDevServicesEnabledCondition implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return LaunchMode.current() == LaunchMode.DEVELOPMENT || LaunchMode.current() == LaunchMode.TEST;
    }
}

