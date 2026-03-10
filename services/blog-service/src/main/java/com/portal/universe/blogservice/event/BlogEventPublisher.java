package com.portal.universe.blogservice.event;

import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 블로그 이벤트를 Spring ApplicationEvent로 발행하여 Kafka 전송을 트리거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishPostLiked(PostLikedEvent event) {
        publishEvent(BlogTopics.POST_LIKED, event.getPostId().toString(), event);
    }

    public void publishCommentCreated(CommentCreatedEvent event) {
        publishEvent(BlogTopics.POST_COMMENTED, event.getPostId().toString(), event);
    }

    public void publishCommentReplied(CommentRepliedEvent event) {
        publishEvent(BlogTopics.COMMENT_REPLIED, event.getPostId().toString(), event);
    }

    private void publishEvent(String topic, String key, SpecificRecord event) {
        applicationEventPublisher.publishEvent(new KafkaPublishEvent(topic, key, event));
    }
}