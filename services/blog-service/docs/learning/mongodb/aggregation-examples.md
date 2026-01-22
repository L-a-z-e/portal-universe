# MongoDB Aggregation Examples

## 개요

Blog Service에서 사용하는 MongoDB Aggregation Pipeline 예제를 학습합니다.

## Aggregation Pipeline 기본 개념

```
데이터 → [Stage 1] → [Stage 2] → [Stage 3] → 결과
        ($match)    ($group)    ($sort)
```

Unix 파이프라인처럼 각 단계의 출력이 다음 단계의 입력이 됩니다.

## 주요 Aggregation 연산자

| 연산자 | 설명 | SQL 대응 |
|--------|------|----------|
| $match | 필터링 | WHERE |
| $group | 그룹화 | GROUP BY |
| $sort | 정렬 | ORDER BY |
| $project | 필드 선택/변환 | SELECT |
| $unwind | 배열 펼치기 | - |
| $limit | 결과 제한 | LIMIT |
| $skip | 결과 건너뛰기 | OFFSET |
| $addFields | 필드 추가 | - |

## 카테고리별 통계 집계

### MongoDB 쿼리

```javascript
db.posts.aggregate([
    // 1. 필터링: PUBLISHED 상태이고 카테고리가 있는 것만
    { $match: {
        status: "PUBLISHED",
        category: { $ne: null }
    }},

    // 2. 그룹화: 카테고리별로
    { $group: {
        _id: "$category",
        postCount: { $sum: 1 },
        latestPostDate: { $max: "$publishedAt" }
    }},

    // 3. 정렬: 게시물 수 내림차순
    { $sort: { postCount: -1 } }
])
```

### Spring Data MongoDB 구현

```java
@Override
public List<CategoryStats> aggregateCategoryStats(PostStatus status) {
    // 1단계: $match
    MatchOperation matchStage = Aggregation.match(
        Criteria.where("status").is(status.name())
            .and("category").ne(null)
    );

    // 2단계: $group
    GroupOperation groupStage = Aggregation.group("category")
        .count().as("postCount")
        .max("publishedAt").as("latestPostDate");

    // 3단계: $sort
    SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "postCount");

    // 파이프라인 조립
    Aggregation aggregation = Aggregation.newAggregation(
        matchStage,
        groupStage,
        sortStage
    );

    // 실행
    AggregationResults<CategoryStatsResult> results = mongoTemplate.aggregate(
        aggregation,
        Post.class,
        CategoryStatsResult.class
    );

    return results.getMappedResults().stream()
        .map(r -> new CategoryStats(r.id(), r.postCount(), r.latestPostDate()))
        .toList();
}
```

## 인기 태그 집계 ($unwind 사용)

### MongoDB 쿼리

```javascript
db.posts.aggregate([
    // 1. 필터링
    { $match: { status: "PUBLISHED" } },

    // 2. 배열 펼치기: tags: ["java", "spring"] →
    //    문서1: { tags: "java" }, 문서2: { tags: "spring" }
    { $unwind: "$tags" },

    // 3. 태그별 그룹화
    { $group: {
        _id: "$tags",
        postCount: { $sum: 1 },
        totalViews: { $sum: "$viewCount" }
    }},

    // 4. 정렬
    { $sort: { postCount: -1 } },

    // 5. 상위 10개만
    { $limit: 10 }
])
```

### Spring Data MongoDB 구현

```java
@Override
public List<TagStats> aggregatePopularTags(PostStatus status, int limit) {
    MatchOperation matchStage = Aggregation.match(
        Criteria.where("status").is(status.name())
    );

    // $unwind: 배열을 개별 문서로 분리
    UnwindOperation unwindStage = Aggregation.unwind("tags");

    GroupOperation groupStage = Aggregation.group("tags")
        .count().as("postCount")
        .sum("viewCount").as("totalViews");

    SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "postCount");
    LimitOperation limitStage = Aggregation.limit(limit);

    Aggregation aggregation = Aggregation.newAggregation(
        matchStage,
        unwindStage,
        groupStage,
        sortStage,
        limitStage
    );

    AggregationResults<TagStatsResult> results = mongoTemplate.aggregate(
        aggregation,
        Post.class,
        TagStatsResult.class
    );

    return results.getMappedResults().stream()
        .map(r -> new TagStats(r.id(), r.postCount(), r.totalViews()))
        .toList();
}
```

## 트렌딩 점수 계산 ($addFields 사용)

### 점수 공식

```
baseScore = viewCount + (likeCount × 3) + (commentCount × 5)
timeDecay = 2^(-hoursElapsed / halfLife)
trendingScore = baseScore × timeDecay
```

### MongoDB 쿼리

```javascript
db.posts.aggregate([
    // 1. 필터링
    { $match: {
        status: "PUBLISHED",
        publishedAt: { $gte: startDate }
    }},

    // 2. 점수 계산 필드 추가
    { $addFields: {
        trendingScore: {
            $multiply: [
                // baseScore
                { $add: [
                    "$viewCount",
                    { $multiply: ["$likeCount", 3] },
                    { $multiply: [{ $ifNull: ["$commentCount", 0] }, 5] }
                ]},
                // timeDecay
                { $pow: [
                    2,
                    { $divide: [
                        { $multiply: [
                            -1,
                            { $divide: [
                                { $subtract: [new Date(), "$publishedAt"] },
                                3600000  // 밀리초 → 시간
                            ]}
                        ]},
                        48  // halfLife (시간)
                    ]}
                ]}
            ]
        }
    }},

    // 3. 점수 기준 정렬
    { $sort: { trendingScore: -1 } },

    // 4. 페이징
    { $skip: 0 },
    { $limit: 10 }
])
```

### Spring Data MongoDB 구현

```java
@Override
public Page<Post> aggregateTrendingPosts(PostStatus status, LocalDateTime startDate,
                                          double halfLifeHours, int page, int size) {
    long nowMillis = System.currentTimeMillis();

    MatchOperation matchStage = Aggregation.match(
        Criteria.where("status").is(status.name())
            .and("publishedAt").gte(startDate)
    );

    // 복잡한 수식은 Document로 직접 작성
    Document addFieldsDoc = new Document("$addFields", new Document("trendingScore",
        new Document("$multiply", Arrays.asList(
            // baseScore
            new Document("$add", Arrays.asList(
                new Document("$ifNull", Arrays.asList("$viewCount", 0)),
                new Document("$multiply", Arrays.asList(
                    new Document("$ifNull", Arrays.asList("$likeCount", 0)), 3)),
                new Document("$multiply", Arrays.asList(
                    new Document("$ifNull", Arrays.asList("$commentCount", 0)), 5))
            )),
            // timeDecay
            new Document("$pow", Arrays.asList(2,
                new Document("$divide", Arrays.asList(
                    new Document("$multiply", Arrays.asList(-1,
                        new Document("$divide", Arrays.asList(
                            new Document("$subtract", Arrays.asList(
                                nowMillis,
                                new Document("$ifNull", Arrays.asList(
                                    new Document("$toLong", "$publishedAt"), nowMillis))
                            )),
                            3600000.0
                        ))
                    )),
                    halfLifeHours
                ))
            ))
        ))
    ));

    SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "trendingScore");
    SkipOperation skipStage = Aggregation.skip((long) page * size);
    LimitOperation limitStage = Aggregation.limit(size);

    Aggregation aggregation = Aggregation.newAggregation(
        matchStage,
        ctx -> addFieldsDoc,  // 커스텀 단계
        sortStage,
        skipStage,
        limitStage
    );

    AggregationResults<Post> results = mongoTemplate.aggregate(
        aggregation, Post.class, Post.class);

    return new PageImpl<>(results.getMappedResults(), PageRequest.of(page, size), total);
}
```

## 성능 최적화 포인트

### 1. $match를 파이프라인 앞에 배치

```java
// 좋은 예: 먼저 필터링 후 그룹화
Aggregation.newAggregation(
    Aggregation.match(criteria),  // 먼저
    Aggregation.group("category")
);

// 나쁜 예: 모든 데이터 그룹화 후 필터링
Aggregation.newAggregation(
    Aggregation.group("category"),
    Aggregation.match(criteria)  // 비효율
);
```

### 2. 인덱스 활용

```java
// $match에서 인덱스 필드 사용
Criteria.where("status").is("PUBLISHED")  // 인덱스 존재
    .and("publishedAt").gte(startDate)    // 복합 인덱스
```

### 3. $project로 필요한 필드만 선택

```java
ProjectionOperation projectStage = Aggregation.project()
    .and("title").as("title")
    .and("viewCount").as("views");
```

## 핵심 포인트

| 연산자 | 사용 시나리오 |
|--------|--------------|
| $match | 필터링 (항상 먼저) |
| $group | COUNT, SUM, AVG 등 집계 |
| $unwind | 배열 필드 펼치기 |
| $addFields | 계산된 필드 추가 |
| $sort + $skip + $limit | 페이징 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepositoryCustomImpl.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/dto/CategoryStats.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/dto/TagStats.java`
