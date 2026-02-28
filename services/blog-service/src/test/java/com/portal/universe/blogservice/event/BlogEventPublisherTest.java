package com.portal.universe.blogservice.event;

import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlogEventPublisher 테스트")
class BlogEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private BlogEventPublisher eventPublisher;

    @Nested
    @DisplayName("publishPostLiked 메서드")
    class PublishPostLikedTests {

        @Test
        @DisplayName("should_publishKafkaPublishEvent_when_called")
        void should_publishKafkaPublishEvent_when_called() {
            // given
            PostLikedEvent event = PostLikedEvent.newBuilder()
                    .setLikeId("like-1").setPostId("post-1").setPostTitle("Post Title")
                    .setAuthorId("user1").setLikerId("user2").setLikerName("User Two")
                    .setTimestamp(Instant.now())
                    .build();

            // when
            eventPublisher.publishPostLiked(event);

            // then
            ArgumentCaptor<KafkaPublishEvent> captor = ArgumentCaptor.forClass(KafkaPublishEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().topic()).isEqualTo(BlogTopics.POST_LIKED);
            assertThat(captor.getValue().key()).isEqualTo("post-1");
        }
    }

    @Nested
    @DisplayName("publishCommentCreated 메서드")
    class PublishCommentCreatedTests {

        @Test
        @DisplayName("should_publishKafkaPublishEvent_when_called")
        void should_publishKafkaPublishEvent_when_called() {
            // given
            CommentCreatedEvent event = CommentCreatedEvent.newBuilder()
                    .setCommentId("comment-1").setPostId("post-1").setPostTitle("Post Title")
                    .setAuthorId("user1").setCommenterId("user2").setCommenterName("User Two")
                    .setContent("Comment content").setTimestamp(Instant.now())
                    .build();

            // when
            eventPublisher.publishCommentCreated(event);

            // then
            ArgumentCaptor<KafkaPublishEvent> captor = ArgumentCaptor.forClass(KafkaPublishEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().topic()).isEqualTo(BlogTopics.POST_COMMENTED);
            assertThat(captor.getValue().key()).isEqualTo("post-1");
        }
    }

    @Nested
    @DisplayName("publishCommentReplied 메서드")
    class PublishCommentRepliedTests {

        @Test
        @DisplayName("should_publishKafkaPublishEvent_when_called")
        void should_publishKafkaPublishEvent_when_called() {
            // given
            CommentRepliedEvent event = CommentRepliedEvent.newBuilder()
                    .setReplyId("comment-2").setPostId("post-1").setParentCommentId("parent-cmt-1")
                    .setParentCommentAuthorId("user2").setReplierId("user3").setReplierName("User Three")
                    .setContent("Reply content").setTimestamp(Instant.now())
                    .build();

            // when
            eventPublisher.publishCommentReplied(event);

            // then
            ArgumentCaptor<KafkaPublishEvent> captor = ArgumentCaptor.forClass(KafkaPublishEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().topic()).isEqualTo(BlogTopics.COMMENT_REPLIED);
            assertThat(captor.getValue().key()).isEqualTo("post-1");
        }
    }
}