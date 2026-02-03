package com.portal.universe.blogservice.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 프로듀서 및 토픽 설정을 담당하는 클래스입니다.
 * Blog 서비스에서 발생하는 이벤트를 Kafka로 발행합니다.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ========================================
    // Topic Names
    // ========================================
    public static final String TOPIC_POST_LIKED = "blog.post.liked";
    public static final String TOPIC_POST_COMMENTED = "blog.post.commented";

    // ========================================
    // Producer Configuration
    // ========================================

    /**
     * Kafka 프로듀서 팩토리를 생성합니다.
     * JSON 직렬화를 사용하여 이벤트 객체를 Kafka로 전송합니다.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // 메시지 전송 신뢰성 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka 템플릿을 생성합니다.
     * 이벤트 발행에 사용됩니다.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========================================
    // Topic Definitions
    // ========================================

    @Bean
    public NewTopic postLikedTopic() {
        return TopicBuilder.name(TOPIC_POST_LIKED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic postCommentedTopic() {
        return TopicBuilder.name(TOPIC_POST_COMMENTED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
