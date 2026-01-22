# 검색 결과 정렬 (Search Ranking)

## 개요

검색 결과의 정렬은 사용자 경험에 큰 영향을 미칩니다. 이 문서에서는 Elasticsearch의 정렬 메커니즘과 관련성 점수(Relevance Score) 커스터마이징 방법을 설명합니다.

## 정렬 옵션

### 기본 정렬 옵션

| 옵션 | 설명 | 용도 |
|------|------|------|
| `relevance` | 검색 관련성 점수 순 | 기본 검색 |
| `price_asc` | 가격 낮은 순 | 가성비 중시 |
| `price_desc` | 가격 높은 순 | 프리미엄 상품 |
| `newest` | 최신 등록 순 | 신상품 확인 |
| `popularity` | 인기 순 (판매량) | 베스트셀러 |
| `rating` | 평점 높은 순 | 품질 중시 |

## Sort 구현

### SortBuilder

```java
@Component
public class SearchSortBuilder {

    public List<SortOptions> buildSort(String sortOption) {
        if (sortOption == null) {
            sortOption = "relevance";
        }

        return switch (sortOption) {
            case "relevance" -> List.of(
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))
            );

            case "price_asc" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Asc))),
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))  // 동점 시 관련성
            );

            case "price_desc" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Desc))),
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))
            );

            case "newest" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );

            case "popularity" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("salesCount").order(SortOrder.Desc)))
            );

            case "rating" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("rating").order(SortOrder.Desc))),
                SortOptions.of(s -> s.field(f -> f.field("reviewCount").order(SortOrder.Desc)))
            );

            default -> List.of(
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))
            );
        };
    }
}
```

## 관련성 점수 (Relevance Score)

### TF-IDF와 BM25

Elasticsearch는 기본적으로 **BM25** 알고리즘을 사용합니다:

```
score = boost * IDF * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * (docLength / avgDocLength)))
```

- **TF (Term Frequency)**: 검색어가 문서에 등장하는 빈도
- **IDF (Inverse Document Frequency)**: 검색어의 희소성
- **Field Length**: 필드 길이 (짧은 필드에서 매칭이 더 중요)

### 필드별 가중치 (Boosting)

```java
Query.of(q -> q
    .multiMatch(m -> m
        .query(keyword)
        .fields(
            "name^3",         // 상품명 3배 가중치
            "brand.name^2",   // 브랜드명 2배 가중치
            "description"     // 설명 1배 (기본)
        )
        .type(TextQueryType.BestFields)
    )
)
```

## Function Score Query

비즈니스 로직을 반영한 점수 커스터마이징:

### 기본 구조

```java
Query functionScoreQuery = Query.of(q -> q
    .functionScore(fs -> fs
        .query(originalQuery)
        .functions(
            FunctionScore.of(fn -> fn
                .filter(Query.of(fq -> fq.term(t -> t.field("isPromoted").value(true))))
                .weight(2.0)
            ),
            FunctionScore.of(fn -> fn
                .fieldValueFactor(fvf -> fvf
                    .field("popularity")
                    .factor(1.2)
                    .modifier(FieldValueFactorModifier.Log1p)
                )
            ),
            FunctionScore.of(fn -> fn
                .gauss(g -> g
                    .field("price")
                    .placement(p -> p
                        .origin(JsonData.of(100000))
                        .scale(JsonData.of(50000))
                    )
                )
            )
        )
        .scoreMode(FunctionScoreMode.Sum)
        .boostMode(FunctionBoostMode.Multiply)
    )
);
```

### Function Score 유형

#### 1. Weight

조건 매칭 시 고정 가중치:

```java
FunctionScore.of(fn -> fn
    .filter(Query.of(fq -> fq
        .term(t -> t.field("isPromoted").value(true))
    ))
    .weight(1.5)  // 프로모션 상품 1.5배
)
```

#### 2. Field Value Factor

필드 값을 점수에 반영:

```java
FunctionScore.of(fn -> fn
    .fieldValueFactor(fvf -> fvf
        .field("salesCount")
        .factor(0.1)
        .modifier(FieldValueFactorModifier.Log1p)
        .missing(1)  // 값이 없을 경우 기본값
    )
)
```

**Modifier 옵션**:
| Modifier | 설명 |
|----------|------|
| `none` | 원본 값 사용 |
| `log` | log(value) |
| `log1p` | log(1 + value) |
| `log2p` | log(2 + value) |
| `ln` | ln(value) |
| `ln1p` | ln(1 + value) |
| `ln2p` | ln(2 + value) |
| `square` | value^2 |
| `sqrt` | sqrt(value) |
| `reciprocal` | 1/value |

#### 3. Decay Functions (가우시안, 선형, 지수)

거리/시간에 따른 점수 감소:

```java
// 가격 기준 가우시안 감쇠 (목표 가격에 가까울수록 높은 점수)
FunctionScore.of(fn -> fn
    .gauss(g -> g
        .field("price")
        .placement(p -> p
            .origin(JsonData.of(100000))  // 목표 가격
            .scale(JsonData.of(50000))    // 감쇠 범위
            .offset(JsonData.of(10000))   // 감쇠 시작 거리
            .decay(0.5)                    // 감쇠율
        )
    )
)

// 날짜 기준 (최신 상품 우선)
FunctionScore.of(fn -> fn
    .exp(e -> e
        .field("createdAt")
        .placement(p -> p
            .origin(JsonData.of("now"))
            .scale(JsonData.of("30d"))
        )
    )
)
```

#### 4. Script Score

커스텀 스크립트로 점수 계산:

```java
FunctionScore.of(fn -> fn
    .scriptScore(ss -> ss
        .script(s -> s
            .source("_score * doc['rating'].value * Math.log(2 + doc['reviewCount'].value)")
        )
    )
)
```

## 복합 랭킹 전략

### 비즈니스 로직 기반 랭킹

```java
@Service
@RequiredArgsConstructor
public class BusinessRankingService {

    public Query buildRankedQuery(String keyword, RankingContext context) {
        Query baseQuery = buildBaseQuery(keyword);

        List<FunctionScore> functions = new ArrayList<>();

        // 1. 프로모션 상품 우선
        functions.add(FunctionScore.of(fn -> fn
            .filter(Query.of(q -> q.term(t -> t.field("isPromoted").value(true))))
            .weight(2.0)
        ));

        // 2. 재고 충분한 상품 우선
        functions.add(FunctionScore.of(fn -> fn
            .filter(Query.of(q -> q.range(r -> r.field("stock").gte(JsonData.of(10)))))
            .weight(1.2)
        ));

        // 3. 인기도 반영
        functions.add(FunctionScore.of(fn -> fn
            .fieldValueFactor(fvf -> fvf
                .field("salesCount")
                .modifier(FieldValueFactorModifier.Log1p)
                .factor(0.1)
            )
        ));

        // 4. 평점 반영
        functions.add(FunctionScore.of(fn -> fn
            .fieldValueFactor(fvf -> fvf
                .field("rating")
                .modifier(FieldValueFactorModifier.None)
                .factor(0.5)
            )
        ));

        // 5. 신상품 약간 우선 (최근 7일)
        functions.add(FunctionScore.of(fn -> fn
            .gauss(g -> g
                .field("createdAt")
                .placement(p -> p
                    .origin(JsonData.of("now"))
                    .scale(JsonData.of("7d"))
                    .decay(0.8)
                )
            )
            .weight(1.1)
        ));

        return Query.of(q -> q
            .functionScore(fs -> fs
                .query(baseQuery)
                .functions(functions)
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Multiply)
                .maxBoost(10.0)  // 최대 부스트 제한
            )
        );
    }
}
```

### 개인화 랭킹

사용자 선호도 기반:

```java
public Query buildPersonalizedQuery(String keyword, UserPreference preference) {
    List<FunctionScore> functions = new ArrayList<>();

    // 선호 카테고리 부스트
    if (preference.getPreferredCategories() != null) {
        for (Long categoryId : preference.getPreferredCategories()) {
            functions.add(FunctionScore.of(fn -> fn
                .filter(Query.of(q -> q
                    .nested(n -> n
                        .path("category")
                        .query(nq -> nq.term(t -> t.field("category.id").value(categoryId)))
                    )
                ))
                .weight(1.3)
            ));
        }
    }

    // 선호 브랜드 부스트
    if (preference.getPreferredBrands() != null) {
        for (Long brandId : preference.getPreferredBrands()) {
            functions.add(FunctionScore.of(fn -> fn
                .filter(Query.of(q -> q.term(t -> t.field("brand.id").value(brandId))))
                .weight(1.2)
            ));
        }
    }

    // 가격대 선호도
    if (preference.getPriceRange() != null) {
        functions.add(FunctionScore.of(fn -> fn
            .gauss(g -> g
                .field("price")
                .placement(p -> p
                    .origin(JsonData.of(preference.getPriceRange().getAverage()))
                    .scale(JsonData.of(preference.getPriceRange().getRange() / 2))
                )
            )
            .weight(1.1)
        ));
    }

    return Query.of(q -> q
        .functionScore(fs -> fs
            .query(buildBaseQuery(keyword))
            .functions(functions)
            .scoreMode(FunctionScoreMode.Sum)
            .boostMode(FunctionBoostMode.Multiply)
        )
    );
}
```

## A/B 테스트

랭킹 알고리즘 비교:

```java
@Service
@RequiredArgsConstructor
public class RankingExperimentService {

    private final Random random = new Random();

    public Query buildQuery(String keyword, String userId) {
        // 사용자를 그룹에 할당
        String group = assignGroup(userId);

        return switch (group) {
            case "control" -> buildControlQuery(keyword);
            case "experiment_a" -> buildExperimentAQuery(keyword);
            case "experiment_b" -> buildExperimentBQuery(keyword);
            default -> buildControlQuery(keyword);
        };
    }

    private String assignGroup(String userId) {
        int hash = Math.abs(userId.hashCode() % 100);
        if (hash < 80) return "control";      // 80%
        if (hash < 90) return "experiment_a"; // 10%
        return "experiment_b";                 // 10%
    }

    // 클릭률, 전환율 등 메트릭 수집
    public void trackExperiment(String userId, String group, String action) {
        experimentTracker.track(group, action);
    }
}
```

## 디버깅

### Explain API

점수 계산 과정 확인:

```java
SearchRequest request = SearchRequest.of(s -> s
    .index(INDEX_NAME)
    .query(query)
    .explain(true)  // 점수 설명 포함
);
```

응답 예시:
```json
{
  "_explanation": {
    "value": 15.2,
    "description": "sum of:",
    "details": [
      {
        "value": 5.0,
        "description": "weight(name:이어폰 in 0)"
      },
      {
        "value": 10.2,
        "description": "function score, computed from: fieldValueFactor(salesCount, Log1p)"
      }
    ]
  }
}
```

## Best Practices

1. **Score Mode 선택**:
   - `sum`: 모든 점수 합산
   - `avg`: 평균
   - `max`: 최대값만 사용
   - `multiply`: 곱셈

2. **Boost Mode 선택**:
   - `multiply`: 원본 점수와 곱셈 (기본)
   - `sum`: 원본 점수와 합산
   - `replace`: 원본 점수 대체

3. **max_boost 설정**: 극단적인 부스팅 방지

4. **테스트**: A/B 테스트로 효과 검증

## 관련 문서

- [Search Service](./search-service.md) - 검색 서비스 로직
- [Faceted Search](./faceted-search.md) - 필터 검색
- [Product Index Design](./product-index-design.md) - 인덱스 설계
