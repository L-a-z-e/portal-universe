# 검색 서비스 로직

## 개요

Shopping Service의 상품 검색은 Elasticsearch를 활용하여 고성능 전문 검색(Full-text Search)을 제공합니다. 이 문서에서는 검색 서비스의 구현 패턴과 주요 기능을 설명합니다.

## 검색 아키텍처

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│   Client    │────→│  Controller  │────→│ SearchService │
└─────────────┘     └──────────────┘     └───────┬───────┘
                                                 │
                         ┌───────────────────────┼───────────────────────┐
                         │                       │                       │
                         ▼                       ▼                       ▼
               ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
               │  Elasticsearch  │    │      Redis      │    │   SuggestSvc    │
               │  (Search Query) │    │  (Popular/Recent)│    │ (Autocomplete)  │
               └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## DTO 설계

### ProductSearchRequest

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
    private String keyword;
    private Double minPrice;
    private Double maxPrice;
    private String sort;  // relevance, price_asc, price_desc

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    public static ProductSearchRequest of(String keyword, int page, int size) {
        return ProductSearchRequest.builder()
            .keyword(keyword)
            .page(page)
            .size(size)
            .build();
    }
}
```

### SearchResponse

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse<T> {
    private List<T> results;
    private long totalHits;
    private int page;
    private int size;
    private int totalPages;

    public static <T> SearchResponse<T> of(List<T> results, long totalHits, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalHits / size);
        return SearchResponse.<T>builder()
            .results(results)
            .totalHits(totalHits)
            .page(page)
            .size(size)
            .totalPages(totalPages)
            .build();
    }
}
```

### ProductSearchResult

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchResult {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private Double score;  // 검색 관련성 점수
}
```

## 검색 서비스 구현

### ProductSearchService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final String INDEX_NAME = "products";

    private final ElasticsearchClient esClient;
    private final SuggestService suggestService;

    /**
     * 상품을 검색합니다.
     */
    public SearchResponse<ProductSearchResult> search(ProductSearchRequest request) {
        try {
            SearchRequest esRequest = buildSearchRequest(request);
            SearchResponse<ProductDocument> response = esClient.search(esRequest, ProductDocument.class);

            List<ProductSearchResult> results = response.hits().hits().stream()
                .map(hit -> ProductSearchResult.builder()
                    .id(hit.source().getId())
                    .name(hit.source().getName())
                    .description(hit.source().getDescription())
                    .price(hit.source().getPrice())
                    .stock(hit.source().getStock())
                    .score(hit.score())
                    .build())
                .toList();

            // 검색어 통계 기록
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                suggestService.incrementSearchCount(request.getKeyword());
            }

            return SearchResponse.of(
                results,
                response.hits().total().value(),
                request.getPage(),
                request.getSize()
            );

        } catch (IOException e) {
            log.error("Search failed: {}", request, e);
            throw new CustomBusinessException(ShoppingErrorCode.SEARCH_FAILED);
        }
    }

    private SearchRequest buildSearchRequest(ProductSearchRequest request) {
        return SearchRequest.of(s -> s
            .index(INDEX_NAME)
            .query(buildQuery(request))
            .sort(buildSort(request))
            .from(request.getPage() * request.getSize())
            .size(request.getSize())
        );
    }

    private Query buildQuery(ProductSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // 키워드 검색 (must)
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            mustQueries.add(Query.of(q -> q
                .multiMatch(m -> m
                    .query(request.getKeyword())
                    .fields("name^3", "description")  // name 필드에 3배 가중치
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                )
            ));
        }

        // 가격 범위 필터 (filter)
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q
                .range(r -> {
                    RangeQuery.Builder builder = r.field("price");
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

        // 재고 있는 상품만 (filter)
        filterQueries.add(Query.of(q -> q
            .range(r -> r.field("stock").gt(JsonData.of(0)))
        ));

        // Bool Query 조합
        return Query.of(q -> q
            .bool(b -> {
                if (!mustQueries.isEmpty()) {
                    b.must(mustQueries);
                }
                if (!filterQueries.isEmpty()) {
                    b.filter(filterQueries);
                }
                if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
                    b.must(Query.of(qq -> qq.matchAll(m -> m)));
                }
                return b;
            })
        );
    }

    private List<SortOptions> buildSort(ProductSearchRequest request) {
        String sort = request.getSort();

        if (sort == null || "relevance".equals(sort)) {
            return List.of(
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))
            );
        }

        return switch (sort) {
            case "price_asc" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Asc)))
            );
            case "price_desc" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Desc)))
            );
            case "newest" -> List.of(
                SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );
            default -> List.of(
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))
            );
        };
    }
}
```

## 고급 검색 기능

### 1. Phrase 검색

정확한 구문 검색:

```java
Query phraseQuery = Query.of(q -> q
    .matchPhrase(mp -> mp
        .field("name")
        .query("무선 이어폰")
        .slop(1)  // 단어 사이 허용 거리
    )
);
```

### 2. Wildcard 검색

패턴 검색:

```java
Query wildcardQuery = Query.of(q -> q
    .wildcard(w -> w
        .field("name.keyword")
        .value("*이어폰*")
    )
);
```

### 3. Highlighting

검색어 하이라이팅:

```java
SearchRequest request = SearchRequest.of(s -> s
    .index(INDEX_NAME)
    .query(query)
    .highlight(h -> h
        .fields("name", hf -> hf
            .preTags("<em>")
            .postTags("</em>")
        )
        .fields("description", hf -> hf
            .preTags("<em>")
            .postTags("</em>")
            .fragmentSize(150)
            .numberOfFragments(3)
        )
    )
);
```

### 4. 검색어 교정 (Did You Mean)

```java
SearchRequest request = SearchRequest.of(s -> s
    .index(INDEX_NAME)
    .suggest(sg -> sg
        .suggesters("spelling", sp -> sp
            .text("무선 이어퐁")  // 오타
            .term(t -> t
                .field("name")
                .suggestMode(SuggestMode.Always)
            )
        )
    )
);
```

## 검색 최적화

### 1. Query Cache

자주 사용되는 필터 쿼리 캐싱:

```json
// elasticsearch.yml
indices.queries.cache.size: 10%
```

### 2. 검색 결과 캐싱 (Redis)

```java
@Service
@RequiredArgsConstructor
public class CachedSearchService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductSearchService searchService;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public SearchResponse<ProductSearchResult> search(ProductSearchRequest request) {
        String cacheKey = buildCacheKey(request);

        // 캐시 조회
        SearchResponse<ProductSearchResult> cached =
            (SearchResponse<ProductSearchResult>) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // 캐시 미스 - ES 검색
        SearchResponse<ProductSearchResult> response = searchService.search(request);

        // 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);

        return response;
    }

    private String buildCacheKey(ProductSearchRequest request) {
        return String.format("search:%s:%s:%s:%d:%d",
            request.getKeyword(),
            request.getMinPrice(),
            request.getMaxPrice(),
            request.getPage(),
            request.getSize()
        );
    }
}
```

### 3. 검색어 전처리

```java
@Component
public class QueryPreprocessor {

    public String preprocess(String query) {
        if (query == null) return null;

        return query
            .trim()
            .toLowerCase()
            .replaceAll("\\s+", " ")  // 연속 공백 제거
            .replaceAll("[^\\p{L}\\p{N}\\s]", "");  // 특수문자 제거
    }
}
```

## REST Controller

```java
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService searchService;
    private final SuggestService suggestService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<SearchResponse<ProductSearchResult>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ProductSearchRequest request = ProductSearchRequest.builder()
            .keyword(keyword)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .sort(sort)
            .page(page)
            .size(size)
            .build();

        SearchResponse<ProductSearchResult> response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<String> suggestions = suggestService.suggest(keyword, size);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<String>>> popular(
            @RequestParam(defaultValue = "10") int size
    ) {
        List<String> keywords = suggestService.getPopularKeywords(size);
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }
}
```

## 에러 처리

### Search ErrorCode

```java
// ShoppingErrorCode.java
SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S1001", "Search operation failed"),
INVALID_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "S1002", "Invalid search query"),
INDEX_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "S1003", "Search index not found"),
SUGGEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S1004", "Autocomplete suggestion failed");
```

### 에러 처리 패턴

```java
public SearchResponse<ProductSearchResult> search(ProductSearchRequest request) {
    try {
        // 검색 로직
    } catch (ElasticsearchException e) {
        if (e.getMessage().contains("index_not_found")) {
            log.error("Index not found: products");
            throw new CustomBusinessException(INDEX_NOT_FOUND);
        }
        throw new CustomBusinessException(SEARCH_FAILED);
    } catch (IOException e) {
        log.error("Search IO error", e);
        throw new CustomBusinessException(SEARCH_FAILED);
    }
}
```

## 모니터링

### 검색 메트릭

```java
@Component
@RequiredArgsConstructor
public class SearchMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSearch(String keyword, long took, long totalHits) {
        // 검색 시간
        meterRegistry.timer("search.latency").record(took, TimeUnit.MILLISECONDS);

        // 검색 결과 수
        meterRegistry.gauge("search.total_hits", totalHits);

        // 검색 횟수
        meterRegistry.counter("search.requests").increment();

        // 빈 결과 추적
        if (totalHits == 0) {
            meterRegistry.counter("search.zero_results", "keyword", keyword).increment();
        }
    }
}
```

## Best Practices

1. **Pagination**: 깊은 페이지네이션은 `search_after` 사용
2. **필드 선택**: `_source` 필터링으로 필요한 필드만 조회
3. **캐싱**: 자주 검색되는 쿼리 결과 캐싱
4. **로깅**: 검색어와 결과 로깅으로 검색 품질 분석

## 관련 문서

- [Product Index Design](./product-index-design.md) - 인덱스 설계
- [Autocomplete](./autocomplete.md) - 자동완성 구현
- [Faceted Search](./faceted-search.md) - 필터 검색
- [Search Ranking](./search-ranking.md) - 검색 결과 정렬
