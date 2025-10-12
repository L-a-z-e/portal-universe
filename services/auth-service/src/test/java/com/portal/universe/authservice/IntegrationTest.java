package com.portal.universe.authservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트를 위한 베이스 클래스
 * - Testcontainers를 사용한 MySQL 컨테이너 제공
 * - 외부 의존성(Config Server, Eureka, Kafka) Mock 처리
 */
 @SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
 @Testcontainers @ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class)
public abstract class IntegrationTest {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    /**
     * Kafka 의존성 제거: 테스트 환경에서는 실제 Kafka 연결이 불필요하므로 Mock 처리
     */
    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Testcontainers MySQL 설정을 Spring 환경에 주입
     */
    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // Cloud Config & Discovery 비활성화
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    
                    // Testcontainers MySQL 동적 설정
                    "spring.datasource.url=" + mySQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mySQLContainer.getUsername(),
                    "spring.datasource.password=" + mySQLContainer.getPassword(),
                    
                    // JPA 설정 강제
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.show-sql=true",
                    
                    // OAuth2 Authorization Server issuer (필수!)
                    "spring.security.oauth2.authorizationserver.issuer=http://localhost:8081",
                    
                    // Kafka 설정 (실제 연결은 MockBean으로 대체)
                    "spring.kafka.bootstrap-servers=localhost:9092"
            );
        }
    }
}
