package com.portal.universe.authservice.common.config;

import com.portal.universe.event.auth.AuthTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userSignedUpTopic() {
        return TopicBuilder.name(AuthTopics.USER_SIGNED_UP)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic roleAssignedTopic() {
        return TopicBuilder.name(AuthTopics.ROLE_ASSIGNED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
