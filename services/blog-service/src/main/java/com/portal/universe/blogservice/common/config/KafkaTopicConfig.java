package com.portal.universe.blogservice.common.config;

import com.portal.universe.event.blog.BlogTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic postLikedTopic() {
        return TopicBuilder.name(BlogTopics.POST_LIKED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic postCommentedTopic() {
        return TopicBuilder.name(BlogTopics.POST_COMMENTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic commentRepliedTopic() {
        return TopicBuilder.name(BlogTopics.COMMENT_REPLIED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userFollowedTopic() {
        return TopicBuilder.name(BlogTopics.USER_FOLLOWED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
