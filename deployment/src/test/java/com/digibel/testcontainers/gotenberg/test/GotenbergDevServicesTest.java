package com.digibel.testcontainers.gotenberg.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GotenbergDevServicesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    @ConfigProperty(name = "quarkus.gotenberg.url")
    String gotenbergUrl;

    @Test
    public void testGotenbergDevServicesStarted() {
        System.out.println("Gotenberg Dev Services URL: " + gotenbergUrl);
        assertNotNull(gotenbergUrl, "Gotenberg URL should be configured by Dev Services");
        assertTrue(gotenbergUrl.contains(":"), "Gotenberg URL should contain a port: " + gotenbergUrl);
    }
}

