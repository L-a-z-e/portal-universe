package com.portal.universe.blogservice.event;

import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.blog.UserFollowedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 블로그 서비스의 이벤트를 Kafka로 발행하는 퍼블리셔입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPostLiked(PostLikedEvent event) {
        publishEvent(BlogTopics.POST_LIKED, event.postId(), event);
    }

    public void publishCommentCreated(CommentCreatedEvent event) {
        publishEvent(BlogTopics.POST_COMMENTED, event.postId(), event);
    }

    public void publishCommentReplied(CommentRepliedEvent event) {
        publishEvent(BlogTopics.COMMENT_REPLIED, event.postId(), event);
    }

    public void publishUserFollowed(UserFollowedEvent event) {
        publishEvent(BlogTopics.USER_FOLLOWED, event.followeeId(), event);
    }

    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Blog event published successfully: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish blog event: topic={}, key={}, error={}",
                        topic, key, ex.getMessage());
            }
        });
    }
}
