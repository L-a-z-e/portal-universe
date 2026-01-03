package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Slf4j
@DisplayName("OidcForwardedHeadersGatewayFilterFactory Integration Test")
public class OidcForwardedHeadersGatewayFilterFactoryIntegrationTest {

    static MockWebServer mockWebServer;

    @BeforeAll
    static void setUpMockWebServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        log.info("Start mockWebServer port: {}", mockWebServer.getPort());
    }

    @AfterAll
    static void tearDownMockWebServer() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("test.mock.webserver.uri", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @TestConfiguration
    static class TestRoutingConfig {
        @Value("${test.mock.webserver.uri}")
        private String mockWebServerUri;

        @Bean
        public RouteLocator testOidcRoutes(
                RouteLocatorBuilder routeLocatorBuilder,
                OidcForwardedHeadersGatewayFilterFactory oidcForwardedHeadersGatewayFilterFactory
        ) {
            var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
            config.setEnabled(true);

            return routeLocatorBuilder.routes()
                    .route("test-oidc-route", r -> r
                            .path("/test/**")  // ← /test/** 경로
                            .filters(f -> f.filter(oidcForwardedHeadersGatewayFilterFactory.apply(config)))  // ← OidcFilter 붙임!
                            .uri(mockWebServerUri))  // ← MockWebServer로!
                    .build();
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private FrontendProperties frontendProperties;

    @Test
    @DisplayName("필터가 X-Forwarded-* 헤더를 올바르게 추가하는지 검증")
    void shouldAddForwardedHeaders() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse().setBody("OK").setResponseCode(200));

        // When
        webTestClient.get()
                .uri("/test/login")
                .exchange()
                .expectStatus().isOk();

        // Then
        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getHeader("X-Forwarded-Host")).isEqualTo("portal-test.com:443");
        assertThat(recorded.getHeader("X-Forwarded-Proto")).isEqualTo("https");
        assertThat(recorded.getHeader("X-Forwarded-Port")).isEqualTo("443");
        assertThat(recorded.getHeader("X-Forwarded-Prefix")).isEqualTo("/auth-service");
    }
}