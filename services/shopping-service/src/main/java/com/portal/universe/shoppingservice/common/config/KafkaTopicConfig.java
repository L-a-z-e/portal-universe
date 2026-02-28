package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.event.shopping.ShoppingTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${app.kafka.topic.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic orderCreatedTopic() {
        return buildTopic(ShoppingTopics.ORDER_CREATED);
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return buildTopic(ShoppingTopics.ORDER_CONFIRMED);
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return buildTopic(ShoppingTopics.ORDER_CANCELLED);
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return buildTopic(ShoppingTopics.PAYMENT_COMPLETED);
    }

    @Bean
    public NewTopic paymentCancelledTopic() {
        return buildTopic(ShoppingTopics.PAYMENT_CANCELLED);
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return buildTopic(ShoppingTopics.PAYMENT_FAILED);
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return buildTopic(ShoppingTopics.INVENTORY_RESERVED);
    }

    @Bean
    public NewTopic deliveryShippedTopic() {
        return buildTopic(ShoppingTopics.DELIVERY_SHIPPED);
    }

    @Bean
    public NewTopic couponIssuedTopic() {
        return buildTopic(ShoppingTopics.COUPON_ISSUED);
    }

    @Bean
    public NewTopic timeDealStartedTopic() {
        return buildTopic(ShoppingTopics.TIMEDEAL_STARTED);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
