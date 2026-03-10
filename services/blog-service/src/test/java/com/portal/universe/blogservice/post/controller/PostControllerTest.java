package com.portal.universe.blogservice.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.blogservice.post.domain.PostSortType;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.dto.stats.CategoryStats;
import com.portal.universe.blogservice.post.service.PostService;
import com.portal.universe.blogservice.common.domain.SortDirection;
import com.portal.universe.blogservice.tag.dto.TagStatsResponse;
import com.portal.universe.commonlibrary.security.config.AuthUserWebConfig;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthUserWebConfig.class)
@DisplayName("PostController 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private com.portal.universe.blogservice.common.feign.AuthFollowClient authFollowClient;

    private AuthUser authUser;
    private PostResponse postResponse;
    private PostSummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser("user-1", "User Name", "UserNick", null);

        Instant now = Instant.now();
        postResponse = new PostResponse(
            "post-1",
            "Test Title",
            "Test Content",
            "Test Summary",
            "user-1",
            "user-1-username",
            "UserNick",
            PostStatus.PUBLISHED,
            Set.of("tag1", "tag2"),
            "Technology",
            "Meta Description",
            "thumbnail.jpg",
            List.of("image1.jpg"),
            100L,
            50L,
            now,
            now,
            now,
            null
        );

        summaryResponse = new PostSummaryResponse(
            "post-1",
            "Test Title",
            "Test Summary",
            "user-1",
            "user-1-username",
            "UserNick",
            Set.of("tag1", "tag2"),
            "Technology",
            "thumbnail.jpg",
            List.of("image1.jpg"),
            100L,
            50L,
            10L,
            now,
            5
        );
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("POST /posts - should_createPost_when_validRequest")
    void should_createPost_when_validRequest() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(
            "Test Title",
            "Test Content",
            "Test Summary",
            Set.of("tag1", "tag2"),
            "Technology",
            "Meta Description",
            "thumbnail.jpg",
            true,
            List.of("image1.jpg"),
            null
        );
        when(postService.createPost(any(PostCreateRequest.class), eq("user-1"), anyString(), anyString()))
            .thenReturn(postResponse);

        // when & then
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("post-1"))
            .andExpect(jsonPath("$.data.title").value("Test Title"));

        verify(postService).createPost(any(PostCreateRequest.class), eq("user-1"), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /posts - should_returnBadRequest_when_titleIsBlank")
    void should_returnBadRequest_when_titleIsBlank() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(
            "",
            "Test Content",
            "Test Summary",
            Set.of("tag1"),
            "Technology",
            null,
            null,
            true,
            null,
            null
        );

        // when & then
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /posts/{postId} - should_returnPost_when_found")
    void should_returnPost_when_found() throws Exception {
        // given
        when(postService.getPostById(eq("post-1"), any())).thenReturn(postResponse);

        // when & then
        mockMvc.perform(get("/posts/post-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("post-1"))
            .andExpect(jsonPath("$.data.title").value("Test Title"));

        verify(postService).getPostById(eq("post-1"), any());
    }

    @Test
    @DisplayName("GET /posts/{postId}/view - should_returnPostWithViewIncrement")
    void should_returnPostWithViewIncrement() throws Exception {
        // given
        when(postService.getPostByIdWithViewIncrement("post-1", "user-1")).thenReturn(postResponse);

        // when & then
        mockMvc.perform(get("/posts/post-1/view")
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("post-1"));

        verify(postService).getPostByIdWithViewIncrement("post-1", "user-1");
    }

    @Test
    @DisplayName("PUT /posts/{postId} - should_updatePost_when_validRequest")
    void should_updatePost_when_validRequest() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(
            "Updated Title",
            "Updated Content",
            "Updated Summary",
            Set.of("tag3"),
            "Science",
            "Updated Meta",
            "new-thumbnail.jpg",
            List.of("image2.jpg")
        );
        when(postService.updatePost(eq("post-1"), any(PostUpdateRequest.class), eq("user-1")))
            .thenReturn(postResponse);

        // when & then
        mockMvc.perform(put("/posts/post-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("post-1"));

        verify(postService).updatePost(eq("post-1"), any(PostUpdateRequest.class), eq("user-1"));
    }

    @Test
    @DisplayName("PUT /posts/{postId} - should_returnBadRequest_when_contentIsBlank")
    void should_returnBadRequest_when_contentIsBlank() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(
            "Title",
            "",
            "Summary",
            Set.of("tag1"),
            "Technology",
            null,
            null,
            null
        );

        // when & then
        mockMvc.perform(put("/posts/post-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /posts/{postId} - should_deletePost")
    void should_deletePost() throws Exception {
        // when & then
        mockMvc.perform(delete("/posts/post-1")
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(postService).deletePost("post-1", "user-1");
    }

    @Test
    @DisplayName("PATCH /posts/{postId}/status - should_changeStatus")
    void should_changeStatus() throws Exception {
        // given
        PostStatusChangeRequest request = new PostStatusChangeRequest(PostStatus.PUBLISHED);
        when(postService.changePostStatus("post-1", PostStatus.PUBLISHED, "user-1"))
            .thenReturn(postResponse);

        // when & then
        mockMvc.perform(patch("/posts/post-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(postService).changePostStatus("post-1", PostStatus.PUBLISHED, "user-1");
    }

    @Test
    @DisplayName("GET /posts - should_returnPublishedPosts")
    void should_returnPublishedPosts() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getPublishedPosts(0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).getPublishedPosts(0, 10);
    }

    @Test
    @DisplayName("GET /posts/author/{authorId} - should_returnAuthorPosts")
    void should_returnAuthorPosts() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getPostsByAuthor("user-1", 0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/author/user-1")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].authorId").value("user-1"));

        verify(postService).getPostsByAuthor("user-1", 0, 10);
    }

    @Test
    @DisplayName("GET /posts/category/{category} - should_returnCategoryPosts")
    void should_returnCategoryPosts() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getPostsByCategory("Technology", 0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/category/Technology")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].category").value("Technology"));

        verify(postService).getPostsByCategory("Technology", 0, 10);
    }

    @Test
    @DisplayName("GET /posts/tags - should_returnTaggedPosts")
    void should_returnTaggedPosts() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getPostsByTags(List.of("tag1", "tag2"), 0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/tags")
                .param("tags", "tag1", "tag2")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).getPostsByTags(List.of("tag1", "tag2"), 0, 10);
    }

    @Test
    @DisplayName("GET /posts/popular - should_returnPopularPosts")
    void should_returnPopularPosts() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getPopularPosts(0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/popular")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).getPopularPosts(0, 10);
    }

    @Test
    @DisplayName("GET /posts/trending - should_returnTrendingPosts")
    void should_returnTrendingPosts() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getTrendingPosts("week", 0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/trending")
                .param("period", "week")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).getTrendingPosts("week", 0, 10);
    }

    @Test
    @DisplayName("GET /posts/recent - should_returnRecentPosts")
    void should_returnRecentPosts() throws Exception {
        // given
        when(postService.getRecentPosts(5)).thenReturn(List.of(summaryResponse));

        // when & then
        mockMvc.perform(get("/posts/recent")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("post-1"));

        verify(postService).getRecentPosts(5);
    }

    @Test
    @DisplayName("GET /posts/search - should_returnSearchResults")
    void should_returnSearchResults() throws Exception {
        // given
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.searchPosts("keyword", 0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/search")
                .param("keyword", "keyword")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).searchPosts("keyword", 0, 10);
    }

    @Test
    @DisplayName("POST /posts/search/advanced - should_returnAdvancedSearchResults")
    void should_returnAdvancedSearchResults() throws Exception {
        // given
        PostSearchRequest searchRequest = new PostSearchRequest(
            "keyword",
            "Technology",
            List.of("tag1"),
            PostStatus.PUBLISHED,
            "user-1",
            null,
            null,
            PostSortType.PUBLISHED_AT,
            SortDirection.DESC,
            10,
            0
        );
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.searchPostsAdvanced(any(PostSearchRequest.class))).thenReturn(page);

        // when & then
        mockMvc.perform(post("/posts/search/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).searchPostsAdvanced(any(PostSearchRequest.class));
    }

    @Test
    @DisplayName("GET /posts/stats/categories - should_returnCategoryStats")
    void should_returnCategoryStats() throws Exception {
        // given
        CategoryStats stats = new CategoryStats("Technology", 10L, Instant.now());
        when(postService.getCategoryStats()).thenReturn(List.of(stats));

        // when & then
        mockMvc.perform(get("/posts/stats/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].categoryName").value("Technology"))
            .andExpect(jsonPath("$.data[0].postCount").value(10));

        verify(postService).getCategoryStats();
    }

    @Test
    @DisplayName("GET /posts/stats/tags - should_returnPopularTags")
    void should_returnPopularTags() throws Exception {
        // given
        TagStatsResponse tagStats = new TagStatsResponse("tag1", 20L, 5000L);
        when(postService.getPopularTags(10)).thenReturn(List.of(tagStats));

        // when & then
        mockMvc.perform(get("/posts/stats/tags")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("tag1"))
            .andExpect(jsonPath("$.data[0].postCount").value(20));

        verify(postService).getPopularTags(10);
    }

    @Test
    @DisplayName("GET /posts/{postId}/navigation - should_returnNavigation")
    void should_returnNavigation() throws Exception {
        // given
        PostNavigationResponse navigation = new PostNavigationResponse(
            summaryResponse,
            summaryResponse,
            null
        );
        when(postService.getPostNavigation("post-1", "all")).thenReturn(navigation);

        // when & then
        mockMvc.perform(get("/posts/post-1/navigation")
                .param("scope", "all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.previousPost.id").value("post-1"));

        verify(postService).getPostNavigation("post-1", "all");
    }

    @Test
    @DisplayName("GET /posts/feed - should_returnFeed")
    void should_returnFeed() throws Exception {
        // given
        var followingIdsDto = new com.portal.universe.blogservice.common.feign.AuthFollowClient.FollowingIdsDto(
                List.of("user-2", "user-3"));
        when(authFollowClient.getFollowingIds("user-1"))
                .thenReturn(com.portal.universe.commonlibrary.response.ApiResponse.success(followingIdsDto));

        Page<PostSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(postService.getFeed(List.of("user-2", "user-3"), 0, 10)).thenReturn(page);

        // when & then
        mockMvc.perform(get("/posts/feed")
                .requestAttr("authUser", authUser)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value("post-1"));

        verify(postService).getFeed(List.of("user-2", "user-3"), 0, 10);
    }

    @Test
    @DisplayName("GET /posts/product/{productId} - should_returnProductPosts")
    void should_returnProductPosts() throws Exception {
        // given
        when(postService.getPostsByProductId("product-1")).thenReturn(List.of(postResponse));

        // when & then
        mockMvc.perform(get("/posts/product/product-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("post-1"));

        verify(postService).getPostsByProductId("product-1");
    }
}
