package com.portal.universe.blogservice.post.service;

import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.dto.stats.AuthorStats;
import com.portal.universe.blogservice.post.dto.stats.BlogStats;
import com.portal.universe.blogservice.post.dto.stats.CategoryStats;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {

    PostResponse createPost(PostCreateRequest request, String authorId);
    List<PostResponse> getAllPosts();
    PostResponse getPostById(String postId);
    PostResponse updatePost(String postId, PostUpdateRequest request, String userId);
    void deletePost(String postId, String userId);
    List<PostResponse> getPostsByProductId(String productId);

    // ===== 블로그 핵심 기능 확장 =====

    /**
     * PRD Phase 1: 페이징된 게시물 목록 조회
     * 메인 블로그 페이지용
     */
    Page<PostSummaryResponse> getPublishedPosts(int page, int size);

    /**
     * 작성자별 게시물 조회 (마이페이지)
     */
    Page<PostSummaryResponse> getPostsByAuthor(String authorId, int page, int size);

    /**
     * 작성자별 + 상태별 조회 (초안/발행 분리)
     */
    Page<PostSummaryResponse> getPostsByAuthorAndStatus(String authorId, PostStatus status, int page, int size);

    /**
     * PRD Phase 1: 카테고리별 게시물 조회
     */
    Page<PostSummaryResponse> getPostsByCategory(String category, int page, int size);

    /**
     * PRD Phase 1: 태그별 게시물 조회
     */
    Page<PostSummaryResponse> getPostsByTags(List<String> tags, int page, int size);

    /**
     * PRD Phase 1: 검색 기능
     */
    Page<PostSummaryResponse> searchPosts(String keyword, int page, int size);

    /**
     * 고급 검색 및 필터링
     */
    Page<PostSummaryResponse> searchPostsAdvanced(PostSearchRequest searchRequest);

    /**
     * PRD Phase 1: 게시물 상세 조회 (조회수 증가 포함)
     */
    PostResponse getPostByIdWithViewIncrement(String postId, String userId);

    /**
     * PRD Phase 1: 상태 변경 (발행/초안 전환)
     */
    PostResponse changePostStatus(String postId, PostStatus newStatus, String userId);

    /**
     * PRD Phase 1: 인기 게시물 조회
     */
    Page<PostSummaryResponse> getPopularPosts(int page, int size);

    /**
     * PRD Phase 1: 트렌딩 게시물 조회 (기간별)
     * @param period 기간 (today, week, month, year)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 트렌딩 게시물 목록
     */
    Page<PostSummaryResponse> getTrendingPosts(String period, int page, int size);

    /**
     * PRD Phase 2 대비: 관련 게시물 추천
     */
    List<PostSummaryResponse> getRelatedPosts(String postId, int limit);

    /**
     * PRD Phase 1: 최근 게시물 조회
     */
    List<PostSummaryResponse> getRecentPosts(int limit);

    // ===== 통계 및 메타 정보 =====

    /**
     * PRD: 카테고리 목록 조회 (네비게이션용)
     */
    List<CategoryStats> getCategoryStats();

    /**
     * PRD: 인기 태그 조회 (태그 클라우드용)
     */
    List<com.portal.universe.blogservice.tag.dto.TagStatsResponse> getPopularTags(int limit);

    /**
     * 작성자 통계
     */
    AuthorStats getAuthorStats(String authorId);

    /**
     * 전체 블로그 통계
     */
    BlogStats getBlogStats();

    /**
     * 포스트 네비게이션 조회 (이전/다음 게시물)
     */
    PostNavigationResponse getPostNavigation(String postId, String scope);

    // ===== 피드 기능 =====

    /**
     * 팔로잉 사용자들의 피드 조회
     * @param followingIds 팔로잉 사용자 UUID 목록
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 팔로잉 사용자들의 발행된 게시물 (최신순)
     */
    Page<PostSummaryResponse> getFeed(List<String> followingIds, int page, int size);
}
