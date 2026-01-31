package com.portal.universe.blogservice.post.repository;

import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.stats.AuthorStats;
import com.portal.universe.blogservice.post.dto.stats.BlogStats;
import com.portal.universe.blogservice.post.dto.stats.CategoryStats;
import com.portal.universe.blogservice.tag.dto.TagStatsResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Aggregation을 사용하는 커스텀 Repository 인터페이스
 *
 * 왜 필요한가?
 * - 기본 MongoRepository는 단순 쿼리만 지원
 * - GROUP BY, SUM, COUNT 등 집계 연산은 Aggregation Pipeline 필요
 * - N+1 쿼리 문제를 한 번의 쿼리로 해결
 */
public interface PostRepositoryCustom {

    /**
     * 카테고리별 통계 조회 (Aggregation 사용)
     *
     * 기존 방식: 카테고리 목록 조회 (1번) + 카테고리별 count (N번) = N+1 쿼리
     * 개선 방식: $group 으로 한 번에 집계 = 1번 쿼리
     *
     * MongoDB Aggregation 파이프라인:
     * 1. $match: status가 PUBLISHED인 문서만 필터링
     * 2. $group: category별로 그룹화하여 count와 최신 날짜 계산
     * 3. $sort: count 내림차순 정렬
     */
    List<CategoryStats> aggregateCategoryStats(PostStatus status);

    /**
     * 인기 태그 통계 조회 (Aggregation 사용)
     *
     * 기존 방식: 모든 게시물 로드 → 메모리에서 태그 집계
     * 개선 방식: $unwind + $group으로 DB에서 집계
     *
     * MongoDB Aggregation 파이프라인:
     * 1. $match: status가 PUBLISHED인 문서만 필터링
     * 2. $unwind: tags 배열을 개별 문서로 펼침
     * 3. $group: 태그별로 그룹화하여 count와 총 조회수 계산
     * 4. $sort: count 내림차순 정렬
     * 5. $limit: 상위 N개만 반환
     */
    List<TagStatsResponse> aggregatePopularTags(PostStatus status, int limit);

    /**
     * 트렌딩 게시물 조회 (Aggregation 사용)
     *
     * 기존 방식: 전체 게시물 로드 → 메모리에서 점수 계산 및 정렬
     * 개선 방식: $addFields로 DB에서 점수 계산 → $sort로 정렬 → $skip/$limit로 페이징
     *
     * 트렌딩 점수 공식:
     * score = (views×1 + likes×3 + comments×5) × timeDecay
     * timeDecay = 2^(-hoursElapsed / halfLife)
     *
     * @param status 게시물 상태
     * @param startDate 조회 시작 날짜
     * @param halfLifeHours 반감기 (시간 단위) - 기간에 따라 다름
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 트렌딩 점수 순으로 정렬된 게시물 페이지
     */
    Page<Post> aggregateTrendingPosts(PostStatus status, LocalDateTime startDate,
                                       double halfLifeHours, int page, int size);

    /**
     * 블로그 전체 통계 조회 (Aggregation 사용)
     *
     * 기존 방식: findAll() 후 메모리에서 viewCount/likeCount 합산 (OOM 위험)
     * 개선 방식: $group으로 DB에서 합산 = 1번 쿼리
     */
    BlogStats aggregateBlogStats(PostStatus publishedStatus, List<String> topCategories,
                                  List<String> topTags, LocalDateTime lastPostDate);

    /**
     * 작성자별 통계 조회 (Aggregation 사용)
     *
     * 기존 방식: Pageable.unpaged()로 전체 로드 후 메모리 합산
     * 개선 방식: $match + $group으로 DB에서 합산 = 1번 쿼리
     */
    AuthorStats aggregateAuthorStats(String authorId);
}
