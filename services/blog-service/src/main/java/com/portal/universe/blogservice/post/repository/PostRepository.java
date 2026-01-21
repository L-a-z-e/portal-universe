package com.portal.universe.blogservice.post.repository;

import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends MongoRepository<Post, String> {
    // ===== 기존 메서드 유지 (하위 호환성) =====
    List<Post> findByProductId(String productId);

    // ===== 블로그 핵심 기능 확장 =====

    /**
     * PRD Phase 1: 발행된 게시물 목록 조회 (최신순)
     * 메인 블로그 페이지에서 사용
     */
    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    /**
     * 모든 게시물 조회 (관리자용 - 상태 무관)
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 작성자별 게시물 조회 (마이페이지)
     */
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    /**
     * 작성자별 + 상태별 조회 (작성자가 자신의 초안/발행 글 분리 조회)
     */
    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(String authorId, PostStatus status, Pageable pageable);

    /**
     * PRD Phase 1: 카테고리별 발행된 게시물 조회
     * 콘텐츠 허브 구조 지원
     */
    Page<Post> findByCategoryAndStatusOrderByPublishedAtDesc(String category, PostStatus status, Pageable pageable);

    /**
     * PRD Phase 1: 태그 검색 (다중 태그 OR 조건)
     * 태그 시스템 지원
     */
    Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(List<String> tags, PostStatus status, Pageable pageable);

    /**
     * PRD Phase 1: 전문 검색 (제목 + 내용)
     * MongoDB Text Index 활용
     */
    @Query("{ $text: { $search: ?0 }, status: ?1 }")
    Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);

    /**
     * 단일 게시물 조회 (조회 권한 고려)
     * 발행된 글이거나 작성자 본인인 경우만
     */
    @Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
    Optional<Post> findByIdAndViewableBy(String postId, String userId);

    /**
     * PRD Phase 1: 인기 게시물 조회 (조회수 기준)
     * 사이드바나 추천 섹션에서 활용
     */
    Page<Post> findByStatusOrderByViewCountDescPublishedAtDesc(PostStatus status, Pageable pageable);

    /**
     * PRD: 최근 발행된 게시물 (특정 기간 이후)
     * 최신 글 위젯에서 활용
     */
    Page<Post> findByStatusAndPublishedAtAfterOrderByPublishedAtDesc(
            PostStatus status, LocalDateTime since, Pageable pageable);

    /**
     * PRD Phase 2: 관련 게시물 추천
     * 동일 카테고리 또는 공통 태그를 가진 글들
     */
    @Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
    List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);

    // ===== 통계 및 집계 메서드 =====

    /**
     * 작성자별 발행된 게시물 수
     */
    long countByAuthorIdAndStatus(String authorId, PostStatus status);

    /**
     * 카테고리별 발행된 게시물 수
     */
    long countByCategoryAndStatus(String category, PostStatus status);

    /**
     * 전체 발행된 게시물 수
     */
    long countByStatus(PostStatus status);

    /**
     * PRD Phase 1: 모든 카테고리 목록 조회 (발행된 글 기준)
     * 카테고리 네비게이션 구성용
     */
    @Query(value = "{ status: ?0 }", fields = "{ category: 1 }")
    List<String> findDistinctCategoriesByStatus(PostStatus status);

    /**
     * PRD Phase 1: 인기 태그 조회 (발행된 글 기준)
     * 태그 클라우드 구성용
     */
    @Query("{ status: ?0 }")
    List<Post> findByStatusForTagAggregation(PostStatus status);

    // ===== PRD Phase 2 대비: 고급 쿼리 =====

    /**
     * 특정 기간 내 인기 게시물 (조회수 + 좋아요 수 복합)
     */
    @Query("{ status: ?0, publishedAt: { $gte: ?1, $lte: ?2 } }")
    Page<Post> findPopularPostsInPeriod(PostStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 시리즈/연재 기능 대비: 동일 태그 그룹의 연속 게시물
     */
    Page<Post> findByTagsContainingAndStatusOrderByPublishedAtAsc(String seriesTag, PostStatus status, Pageable pageable);

    // ===== 피드 기능 =====

    /**
     * 팔로잉 사용자들의 게시물 조회 (피드)
     * authorIds에 포함된 작성자들의 발행된 게시물을 최신순으로 조회
     */
    Page<Post> findByAuthorIdInAndStatusOrderByPublishedAtDesc(
            List<String> authorIds, PostStatus status, Pageable pageable);

    // ===== 네비게이션 기능 =====

    /**
     * 이전 게시물 조회 (publishedAt이 현재보다 작은 것 중 가장 최신)
     */
    Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            PostStatus status, LocalDateTime publishedAt);

    /**
     * 다음 게시물 조회 (publishedAt이 현재보다 큰 것 중 가장 오래된 것)
     */
    Optional<Post> findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            PostStatus status, LocalDateTime publishedAt);

    /**
     * 같은 작성자의 이전 게시물 조회
     */
    Optional<Post> findFirstByAuthorIdAndStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            String authorId, PostStatus status, LocalDateTime publishedAt);

    /**
     * 같은 작성자의 다음 게시물 조회
     */
    Optional<Post> findFirstByAuthorIdAndStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            String authorId, PostStatus status, LocalDateTime publishedAt);

    /**
     * 같은 카테고리의 이전 게시물 조회
     */
    Optional<Post> findFirstByCategoryAndStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            String category, PostStatus status, LocalDateTime publishedAt);

    /**
     * 같은 카테고리의 다음 게시물 조회
     */
    Optional<Post> findFirstByCategoryAndStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            String category, PostStatus status, LocalDateTime publishedAt);
}
