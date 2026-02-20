package com.portal.universe.blogservice.event;

import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.CommentCreatedEvent;
import com.portal.universe.event.blog.CommentRepliedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
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

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("BlogEventPublisher 테스트")
class BlogEventPublisherTest {

    @Mock
    private KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @InjectMocks
    private BlogEventPublisher eventPublisher;

    @Nested
    @DisplayName("publishPostLiked 메서드")
    class PublishPostLikedTests {

        @Test
        @DisplayName("should_sendToCorrectTopic")
        void should_sendToCorrectTopic() {
            // given
            PostLikedEvent event = PostLikedEvent.newBuilder()
                    .setLikeId("like-1").setPostId("post-1").setPostTitle("Post Title")
                    .setAuthorId("user1").setLikerId("user2").setLikerName("User Two")
                    .setTimestamp(Instant.now())
                    .build();

            @SuppressWarnings("unchecked")
            CompletableFuture<SendResult<String, SpecificRecord>> future = CompletableFuture.completedFuture(
                    mock(SendResult.class)
            );
            when(avroKafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            eventPublisher.publishPostLiked(event);

            // then
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());
            assertThat(topicCaptor.getValue()).isEqualTo(BlogTopics.POST_LIKED);
            assertThat(keyCaptor.getValue()).isEqualTo("post-1");
        }
    }

    @Nested
    @DisplayName("publishCommentCreated 메서드")
    class PublishCommentCreatedTests {

        @Test
        @DisplayName("should_sendToCorrectTopic")
        void should_sendToCorrectTopic() {
            // given
            CommentCreatedEvent event = CommentCreatedEvent.newBuilder()
                    .setCommentId("comment-1").setPostId("post-1").setPostTitle("Post Title")
                    .setAuthorId("user1").setCommenterId("user2").setCommenterName("User Two")
                    .setContent("Comment content").setTimestamp(Instant.now())
                    .build();

            @SuppressWarnings("unchecked")
            CompletableFuture<SendResult<String, SpecificRecord>> future = CompletableFuture.completedFuture(
                    mock(SendResult.class)
            );
            when(avroKafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            eventPublisher.publishCommentCreated(event);

            // then
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());
            assertThat(topicCaptor.getValue()).isEqualTo(BlogTopics.POST_COMMENTED);
            assertThat(keyCaptor.getValue()).isEqualTo("post-1");
        }
    }

    @Nested
    @DisplayName("publishCommentReplied 메서드")
    class PublishCommentRepliedTests {

        @Test
        @DisplayName("should_sendToCorrectTopic")
        void should_sendToCorrectTopic() {
            // given
            CommentRepliedEvent event = CommentRepliedEvent.newBuilder()
                    .setReplyId("comment-2").setPostId("post-1").setParentCommentId("parent-cmt-1")
                    .setParentCommentAuthorId("user2").setReplierId("user3").setReplierName("User Three")
                    .setContent("Reply content").setTimestamp(Instant.now())
                    .build();

            @SuppressWarnings("unchecked")
            CompletableFuture<SendResult<String, SpecificRecord>> future = CompletableFuture.completedFuture(
                    mock(SendResult.class)
            );
            when(avroKafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            eventPublisher.publishCommentReplied(event);

            // then
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());
            assertThat(topicCaptor.getValue()).isEqualTo(BlogTopics.COMMENT_REPLIED);
            assertThat(keyCaptor.getValue()).isEqualTo("post-1");
        }
    }
}
