package com.portal.universe.blogservice.post.repository;

import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.stats.CategoryStats;
import com.portal.universe.blogservice.tag.dto.TagStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * MongoDB Aggregation Pipeline 구현체
 *
 * MongoTemplate vs MongoRepository:
 * - MongoRepository: 간단한 CRUD, 메서드 이름 기반 쿼리
 * - MongoTemplate: 복잡한 쿼리, Aggregation, 벌크 연산 등
 *
 * Aggregation Pipeline 이해하기:
 * - Unix 파이프라인처럼 여러 단계를 거쳐 데이터 변환
 * - 각 단계의 출력이 다음 단계의 입력이 됨
 * - 예: match → group → sort → project
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    /**
     * 카테고리별 통계를 한 번의 쿼리로 집계
     *
     * 생성되는 MongoDB 쿼리:
     * db.posts.aggregate([
     *   { $match: { status: "PUBLISHED", category: { $ne: null } } },
     *   { $group: {
     *       _id: "$category",
     *       postCount: { $sum: 1 },
     *       latestPostDate: { $max: "$publishedAt" }
     *   }},
     *   { $sort: { postCount: -1 } }
     * ])
     */
    @Override
    public List<CategoryStats> aggregateCategoryStats(PostStatus status) {
        log.debug("Aggregating category stats for status: {}", status);

        // 1단계: $match - PUBLISHED 상태이고 카테고리가 있는 문서만 필터링
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("status").is(status.name())
                        .and("category").ne(null)
        );

        // 2단계: $group - 카테고리별로 그룹화
        // _id: 그룹 키 (여기서는 category 필드)
        // $sum: 1 = 각 문서당 1씩 더함 = COUNT(*)
        // $max: 해당 그룹에서 가장 큰 값 = MAX()
        GroupOperation groupStage = Aggregation.group("category")
                .count().as("postCount")
                .max("publishedAt").as("latestPostDate");

        // 3단계: $sort - 게시물 수 내림차순
        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "postCount");

        // 파이프라인 조립
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                groupStage,
                sortStage
        );

        // 실행 및 결과 매핑
        AggregationResults<CategoryStatsResult> results = mongoTemplate.aggregate(
                aggregation,
                Post.class,  // 입력 컬렉션
                CategoryStatsResult.class  // 출력 타입
        );

        // 내부 결과 클래스 → DTO 변환
        return results.getMappedResults().stream()
                .map(r -> new CategoryStats(r.id(), r.postCount(), r.latestPostDate()))
                .toList();
    }

    /**
     * 인기 태그를 한 번의 쿼리로 집계
     *
     * $unwind 설명:
     * - 배열 필드를 "펼쳐서" 각 요소를 별도 문서로 만듦
     * - 예: { tags: ["java", "spring"] } →
     *       { tags: "java" }, { tags: "spring" }
     *
     * 생성되는 MongoDB 쿼리:
     * db.posts.aggregate([
     *   { $match: { status: "PUBLISHED" } },
     *   { $unwind: "$tags" },
     *   { $group: {
     *       _id: "$tags",
     *       postCount: { $sum: 1 },
     *       totalViews: { $sum: "$viewCount" }
     *   }},
     *   { $sort: { postCount: -1 } },
     *   { $limit: 10 }
     * ])
     */
    @Override
    public List<TagStatsResponse> aggregatePopularTags(PostStatus status, int limit) {
        log.debug("Aggregating popular tags for status: {}, limit: {}", status, limit);

        // 1단계: $match - PUBLISHED 상태만
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("status").is(status.name())
        );

        // 2단계: $unwind - tags 배열 펼치기
        // tags: ["java", "spring", "mongodb"]인 문서가
        // 3개의 문서로 분리됨 (각각 하나의 태그만 가짐)
        UnwindOperation unwindStage = Aggregation.unwind("tags");

        // 3단계: $group - 태그별 그룹화
        GroupOperation groupStage = Aggregation.group("tags")
                .count().as("postCount")
                .sum("viewCount").as("totalViews");

        // 4단계: $sort - 게시물 수 내림차순
        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "postCount");

        // 5단계: $limit - 상위 N개만
        LimitOperation limitStage = Aggregation.limit(limit);

        // 파이프라인 조립
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                unwindStage,
                groupStage,
                sortStage,
                limitStage
        );

        // 실행 및 결과 매핑
        AggregationResults<TagStatsResult> results = mongoTemplate.aggregate(
                aggregation,
                Post.class,
                TagStatsResult.class
        );

        return results.getMappedResults().stream()
                .map(r -> new TagStatsResponse(r.id(), r.postCount(), r.totalViews()))
                .toList();
    }

    /**
     * Aggregation 결과를 담는 내부 record
     * MongoDB의 _id 필드가 그룹 키를 담음
     */
    private record CategoryStatsResult(
            String id,  // _id 필드 = category 값
            Long postCount,
            LocalDateTime latestPostDate
    ) {}

    private record TagStatsResult(
            String id,  // _id 필드 = tag 값
            Long postCount,
            Long totalViews
    ) {}

    /**
     * 트렌딩 게시물을 점수 계산 후 정렬하여 조회
     *
     * [핵심 개념] $addFields
     * - 기존 문서에 새로운 필드를 추가하는 단계
     * - 여기서는 트렌딩 점수(trendingScore)를 계산해서 추가
     *
     * 점수 계산 공식:
     * baseScore = viewCount + (likeCount × 3) + (commentCount × 5)
     * hoursElapsed = (현재시간 - publishedAt) / 3600000  (밀리초→시간)
     * timeDecay = 2^(-hoursElapsed / halfLife)
     * trendingScore = baseScore × timeDecay
     *
     * 생성되는 MongoDB 쿼리:
     * db.posts.aggregate([
     *   { $match: { status: "PUBLISHED", publishedAt: { $gte: startDate } } },
     *   { $addFields: {
     *       trendingScore: {
     *         $multiply: [
     *           { $add: ["$viewCount", { $multiply: ["$likeCount", 3] }, { $multiply: [{ $ifNull: ["$commentCount", 0] }, 5] }] },
     *           { $pow: [2, { $divide: [{ $multiply: [-1, { $divide: [{ $subtract: [new Date(), "$publishedAt"] }, 3600000] }] }, halfLife] }] }
     *         ]
     *       }
     *   }},
     *   { $sort: { trendingScore: -1 } },
     *   { $skip: page * size },
     *   { $limit: size }
     * ])
     */
    @Override
    public Page<Post> aggregateTrendingPosts(PostStatus status, LocalDateTime startDate,
                                              double halfLifeHours, int page, int size) {
        log.debug("Aggregating trending posts: status={}, startDate={}, halfLife={}, page={}, size={}",
                status, startDate, halfLifeHours, page, size);

        // 현재 시간을 밀리초로 (MongoDB에서 날짜 계산용)
        long nowMillis = System.currentTimeMillis();

        // 1단계: $match - 상태와 기간 필터링
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("status").is(status.name())
                        .and("publishedAt").gte(startDate)
        );

        // 2단계: $addFields - 트렌딩 점수 계산
        // MongoDB의 복잡한 수식은 Document로 직접 작성
        Document addFieldsDoc = new Document("$addFields", new Document("trendingScore",
                // baseScore × timeDecay
                new Document("$multiply", Arrays.asList(
                        // baseScore = viewCount + likeCount*3 + commentCount*5
                        new Document("$add", Arrays.asList(
                                new Document("$ifNull", Arrays.asList("$viewCount", 0)),
                                new Document("$multiply", Arrays.asList(
                                        new Document("$ifNull", Arrays.asList("$likeCount", 0)),
                                        3
                                )),
                                new Document("$multiply", Arrays.asList(
                                        new Document("$ifNull", Arrays.asList("$commentCount", 0)),
                                        5
                                ))
                        )),
                        // timeDecay = 2^(-hoursElapsed / halfLife)
                        new Document("$pow", Arrays.asList(
                                2,
                                new Document("$divide", Arrays.asList(
                                        new Document("$multiply", Arrays.asList(
                                                -1,
                                                new Document("$divide", Arrays.asList(
                                                        // 밀리초 차이 → 시간 차이
                                                        new Document("$subtract", Arrays.asList(
                                                                nowMillis,
                                                                new Document("$ifNull", Arrays.asList(
                                                                        new Document("$toLong", "$publishedAt"),
                                                                        nowMillis
                                                                ))
                                                        )),
                                                        3600000.0  // 1시간 = 3600000 밀리초
                                                ))
                                        )),
                                        halfLifeHours
                                ))
                        ))
                ))
        ));

        // 3단계: $sort - 점수 내림차순
        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "trendingScore");

        // 4단계: $skip - 페이지 오프셋
        SkipOperation skipStage = Aggregation.skip((long) page * size);

        // 5단계: $limit - 페이지 크기
        LimitOperation limitStage = Aggregation.limit(size);

        // 파이프라인 조립 (addFields는 커스텀 단계로 추가)
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                ctx -> addFieldsDoc,  // 커스텀 단계: Document 직접 사용
                sortStage,
                skipStage,
                limitStage
        );

        // 실행
        AggregationResults<Post> results = mongoTemplate.aggregate(
                aggregation,
                Post.class,
                Post.class
        );

        List<Post> posts = results.getMappedResults();

        // 전체 개수 조회 (페이지 정보용)
        long total = mongoTemplate.count(
                Query.query(Criteria.where("status").is(status.name())
                        .and("publishedAt").gte(startDate)),
                Post.class
        );

        return new PageImpl<>(posts, PageRequest.of(page, size), total);
    }
}
