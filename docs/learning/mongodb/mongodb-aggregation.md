# MongoDB Aggregation Framework

## 학습 목표
- Aggregation Pipeline 개념 이해
- 주요 스테이지: $match, $group, $lookup, $unwind, $project
- Portal Universe의 실제 Aggregation 구현 분석
- 성능 최적화 패턴 학습

---

## 1. Aggregation Pipeline 개념

Aggregation Pipeline은 **Unix 파이프라인**처럼 여러 단계를 거쳐 데이터를 변환합니다.

```
[Documents] → [$match] → [$group] → [$sort] → [$project] → [Results]
```

### 1.1 파이프라인 흐름

```javascript
db.posts.aggregate([
  { $match: { status: "PUBLISHED" } },     // 1단계: 필터링
  { $group: { _id: "$category", count: { $sum: 1 } } },  // 2단계: 그룹화
  { $sort: { count: -1 } },                // 3단계: 정렬
  { $limit: 10 }                           // 4단계: 제한
])
```

### 1.2 주요 특징

| 특징 | 설명 |
|------|------|
| **순차 처리** | 각 단계의 출력이 다음 단계의 입력 |
| **서버 사이드** | 데이터베이스에서 처리 (클라이언트로 전송 최소화) |
| **인덱스 활용** | $match, $sort 초기 단계는 인덱스 사용 가능 |
| **메모리 제한** | 기본 100MB (allowDiskUse로 확장) |

---

## 2. 핵심 스테이지

### 2.1 $match - 필터링

SQL의 `WHERE`에 해당합니다.

```javascript
// 기본 필터
{ $match: { status: "PUBLISHED" } }

// 복합 조건
{ $match: {
  status: "PUBLISHED",
  publishedAt: { $gte: ISODate("2024-01-01") },
  viewCount: { $gte: 100 }
}}

// 논리 연산
{ $match: {
  $or: [
    { category: "tech" },
    { tags: { $in: ["mongodb", "database"] } }
  ]
}}
```

**Best Practice:** $match를 **파이프라인 초반**에 배치하여 처리 데이터량 최소화

### 2.2 $group - 그룹화 및 집계

SQL의 `GROUP BY`에 해당합니다.

```javascript
// 기본 그룹화
{
  $group: {
    _id: "$category",           // 그룹 키 (필수)
    count: { $sum: 1 },         // 문서 수
    totalViews: { $sum: "$viewCount" },  // 합계
    avgViews: { $avg: "$viewCount" },    // 평균
    maxLikes: { $max: "$likeCount" },    // 최대값
    minLikes: { $min: "$likeCount" },    // 최소값
    posts: { $push: "$title" }  // 배열로 수집
  }
}

// 복합 그룹 키
{
  $group: {
    _id: {
      category: "$category",
      year: { $year: "$publishedAt" }
    },
    count: { $sum: 1 }
  }
}

// 전체 통계 (_id: null)
{
  $group: {
    _id: null,
    totalPosts: { $sum: 1 },
    totalViews: { $sum: "$viewCount" }
  }
}
```

#### 집계 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `$sum` | 합계 | `{ $sum: "$viewCount" }` 또는 `{ $sum: 1 }` (카운트) |
| `$avg` | 평균 | `{ $avg: "$viewCount" }` |
| `$min` | 최소값 | `{ $min: "$publishedAt" }` |
| `$max` | 최대값 | `{ $max: "$likeCount" }` |
| `$first` | 첫 번째 값 | `{ $first: "$title" }` |
| `$last` | 마지막 값 | `{ $last: "$title" }` |
| `$push` | 배열로 수집 | `{ $push: "$tags" }` |
| `$addToSet` | 중복 없이 수집 | `{ $addToSet: "$authorId" }` |

### 2.3 $project - 필드 선택 및 변환

SQL의 `SELECT`에 해당합니다.

```javascript
// 필드 포함/제외
{
  $project: {
    title: 1,           // 포함
    authorName: 1,      // 포함
    _id: 0,             // 제외
    content: 0          // 제외
  }
}

// 필드 이름 변경
{
  $project: {
    postTitle: "$title",
    writer: "$authorName"
  }
}

// 계산된 필드
{
  $project: {
    title: 1,
    engagement: { $add: ["$viewCount", "$likeCount", "$commentCount"] },
    isPopular: { $gte: ["$viewCount", 1000] }
  }
}

// 조건부 필드
{
  $project: {
    title: 1,
    status: 1,
    statusLabel: {
      $cond: {
        if: { $eq: ["$status", "PUBLISHED"] },
        then: "공개",
        else: "비공개"
      }
    }
  }
}
```

### 2.4 $unwind - 배열 펼치기

배열의 각 요소를 별도 문서로 분리합니다.

```javascript
// 원본 문서
{
  _id: "post1",
  title: "MongoDB",
  tags: ["mongodb", "nosql", "database"]
}

// $unwind 적용
{ $unwind: "$tags" }

// 결과: 3개의 문서
{ _id: "post1", title: "MongoDB", tags: "mongodb" }
{ _id: "post1", title: "MongoDB", tags: "nosql" }
{ _id: "post1", title: "MongoDB", tags: "database" }
```

**옵션:**

```javascript
{
  $unwind: {
    path: "$tags",
    preserveNullAndEmptyArrays: true,  // 빈 배열/null도 유지
    includeArrayIndex: "tagIndex"      // 인덱스 필드 추가
  }
}
```

### 2.5 $lookup - 조인

다른 컬렉션과 조인합니다.

```javascript
// 기본 lookup (LEFT OUTER JOIN)
{
  $lookup: {
    from: "users",           // 조인할 컬렉션
    localField: "authorId",  // 현재 컬렉션 필드
    foreignField: "_id",     // 대상 컬렉션 필드
    as: "authorDetails"      // 결과 필드명 (배열)
  }
}

// 결과
{
  _id: "post1",
  title: "MongoDB",
  authorId: "user1",
  authorDetails: [           // 배열로 반환
    { _id: "user1", name: "홍길동", email: "hong@example.com" }
  ]
}

// 단일 객체로 변환 (배열의 첫 번째)
{ $unwind: { path: "$authorDetails", preserveNullAndEmptyArrays: true } }
```

**Pipeline lookup (복잡한 조인):**

```javascript
{
  $lookup: {
    from: "comments",
    let: { postId: "$_id" },  // 변수 정의
    pipeline: [
      { $match: {
        $expr: { $eq: ["$postId", "$$postId"] },
        isDeleted: false
      }},
      { $sort: { createdAt: -1 } },
      { $limit: 5 }
    ],
    as: "recentComments"
  }
}
```

### 2.6 $sort - 정렬

```javascript
// 단일 필드 정렬
{ $sort: { publishedAt: -1 } }  // -1: 내림차순, 1: 오름차순

// 복합 정렬
{ $sort: { category: 1, viewCount: -1 } }

// 텍스트 검색 점수 정렬
{ $sort: { score: { $meta: "textScore" } } }
```

### 2.7 $skip / $limit - 페이지네이션

```javascript
// 페이지 3, 페이지당 10개
{ $skip: 20 },
{ $limit: 10 }
```

### 2.8 $addFields - 필드 추가

기존 필드를 유지하면서 새 필드를 추가합니다.

```javascript
{
  $addFields: {
    totalEngagement: {
      $add: ["$viewCount", "$likeCount", "$commentCount"]
    },
    isHot: { $gte: ["$viewCount", 1000] }
  }
}
```

---

## 3. Portal Universe 실제 구현

### 3.1 카테고리별 통계

```java
// PostRepositoryCustomImpl.java

/**
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

### 3.2 인기 태그 집계 ($unwind 활용)

```java
/**
 * db.posts.aggregate([
 *   { $match: { status: "PUBLISHED" } },
 *   { $unwind: "$tags" },           // 태그 배열 펼치기
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
public List<TagStats> aggregatePopularTags(PostStatus status, int limit) {
    MatchOperation matchStage = Aggregation.match(
        Criteria.where("status").is(status.name())
    );

    // $unwind: tags 배열의 각 요소를 별도 문서로
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

### 3.3 트렌딩 게시물 ($addFields 활용)

```java
/**
 * 트렌딩 점수 공식:
 * baseScore = viewCount + (likeCount × 3) + (commentCount × 5)
 * timeDecay = 2^(-hoursElapsed / halfLife)
 * trendingScore = baseScore × timeDecay
 */
@Override
public Page<Post> aggregateTrendingPosts(PostStatus status, LocalDateTime startDate,
                                          double halfLifeHours, int page, int size) {
    long nowMillis = System.currentTimeMillis();

    // 1단계: $match
    MatchOperation matchStage = Aggregation.match(
        Criteria.where("status").is(status.name())
                .and("publishedAt").gte(startDate)
    );

    // 2단계: $addFields - 트렌딩 점수 계산
    Document addFieldsDoc = new Document("$addFields", new Document("trendingScore",
        new Document("$multiply", Arrays.asList(
            // baseScore
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
                            new Document("$subtract", Arrays.asList(
                                nowMillis,
                                new Document("$ifNull", Arrays.asList(
                                    new Document("$toLong", "$publishedAt"),
                                    nowMillis
                                ))
                            )),
                            3600000.0  // 1시간 = 3600000ms
                        ))
                    )),
                    halfLifeHours
                ))
            ))
        ))
    ));

    // 3단계: $sort
    SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "trendingScore");

    // 4단계: $skip, $limit
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
        aggregation,
        Post.class,
        Post.class
    );

    return new PageImpl<>(results.getMappedResults(), PageRequest.of(page, size), total);
}
```

---

## 4. 고급 패턴

### 4.1 Facet - 다중 집계

하나의 쿼리로 여러 집계를 동시에 수행합니다.

```javascript
db.posts.aggregate([
  { $match: { status: "PUBLISHED" } },
  {
    $facet: {
      // 카테고리별 통계
      "byCategory": [
        { $group: { _id: "$category", count: { $sum: 1 } } },
        { $sort: { count: -1 } }
      ],
      // 월별 통계
      "byMonth": [
        { $group: {
          _id: { $month: "$publishedAt" },
          count: { $sum: 1 }
        }},
        { $sort: { "_id": 1 } }
      ],
      // 인기 작성자
      "topAuthors": [
        { $group: {
          _id: "$authorId",
          authorName: { $first: "$authorName" },
          totalViews: { $sum: "$viewCount" }
        }},
        { $sort: { totalViews: -1 } },
        { $limit: 5 }
      ]
    }
  }
])
```

### 4.2 Bucket - 범위별 그룹화

```javascript
// 조회수 범위별 그룹화
db.posts.aggregate([
  { $match: { status: "PUBLISHED" } },
  {
    $bucket: {
      groupBy: "$viewCount",
      boundaries: [0, 100, 500, 1000, 5000, Infinity],
      default: "Other",
      output: {
        count: { $sum: 1 },
        titles: { $push: "$title" }
      }
    }
  }
])

// 결과
[
  { _id: 0, count: 45, titles: [...] },      // 0-99
  { _id: 100, count: 120, titles: [...] },   // 100-499
  { _id: 500, count: 80, titles: [...] },    // 500-999
  { _id: 1000, count: 35, titles: [...] },   // 1000-4999
  { _id: 5000, count: 10, titles: [...] }    // 5000+
]
```

### 4.3 graphLookup - 재귀적 조인

대댓글 구조 조회에 유용합니다.

```javascript
db.comments.aggregate([
  { $match: { postId: "post123", parentCommentId: null } },  // 루트 댓글
  {
    $graphLookup: {
      from: "comments",
      startWith: "$_id",
      connectFromField: "_id",
      connectToField: "parentCommentId",
      as: "replies",
      maxDepth: 5,                    // 최대 깊이
      depthField: "depth",
      restrictSearchWithMatch: { isDeleted: false }
    }
  }
])
```

---

## 5. 성능 최적화

### 5.1 파이프라인 순서

```javascript
// Good: $match 먼저
db.posts.aggregate([
  { $match: { status: "PUBLISHED" } },  // 먼저 필터링
  { $group: { ... } }
])

// Bad: 전체 데이터에 $group 적용
db.posts.aggregate([
  { $group: { ... } },  // 모든 문서 처리
  { $match: { ... } }   // 늦은 필터링
])
```

### 5.2 인덱스 활용

```javascript
// $match와 $sort가 초기에 있고 인덱스와 일치하면 활용
db.posts.createIndex({ status: 1, publishedAt: -1 })

db.posts.aggregate([
  { $match: { status: "PUBLISHED" } },
  { $sort: { publishedAt: -1 } },       // 인덱스 사용
  { $limit: 10 }
])
```

### 5.3 $project로 조기 필드 제한

```javascript
db.posts.aggregate([
  { $match: { status: "PUBLISHED" } },
  { $project: { title: 1, viewCount: 1 } },  // 필요한 필드만
  { $group: { ... } }
])
```

### 5.4 allowDiskUse

```javascript
// 대용량 데이터 처리 시
db.posts.aggregate([
  { ... },
  { ... }
], { allowDiskUse: true })  // 메모리 100MB 초과 시 디스크 사용
```

---

## 6. 핵심 정리

| 스테이지 | 용도 | SQL 대응 |
|----------|------|----------|
| `$match` | 필터링 | WHERE |
| `$group` | 그룹화, 집계 | GROUP BY |
| `$project` | 필드 선택/변환 | SELECT |
| `$unwind` | 배열 펼치기 | - |
| `$lookup` | 조인 | JOIN |
| `$sort` | 정렬 | ORDER BY |
| `$skip` / `$limit` | 페이지네이션 | OFFSET / LIMIT |
| `$addFields` | 필드 추가 | - |

---

## 다음 학습

- [MongoDB 인덱스](./mongodb-indexes.md)
- [MongoDB Transactions](./mongodb-transactions.md)
- [MongoDB Portal Universe 분석](./mongodb-portal-universe.md)

---

## 참고 자료

- [MongoDB Aggregation](https://www.mongodb.com/docs/manual/aggregation/)
- [Aggregation Pipeline Stages](https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/)
- [Aggregation Pipeline Optimization](https://www.mongodb.com/docs/manual/core/aggregation-pipeline-optimization/)
