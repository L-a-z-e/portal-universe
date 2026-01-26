package com.portal.universe.authservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * `auth-service`의 모든 통합 테스트를 위한 추상 базовый(base) 클래스입니다.
 * 주요 역할:
 * 1. Testcontainers를 사용하여 테스트용 MySQL 데이터베이스 인스턴스를 실행합니다.
 * 2. 외부 시스템(Config Server, Eureka, Kafka)에 대한 의존성을 비활성화하거나 Mock으로 대체합니다.
 * 3. 모든 통합 테스트 클래스는 이 클래스를 상속받아 일관된 테스트 환경을 공유합니다.
 */
 @SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT // 테스트 실행 시 실제 서블릿 컨테이너를 랜덤 포트로 실행
)
 @Testcontainers // Testcontainers 기능을 활성화
 @ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class) // 커스텀 설정을 적용하기 위한 Initializer 등록
public abstract class IntegrationTest {

    /**
     * 테스트에 사용될 MySQL 데이터베이스 컨테이너입니다.
     * 모든 테스트 클래스에서 동일한 컨테이너 인스턴스를 공유하도록 static으로 선언되었습니다.
     */
    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    /**
     * 테스트에 사용될 Redis 컨테이너입니다.
     * JWT Refresh Token 저장 및 블랙리스트 관리에 사용됩니다.
     */
    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    /**
     * KafkaTemplate을 MockBean으로 대체합니다.
     * 통합 테스트 중 실제 Kafka 브로커로 메시지를 보내는 것을 방지합니다.
     */
    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Testcontainers로 실행된 MySQL/Redis 컨테이너의 동적 설정을
     * Spring의 ApplicationContext에 주입하는 역할을 합니다.
     */
    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // --- 외부 시스템 의존성 비활성화 ---
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",

                    // --- Testcontainers를 사용한 동적 DB 설정 ---
                    "spring.datasource.url=" + mySQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mySQLContainer.getUsername(),
                    "spring.datasource.password=" + mySQLContainer.getPassword(),

                    // --- Testcontainers를 사용한 동적 Redis 설정 ---
                    "spring.data.redis.host=" + redisContainer.getHost(),
                    "spring.data.redis.port=" + redisContainer.getMappedPort(6379),

                    // --- 테스트용 JPA/Hibernate 설정 ---
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.show-sql=true",

                    // --- 테스트용 JWT 설정 ---
                    "jwt.secret-key=test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-for-hmac-sha256",
                    "jwt.access-token-expiration=900000",
                    "jwt.refresh-token-expiration=604800000",

                    // --- 테스트용 OAuth2 Client 설정 (더미) ---
                    "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                    "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
                    "spring.security.oauth2.client.registration.google.scope=email,profile",
                    "spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
                    "spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth",
                    "spring.security.oauth2.client.provider.google.token-uri=https://www.googleapis.com/oauth2/v4/token",
                    "spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo",
                    "spring.security.oauth2.client.provider.google.user-name-attribute=sub",

                    // --- 테스트용 Kafka 설정 ---
                    "spring.kafka.bootstrap-servers=localhost:9092"
            );
        }
    }
}