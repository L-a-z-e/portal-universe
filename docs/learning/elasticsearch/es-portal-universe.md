# Elasticsearch Portal Universe 적용

## 학습 목표
- Portal Universe 상품 검색 구현 상세 이해
- 검색 기능별 구현 패턴 학습
- 성능 최적화 및 모범 사례 파악

---

## 1. 프로젝트 구조

### 1.1 디렉토리 레이아웃

```
services/shopping-service/
├── src/main/java/.../search/
│   ├── config/
│   │   └── ElasticsearchConfig.java      # ES 클라이언트 설정
│   ├── controller/
│   │   └── SearchController.java         # 검색 API 엔드포인트
│   ├── document/
│   │   └── ProductDocument.java          # ES 문서 클래스
│   ├── dto/
│   │   ├── ProductSearchRequest.java     # 검색 요청 DTO
│   │   └── ProductSearchResponse.java    # 검색 응답 DTO
│   └── service/
│       ├── ProductSearchService.java     # 검색 비즈니스 로직
│       ├── SuggestService.java           # 자동완성 서비스
│       └── IndexInitializationService.java # 인덱스 초기화
└── src/main/resources/
    └── elasticsearch/
        └── products-mapping.json         # 인덱스 매핑 정의
```

---

## 2. 설정

### 2.1 ElasticsearchConfig.java

```java
@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    private final ObjectMapper objectMapper;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // REST 클라이언트 생성
        RestClient restClient = RestClient.builder(
            HttpHost.create(elasticsearchUri)
        ).build();

        // JSON 매퍼 설정 (Java Time 지원)
        ObjectMapper mapper = objectMapper.copy()
            .registerModule(new JavaTimeModule());

        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);

        // Transport 및 Client 생성
        RestClientTransport transport =
            new RestClientTransport(restClient, jsonpMapper);

        return new ElasticsearchClient(transport);
    }
}
```

### 2.2 인덱스 매핑 (products-mapping.json)

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_readingform"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "long"
      },
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": {
            "type": "keyword"
          },
          "suggest": {
            "type": "completion",
            "analyzer": "korean"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean"
      },
      "price": {
        "type": "double"
      },
      "stock": {
        "type": "integer"
      },
      "category": {
        "type": "keyword"
      },
      "createdAt": {
        "type": "date"
      }
    }
  }
}
```

**매핑 설계 포인트:**
- `name` 필드: text(검색) + keyword(정렬) + completion(자동완성)
- `korean` 분석기: Nori 토크나이저로 한글 형태소 분석
- `category`: keyword 타입으로 정확한 필터링

---

## 3. Document 클래스

### 3.1 ProductDocument.java

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private LocalDateTime createdAt;

    /**
     * JPA Entity → ES Document 변환
     */
    public static ProductDocument from(Product product) {
        return ProductDocument.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice().doubleValue())
            .stock(product.getStock())
            .category(product.getCategory())
            .createdAt(product.getCreatedAt())
            .build();
    }
}
```

---

## 4. 인덱싱 (색인)

### 4.1 인덱스 초기화

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexInitializationService {

    private final ElasticsearchClient client;

    private static final String INDEX_NAME = "products";

    @PostConstruct
    public void initIndex() {
        try {
            boolean exists = client.indices()
                .exists(e -> e.index(INDEX_NAME))
                .value();

            if (!exists) {
                // 매핑 파일 로드
                String mapping = loadMappingJson();

                // 인덱스 생성
                client.indices().create(c -> c
                    .index(INDEX_NAME)
                    .withJson(new StringReader(mapping))
                );

                log.info("Elasticsearch index '{}' created", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("Failed to initialize ES index", e);
        }
    }

    private String loadMappingJson() throws IOException {
        ClassPathResource resource =
            new ClassPathResource("elasticsearch/products-mapping.json");
        return new String(resource.getInputStream().readAllBytes(),
                          StandardCharsets.UTF_8);
    }
}
```

### 4.2 문서 인덱싱

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ElasticsearchClient client;
    private static final String INDEX_NAME = "products";

    /**
     * 상품 인덱싱 (생성/수정)
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument doc = ProductDocument.from(product);

            client.index(i -> i
                .index(INDEX_NAME)
                .id(product.getId().toString())
                .document(doc)
            );

            log.info("Product indexed: id={}", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product: id={}", product.getId(), e);
        }
    }

    /**
     * 재고만 업데이트 (부분 업데이트)
     */
    public void updateStock(Long productId, Integer stock) {
        try {
            client.update(u -> u
                .index(INDEX_NAME)
                .id(productId.toString())
                .doc(Map.of("stock", stock))
            , ProductDocument.class);
        } catch (Exception e) {
            log.error("Failed to update stock: id={}", productId, e);
        }
    }

    /**
     * 상품 삭제
     */
    public void deleteProduct(Long productId) {
        try {
            client.delete(d -> d
                .index(INDEX_NAME)
                .id(productId.toString())
            );

            log.info("Product deleted from index: id={}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product: id={}", productId, e);
        }
    }
}
```

---

## 5. 검색 구현

### 5.1 검색 요청 DTO

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {

    private String keyword;          // 검색어
    private Double minPrice;         // 최소 가격
    private Double maxPrice;         // 최대 가격
    private String category;         // 카테고리 필터

    @Builder.Default
    private String sort = "relevance";  // 정렬 기준
    // relevance, price_asc, price_desc, newest

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
```

### 5.2 검색 서비스

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ElasticsearchClient client;
    private static final String INDEX_NAME = "products";

    /**
     * 상품 검색
     */
    public ProductSearchResponse search(ProductSearchRequest request) {
        try {
            SearchResponse<ProductDocument> response = client.search(s -> s
                .index(INDEX_NAME)
                .query(buildQuery(request))
                .sort(buildSort(request))
                .from(request.getPage() * request.getSize())
                .size(request.getSize())
                .highlight(buildHighlight()),
                ProductDocument.class
            );

            return toResponse(response, request);

        } catch (Exception e) {
            log.error("Search failed", e);
            return ProductSearchResponse.empty();
        }
    }

    /**
     * 검색 쿼리 구성
     */
    private Query buildQuery(ProductSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 1. 키워드 검색 (must)
        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(m -> m
                .multiMatch(mm -> mm
                    .query(request.getKeyword())
                    .fields("name^3", "description")  // name 가중치 3배
                    .fuzziness("AUTO")                // 오타 허용
                    .type(TextQueryType.BestFields)
                )
            );
        }

        // 2. 가격 범위 필터 (filter)
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            boolBuilder.filter(f -> f
                .range(r -> {
                    NumberRangeQuery.Builder rangeBuilder =
                        new NumberRangeQuery.Builder().field("price");

                    if (request.getMinPrice() != null) {
                        rangeBuilder.gte(request.getMinPrice());
                    }
                    if (request.getMaxPrice() != null) {
                        rangeBuilder.lte(request.getMaxPrice());
                    }

                    return r.number(rangeBuilder.build());
                })
            );
        }

        // 3. 카테고리 필터 (filter)
        if (StringUtils.hasText(request.getCategory())) {
            boolBuilder.filter(f -> f
                .term(t -> t
                    .field("category")
                    .value(request.getCategory())
                )
            );
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 정렬 구성
     */
    private List<SortOptions> buildSort(ProductSearchRequest request) {
        return switch (request.getSort()) {
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
                SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)))  // relevance
            );
        };
    }

    /**
     * 하이라이트 설정
     */
    private Highlight buildHighlight() {
        return Highlight.of(h -> h
            .fields("name", f -> f
                .preTags("<em>")
                .postTags("</em>")
            )
            .fields("description", f -> f
                .preTags("<em>")
                .postTags("</em>")
            )
        );
    }

    /**
     * 응답 변환
     */
    private ProductSearchResponse toResponse(
            SearchResponse<ProductDocument> response,
            ProductSearchRequest request) {

        List<ProductSearchResult> results = response.hits().hits().stream()
            .map(hit -> {
                ProductDocument doc = hit.source();

                // 하이라이트 추출
                String highlightedName = extractHighlight(hit, "name", doc.getName());
                String highlightedDesc = extractHighlight(hit, "description", doc.getDescription());

                return ProductSearchResult.builder()
                    .id(doc.getId())
                    .name(doc.getName())
                    .description(doc.getDescription())
                    .price(doc.getPrice())
                    .stock(doc.getStock())
                    .highlightedName(highlightedName)
                    .highlightedDescription(highlightedDesc)
                    .score(hit.score())
                    .build();
            })
            .toList();

        long totalHits = response.hits().total() != null
            ? response.hits().total().value()
            : 0L;

        return ProductSearchResponse.builder()
            .results(results)
            .totalHits(totalHits)
            .page(request.getPage())
            .size(request.getSize())
            .totalPages((int) Math.ceil((double) totalHits / request.getSize()))
            .build();
    }

    private String extractHighlight(Hit<ProductDocument> hit,
                                    String field,
                                    String defaultValue) {
        if (hit.highlight() != null && hit.highlight().containsKey(field)) {
            return String.join("...", hit.highlight().get(field));
        }
        return defaultValue;
    }
}
```

---

## 6. 자동완성 구현

### 6.1 SuggestService.java

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestService {

    private final ElasticsearchClient client;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String INDEX_NAME = "products";

    /**
     * 자동완성 (Elasticsearch Completion Suggester)
     */
    public List<String> getAutocompleteSuggestions(String keyword, int size) {
        try {
            SearchResponse<Void> response = client.search(s -> s
                .index(INDEX_NAME)
                .suggest(su -> su
                    .suggesters("product-suggest", sg -> sg
                        .prefix(keyword)
                        .completion(c -> c
                            .field("name.suggest")
                            .size(size)
                            .skipDuplicates(true)
                            .fuzzy(f -> f.fuzziness("AUTO"))
                        )
                    )
                ),
                Void.class
            );

            // 제안 결과 추출
            return response.suggest()
                .get("product-suggest")
                .stream()
                .flatMap(s -> s.completion().options().stream())
                .map(o -> o.text())
                .distinct()
                .limit(size)
                .toList();

        } catch (Exception e) {
            log.error("Autocomplete failed", e);
            return List.of();
        }
    }

    /**
     * 인기 검색어 (Redis Sorted Set)
     */
    public List<String> getPopularKeywords(int size) {
        Set<Object> keywords = redisTemplate.opsForZSet()
            .reverseRange("search:popular", 0, size - 1);

        return keywords != null
            ? keywords.stream().map(Object::toString).toList()
            : List.of();
    }

    /**
     * 검색 시 인기도 카운트 증가
     */
    public void incrementSearchCount(String keyword) {
        redisTemplate.opsForZSet()
            .incrementScore("search:popular", keyword, 1);
    }

    /**
     * 최근 검색어 (Redis List)
     */
    public List<String> getRecentKeywords(String userId, int size) {
        String key = "search:recent:" + userId;
        List<Object> keywords = redisTemplate.opsForList()
            .range(key, 0, size - 1);

        return keywords != null
            ? keywords.stream().map(Object::toString).toList()
            : List.of();
    }

    /**
     * 최근 검색어 추가
     */
    public void addRecentKeyword(String userId, String keyword) {
        String key = "search:recent:" + userId;

        // 중복 제거 후 추가
        redisTemplate.opsForList().remove(key, 0, keyword);
        redisTemplate.opsForList().leftPush(key, keyword);

        // 최대 20개 유지
        redisTemplate.opsForList().trim(key, 0, 19);
    }

    /**
     * 최근 검색어 삭제
     */
    public void removeRecentKeyword(String userId, String keyword) {
        String key = "search:recent:" + userId;
        redisTemplate.opsForList().remove(key, 0, keyword);
    }
}
```

---

## 7. API 엔드포인트

### 7.1 SearchController.java

```java
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService searchService;
    private final SuggestService suggestService;

    /**
     * 상품 검색
     * GET /search/products?keyword=삼성&minPrice=100&maxPrice=1000&sort=price_asc
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProducts(
            @ModelAttribute ProductSearchRequest request) {

        // 인기도 카운트 증가
        if (StringUtils.hasText(request.getKeyword())) {
            suggestService.incrementSearchCount(request.getKeyword());
        }

        return ResponseEntity.ok(
            ApiResponse.success(searchService.search(request))
        );
    }

    /**
     * 자동완성
     * GET /search/suggest?keyword=삼성&size=10
     */
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
            ApiResponse.success(
                suggestService.getAutocompleteSuggestions(keyword, size)
            )
        );
    }

    /**
     * 인기 검색어
     * GET /search/popular?size=10
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<String>>> getPopularKeywords(
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
            ApiResponse.success(suggestService.getPopularKeywords(size))
        );
    }

    /**
     * 최근 검색어 조회
     * GET /search/recent?size=20
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<String>>> getRecentKeywords(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
            ApiResponse.success(suggestService.getRecentKeywords(userId, size))
        );
    }

    /**
     * 최근 검색어 추가
     * POST /search/recent
     */
    @PostMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> addRecentKeyword(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String keyword) {

        suggestService.addRecentKeyword(userId, keyword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 최근 검색어 삭제
     * DELETE /search/recent
     */
    @DeleteMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> removeRecentKeyword(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String keyword) {

        suggestService.removeRecentKeyword(userId, keyword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

---

## 8. 데이터 동기화 전략

### 8.1 현재 구현 (동기 방식)

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchService searchService;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.create(request);
        Product saved = productRepository.save(product);

        // ES 인덱싱 (동기)
        searchService.indexProduct(saved);

        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(PRODUCT_NOT_FOUND));

        product.update(request);
        Product saved = productRepository.save(product);

        // ES 업데이트 (동기)
        searchService.indexProduct(saved);

        return ProductResponse.from(saved);
    }
}
```

### 8.2 권장 개선 (비동기 이벤트 방식)

```java
// 이벤트 발행
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.create(request);
        Product saved = productRepository.save(product);

        // 이벤트 발행 (비동기 인덱싱)
        eventPublisher.publishEvent(new ProductCreatedEvent(saved));

        return ProductResponse.from(saved);
    }
}

// 이벤트 리스너
@Component
@RequiredArgsConstructor
public class ProductIndexEventListener {

    private final ProductSearchService searchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductCreated(ProductCreatedEvent event) {
        searchService.indexProduct(event.getProduct());
    }
}
```

---

## 9. 검색 기능 요약

| 기능 | 구현 방식 | 저장소 |
|------|----------|--------|
| **키워드 검색** | Multi-match + Fuzzy | Elasticsearch |
| **가격 필터** | Range Query | Elasticsearch |
| **카테고리 필터** | Term Query | Elasticsearch |
| **정렬** | Sort Options | Elasticsearch |
| **하이라이트** | Highlight | Elasticsearch |
| **자동완성** | Completion Suggester | Elasticsearch |
| **인기 검색어** | Sorted Set (ZSET) | Redis |
| **최근 검색어** | List | Redis |

---

## 10. 핵심 정리

| 컴포넌트 | 역할 |
|----------|------|
| `ElasticsearchConfig` | ES 클라이언트 설정 |
| `ProductSearchService` | 인덱싱 및 검색 로직 |
| `SuggestService` | 자동완성, 검색어 추천 |
| `IndexInitializationService` | 인덱스 자동 생성 |
| `products-mapping.json` | 인덱스 매핑 정의 |

---

## 다음 학습

- [Elasticsearch Query 최적화](./es-optimization.md)
- [Elasticsearch Aggregation](./es-aggregations.md)
- [검색 성능 튜닝](./es-performance.md)

---

## 참고 자료

- [Elasticsearch Java API Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Completion Suggester](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html#completion-suggester)
- [Highlighting](https://www.elastic.co/guide/en/elasticsearch/reference/current/highlighting.html)
