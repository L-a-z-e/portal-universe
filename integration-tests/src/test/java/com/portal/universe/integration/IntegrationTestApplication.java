package com.portal.universe.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for integration tests.
 * This is a minimal application context for running tests.
 */
@SpringBootApplication
public class IntegrationTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }
}
