package com.portal.universe.blogservice.common.config;

import com.portal.universe.event.blog.BlogTopics;
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
    public NewTopic postLikedTopic() {
        return buildTopic(BlogTopics.POST_LIKED);
    }

    @Bean
    public NewTopic postCommentedTopic() {
        return buildTopic(BlogTopics.POST_COMMENTED);
    }

    @Bean
    public NewTopic commentRepliedTopic() {
        return buildTopic(BlogTopics.COMMENT_REPLIED);
    }

    @Bean
    public NewTopic userFollowedTopic() {
        return buildTopic(BlogTopics.USER_FOLLOWED);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
