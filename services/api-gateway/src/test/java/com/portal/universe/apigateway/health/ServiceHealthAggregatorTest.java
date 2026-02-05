package com.portal.universe.apigateway.health;

import com.portal.universe.apigateway.health.config.HealthCheckProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ServiceHealthAggregator Test")
class ServiceHealthAggregatorTest {

    private MockWebServer mockWebServer;
    private HealthCheckProperties properties;
    private HealthEndpoint healthEndpoint;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        properties = new HealthCheckProperties();
        healthEndpoint = mock(HealthEndpoint.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private ServiceHealthAggregator createAggregator(String applicationName) {
        var aggregator = new ServiceHealthAggregator(properties, null, healthEndpoint);
        // applicationName 설정 (리플렉션)
        try {
            var field = ServiceHealthAggregator.class.getDeclaredField("applicationName");
            field.setAccessible(true);
            field.set(aggregator, applicationName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return aggregator;
    }

    private HealthCheckProperties.ServiceConfig createServiceConfig(String name, String displayName) {
        var config = new HealthCheckProperties.ServiceConfig();
        config.setName(name);
        config.setDisplayName(displayName);
        config.setUrl(mockWebServer.url("/").toString().replaceAll("/$", ""));
        config.setHealthPath("/actuator/health");
        return config;
    }

    @Nested
    @DisplayName("aggregateHealth")
    class AggregateHealth {

        @Test
        @DisplayName("모든 서비스가 정상이면 overallStatus는 up이다")
        void should_returnUpForAllServices_when_allHealthy() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"status\":\"UP\"}")
                    .addHeader("Content-Type", "application/json"));
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"status\":\"UP\"}")
                    .addHeader("Content-Type", "application/json"));

            var config1 = createServiceConfig("auth", "Auth Service");
            var config2 = createServiceConfig("blog", "Blog Service");
            properties.setServices(List.of(config1, config2));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        assertThat(response.overallStatus()).isEqualTo("up");
                        assertThat(response.services()).hasSize(2);
                    })
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("모든 서비스가 장애이면 overallStatus는 down이다")
        void should_returnDown_when_allServicesDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            var config1 = createServiceConfig("auth", "Auth Service");
            var config2 = createServiceConfig("blog", "Blog Service");
            properties.setServices(List.of(config1, config2));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        assertThat(response.overallStatus()).isEqualTo("down");
                        assertThat(response.services()).hasSize(2);
                        assertThat(response.services()).allMatch(s -> "down".equals(s.status()));
                    })
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("서비스 응답 timeout 시 down을 반환한다")
        void should_returnDown_when_timeout() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"status\":\"UP\"}")
                    .addHeader("Content-Type", "application/json")
                    .setBodyDelay(5, TimeUnit.SECONDS));

            var config = createServiceConfig("auth", "Auth Service");
            properties.setServices(List.of(config));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        assertThat(response.services()).hasSize(1);
                        assertThat(response.services().get(0).status()).isEqualTo("down");
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("일부 서비스가 장애이면 overallStatus는 degraded이다")
        void should_returnDegraded_when_someServiceDown() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"status\":\"UP\"}")
                    .addHeader("Content-Type", "application/json"));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            var config1 = createServiceConfig("auth", "Auth Service");
            var config2 = createServiceConfig("blog", "Blog Service");
            properties.setServices(List.of(config1, config2));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response ->
                            assertThat(response.overallStatus()).isEqualTo("degraded"))
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    @DisplayName("checkSelf")
    class CheckSelf {

        @Test
        @DisplayName("gateway 자신의 헬스가 UP이면 up을 반환한다")
        void should_returnUp_when_selfHealthUp() {
            when(healthEndpoint.health()).thenReturn(Health.up().build());

            var config = new HealthCheckProperties.ServiceConfig();
            config.setName("api-gateway");
            config.setDisplayName("API Gateway");
            config.setUrl("http://localhost:8080");
            config.setHealthPath("/actuator/health");
            properties.setServices(List.of(config));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        assertThat(response.services()).hasSize(1);
                        assertThat(response.services().get(0).status()).isEqualTo("up");
                    })
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("gateway 자신의 헬스 체크가 실패하면 down을 반환한다")
        void should_returnDown_when_selfHealthException() {
            when(healthEndpoint.health()).thenThrow(new RuntimeException("Health check failed"));

            var config = new HealthCheckProperties.ServiceConfig();
            config.setName("api-gateway");
            config.setDisplayName("API Gateway");
            config.setUrl("http://localhost:8080");
            config.setHealthPath("/actuator/health");
            properties.setServices(List.of(config));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        assertThat(response.services()).hasSize(1);
                        assertThat(response.services().get(0).status()).isEqualTo("down");
                    })
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    @DisplayName("resolveStatus")
    class ResolveStatus {

        private String invokeResolveStatus(Map<String, Object> body) throws Exception {
            var aggregator = createAggregator("api-gateway");
            Method method = ServiceHealthAggregator.class.getDeclaredMethod("resolveStatus", Map.class);
            method.setAccessible(true);
            return (String) method.invoke(aggregator, body);
        }

        @Test
        @DisplayName("Spring Boot 형식 UP → up")
        void should_resolveUp_when_springBootFormat() throws Exception {
            assertThat(invokeResolveStatus(Map.of("status", "UP"))).isEqualTo("up");
        }

        @Test
        @DisplayName("Custom 형식 {success:true, data:{status:ok}} → up")
        void should_resolveUp_when_customFormat() throws Exception {
            Map<String, Object> body = Map.of(
                    "success", true,
                    "data", Map.of("status", "ok")
            );
            assertThat(invokeResolveStatus(body)).isEqualTo("up");
        }

        @Test
        @DisplayName("Spring Boot 형식 DOWN → down")
        void should_resolveDown_when_statusDown() throws Exception {
            assertThat(invokeResolveStatus(Map.of("status", "DOWN"))).isEqualTo("down");
        }

        @Test
        @DisplayName("알 수 없는 status → degraded")
        void should_resolveDegraded_when_unknownStatus() throws Exception {
            assertThat(invokeResolveStatus(Map.of("status", "UNKNOWN"))).isEqualTo("degraded");
        }

        @Test
        @DisplayName("status 필드가 없으면 unknown")
        void should_resolveUnknown_when_noStatusField() throws Exception {
            assertThat(invokeResolveStatus(Map.of("other", "value"))).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("K8s enrichment")
    class KubernetesEnrichment {

        @Test
        @DisplayName("kubernetesClient가 null이면 K8s 정보 없이 반환한다")
        void should_skipK8sEnrichment_when_noClient() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"status\":\"UP\"}")
                    .addHeader("Content-Type", "application/json"));

            var config = createServiceConfig("auth", "Auth Service");
            properties.setServices(List.of(config));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        var service = response.services().get(0);
                        assertThat(service.replicas()).isNull();
                        assertThat(service.pods()).isNull();
                    })
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("k8sDeploymentName이 null이면 K8s 정보 없이 반환한다")
        void should_skipK8sEnrichment_when_noDeploymentName() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"status\":\"UP\"}")
                    .addHeader("Content-Type", "application/json"));

            var config = createServiceConfig("auth", "Auth Service");
            config.setK8sDeploymentName(null);
            properties.setServices(List.of(config));

            var aggregator = createAggregator("api-gateway");

            StepVerifier.create(aggregator.aggregateHealth())
                    .assertNext(response -> {
                        var service = response.services().get(0);
                        assertThat(service.replicas()).isNull();
                    })
                    .expectComplete()
                    .verify();
        }
    }
}
