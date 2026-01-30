package com.portal.universe.authservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * 로컬 Docker 환경(MySQL:3307, Redis:6379)을 사용하는 통합 테스트 base 클래스입니다.
 * Testcontainers가 Docker Desktop과 호환되지 않을 때 대안으로 사용합니다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = LocalIntegrationTest.DataSourceInitializer.class)
public abstract class LocalIntegrationTest {

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // 외부 시스템 비활성화
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",

                    // 기존 MySQL 사용
                    "spring.datasource.url=jdbc:mysql://localhost:3307/auth_test_db?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
                    "spring.datasource.username=root",
                    "spring.datasource.password=root",

                    // 기존 Redis 사용
                    "spring.data.redis.host=localhost",
                    "spring.data.redis.port=6379",

                    // JPA 설정
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.show-sql=true",

                    // JWT 설정
                    "jwt.secret-key=test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-for-hmac-sha256",
                    "jwt.access-token-expiration=900000",
                    "jwt.refresh-token-expiration=604800000",

                    // JWT Key Rotation 설정
                    "jwt.current-key-id=test-key-1",
                    "jwt.keys.test-key-1.secret-key=test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-for-hmac-sha256",
                    "jwt.keys.test-key-1.activated-at=2025-01-01T00:00:00",

                    // OAuth2 Client 더미 설정
                    "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                    "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
                    "spring.security.oauth2.client.registration.google.scope=email,profile",
                    "spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
                    "spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth",
                    "spring.security.oauth2.client.provider.google.token-uri=https://www.googleapis.com/oauth2/v4/token",
                    "spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo",
                    "spring.security.oauth2.client.provider.google.user-name-attribute=sub",

                    // Kafka 설정
                    "spring.kafka.bootstrap-servers=localhost:9092"
            );
        }
    }
}
