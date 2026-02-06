package com.portal.universe.blogservice.post.repository;

import com.portal.universe.blogservice.post.dto.stats.AuthorStats;
import com.portal.universe.blogservice.post.dto.stats.BlogStats;
import com.portal.universe.blogservice.post.dto.stats.CategoryStats;
import com.portal.universe.blogservice.tag.dto.TagStatsResponse;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("PostRepositoryCustomImpl 테스트")
class PostRepositoryCustomImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PostRepositoryCustomImpl postRepositoryCustom;

    @Nested
    @DisplayName("aggregateCategoryStats 메서드")
    class AggregateCategoryStatsTests {

        @Test
        @DisplayName("should_returnCategoryStats")
        void should_returnCategoryStats() throws Exception {
            // given
            LocalDateTime now = LocalDateTime.now();
            Object categoryResult = createCategoryStatsResult("tech", 10L, now);

            @SuppressWarnings("unchecked")
            AggregationResults<Object> aggregationResults = mock(AggregationResults.class);
            when(aggregationResults.getMappedResults()).thenReturn(List.of(categoryResult));
            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), any(Class.class)))
                    .thenReturn(aggregationResults);

            // when
            List<CategoryStats> result = postRepositoryCustom.aggregateCategoryStats(PostStatus.PUBLISHED);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryName()).isEqualTo("tech");
            assertThat(result.get(0).postCount()).isEqualTo(10L);
            assertThat(result.get(0).latestPostDate()).isEqualTo(now);
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), any(Class.class));
        }

        @Test
        @DisplayName("should_returnEmptyList_when_noPosts")
        void should_returnEmptyList_when_noPosts() {
            // given
            AggregationResults<Object> aggregationResults = new AggregationResults<>(List.of(), new Document());
            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), any(Class.class)))
                    .thenReturn(aggregationResults);

            // when
            List<CategoryStats> result = postRepositoryCustom.aggregateCategoryStats(PostStatus.PUBLISHED);

            // then
            assertThat(result).isEmpty();
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), any(Class.class));
        }
    }

    @Nested
    @DisplayName("aggregatePopularTags 메서드")
    class AggregatePopularTagsTests {

        @Test
        @DisplayName("should_returnPopularTags")
        void should_returnPopularTags() throws Exception {
            // given
            Object tagResult = createTagStatsResult("java", 15L, 1000L);

            @SuppressWarnings("unchecked")
            AggregationResults<Object> aggregationResults = mock(AggregationResults.class);
            when(aggregationResults.getMappedResults()).thenReturn(List.of(tagResult));
            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), any(Class.class)))
                    .thenReturn(aggregationResults);

            // when
            List<TagStatsResponse> result = postRepositoryCustom.aggregatePopularTags(PostStatus.PUBLISHED, 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("java");
            assertThat(result.get(0).postCount()).isEqualTo(15L);
            assertThat(result.get(0).totalViews()).isEqualTo(1000L);
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), any(Class.class));
        }
    }

    @Nested
    @DisplayName("aggregateTrendingPosts 메서드")
    class AggregateTrendingPostsTests {

        @Test
        @DisplayName("should_returnTrendingPosts_with_pagination")
        void should_returnTrendingPosts_with_pagination() {
            // given
            Post post = Post.builder()
                    .title("Trending Post")
                    .content("Content")
                    .authorId("user1")
                    .status(PostStatus.PUBLISHED)
                    .build();

            @SuppressWarnings("unchecked")
            AggregationResults<Post> aggregationResults = mock(AggregationResults.class);
            when(aggregationResults.getMappedResults()).thenReturn(List.of(post));
            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), eq(Post.class)))
                    .thenReturn(aggregationResults);
            when(mongoTemplate.count(any(Query.class), eq(Post.class))).thenReturn(1L);

            // when
            Page<Post> result = postRepositoryCustom.aggregateTrendingPosts(
                    PostStatus.PUBLISHED,
                    LocalDateTime.now().minusDays(7),
                    24.0,
                    0,
                    10
            );

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Trending Post");
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), eq(Post.class));
            verify(mongoTemplate).count(any(Query.class), eq(Post.class));
        }
    }

    @Nested
    @DisplayName("aggregateBlogStats 메서드")
    class AggregateBlogStatsTests {

        @Test
        @DisplayName("should_returnBlogStats")
        void should_returnBlogStats() {
            // given
            LocalDateTime lastPostDate = LocalDateTime.now();
            List<String> topCategories = List.of("tech", "java");
            List<String> topTags = List.of("spring", "boot");

            Document statsDoc = new Document();
            statsDoc.put("totalViews", 5000L);
            statsDoc.put("totalLikes", 300L);

            AggregationResults<Document> aggregationResults = new AggregationResults<>(
                    List.of(statsDoc),
                    new Document()
            );

            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), eq(Document.class)))
                    .thenReturn(aggregationResults);
            when(mongoTemplate.count(any(Query.class), eq(Post.class)))
                    .thenReturn(100L)  // totalPosts
                    .thenReturn(80L);  // publishedPosts

            // when
            BlogStats result = postRepositoryCustom.aggregateBlogStats(
                    PostStatus.PUBLISHED,
                    topCategories,
                    topTags,
                    lastPostDate
            );

            // then
            assertThat(result.totalPosts()).isEqualTo(100L);
            assertThat(result.totalViews()).isEqualTo(5000L);
            assertThat(result.totalLikes()).isEqualTo(300L);
            assertThat(result.publishedPosts()).isEqualTo(80L);
            assertThat(result.topCategories()).isEqualTo(topCategories);
            assertThat(result.topTags()).isEqualTo(topTags);
            assertThat(result.lastPostDate()).isEqualTo(lastPostDate);
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), eq(Document.class));
        }
    }

    @Nested
    @DisplayName("aggregateAuthorStats 메서드")
    class AggregateAuthorStatsTests {

        @Test
        @DisplayName("should_returnAuthorStats")
        void should_returnAuthorStats() {
            // given
            Document statsDoc = new Document();
            statsDoc.put("totalPosts", 50L);
            statsDoc.put("totalViews", 2500L);
            statsDoc.put("totalLikes", 150L);
            statsDoc.put("authorName", "Author Name");
            statsDoc.put("firstPostDate", java.util.Date.from(
                    LocalDateTime.now().minusDays(30).atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));
            statsDoc.put("lastPostDate", java.util.Date.from(
                    LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));

            AggregationResults<Document> aggregationResults = new AggregationResults<>(
                    List.of(statsDoc),
                    new Document()
            );

            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), eq(Document.class)))
                    .thenReturn(aggregationResults);
            when(mongoTemplate.count(any(Query.class), eq(Post.class))).thenReturn(45L);

            // when
            AuthorStats result = postRepositoryCustom.aggregateAuthorStats("user1");

            // then
            assertThat(result.authorId()).isEqualTo("user1");
            assertThat(result.authorName()).isEqualTo("Author Name");
            assertThat(result.totalPosts()).isEqualTo(50L);
            assertThat(result.publishedPosts()).isEqualTo(45L);
            assertThat(result.totalViews()).isEqualTo(2500L);
            assertThat(result.totalLikes()).isEqualTo(150L);
            assertThat(result.firstPostDate()).isNotNull();
            assertThat(result.lastPostDate()).isNotNull();
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), eq(Document.class));
            verify(mongoTemplate).count(any(Query.class), eq(Post.class));
        }

        @Test
        @DisplayName("should_returnDefaultStats_when_noPosts")
        void should_returnDefaultStats_when_noPosts() {
            // given
            AggregationResults<Document> aggregationResults = new AggregationResults<>(List.of(), new Document());
            when(mongoTemplate.aggregate(any(Aggregation.class), eq(Post.class), eq(Document.class)))
                    .thenReturn(aggregationResults);

            // when
            AuthorStats result = postRepositoryCustom.aggregateAuthorStats("user1");

            // then
            assertThat(result.authorId()).isEqualTo("user1");
            assertThat(result.authorName()).isNull();
            assertThat(result.totalPosts()).isZero();
            assertThat(result.publishedPosts()).isZero();
            assertThat(result.totalViews()).isZero();
            assertThat(result.totalLikes()).isZero();
            verify(mongoTemplate).aggregate(any(Aggregation.class), eq(Post.class), eq(Document.class));
        }
    }

    // Helper methods for creating private record instances via reflection
    private Object createCategoryStatsResult(String id, Long postCount, LocalDateTime latestPostDate) throws Exception {
        Class<?> clazz = Class.forName("com.portal.universe.blogservice.post.repository.PostRepositoryCustomImpl$CategoryStatsResult");
        Constructor<?> ctor = clazz.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(id, postCount, latestPostDate);
    }

    private Object createTagStatsResult(String tag, Long postCount, Long totalViews) throws Exception {
        Class<?> clazz = Class.forName("com.portal.universe.blogservice.post.repository.PostRepositoryCustomImpl$TagStatsResult");
        Constructor<?> ctor = clazz.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(tag, postCount, totalViews);
    }
}
