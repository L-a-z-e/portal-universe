package com.portal.universe.blogservice.event;

import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("BlogEventPublisher 테스트")
class BlogEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BlogEventPublisher eventPublisher;

    @Nested
    @DisplayName("publishPostLiked 메서드")
    class PublishPostLikedTests {

        @Test
        @DisplayName("should_sendToCorrectTopic")
        void should_sendToCorrectTopic() {
            // given
            PostLikedEvent event = new PostLikedEvent(
                    "like-1",
                    "post-1",
                    "Post Title",
                    "user1",
                    "user2",
                    "User Two",
                    LocalDateTime.now()
            );

            @SuppressWarnings("unchecked")
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(
                    mock(SendResult.class)
            );
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PostLikedEvent> eventCaptor = ArgumentCaptor.forClass(PostLikedEvent.class);

            // when
            eventPublisher.publishPostLiked(event);

            // then
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo(BlogTopics.POST_LIKED);
            assertThat(keyCaptor.getValue()).isEqualTo("post-1");
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("publishCommentCreated 메서드")
    class PublishCommentCreatedTests {

        @Test
        @DisplayName("should_sendToCorrectTopic")
        void should_sendToCorrectTopic() {
            // given
            CommentCreatedEvent event = new CommentCreatedEvent(
                    "comment-1",
                    "post-1",
                    "Post Title",
                    "user1",
                    "user2",
                    "User Two",
                    "Comment content",
                    LocalDateTime.now()
            );

            @SuppressWarnings("unchecked")
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(
                    mock(SendResult.class)
            );
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);

            // when
            eventPublisher.publishCommentCreated(event);

            // then
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo(BlogTopics.POST_COMMENTED);
            assertThat(keyCaptor.getValue()).isEqualTo("post-1");
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("publishCommentReplied 메서드")
    class PublishCommentRepliedTests {

        @Test
        @DisplayName("should_sendToCorrectTopic")
        void should_sendToCorrectTopic() {
            // given
            CommentRepliedEvent event = new CommentRepliedEvent(
                    "comment-2",
                    "post-1",
                    "Post Title",
                    "user2",
                    "user3",
                    "User Three",
                    "Reply content",
                    LocalDateTime.now()
            );

            @SuppressWarnings("unchecked")
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(
                    mock(SendResult.class)
            );
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<CommentRepliedEvent> eventCaptor = ArgumentCaptor.forClass(CommentRepliedEvent.class);

            // when
            eventPublisher.publishCommentReplied(event);

            // then
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo(BlogTopics.COMMENT_REPLIED);
            assertThat(keyCaptor.getValue()).isEqualTo("post-1");
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }
    }
}
