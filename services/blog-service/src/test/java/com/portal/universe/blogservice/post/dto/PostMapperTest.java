package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostMapper 테스트")
class PostMapperTest {

    @Nested
    @DisplayName("toSummary() 테스트")
    class ToSummaryTest {

        @Test
        @DisplayName("should_mapAllFields")
        void should_mapAllFields() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content " + "A".repeat(200))
                    .summary("Test Summary")
                    .authorId("author1")
                    .authorUsername("author1_handle")
                    .authorNickname("Author Name")
                    .status(PostStatus.PUBLISHED)
                    .tags(Set.of("tag1", "tag2"))
                    .category("Tech")
                    .metaDescription("Test Meta")
                    .thumbnailUrl("thumb.jpg")
                    .images(List.of("image1.jpg", "image2.jpg"))
                    .build();
            post.publish();

            // when
            PostSummaryResponse response = PostMapper.toSummary(post);

            // then
            assertThat(response.id()).isNull(); // ID는 MongoDB에 저장될 때 생성됨
            assertThat(response.title()).isEqualTo("Test Title");
            assertThat(response.summary()).isEqualTo("Test Summary");
            assertThat(response.authorId()).isEqualTo("author1");
            assertThat(response.authorUsername()).isEqualTo("author1_handle");
            assertThat(response.authorNickname()).isEqualTo("Author Name");
            assertThat(response.tags()).containsExactlyInAnyOrder("tag1", "tag2");
            assertThat(response.category()).isEqualTo("Tech");
            assertThat(response.thumbnailUrl()).isEqualTo("thumb.jpg");
            assertThat(response.images()).containsExactly("image1.jpg", "image2.jpg");
            assertThat(response.viewCount()).isEqualTo(0L);
            assertThat(response.likeCount()).isEqualTo(0L);
            assertThat(response.commentCount()).isEqualTo(0L);
            assertThat(response.publishedAt()).isNotNull();
            assertThat(response.estimatedReadTime()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should_handleNullCommentCount")
        void should_handleNullCommentCount() {
            // given
            Post post = Post.builder()
                    .title("Test Title")
                    .content("Test Content")
                    .authorId("author1")
                    .build();

            // when
            PostSummaryResponse response = PostMapper.toSummary(post);

            // then
            assertThat(response.commentCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("calculateReadTime() 테스트")
    class CalculateReadTimeTest {

        @Test
        @DisplayName("should_returnOneMinute_when_shortContent")
        void should_returnOneMinute_when_shortContent() {
            // given
            String content = "A".repeat(200);

            // when
            int readTime = PostMapper.calculateReadTime(content);

            // then
            assertThat(readTime).isEqualTo(1);
        }

        @Test
        @DisplayName("should_calculateCorrectly_when_longContent")
        void should_calculateCorrectly_when_longContent() {
            // given
            String content = "A".repeat(401);

            // when
            int readTime = PostMapper.calculateReadTime(content);

            // then
            assertThat(readTime).isEqualTo(3); // ceil(401/200) = 3
        }

        @Test
        @DisplayName("should_returnOne_when_contentIsNull")
        void should_returnOne_when_contentIsNull() {
            // given
            String content = null;

            // when
            int readTime = PostMapper.calculateReadTime(content);

            // then
            assertThat(readTime).isEqualTo(1);
        }
    }
}
