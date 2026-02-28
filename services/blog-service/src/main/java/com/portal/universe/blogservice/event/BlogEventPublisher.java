package com.portal.universe.blogservice.event;

import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.blog.UserFollowedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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

    public void publishUserFollowed(UserFollowedEvent event) {
        publishEvent(BlogTopics.USER_FOLLOWED, event.getFolloweeId().toString(), event);
    }

    private void publishEvent(String topic, String key, SpecificRecord event) {
        applicationEventPublisher.publishEvent(new KafkaPublishEvent(topic, key, event));
    }
}