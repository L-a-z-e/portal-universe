package com.portal.universe.blogservice.series.service;

import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.PostSummaryResponse;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.blogservice.series.dto.SeriesCreateRequest;
import com.portal.universe.blogservice.series.dto.SeriesPostOrderRequest;
import com.portal.universe.blogservice.series.dto.SeriesResponse;
import com.portal.universe.blogservice.series.dto.SeriesUpdateRequest;
import com.portal.universe.blogservice.series.domain.Series;
import com.portal.universe.blogservice.series.repository.SeriesRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesService 테스트")
class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private SeriesService seriesService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createSeries 메서드")
    class CreateSeriesTests {

        @Test
        @DisplayName("should_createSeries")
        void should_createSeries() {
            // given
            SeriesCreateRequest request = new SeriesCreateRequest(
                    "Test Series",
                    "Series Description",
                    "thumbnail.jpg"
            );

            Series savedSeries = Series.builder()
                    .name(request.name())
                    .description(request.description())
                    .authorId("user1")
                    .authorName("User One")
                    .thumbnailUrl(request.thumbnailUrl())
                    .build();
            ReflectionTestUtils.setField(savedSeries, "id", "series-1");

            when(seriesRepository.save(any(Series.class))).thenReturn(savedSeries);

            // when
            SeriesResponse result = seriesService.createSeries(request, "user1", "User One");

            // then
            assertThat(result.name()).isEqualTo("Test Series");
            assertThat(result.authorId()).isEqualTo("user1");
            verify(seriesRepository).save(any(Series.class));
        }
    }

    @Nested
    @DisplayName("updateSeries 메서드")
    class UpdateSeriesTests {

        @Test
        @DisplayName("should_updateSeries_when_author")
        void should_updateSeries_when_author() {
            // given
            Series existingSeries = createTestSeries("series-1", "user1");
            SeriesUpdateRequest request = new SeriesUpdateRequest(
                    "Updated Series",
                    "Updated Description",
                    "new-thumbnail.jpg"
            );

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(existingSeries));
            when(seriesRepository.save(any(Series.class))).thenReturn(existingSeries);

            // when
            SeriesResponse result = seriesService.updateSeries("series-1", request, "user1");

            // then
            assertThat(result.name()).isEqualTo("Updated Series");
            verify(seriesRepository).save(any(Series.class));
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Series existingSeries = createTestSeries("series-1", "user1");
            SeriesUpdateRequest request = new SeriesUpdateRequest(
                    "Updated", "Desc", null
            );

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(existingSeries));

            // when & then
            assertThatThrownBy(() -> seriesService.updateSeries("series-1", request, "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.SERIES_UPDATE_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deleteSeries 메서드")
    class DeleteSeriesTests {

        @Test
        @DisplayName("should_deleteSeries_when_author")
        void should_deleteSeries_when_author() {
            // given
            Series series = createTestSeries("series-1", "user1");
            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));

            // when
            seriesService.deleteSeries("series-1", "user1");

            // then
            verify(seriesRepository).delete(series);
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Series series = createTestSeries("series-1", "user1");
            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));

            // when & then
            assertThatThrownBy(() -> seriesService.deleteSeries("series-1", "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.SERIES_DELETE_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("getSeriesById 메서드")
    class GetSeriesByIdTests {

        @Test
        @DisplayName("should_returnSeries_when_found")
        void should_returnSeries_when_found() {
            // given
            Series series = createTestSeries("series-1", "user1");
            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));

            // when
            SeriesResponse result = seriesService.getSeriesById("series-1");

            // then
            assertThat(result.id()).isEqualTo("series-1");
            verify(seriesRepository).findById("series-1");
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(seriesRepository.findById("series-1")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> seriesService.getSeriesById("series-1"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.SERIES_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("addPostToSeries 메서드")
    class AddPostToSeriesTests {

        @Test
        @DisplayName("should_addPost")
        void should_addPost() {
            // given
            Series series = createTestSeries("series-1", "user1");

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));
            when(seriesRepository.save(any(Series.class))).thenReturn(series);

            // when
            SeriesResponse result = seriesService.addPostToSeries("series-1", "post-1", "user1");

            // then
            assertThat(result.postIds()).contains("post-1");
            verify(seriesRepository).save(any(Series.class));
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Series series = createTestSeries("series-1", "user1");

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));
            // postRepository.findById는 권한 체크 후 호출되므로 모킹하지 않음

            // when & then
            assertThatThrownBy(() -> seriesService.addPostToSeries("series-1", "post-1", "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.SERIES_ADD_POST_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("removePostFromSeries 메서드")
    class RemovePostFromSeriesTests {

        @Test
        @DisplayName("should_removePost")
        void should_removePost() {
            // given
            Series series = createTestSeries("series-1", "user1");
            series.addPost("post-1");

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));
            when(seriesRepository.save(any(Series.class))).thenReturn(series);

            // when
            SeriesResponse result = seriesService.removePostFromSeries("series-1", "post-1", "user1");

            // then
            assertThat(result.postIds()).doesNotContain("post-1");
            verify(seriesRepository).save(any(Series.class));
        }
    }

    @Nested
    @DisplayName("reorderPosts 메서드")
    class ReorderPostsTests {

        @Test
        @DisplayName("should_reorderPosts")
        void should_reorderPosts() {
            // given
            Series series = createTestSeries("series-1", "user1");
            series.addPost("post-1");
            series.addPost("post-2");

            SeriesPostOrderRequest request = new SeriesPostOrderRequest(
                    List.of("post-2", "post-1")
            );

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));
            when(seriesRepository.save(any(Series.class))).thenReturn(series);

            // when
            SeriesResponse result = seriesService.reorderPosts("series-1", request, "user1");

            // then
            assertThat(result.postIds()).containsExactly("post-2", "post-1");
            verify(seriesRepository).save(any(Series.class));
        }
    }

    @Nested
    @DisplayName("getSeriesPosts 메서드")
    class GetSeriesPostsTests {

        @Test
        @DisplayName("should_returnPostsInOrder")
        void should_returnPostsInOrder() {
            // given
            Series series = createTestSeries("series-1", "user1");
            series.addPost("post-1");
            series.addPost("post-2");

            Post post1 = createTestPost("post-1", "user1");
            Post post2 = createTestPost("post-2", "user1");

            when(seriesRepository.findById("series-1")).thenReturn(Optional.of(series));
            when(postRepository.findAllById(List.of("post-1", "post-2")))
                    .thenReturn(List.of(post1, post2));

            // when
            List<PostSummaryResponse> result = seriesService.getSeriesPosts("series-1");

            // then
            assertThat(result).hasSize(2);
            verify(postRepository).findAllById(List.of("post-1", "post-2"));
        }
    }

    private Series createTestSeries(String id, String authorId) {
        Series series = Series.builder()
                .name("Test Series")
                .description("Description")
                .authorId(authorId)
                .authorName("Author Name")
                .build();
        ReflectionTestUtils.setField(series, "id", id);
        return series;
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
}
