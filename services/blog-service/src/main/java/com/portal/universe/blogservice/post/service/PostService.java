package com.portal.universe.blogservice.post.service;

import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.dto.stats.AuthorStats;
import com.portal.universe.blogservice.post.dto.stats.BlogStats;
import com.portal.universe.blogservice.post.dto.stats.CategoryStats;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {

    PostResponse createPost(PostCreateRequest request, String authorId, String authorUsername, String authorNickname);
    Page<PostResponse> getAllPosts(int page, int size);
    PostResponse getPostById(String postId, String userId);
    PostResponse updatePost(String postId, PostUpdateRequest request, String userId);
    void deletePost(String postId, String userId);
    List<PostResponse> getPostsByProductId(String productId);

    Page<PostSummaryResponse> getPublishedPosts(int page, int size);
    Page<PostSummaryResponse> getPostsByAuthor(String authorId, int page, int size);
    Page<PostSummaryResponse> getPostsByAuthorAndStatus(String authorId, PostStatus status, int page, int size);
    Page<PostSummaryResponse> getPostsByCategory(String category, int page, int size);
    Page<PostSummaryResponse> getPostsByTags(List<String> tags, int page, int size);
    Page<PostSummaryResponse> searchPosts(String keyword, int page, int size);
    Page<PostSummaryResponse> searchPostsAdvanced(PostSearchRequest searchRequest);
    PostResponse getPostByIdWithViewIncrement(String postId, String userId);
    PostResponse changePostStatus(String postId, PostStatus newStatus, String userId);
    Page<PostSummaryResponse> getPopularPosts(int page, int size);
    Page<PostSummaryResponse> getTrendingPosts(String period, int page, int size);
    List<PostSummaryResponse> getRelatedPosts(String postId, int limit);
    List<PostSummaryResponse> getRecentPosts(int limit);

    List<CategoryStats> getCategoryStats();
    List<com.portal.universe.blogservice.tag.dto.TagStatsResponse> getPopularTags(int limit);
    AuthorStats getAuthorStats(String authorId);
    List<CategoryStats> getAuthorCategoryStats(String authorId);
    List<com.portal.universe.blogservice.tag.dto.TagStatsResponse> getAuthorPopularTags(String authorId, int limit);
    BlogStats getBlogStats();
    PostNavigationResponse getPostNavigation(String postId, String scope);

    Page<PostSummaryResponse> getFeed(List<String> followingIds, int page, int size);
}
