package com.portal.universe.notificationservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * notification-service 통합 테스트 베이스 클래스.
 * Testcontainers로 MySQL + Redis를 실행하고
 * Flyway로 운영과 동일한 스키마를 생성합니다.
 * 외부 시스템(Kafka, SQS)은 Mock으로 대체합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class)
public abstract class IntegrationTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("notification_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    protected SqsClient sqsClient;

    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    "spring.cloud.aws.region.static=ap-northeast-2",
                    "spring.cloud.aws.credentials.access-key=test",
                    "spring.cloud.aws.credentials.secret-key=test",
                    "spring.cloud.aws.endpoint=http://localhost:4566",
                    "spring.cloud.aws.secretsmanager.enabled=false",
                    "spring.cloud.aws.parameterstore.enabled=false",

                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mysqlContainer.getUsername(),
                    "spring.datasource.password=" + mysqlContainer.getPassword(),
                    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",

                    "spring.flyway.enabled=true",
                    "spring.jpa.hibernate.ddl-auto=validate",
                    "spring.jpa.show-sql=true",
                    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",

                    "spring.data.redis.host=" + redisContainer.getHost(),
                    "spring.data.redis.port=" + redisContainer.getMappedPort(6379),

                    "spring.kafka.bootstrap-servers=localhost:9092",

                    "aws.sqs.email-queue-url=http://localhost:4566/000000000000/email-queue"
            );
        }
    }
}
