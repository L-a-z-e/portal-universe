package com.portal.universe.blogservice.like.service;

import com.mongodb.client.result.UpdateResult;
import com.portal.universe.blogservice.event.BlogEventPublisher;
import com.portal.universe.blogservice.like.dto.LikeStatusResponse;
import com.portal.universe.blogservice.like.dto.LikeToggleResponse;
import com.portal.universe.blogservice.like.dto.LikerResponse;
import com.portal.universe.blogservice.like.domain.Like;
import com.portal.universe.blogservice.like.repository.LikeRepository;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.event.blog.PostLikedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService 테스트")
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private BlogEventPublisher eventPublisher;

    @InjectMocks
    private LikeService likeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("toggleLike 메서드")
    class ToggleLikeTests {

        @Test
        @DisplayName("should_addLike_when_notExists")
        void should_addLike_when_notExists() {
            // given
            Post post = createTestPost("post-1", "user2");
            Like newLike = Like.builder()
                    .postId("post-1")
                    .userId("user1")
                    .userName("User One")
                    .build();
            ReflectionTestUtils.setField(newLike, "id", "like-1");

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.findByPostIdAndUserId("post-1", "user1")).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenReturn(newLike);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            LikeToggleResponse result = likeService.toggleLike("post-1", "user1", "User One");

            // then
            assertThat(result.liked()).isTrue();
            verify(likeRepository).save(any(Like.class));
            verify(likeRepository, never()).delete(any(Like.class));
        }

        @Test
        @DisplayName("should_removeLike_when_exists")
        void should_removeLike_when_exists() {
            // given
            Post post = createTestPost("post-1", "user2");
            Like existingLike = Like.builder()
                    .postId("post-1")
                    .userId("user1")
                    .userName("User One")
                    .build();

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.findByPostIdAndUserId("post-1", "user1")).thenReturn(Optional.of(existingLike));
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            LikeToggleResponse result = likeService.toggleLike("post-1", "user1", "User One");

            // then
            assertThat(result.liked()).isFalse();
            verify(likeRepository).delete(existingLike);
            verify(likeRepository, never()).save(any(Like.class));
        }

        @Test
        @DisplayName("should_incrementLikeCount_when_added")
        void should_incrementLikeCount_when_added() {
            // given
            Post post = createTestPost("post-1", "user2");
            Like newLike = Like.builder()
                    .postId("post-1")
                    .userId("user1")
                    .userName("User One")
                    .build();

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.findByPostIdAndUserId("post-1", "user1")).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenReturn(newLike);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            likeService.toggleLike("post-1", "user1", "User One");

            // then
            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Post.class));
        }

        @Test
        @DisplayName("should_decrementLikeCount_when_removed")
        void should_decrementLikeCount_when_removed() {
            // given
            Post post = createTestPost("post-1", "user2");
            Like existingLike = Like.builder()
                    .postId("post-1")
                    .userId("user1")
                    .userName("User One")
                    .build();

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.findByPostIdAndUserId("post-1", "user1")).thenReturn(Optional.of(existingLike));
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            likeService.toggleLike("post-1", "user1", "User One");

            // then
            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Post.class));
        }

        @Test
        @DisplayName("should_publishEvent_when_likedOtherPost")
        void should_publishEvent_when_likedOtherPost() {
            // given
            Post post = createTestPost("post-1", "user2");
            Like newLike = Like.builder()
                    .postId("post-1")
                    .userId("user1")
                    .userName("User One")
                    .build();
            ReflectionTestUtils.setField(newLike, "id", "like-1");

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.findByPostIdAndUserId("post-1", "user1")).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenReturn(newLike);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            likeService.toggleLike("post-1", "user1", "User One");

            // then
            verify(eventPublisher).publishPostLiked(any(PostLikedEvent.class));
        }

        @Test
        @DisplayName("should_notPublishEvent_when_likedOwnPost")
        void should_notPublishEvent_when_likedOwnPost() {
            // given
            Post post = createTestPost("post-1", "user1");
            Like newLike = Like.builder()
                    .postId("post-1")
                    .userId("user1")
                    .userName("User One")
                    .build();

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.findByPostIdAndUserId("post-1", "user1")).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenReturn(newLike);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            likeService.toggleLike("post-1", "user1", "User One");

            // then
            verify(eventPublisher, never()).publishPostLiked(any(PostLikedEvent.class));
        }
    }

    @Nested
    @DisplayName("getLikeStatus 메서드")
    class GetLikeStatusTests {

        @Test
        @DisplayName("should_returnStatus_with_count")
        void should_returnStatus_with_count() {
            // given
            Post post = createTestPost("post-1", "user2");
            ReflectionTestUtils.setField(post, "likeCount", 42L);

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(likeRepository.existsByPostIdAndUserId("post-1", "user1")).thenReturn(true);

            // when
            LikeStatusResponse result = likeService.getLikeStatus("post-1", "user1");

            // then
            assertThat(result.liked()).isTrue();
            assertThat(result.likeCount()).isEqualTo(42L);
            verify(likeRepository).existsByPostIdAndUserId("post-1", "user1");
        }
    }

    @Nested
    @DisplayName("getLikers 메서드")
    class GetLikersTests {

        @Test
        @DisplayName("should_returnPagedLikers")
        void should_returnPagedLikers() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Like> likes = List.of(
                    createTestLike("like-1", "post-1", "user1", "User One"),
                    createTestLike("like-2", "post-1", "user2", "User Two")
            );
            Page<Like> likePage = new PageImpl<>(likes, pageable, 2);

            when(postRepository.findById("post-1")).thenReturn(Optional.of(createTestPost("post-1", "author1")));
            when(likeRepository.findByPostId("post-1", pageable)).thenReturn(likePage);

            // when
            Page<LikerResponse> result = likeService.getLikers("post-1", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2L);
            verify(likeRepository).findByPostId("post-1", pageable);
        }
    }

    private Post createTestPost(String id, String authorId) {
        Post post = Post.builder()
                .title("Test Post")
                .content("Content")
                .authorId(authorId)
                .status(PostStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Like createTestLike(String id, String postId, String userId, String userName) {
        Like like = Like.builder()
                .postId(postId)
                .userId(userId)
                .userName(userName)
                .build();
        ReflectionTestUtils.setField(like, "id", id);
        return like;
    }
}
