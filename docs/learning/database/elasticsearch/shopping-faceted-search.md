# Faceted Search (필터 검색)

## 개요

Faceted Search는 카테고리, 가격 범위, 브랜드 등 다양한 조건으로 검색 결과를 필터링하고, 각 필터의 결과 수를 함께 제공하는 기능입니다. 이 문서에서는 Elasticsearch Aggregation을 활용한 Faceted Search 구현을 설명합니다.

## Faceted Search 개념

```
검색 결과: "이어폰" (150건)

┌─────────────────────────────────────────────┐
│ 카테고리                                     │
│ ├── 무선 이어폰 (80)                         │
│ ├── 유선 이어폰 (45)                         │
│ └── 이어폰 액세서리 (25)                     │
├─────────────────────────────────────────────┤
│ 가격대                                       │
│ ├── ~5만원 (45)                             │
│ ├── 5만원~10만원 (60)                       │
│ ├── 10만원~20만원 (35)                      │
│ └── 20만원~ (10)                            │
├─────────────────────────────────────────────┤
│ 브랜드                                       │
│ ├── Samsung (40)                            │
│ ├── Apple (35)                              │
│ ├── Sony (30)                               │
│ └── 기타 (45)                               │
└─────────────────────────────────────────────┘
```

## 인덱스 설계 (Faceted Search용)

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "price": { "type": "double" },
      "category": {
        "type": "nested",
        "properties": {
          "id": { "type": "long" },
          "name": { "type": "keyword" },
          "depth": { "type": "integer" }
        }
      },
      "brand": {
        "type": "object",
        "properties": {
          "id": { "type": "long" },
          "name": { "type": "keyword" }
        }
      },
      "attributes": {
        "type": "nested",
        "properties": {
          "name": { "type": "keyword" },
          "value": { "type": "keyword" }
        }
      },
      "rating": { "type": "float" },
      "inStock": { "type": "boolean" }
    }
  }
}
```

## FacetedSearchService 구현

### DTO 정의

```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetedSearchRequest {
    private String keyword;
    private List<Long> categoryIds;
    private List<Long> brandIds;
    private Double minPrice;
    private Double maxPrice;
    private Float minRating;
    private Boolean inStock;
    private Map<String, List<String>> attributes;  // color: [black, white]

    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
    private String sort;
}

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetedSearchResponse<T> {
    private List<T> results;
    private long totalHits;
    private int page;
    private int size;
    private int totalPages;
    private Facets facets;
}

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Facets {
    private List<FacetBucket> categories;
    private List<FacetBucket> brands;
    private List<FacetBucket> priceRanges;
    private List<FacetBucket> ratings;
    private Map<String, List<FacetBucket>> attributes;
}

@Getter
@AllArgsConstructor
public class FacetBucket {
    private String key;
    private String label;
    private long count;
}
```

### Service 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FacetedSearchService {

    private static final String INDEX_NAME = "products";

    private final ElasticsearchClient esClient;

    public FacetedSearchResponse<ProductSearchResult> search(FacetedSearchRequest request) {
        try {
            SearchRequest esRequest = buildSearchRequest(request);
            SearchResponse<ProductDocument> response = esClient.search(esRequest, ProductDocument.class);

            // 검색 결과 매핑
            List<ProductSearchResult> results = mapResults(response);

            // Facet 결과 매핑
            Facets facets = mapFacets(response.aggregations());

            return FacetedSearchResponse.<ProductSearchResult>builder()
                .results(results)
                .totalHits(response.hits().total().value())
                .page(request.getPage())
                .size(request.getSize())
                .totalPages((int) Math.ceil((double) response.hits().total().value() / request.getSize()))
                .facets(facets)
                .build();

        } catch (IOException e) {
            log.error("Faceted search failed", e);
            throw new CustomBusinessException(ShoppingErrorCode.SEARCH_FAILED);
        }
    }

    private SearchRequest buildSearchRequest(FacetedSearchRequest request) {
        return SearchRequest.of(s -> s
            .index(INDEX_NAME)
            .query(buildQuery(request))
            .aggregations(buildAggregations())
            .from(request.getPage() * request.getSize())
            .size(request.getSize())
            .sort(buildSort(request))
        );
    }

    private Query buildQuery(FacetedSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // 키워드 검색
        if (StringUtils.hasText(request.getKeyword())) {
            mustQueries.add(Query.of(q -> q
                .multiMatch(m -> m
                    .query(request.getKeyword())
                    .fields("name^3", "description", "brand.name")
                )
            ));
        }

        // 카테고리 필터
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            filterQueries.add(Query.of(q -> q
                .nested(n -> n
                    .path("category")
                    .query(nq -> nq
                        .terms(t -> t
                            .field("category.id")
                            .terms(ts -> ts.value(
                                request.getCategoryIds().stream()
                                    .map(FieldValue::of)
                                    .toList()
                            ))
                        )
                    )
                )
            ));
        }

        // 브랜드 필터
        if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
            filterQueries.add(Query.of(q -> q
                .terms(t -> t
                    .field("brand.id")
                    .terms(ts -> ts.value(
                        request.getBrandIds().stream()
                            .map(FieldValue::of)
                            .toList()
                    ))
                )
            ));
        }

        // 가격 범위 필터
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q
                .range(r -> {
                    var builder = r.field("price");
                    if (request.getMinPrice() != null) {
                        builder.gte(JsonData.of(request.getMinPrice()));
                    }
                    if (request.getMaxPrice() != null) {
                        builder.lte(JsonData.of(request.getMaxPrice()));
                    }
                    return builder;
                })
            ));
        }

        // 평점 필터
        if (request.getMinRating() != null) {
            filterQueries.add(Query.of(q -> q
                .range(r -> r
                    .field("rating")
                    .gte(JsonData.of(request.getMinRating()))
                )
            ));
        }

        // 재고 필터
        if (request.getInStock() != null && request.getInStock()) {
            filterQueries.add(Query.of(q -> q
                .term(t -> t.field("inStock").value(true))
            ));
        }

        // 속성 필터 (색상, 사이즈 등)
        if (request.getAttributes() != null) {
            for (var entry : request.getAttributes().entrySet()) {
                String attrName = entry.getKey();
                List<String> attrValues = entry.getValue();

                filterQueries.add(Query.of(q -> q
                    .nested(n -> n
                        .path("attributes")
                        .query(nq -> nq
                            .bool(b -> b
                                .must(
                                    Query.of(qq -> qq.term(t -> t.field("attributes.name").value(attrName))),
                                    Query.of(qq -> qq.terms(t -> t
                                        .field("attributes.value")
                                        .terms(ts -> ts.value(
                                            attrValues.stream().map(FieldValue::of).toList()
                                        ))
                                    ))
                                )
                            )
                        )
                    )
                ));
            }
        }

        return Query.of(q -> q
            .bool(b -> {
                if (!mustQueries.isEmpty()) b.must(mustQueries);
                if (!filterQueries.isEmpty()) b.filter(filterQueries);
                if (mustQueries.isEmpty()) b.must(Query.of(qq -> qq.matchAll(m -> m)));
                return b;
            })
        );
    }

    private Map<String, Aggregation> buildAggregations() {
        Map<String, Aggregation> aggs = new HashMap<>();

        // 카테고리 집계
        aggs.put("categories", Aggregation.of(a -> a
            .nested(n -> n.path("category"))
            .aggregations("category_names", Aggregation.of(aa -> aa
                .terms(t -> t.field("category.name").size(50))
            ))
        ));

        // 브랜드 집계
        aggs.put("brands", Aggregation.of(a -> a
            .terms(t -> t.field("brand.name").size(20))
        ));

        // 가격 범위 집계
        aggs.put("price_ranges", Aggregation.of(a -> a
            .range(r -> r
                .field("price")
                .ranges(
                    AggregationRange.of(ar -> ar.to("50000").key("~5만원")),
                    AggregationRange.of(ar -> ar.from("50000").to("100000").key("5~10만원")),
                    AggregationRange.of(ar -> ar.from("100000").to("200000").key("10~20만원")),
                    AggregationRange.of(ar -> ar.from("200000").key("20만원~"))
                )
            )
        ));

        // 평점 집계
        aggs.put("ratings", Aggregation.of(a -> a
            .range(r -> r
                .field("rating")
                .ranges(
                    AggregationRange.of(ar -> ar.from("4").key("4점 이상")),
                    AggregationRange.of(ar -> ar.from("3").to("4").key("3~4점")),
                    AggregationRange.of(ar -> ar.to("3").key("3점 미만"))
                )
            )
        ));

        // 동적 속성 집계 (색상 등)
        aggs.put("attributes", Aggregation.of(a -> a
            .nested(n -> n.path("attributes"))
            .aggregations("attr_names", Aggregation.of(aa -> aa
                .terms(t -> t.field("attributes.name").size(10))
                .aggregations("attr_values", Aggregation.of(aaa -> aaa
                    .terms(t -> t.field("attributes.value").size(20))
                ))
            ))
        ));

        return aggs;
    }

    private Facets mapFacets(Map<String, Aggregate> aggregations) {
        List<FacetBucket> categories = new ArrayList<>();
        List<FacetBucket> brands = new ArrayList<>();
        List<FacetBucket> priceRanges = new ArrayList<>();
        List<FacetBucket> ratings = new ArrayList<>();
        Map<String, List<FacetBucket>> attributes = new HashMap<>();

        // 카테고리
        if (aggregations.containsKey("categories")) {
            var nested = aggregations.get("categories").nested();
            var terms = nested.aggregations().get("category_names").sterms();
            for (var bucket : terms.buckets().array()) {
                categories.add(new FacetBucket(
                    bucket.key().stringValue(),
                    bucket.key().stringValue(),
                    bucket.docCount()
                ));
            }
        }

        // 브랜드
        if (aggregations.containsKey("brands")) {
            var terms = aggregations.get("brands").sterms();
            for (var bucket : terms.buckets().array()) {
                brands.add(new FacetBucket(
                    bucket.key().stringValue(),
                    bucket.key().stringValue(),
                    bucket.docCount()
                ));
            }
        }

        // 가격 범위
        if (aggregations.containsKey("price_ranges")) {
            var range = aggregations.get("price_ranges").range();
            for (var bucket : range.buckets().array()) {
                priceRanges.add(new FacetBucket(
                    bucket.key(),
                    bucket.key(),
                    bucket.docCount()
                ));
            }
        }

        // 평점
        if (aggregations.containsKey("ratings")) {
            var range = aggregations.get("ratings").range();
            for (var bucket : range.buckets().array()) {
                ratings.add(new FacetBucket(
                    bucket.key(),
                    bucket.key(),
                    bucket.docCount()
                ));
            }
        }

        return Facets.builder()
            .categories(categories)
            .brands(brands)
            .priceRanges(priceRanges)
            .ratings(ratings)
            .attributes(attributes)
            .build();
    }
}
```

## REST API

### Controller

```java
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class FacetedSearchController {

    private final FacetedSearchService facetedSearchService;

    @GetMapping("/faceted")
    public ResponseEntity<ApiResponse<FacetedSearchResponse<ProductSearchResult>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> brandIds,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Map<String, List<String>> attributes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sort
    ) {
        FacetedSearchRequest request = FacetedSearchRequest.builder()
            .keyword(keyword)
            .categoryIds(categoryIds)
            .brandIds(brandIds)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .minRating(minRating)
            .inStock(inStock)
            .attributes(attributes)
            .page(page)
            .size(size)
            .sort(sort)
            .build();

        return ResponseEntity.ok(
            ApiResponse.success(facetedSearchService.search(request))
        );
    }
}
```

## 응답 예시

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "id": 101,
        "name": "무선 블루투스 이어폰",
        "price": 89000,
        "rating": 4.5
      }
    ],
    "totalHits": 150,
    "page": 0,
    "size": 20,
    "totalPages": 8,
    "facets": {
      "categories": [
        { "key": "무선 이어폰", "label": "무선 이어폰", "count": 80 },
        { "key": "유선 이어폰", "label": "유선 이어폰", "count": 45 }
      ],
      "brands": [
        { "key": "Samsung", "label": "Samsung", "count": 40 },
        { "key": "Apple", "label": "Apple", "count": 35 }
      ],
      "priceRanges": [
        { "key": "~5만원", "label": "~5만원", "count": 45 },
        { "key": "5~10만원", "label": "5~10만원", "count": 60 }
      ],
      "ratings": [
        { "key": "4점 이상", "label": "4점 이상", "count": 85 }
      ],
      "attributes": {
        "color": [
          { "key": "black", "label": "블랙", "count": 50 },
          { "key": "white", "label": "화이트", "count": 45 }
        ]
      }
    }
  }
}
```

## Best Practices

### 1. Filter vs Post-Filter

```java
// filter: 결과와 집계 모두에 적용
.query(q -> q.bool(b -> b.filter(filterQuery)))

// post_filter: 결과에만 적용, 집계에는 미적용
.postFilter(filterQuery)
```

선택된 필터를 post_filter로 적용하면 다른 필터의 카운트가 유지됩니다.

### 2. 성능 최적화

- **Global Aggregation**: 필터와 무관한 전체 집계
- **Sampler Aggregation**: 샘플링으로 집계 성능 향상
- **캐싱**: 자주 사용되는 필터 조합 캐싱

### 3. UX 고려사항

- 필터 선택 시 다른 필터의 카운트 실시간 업데이트
- "필터 초기화" 기능 제공
- 선택된 필터 표시

## 관련 문서

- [Product Index Design](./product-index-design.md) - 인덱스 설계
- [Search Service](./search-service.md) - 검색 서비스 로직
- [Search Ranking](./search-ranking.md) - 검색 결과 정렬
