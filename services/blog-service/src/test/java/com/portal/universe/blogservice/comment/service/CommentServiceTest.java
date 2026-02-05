package com.portal.universe.blogservice.comment.service;

import com.mongodb.client.result.UpdateResult;
import com.portal.universe.blogservice.comment.dto.CommentCreateRequest;
import com.portal.universe.blogservice.comment.dto.CommentUpdateRequest;
import com.portal.universe.blogservice.comment.dto.CommentResponse;
import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import com.portal.universe.blogservice.event.BlogEventPublisher;
import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private BlogEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createComment 메서드")
    class CreateCommentTests {

        @Test
        @DisplayName("should_createRootComment")
        void should_createRootComment() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("post-1", null, "Test comment");
            Post post = createTestPost("post-1", "user2");

            Comment savedComment = Comment.builder()
                    .postId(request.postId())
                    .content(request.content())
                    .authorId("user1")
                    .authorName("User One")
                    .build();
            ReflectionTestUtils.setField(savedComment, "id", "comment-1");

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            CommentResponse result = commentService.createComment(request, "user1", "User One");

            // then
            assertThat(result.content()).isEqualTo("Test comment");
            assertThat(result.postId()).isEqualTo("post-1");
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("should_incrementCommentCount_atomically")
        void should_incrementCommentCount_atomically() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("post-1", null, "Comment");
            Post post = createTestPost("post-1", "user2");

            Comment savedComment = Comment.builder()
                    .postId(request.postId())
                    .content(request.content())
                    .authorId("user1")
                    .authorName("User One")
                    .build();

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            commentService.createComment(request, "user1", "User One");

            // then
            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Post.class));
        }

        @Test
        @DisplayName("should_publishEvent_when_notOwnPost")
        void should_publishEvent_when_notOwnPost() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("post-1", null, "Comment");
            Post post = createTestPost("post-1", "user2");

            Comment savedComment = Comment.builder()
                    .postId(request.postId())
                    .content(request.content())
                    .authorId("user1")
                    .authorName("User One")
                    .build();
            ReflectionTestUtils.setField(savedComment, "id", "comment-1");

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            commentService.createComment(request, "user1", "User One");

            // then
            verify(eventPublisher).publishCommentCreated(any());
        }

        @Test
        @DisplayName("should_notPublishEvent_when_ownPost")
        void should_notPublishEvent_when_ownPost() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("post-1", null, "Comment");
            Post post = createTestPost("post-1", "user1");

            Comment savedComment = Comment.builder()
                    .postId(request.postId())
                    .content(request.content())
                    .authorId("user1")
                    .authorName("User One")
                    .build();

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            commentService.createComment(request, "user1", "User One");

            // then
            verify(eventPublisher, never()).publishCommentCreated(any());
        }

        @Test
        @DisplayName("should_throwException_when_postNotFound")
        void should_throwException_when_postNotFound() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("post-1", null, "Comment");
            when(postRepository.findById("post-1")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(request, "user1", "User One"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateComment 메서드")
    class UpdateCommentTests {

        @Test
        @DisplayName("should_updateContent_when_author")
        void should_updateContent_when_author() {
            // given
            Comment comment = createTestComment("comment-1", "post-1", "user1");
            CommentUpdateRequest request = new CommentUpdateRequest("Updated content");

            when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
            when(commentRepository.save(any(Comment.class))).thenReturn(comment);

            // when
            CommentResponse result = commentService.updateComment("comment-1", request, "user1");

            // then
            assertThat(result.content()).isEqualTo("Updated content");
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Comment comment = createTestComment("comment-1", "post-1", "user1");
            CommentUpdateRequest request = new CommentUpdateRequest("Updated content");

            when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment("comment-1", request, "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.COMMENT_UPDATE_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deleteComment 메서드")
    class DeleteCommentTests {

        @Test
        @DisplayName("should_softDelete_when_author")
        void should_softDelete_when_author() {
            // given
            Comment comment = createTestComment("comment-1", "post-1", "user1");

            when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
            when(commentRepository.save(any(Comment.class))).thenReturn(comment);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            commentService.deleteComment("comment-1", "user1");

            // then
            verify(commentRepository).save(any(Comment.class));
            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Post.class));
        }

        @Test
        @DisplayName("should_decrementCommentCount")
        void should_decrementCommentCount() {
            // given
            Comment comment = createTestComment("comment-1", "post-1", "user1");

            when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
            when(commentRepository.save(any(Comment.class))).thenReturn(comment);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            commentService.deleteComment("comment-1", "user1");

            // then
            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Post.class));
        }
    }

    @Nested
    @DisplayName("getCommentsByPostId 메서드")
    class GetCommentsByPostIdTests {

        @Test
        @DisplayName("should_returnComments")
        void should_returnComments() {
            // given
            List<Comment> comments = List.of(
                    createTestComment("comment-1", "post-1", "user1"),
                    createTestComment("comment-2", "post-1", "user2")
            );

            when(commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc("post-1"))
                    .thenReturn(comments);

            // when
            List<CommentResponse> result = commentService.getCommentsByPostId("post-1");

            // then
            assertThat(result).hasSize(2);
            verify(commentRepository).findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc("post-1");
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

    private Comment createTestComment(String id, String postId, String authorId) {
        Comment comment = Comment.builder()
                .postId(postId)
                .content("Test comment")
                .authorId(authorId)
                .authorName("Test Author")
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
