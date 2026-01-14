package com.digibel.testcontainers.gotenberg.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TestcontainersGotenbergResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/testcontainers-gotenberg")
                .then()
                .statusCode(200)
                .body(is("Hello testcontainers-gotenberg"));
    }
}
