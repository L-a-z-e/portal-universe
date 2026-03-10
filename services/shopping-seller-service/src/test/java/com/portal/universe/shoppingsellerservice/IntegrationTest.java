package com.portal.universe.shoppingsellerservice;

import org.apache.avro.specific.SpecificRecord;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * shopping-seller-service 통합 테스트 베이스 클래스.
 * Testcontainers로 PostgreSQL을 실행하고
 * Flyway로 운영과 동일한 스키마를 생성합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class)
public abstract class IntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

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

                    "spring.flyway.enabled=true",
                    "spring.jpa.hibernate.ddl-auto=validate",
                    "spring.jpa.show-sql=true",

                    "spring.kafka.bootstrap-servers=localhost:9092"
            );
        }
    }
}
