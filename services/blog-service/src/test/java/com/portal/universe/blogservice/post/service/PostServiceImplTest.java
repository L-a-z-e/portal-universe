package com.portal.universe.blogservice.post.service;

import com.mongodb.client.result.UpdateResult;
import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostSortType;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.common.domain.SortDirection;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.blogservice.series.repository.SeriesRepository;
import com.portal.universe.blogservice.tag.service.TagService;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl 테스트")
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private TagService tagService;

    @InjectMocks
    private PostServiceImpl postService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createPost 메서드")
    class CreatePostTests {

        @Test
        @DisplayName("should_createDraftPost_when_publishImmediatelyIsFalse")
        void should_createDraftPost_when_publishImmediatelyIsFalse() {
            // given
            PostCreateRequest request = new PostCreateRequest(
                    "Test Title",
                    "Test Content",
                    "Summary",
                    Set.of("tag1", "tag2"),
                    "tech",
                    "Meta Description",
                    "thumbnail.jpg",
                    false,
                    List.of("image1.jpg"),
                    null
            );

            Post savedPost = Post.builder()
                    .title(request.title())
                    .content(request.content())
                    .authorId("user1")
                    .authorUsername("user1_handle")
                    .authorNickname("User One")
                    .status(PostStatus.DRAFT)
                    .tags(request.tags())
                    .build();
            ReflectionTestUtils.setField(savedPost, "id", "post-1");

            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // when
            PostResponse result = postService.createPost(request, "user1", "user1_handle", "User One");

            // then
            assertThat(result.status()).isEqualTo(PostStatus.DRAFT);
            assertThat(result.publishedAt()).isNull();
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should_createPublishedPost_when_publishImmediatelyIsTrue")
        void should_createPublishedPost_when_publishImmediatelyIsTrue() {
            // given
            PostCreateRequest request = new PostCreateRequest(
                    "Test Title",
                    "Test Content",
                    "Summary",
                    Set.of("tag1"),
                    "tech",
                    "Meta",
                    "thumb.jpg",
                    true,
                    List.of(),
                    null
            );

            Post savedPost = Post.builder()
                    .title(request.title())
                    .content(request.content())
                    .authorId("user1")
                    .authorUsername("user1_handle")
                    .authorNickname("User One")
                    .status(PostStatus.PUBLISHED)
                    .tags(request.tags())
                    .build();
            ReflectionTestUtils.setField(savedPost, "id", "post-1");
            ReflectionTestUtils.setField(savedPost, "publishedAt", LocalDateTime.now());

            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // when
            PostResponse result = postService.createPost(request, "user1", "user1_handle", "User One");

            // then
            assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(result.publishedAt()).isNotNull();
            verify(postRepository).save(any(Post.class));
        }
    }

    @Nested
    @DisplayName("getPostById 메서드")
    class GetPostByIdTests {

        @Test
        @DisplayName("should_returnPost_when_found")
        void should_returnPost_when_found() {
            // given
            Post post = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

            // when
            PostResponse result = postService.getPostById("post-1");

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("post-1");
            verify(postRepository).findById("post-1");
        }

        @Test
        @DisplayName("should_throwException_when_postNotFound")
        void should_throwException_when_postNotFound() {
            // given
            when(postRepository.findById("post-1")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.getPostById("post-1"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.POST_NOT_FOUND);
            verify(postRepository).findById("post-1");
        }
    }

    @Nested
    @DisplayName("updatePost 메서드")
    class UpdatePostTests {

        @Test
        @DisplayName("should_updatePost_when_author")
        void should_updatePost_when_author() {
            // given
            Post existingPost = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            PostUpdateRequest request = new PostUpdateRequest(
                    "Updated Title",
                    "Updated Content",
                    "Updated Summary",
                    Set.of("tag3"),
                    "tech",
                    "Updated Meta",
                    "new-thumb.jpg",
                    List.of()
            );

            when(postRepository.findById("post-1")).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(Post.class))).thenReturn(existingPost);

            // when
            PostResponse result = postService.updatePost("post-1", request, "user1");

            // then
            assertThat(result).isNotNull();
            verify(postRepository).findById("post-1");
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should_updatePost_when_blogAdmin")
        void should_updatePost_when_blogAdmin() {
            // given
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("adminId", null,
                            List.of(new SimpleGrantedAuthority("ROLE_BLOG_ADMIN")))
            );

            Post existingPost = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            PostUpdateRequest request = new PostUpdateRequest(
                    "Admin Update",
                    "Content",
                    "Summary",
                    Set.of(),
                    "tech",
                    "Meta",
                    null,
                    List.of()
            );

            when(postRepository.findById("post-1")).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(Post.class))).thenReturn(existingPost);

            // when
            PostResponse result = postService.updatePost("post-1", request, "adminId");

            // then
            assertThat(result).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Post existingPost = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            PostUpdateRequest request = new PostUpdateRequest(
                    "Title", "Content", "Summary", Set.of(), "tech", "Meta", null, List.of()
            );

            when(postRepository.findById("post-1")).thenReturn(Optional.of(existingPost));

            // when & then
            assertThatThrownBy(() -> postService.updatePost("post-1", request, "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.POST_UPDATE_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deletePost 메서드")
    class DeletePostTests {

        @Test
        @DisplayName("should_deletePost_when_author")
        void should_deletePost_when_author() {
            // given
            Post post = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

            // when
            postService.deletePost("post-1", "user1");

            // then
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("should_deletePost_when_blogAdmin")
        void should_deletePost_when_blogAdmin() {
            // given
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("adminId", null,
                            List.of(new SimpleGrantedAuthority("ROLE_BLOG_ADMIN")))
            );

            Post post = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

            // when
            postService.deletePost("post-1", "adminId");

            // then
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Post post = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.deletePost("post-1", "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.POST_DELETE_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("getPostByIdWithViewIncrement 메서드")
    class GetPostByIdWithViewIncrementTests {

        @Test
        @DisplayName("should_incrementViewCount_atomically")
        void should_incrementViewCount_atomically() {
            // given
            Post post = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Post.class)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null));

            // when
            PostResponse result = postService.getPostByIdWithViewIncrement("post-1", "user2");

            // then
            assertThat(result).isNotNull();
            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Post.class));
        }

        @Test
        @DisplayName("should_throwException_when_notViewable")
        void should_throwException_when_notViewable() {
            // given
            Post draftPost = createTestPost("post-1", "user1", PostStatus.DRAFT);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(draftPost));

            // when & then
            assertThatThrownBy(() -> postService.getPostByIdWithViewIncrement("post-1", "user2"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("changePostStatus 메서드")
    class ChangePostStatusTests {

        @Test
        @DisplayName("should_publishPost_when_statusIsPublished")
        void should_publishPost_when_statusIsPublished() {
            // given
            Post draftPost = createTestPost("post-1", "user1", PostStatus.DRAFT);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(draftPost));
            when(postRepository.save(any(Post.class))).thenReturn(draftPost);

            // when
            PostResponse result = postService.changePostStatus("post-1", PostStatus.PUBLISHED, "user1");

            // then
            assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should_throwException_when_forbidden")
        void should_throwException_when_forbidden() {
            // given
            Post post = createTestPost("post-1", "user1", PostStatus.DRAFT);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.changePostStatus("post-1", PostStatus.PUBLISHED, "user2"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getPublishedPosts 메서드")
    class GetPublishedPostsTests {

        @Test
        @DisplayName("should_returnPagedPosts")
        void should_returnPagedPosts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getPublishedPosts(0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable);
        }
    }

    @Nested
    @DisplayName("getPostsByAuthor 메서드")
    class GetPostsByAuthorTests {

        @Test
        @DisplayName("should_returnAuthorPosts")
        void should_returnAuthorPosts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByAuthorIdOrderByCreatedAtDesc("user1", pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getPostsByAuthor("user1", 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByAuthorIdOrderByCreatedAtDesc("user1", pageable);
        }
    }

    @Nested
    @DisplayName("getPostsByCategory 메서드")
    class GetPostsByCategoryTests {

        @Test
        @DisplayName("should_returnCategoryPosts")
        void should_returnCategoryPosts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByCategoryAndStatusOrderByPublishedAtDesc("tech", PostStatus.PUBLISHED, pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getPostsByCategory("tech", 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByCategoryAndStatusOrderByPublishedAtDesc("tech", PostStatus.PUBLISHED, pageable);
        }
    }

    @Nested
    @DisplayName("getPostsByTags 메서드")
    class GetPostsByTagsTests {

        @Test
        @DisplayName("should_returnTaggedPosts")
        void should_returnTaggedPosts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByTagsInAndStatusOrderByPublishedAtDesc(List.of("java"), PostStatus.PUBLISHED, pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getPostsByTags(List.of("java"), 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByTagsInAndStatusOrderByPublishedAtDesc(List.of("java"), PostStatus.PUBLISHED, pageable);
        }
    }

    @Nested
    @DisplayName("searchPosts 메서드")
    class SearchPostsTests {

        @Test
        @DisplayName("should_returnSearchResults")
        void should_returnSearchResults() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByTextSearchAndStatus("keyword", PostStatus.PUBLISHED, pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.searchPosts("keyword", 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByTextSearchAndStatus("keyword", PostStatus.PUBLISHED, pageable);
        }
    }

    @Nested
    @DisplayName("searchPostsAdvanced 메서드")
    class SearchPostsAdvancedTests {

        @Test
        @DisplayName("should_filterByKeyword")
        void should_filterByKeyword() {
            // given
            PostSearchRequest request = new PostSearchRequest(
                    "keyword", null, null, null, null, null, null,
                    PostSortType.PUBLISHED_AT, SortDirection.DESC, 10, 0
            );
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, PageRequest.of(0, 10), 1);

            when(postRepository.findByTextSearchAndStatus(eq("keyword"), eq(PostStatus.PUBLISHED), any(Pageable.class)))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.searchPostsAdvanced(request);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByTextSearchAndStatus(eq("keyword"), eq(PostStatus.PUBLISHED), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getPopularPosts 메서드")
    class GetPopularPostsTests {

        @Test
        @DisplayName("should_returnByViewCount")
        void should_returnByViewCount() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByStatusOrderByViewCountDescPublishedAtDesc(PostStatus.PUBLISHED, pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getPopularPosts(0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByStatusOrderByViewCountDescPublishedAtDesc(PostStatus.PUBLISHED, pageable);
        }
    }

    @Nested
    @DisplayName("getTrendingPosts 메서드")
    class GetTrendingPostsTests {

        @Test
        @DisplayName("should_returnTrendingPosts_by_period")
        void should_returnTrendingPosts_by_period() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Post> posts = List.of(createTestPost("post-1", "user1", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.aggregateTrendingPosts(
                    eq(PostStatus.PUBLISHED), any(LocalDateTime.class), eq(48.0), eq(0), eq(10)))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getTrendingPosts("week", 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).aggregateTrendingPosts(
                    eq(PostStatus.PUBLISHED), any(LocalDateTime.class), eq(48.0), eq(0), eq(10));
        }
    }

    @Nested
    @DisplayName("getPostNavigation 메서드")
    class GetPostNavigationTests {

        @Test
        @DisplayName("should_returnPrevAndNext")
        void should_returnPrevAndNext() {
            // given
            Post currentPost = createTestPost("post-2", "user1", PostStatus.PUBLISHED);
            ReflectionTestUtils.setField(currentPost, "publishedAt", LocalDateTime.now());

            Post prevPost = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            Post nextPost = createTestPost("post-3", "user1", PostStatus.PUBLISHED);

            when(postRepository.findById("post-2")).thenReturn(Optional.of(currentPost));
            when(postRepository.findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
                    eq(PostStatus.PUBLISHED), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(prevPost));
            when(postRepository.findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
                    eq(PostStatus.PUBLISHED), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(nextPost));

            // when
            PostNavigationResponse result = postService.getPostNavigation("post-2", "all");

            // then
            assertThat(result.previousPost()).isNotNull();
            assertThat(result.nextPost()).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_notPublished")
        void should_throwException_when_notPublished() {
            // given
            Post draftPost = createTestPost("post-1", "user1", PostStatus.DRAFT);
            when(postRepository.findById("post-1")).thenReturn(Optional.of(draftPost));

            // when & then
            assertThatThrownBy(() -> postService.getPostNavigation("post-1", "all"))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.POST_NOT_PUBLISHED);
        }
    }

    @Nested
    @DisplayName("getFeed 메서드")
    class GetFeedTests {

        @Test
        @DisplayName("should_returnFeedPosts")
        void should_returnFeedPosts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<String> followingIds = List.of("user2", "user3");
            List<Post> posts = List.of(createTestPost("post-1", "user2", PostStatus.PUBLISHED));
            Page<Post> page = new PageImpl<>(posts, pageable, 1);

            when(postRepository.findByAuthorIdInAndStatusOrderByPublishedAtDesc(
                    followingIds, PostStatus.PUBLISHED, pageable))
                    .thenReturn(page);

            // when
            Page<PostSummaryResponse> result = postService.getFeed(followingIds, 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(postRepository).findByAuthorIdInAndStatusOrderByPublishedAtDesc(
                    followingIds, PostStatus.PUBLISHED, pageable);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noFollowing")
        void should_returnEmptyPage_when_noFollowing() {
            // given

            // when
            Page<PostSummaryResponse> result = postService.getFeed(List.of(), 0, 10);

            // then
            assertThat(result.getContent()).isEmpty();
            verify(postRepository, never()).findByAuthorIdInAndStatusOrderByPublishedAtDesc(
                    any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getRelatedPosts 메서드")
    class GetRelatedPostsTests {

        @Test
        @DisplayName("should_returnRelatedPosts")
        void should_returnRelatedPosts() {
            // given
            Post post = createTestPost("post-1", "user1", PostStatus.PUBLISHED);
            Set<String> tags = new HashSet<>(Set.of("java", "spring"));
            ReflectionTestUtils.setField(post, "tags", tags);
            ReflectionTestUtils.setField(post, "category", "tech");

            when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
            when(postRepository.findRelatedPosts(
                    eq("tech"), eq(new ArrayList<>(tags)), eq(PostStatus.PUBLISHED), eq("post-1")))
                    .thenReturn(List.of(createTestPost("post-2", "user1", PostStatus.PUBLISHED)));

            // when
            List<PostSummaryResponse> result = postService.getRelatedPosts("post-1", 5);

            // then
            assertThat(result).hasSize(1);
            verify(postRepository).findRelatedPosts(
                    eq("tech"), eq(new ArrayList<>(tags)), eq(PostStatus.PUBLISHED), eq("post-1"));
        }
    }

    private Post createTestPost(String id, String authorId, PostStatus status) {
        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .summary("Test Summary")
                .authorId(authorId)
                .authorUsername(authorId + "_handle")
                .authorNickname("Test Author")
                .status(status)
                .tags(new HashSet<>())
                .category("tech")
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        if (status == PostStatus.PUBLISHED) {
            ReflectionTestUtils.setField(post, "publishedAt", LocalDateTime.now());
        }
        return post;
    }
}
