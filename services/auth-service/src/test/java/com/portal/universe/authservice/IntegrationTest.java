package com.portal.universe.authservice;

import org.apache.avro.specific.SpecificRecord;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * auth-service 통합 테스트 베이스 클래스.
 * Testcontainers로 PostgreSQL + Redis를 실행하고
 * 외부 시스템(Kafka 등) 의존성을 Mock으로 대체합니다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class)
public abstract class IntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @MockitoBean
    protected KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",

                    "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgresContainer.getUsername(),
                    "spring.datasource.password=" + postgresContainer.getPassword(),
                    "spring.flyway.enabled=false",

                    "spring.data.redis.host=" + redisContainer.getHost(),
                    "spring.data.redis.port=" + redisContainer.getMappedPort(6379),

                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.show-sql=true",

                    "jwt.secret-key=test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-for-hmac-sha256",
                    "jwt.access-token-expiration=900000",
                    "jwt.refresh-token-expiration=604800000",

                    "jwt.current-key-id=test-key-1",
                    "jwt.keys.test-key-1.secret-key=test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-for-hmac-sha256",
                    "jwt.keys.test-key-1.activated-at=2025-01-01T00:00:00",

                    "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                    "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
                    "spring.security.oauth2.client.registration.google.scope=email,profile",
                    "spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
                    "spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth",
                    "spring.security.oauth2.client.provider.google.token-uri=https://www.googleapis.com/oauth2/v4/token",
                    "spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo",
                    "spring.security.oauth2.client.provider.google.user-name-attribute=sub",

                    "spring.kafka.bootstrap-servers=localhost:9092"
            );
        }
    }
}
