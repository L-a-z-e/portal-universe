package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.event.shopping.ShoppingTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ShoppingTopics.ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(ShoppingTopics.ORDER_CONFIRMED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(ShoppingTopics.ORDER_CANCELLED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(ShoppingTopics.PAYMENT_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(ShoppingTopics.PAYMENT_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(ShoppingTopics.INVENTORY_RESERVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deliveryShippedTopic() {
        return TopicBuilder.name(ShoppingTopics.DELIVERY_SHIPPED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic couponIssuedTopic() {
        return TopicBuilder.name(ShoppingTopics.COUPON_ISSUED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic timeDealStartedTopic() {
        return TopicBuilder.name(ShoppingTopics.TIMEDEAL_STARTED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
