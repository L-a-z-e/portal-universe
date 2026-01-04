package com.portal.universe.apigateway.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Frontend properties
        "app.frontend.host=localhost:30000",
        "app.frontend.scheme=http",
        "app.frontend.port=30000",

        // JWT/Security properties
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json",

        // Eureka 비활성화
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class GlobalForwardedHeadersFilterTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // WireMock 서버 시작
        wireMockServer = new WireMockServer(options().port(8081));
        wireMockServer.start();

        // Gateway 라우트를 WireMock으로 오버라이드
        registry.add("spring.cloud.gateway.routes[0].id", () -> "auth-service-test");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:8081");
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/auth-service/**");
        registry.add("spring.cloud.gateway.routes[0].filters[0]", () -> "StripPrefix=1");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    void shouldAddForwardedHeadersToAuthService() {
        // WireMock으로 Auth Service 모킹
        wireMockServer.stubFor(get(urlPathEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")));

        // Gateway로 요청
        webTestClient.get()
                .uri("/auth-service/oauth2/authorize?client_id=portal-client")
                .header("Host", "localhost:30000")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("OK");

        // Auth Service가 받은 헤더 확인
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/oauth2/authorize"))
                .withHeader("X-Forwarded-Host", equalTo("localhost:30000"))
                .withHeader("X-Forwarded-Proto", equalTo("http"))
                .withHeader("X-Forwarded-Port", equalTo("30000"))
                .withHeader("X-Forwarded-For", matching(".*")));
    }

    @Test
    void shouldPreserveHostHeader() {
        wireMockServer.stubFor(get(urlPathEqualTo("/login"))
                .willReturn(aResponse().withStatus(200)));

        webTestClient.get()
                .uri("/auth-service/login")
                .header("Host", "localhost:30000")
                .exchange()
                .expectStatus().isOk();

        // Host 헤더가 원본 유지되는지 확인
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/login"))
                .withHeader("Host", equalTo("localhost:30000")));
    }

    @Test
    void debugActualHeaders() {
        wireMockServer.stubFor(get(urlPathMatching("/oauth2/.*"))
                .willReturn(aResponse().withStatus(200)));

        webTestClient.get()
                .uri("/auth-service/oauth2/authorize?client_id=portal-client")
                .header("Host", "localhost:30000")
                .exchange()
                .expectStatus().isOk();

        // WireMock이 받은 실제 헤더 출력
        System.out.println("=== Received Headers ===");
        wireMockServer.getAllServeEvents().forEach(event -> {
            event.getRequest().getHeaders().all().forEach(header ->
                    System.out.println(header.key() + ": " + header.values())
            );
        });
    }
}
