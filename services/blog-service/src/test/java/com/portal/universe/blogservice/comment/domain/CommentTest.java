package com.portal.universe.blogservice.comment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comment 도메인 테스트")
class CommentTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("should_createComment_with_defaultValues")
        void should_createComment_with_defaultValues() {
            // given & when
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Test Comment")
                    .build();

            // then
            assertThat(comment.getIsDeleted()).isFalse();
            assertThat(comment.getLikeCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("update() 테스트")
    class UpdateTest {

        @Test
        @DisplayName("should_updateContent")
        void should_updateContent() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Old Content")
                    .build();

            // when
            comment.update("New Content");

            // then
            assertThat(comment.getContent()).isEqualTo("New Content");
            assertThat(comment.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete() 테스트")
    class DeleteTest {

        @Test
        @DisplayName("should_softDelete")
        void should_softDelete() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Test Content")
                    .build();

            // when
            comment.delete();

            // then
            assertThat(comment.getIsDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("isRootComment() 테스트")
    class IsRootCommentTest {

        @Test
        @DisplayName("should_returnTrue_when_noParent")
        void should_returnTrue_when_noParent() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Root Comment")
                    .parentCommentId(null)
                    .build();

            // when & then
            assertThat(comment.isRootComment()).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_hasParent")
        void should_returnFalse_when_hasParent() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Reply Comment")
                    .parentCommentId("parent1")
                    .build();

            // when & then
            assertThat(comment.isRootComment()).isFalse();
        }
    }

    @Nested
    @DisplayName("incrementLikeCount() 테스트")
    class IncrementLikeCountTest {

        @Test
        @DisplayName("should_incrementLikeCount")
        void should_incrementLikeCount() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Test Comment")
                    .build();

            // when
            comment.incrementLikeCount();

            // then
            assertThat(comment.getLikeCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("decrementLikeCount() 테스트")
    class DecrementLikeCountTest {

        @Test
        @DisplayName("should_decrementLikeCount")
        void should_decrementLikeCount() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Test Comment")
                    .build();
            comment.incrementLikeCount();

            // when
            comment.decrementLikeCount();

            // then
            assertThat(comment.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should_notGoBelowZero")
        void should_notGoBelowZero() {
            // given
            Comment comment = Comment.builder()
                    .postId("post1")
                    .authorId("author1")
                    .content("Test Comment")
                    .build();

            // when
            comment.decrementLikeCount();

            // then
            assertThat(comment.getLikeCount()).isEqualTo(0L);
        }
    }
}
