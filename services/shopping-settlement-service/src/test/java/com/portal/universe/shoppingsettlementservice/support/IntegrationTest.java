package com.portal.universe.shoppingsettlementservice.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class)
public abstract class IntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("settlement_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    static class DataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx,
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.flyway.enabled=false",
                    "spring.batch.jdbc.initialize-schema=always",
                    "spring.batch.job.enabled=false",
                    "settlement.batch.chunk-size=2",
                    "settlement.batch.grid-size=2",
                    "settlement.batch.retry-limit=3",
                    "settlement.batch.skip-limit=10",
                    "settlement.batch.core-pool-size=2",
                    "settlement.batch.scheduled-enabled=false"
            );
        }
    }
}
