package com.portal.universe.blogservice.event;

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

    public static final String TOPIC_POST_LIKED = "blog.post.liked";
    public static final String TOPIC_POST_COMMENTED = "blog.post.commented";
    public static final String TOPIC_COMMENT_REPLIED = "blog.comment.replied";
    public static final String TOPIC_USER_FOLLOWED = "blog.user.followed";

    /**
     * 포스트 좋아요 이벤트를 발행합니다.
     */
    public void publishPostLiked(PostLikedEvent event) {
        publishEvent(TOPIC_POST_LIKED, event.postId(), event);
    }

    /**
     * 댓글 생성 이벤트를 발행합니다.
     */
    public void publishCommentCreated(CommentCreatedEvent event) {
        publishEvent(TOPIC_POST_COMMENTED, event.postId(), event);
    }

    /**
     * 댓글 답글 이벤트를 발행합니다.
     */
    public void publishCommentReplied(CommentRepliedEvent event) {
        publishEvent(TOPIC_COMMENT_REPLIED, event.postId(), event);
    }

    /**
     * 사용자 팔로우 이벤트를 발행합니다.
     */
    public void publishUserFollowed(UserFollowedEvent event) {
        publishEvent(TOPIC_USER_FOLLOWED, event.followeeId(), event);
    }

    /**
     * 이벤트를 Kafka로 발행합니다.
     *
     * @param topic 토픽명
     * @param key 메시지 키
     * @param event 이벤트 객체
     */
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
