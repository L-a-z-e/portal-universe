package com.portal.universe.authservice.common.config;

import com.portal.universe.event.auth.AuthTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Auth 서비스에서 사용하는 Kafka 토픽 자동 생성 설정.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${app.kafka.topic.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic userSignedUpTopic() {
        return buildTopic(AuthTopics.USER_SIGNED_UP);
    }

    @Bean
    public NewTopic roleAssignedTopic() {
        return buildTopic(AuthTopics.ROLE_ASSIGNED);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
