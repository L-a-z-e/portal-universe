package com.portal.universe.blogservice.post.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Post 도메인 테스트")
class PostTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("should_createPost_with_defaultValues")
        void should_createPost_with_defaultValues() {
            // given & when
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // then
            assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
            assertThat(post.getTags()).isEmpty();
            assertThat(post.getImages()).isEmpty();
            assertThat(post.getViewCount()).isEqualTo(0L);
            assertThat(post.getLikeCount()).isEqualTo(0L);
            assertThat(post.getCommentCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should_generateSummary_when_summaryIsNull")
        void should_generateSummary_when_summaryIsNull() {
            // given
            String content = "A".repeat(250);

            // when
            Post post = Post.builder()
                    .title("Test Title")
                    .content(content)
                    .authorId("author1")
                    .summary(null)
                    .build();

            // then
            assertThat(post.getSummary()).hasSize(203); // 200 + "..."
            assertThat(post.getSummary()).endsWith("...");
        }

        @Test
        @DisplayName("should_generateMetaDescription_when_null")
        void should_generateMetaDescription_when_null() {
            // given
            String content = "B".repeat(200);

            // when
            Post post = Post.builder()
                    .title("Test Title")
                    .content(content)
                    .authorId("author1")
                    .metaDescription(null)
                    .build();

            // then
            assertThat(post.getMetaDescription()).hasSize(163); // 160 + "..."
            assertThat(post.getMetaDescription()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("publish() 테스트")
    class PublishTest {

        @Test
        @DisplayName("should_setPublishedStatus_when_draft")
        void should_setPublishedStatus_when_draft() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            post.publish();

            // then
            assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(post.getPublishedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("unpublish() 테스트")
    class UnpublishTest {

        @Test
        @DisplayName("should_setDraftStatus_when_published")
        void should_setDraftStatus_when_published() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();
            post.publish();

            // when
            post.unpublish();

            // then
            assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
            assertThat(post.getPublishedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("isPublished() 테스트")
    class IsPublishedTest {

        @Test
        @DisplayName("should_returnTrue_when_statusIsPublished")
        void should_returnTrue_when_statusIsPublished() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();
            post.publish();

            // when & then
            assertThat(post.isPublished()).isTrue();
        }
    }

    @Nested
    @DisplayName("isViewableBy() 테스트")
    class IsViewableByTest {

        @Test
        @DisplayName("should_returnTrue_when_published")
        void should_returnTrue_when_published() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();
            post.publish();

            // when & then
            assertThat(post.isViewableBy("otherUser")).isTrue();
        }

        @Test
        @DisplayName("should_returnTrue_when_authorViewsDraft")
        void should_returnTrue_when_authorViewsDraft() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when & then
            assertThat(post.isViewableBy("author1")).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_otherViewsDraft")
        void should_returnFalse_when_otherViewsDraft() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when & then
            assertThat(post.isViewableBy("otherUser")).isFalse();
        }
    }

    @Nested
    @DisplayName("incrementViewCount() 테스트")
    class IncrementViewCountTest {

        @Test
        @DisplayName("should_incrementViewCount")
        void should_incrementViewCount() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            post.incrementViewCount();

            // then
            assertThat(post.getViewCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("incrementLikeCount() 테스트")
    class IncrementLikeCountTest {

        @Test
        @DisplayName("should_incrementLikeCount")
        void should_incrementLikeCount() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            post.incrementLikeCount();

            // then
            assertThat(post.getLikeCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("decrementLikeCount() 테스트")
    class DecrementLikeCountTest {

        @Test
        @DisplayName("should_decrementLikeCount")
        void should_decrementLikeCount() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();
            post.incrementLikeCount();

            // when
            post.decrementLikeCount();

            // then
            assertThat(post.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should_notGoBelowZero_when_likeCountIsZero")
        void should_notGoBelowZero_when_likeCountIsZero() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            post.decrementLikeCount();

            // then
            assertThat(post.getLikeCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("decrementCommentCount() 테스트")
    class DecrementCommentCountTest {

        @Test
        @DisplayName("should_notGoBelowZero_when_commentCountIsZero")
        void should_notGoBelowZero_when_commentCountIsZero() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            post.decrementCommentCount();

            // then
            assertThat(post.getCommentCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("update() 테스트")
    class UpdateTest {

        @Test
        @DisplayName("should_updateAllFields")
        void should_updateAllFields() {
            // given
            Post post = Post.builder()
                    .title("Old Title")
                    .content("Old Content")
                    .authorId("author1")
                    .build();

            Set<String> newTags = Set.of("tag1", "tag2");
            List<String> newImages = List.of("image1.jpg", "image2.jpg");

            // when
            post.update(
                    "New Title",
                    "New Content",
                    "New Summary",
                    newTags,
                    "New Category",
                    "New Meta Description",
                    "thumbnail.jpg",
                    newImages
            );

            // then
            assertThat(post.getTitle()).isEqualTo("New Title");
            assertThat(post.getContent()).isEqualTo("New Content");
            assertThat(post.getSummary()).isEqualTo("New Summary");
            assertThat(post.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
            assertThat(post.getCategory()).isEqualTo("New Category");
            assertThat(post.getMetaDescription()).isEqualTo("New Meta Description");
            assertThat(post.getThumbnailUrl()).isEqualTo("thumbnail.jpg");
            assertThat(post.getImages()).containsExactly("image1.jpg", "image2.jpg");
        }
    }

    @Nested
    @DisplayName("addImage() 테스트")
    class AddImageTest {

        @Test
        @DisplayName("should_addImage_when_validUrl")
        void should_addImage_when_validUrl() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            post.addImage("newImage.jpg");

            // then
            assertThat(post.getImages()).hasSize(1);
            assertThat(post.getImages()).contains("newImage.jpg");
        }
    }

    @Nested
    @DisplayName("setDefaultThumbnailIfNeeded() 테스트")
    class SetDefaultThumbnailIfNeededTest {

        @Test
        @DisplayName("should_setFirstImage_when_thumbnailIsNull")
        void should_setFirstImage_when_thumbnailIsNull() {
            // given
            List<String> images = new ArrayList<>();
            images.add("firstImage.jpg");
            images.add("secondImage.jpg");

            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .images(images)
                    .thumbnailUrl(null)
                    .build();

            // when
            post.setDefaultThumbnailIfNeeded();

            // then
            assertThat(post.getThumbnailUrl()).isEqualTo("firstImage.jpg");
        }
    }
}
