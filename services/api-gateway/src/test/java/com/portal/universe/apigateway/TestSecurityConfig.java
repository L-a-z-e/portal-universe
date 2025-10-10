package com.portal.universe.apigateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return mock(ReactiveJwtDecoder.class);
    }
}
