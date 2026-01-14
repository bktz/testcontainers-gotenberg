package com.digibel.testcontainers.gotenberg.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class TestcontainersGotenbergProcessor {

    private static final String FEATURE = "testcontainers-gotenberg";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
