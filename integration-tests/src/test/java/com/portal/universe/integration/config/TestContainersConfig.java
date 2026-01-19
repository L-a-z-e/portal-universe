package com.portal.universe.integration.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * Provides MySQL, Redis, Kafka, and Elasticsearch containers.
 */
public class TestContainersConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    // Container definitions - shared across all tests
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("portal_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .withReuse(true);

    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withReuse(true);

    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"))
            .withReuse(true);

    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withReuse(true);

    static {
        // Start all containers in parallel
        mysql.start();
        redis.start();
        kafka.start();
        elasticsearch.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                // MySQL configuration
                "spring.datasource.url=" + mysql.getJdbcUrl(),
                "spring.datasource.username=" + mysql.getUsername(),
                "spring.datasource.password=" + mysql.getPassword(),
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",

                // Redis configuration
                "spring.data.redis.host=" + redis.getHost(),
                "spring.data.redis.port=" + redis.getFirstMappedPort(),

                // Kafka configuration
                "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.group-id=integration-test-group",

                // Elasticsearch configuration
                "spring.elasticsearch.uris=http://" + elasticsearch.getHost() + ":" + elasticsearch.getFirstMappedPort()
        ).applyTo(ctx.getEnvironment());
    }

    // Utility methods to get container connection info
    public static String getMysqlJdbcUrl() {
        return mysql.getJdbcUrl();
    }

    public static String getRedisHost() {
        return redis.getHost();
    }

    public static Integer getRedisPort() {
        return redis.getFirstMappedPort();
    }

    public static String getKafkaBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    public static String getElasticsearchUrl() {
        return "http://" + elasticsearch.getHost() + ":" + elasticsearch.getFirstMappedPort();
    }
}
