package com.portal.universe.shoppingservice.common.config;

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
 * Shopping 서비스에서 발생하는 이벤트를 Kafka로 발행합니다.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ========================================
    // Topic Names
    // ========================================
    public static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    public static final String TOPIC_ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String TOPIC_ORDER_CANCELLED = "shopping.order.cancelled";
    public static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "shopping.payment.failed";
    public static final String TOPIC_INVENTORY_RESERVED = "shopping.inventory.reserved";
    public static final String TOPIC_DELIVERY_SHIPPED = "shopping.delivery.shipped";

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
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CONFIRMED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CANCELLED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_RESERVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deliveryShippedTopic() {
        return TopicBuilder.name(TOPIC_DELIVERY_SHIPPED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
