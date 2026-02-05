package com.portal.universe.blogservice.series.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Series 도메인 테스트")
class SeriesTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("should_createSeries_with_emptyPostIds")
        void should_createSeries_with_emptyPostIds() {
            // given & when
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();

            // then
            assertThat(series.getPostIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addPost() 테스트")
    class AddPostTest {

        @Test
        @DisplayName("should_addPost_to_series")
        void should_addPost_to_series() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();

            // when
            series.addPost("post1");

            // then
            assertThat(series.getPostIds()).containsExactly("post1");
        }

        @Test
        @DisplayName("should_notAddDuplicate")
        void should_notAddDuplicate() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");

            // when
            series.addPost("post1");

            // then
            assertThat(series.getPostIds()).hasSize(1);
            assertThat(series.getPostIds()).containsExactly("post1");
        }
    }

    @Nested
    @DisplayName("addPostAt() 테스트")
    class AddPostAtTest {

        @Test
        @DisplayName("should_addPostAtIndex")
        void should_addPostAtIndex() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");
            series.addPost("post3");

            // when
            series.addPostAt("post2", 1);

            // then
            assertThat(series.getPostIds()).containsExactly("post1", "post2", "post3");
        }

        @Test
        @DisplayName("should_throwException_when_invalidIndex")
        void should_throwException_when_invalidIndex() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();

            // when & then
            assertThatThrownBy(() -> series.addPostAt("post1", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid index");

            assertThatThrownBy(() -> series.addPostAt("post1", 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid index");
        }
    }

    @Nested
    @DisplayName("removePost() 테스트")
    class RemovePostTest {

        @Test
        @DisplayName("should_removePost_from_series")
        void should_removePost_from_series() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");
            series.addPost("post2");

            // when
            series.removePost("post1");

            // then
            assertThat(series.getPostIds()).containsExactly("post2");
        }
    }

    @Nested
    @DisplayName("reorderPosts() 테스트")
    class ReorderPostsTest {

        @Test
        @DisplayName("should_reorderPosts")
        void should_reorderPosts() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");
            series.addPost("post2");
            series.addPost("post3");

            // when
            series.reorderPosts(List.of("post3", "post1", "post2"));

            // then
            assertThat(series.getPostIds()).containsExactly("post3", "post1", "post2");
        }

        @Test
        @DisplayName("should_throwException_when_postsMismatch")
        void should_throwException_when_postsMismatch() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");
            series.addPost("post2");

            // when & then
            assertThatThrownBy(() -> series.reorderPosts(List.of("post1", "post3")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Post IDs mismatch");
        }
    }

    @Nested
    @DisplayName("containsPost() 테스트")
    class ContainsPostTest {

        @Test
        @DisplayName("should_returnTrue_when_postExists")
        void should_returnTrue_when_postExists() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");

            // when & then
            assertThat(series.containsPost("post1")).isTrue();
        }
    }

    @Nested
    @DisplayName("getPostOrder() 테스트")
    class GetPostOrderTest {

        @Test
        @DisplayName("should_returnIndex_when_postExists")
        void should_returnIndex_when_postExists() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();
            series.addPost("post1");
            series.addPost("post2");

            // when & then
            assertThat(series.getPostOrder("post1")).isEqualTo(0);
            assertThat(series.getPostOrder("post2")).isEqualTo(1);
        }

        @Test
        @DisplayName("should_returnMinusOne_when_notFound")
        void should_returnMinusOne_when_notFound() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();

            // when & then
            assertThat(series.getPostOrder("nonexistent")).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("isEmpty() 테스트")
    class IsEmptyTest {

        @Test
        @DisplayName("should_returnTrue_when_noPostIds")
        void should_returnTrue_when_noPostIds() {
            // given
            Series series = Series.builder()
                    .name("Test Series")
                    .authorId("author1")
                    .build();

            // when & then
            assertThat(series.isEmpty()).isTrue();
        }
    }
}
