# Elasticsearch Spring Integration

Spring Boot와 Elasticsearch 연동 방법을 학습합니다. Spring Data Elasticsearch와 ElasticsearchClient 사용법을 다룹니다.

---

## 목차

1. [의존성 및 설정](#1-의존성-및-설정)
2. [Spring Data Elasticsearch](#2-spring-data-elasticsearch)
3. [ElasticsearchClient (Java API Client)](#3-elasticsearchclient-java-api-client)
4. [검색 기능 구현](#4-검색-기능-구현)
5. [실전 예제](#5-실전-예제)
6. [테스트 전략](#6-테스트-전략)

---

## 1. 의존성 및 설정

### Gradle 의존성

```groovy
// build.gradle
dependencies {
    // Spring Data Elasticsearch
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'

    // Elasticsearch Java Client (8.x)
    implementation 'co.elastic.clients:elasticsearch-java:8.11.0'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'

    // Test
    testImplementation 'org.testcontainers:elasticsearch:1.19.0'
}
```

### Maven 의존성

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
    <dependency>
        <groupId>co.elastic.clients</groupId>
        <artifactId>elasticsearch-java</artifactId>
        <version>8.11.0</version>
    </dependency>
</dependencies>
```

### application.yml 설정

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: changeme
    connection-timeout: 5s
    socket-timeout: 30s

# 프로파일별 설정
---
spring:
  config:
    activate:
      on-profile: docker
  elasticsearch:
    uris: http://elasticsearch:9200

---
spring:
  config:
    activate:
      on-profile: k8s
  elasticsearch:
    uris: http://elasticsearch-master:9200
```

### Configuration 클래스

```java
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.portal.universe.repository.elasticsearch")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
            ClientConfiguration.builder()
                .connectedTo(elasticsearchUri.replace("http://", ""))
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(30));

        if (StringUtils.hasText(username)) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
            HttpHost.create(elasticsearchUri)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
}
```

---

## 2. Spring Data Elasticsearch

### Entity 정의

```java
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Mapping(mappingPath = "elasticsearch/product-mappings.json")
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String name;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Integer)
    private Integer price;

    @Field(type = FieldType.Float)
    private Float rating;

    @Field(type = FieldType.Integer)
    private Integer salesCount;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Nested)
    private List<SpecificationDocument> specifications;

    @GeoPointField
    private GeoPoint location;

    // Constructors, Getters, Setters
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecificationDocument {

    @Field(type = FieldType.Keyword)
    private String key;

    @Field(type = FieldType.Text)
    private String value;
}
```

### 설정 파일 (JSON)

```json
// resources/elasticsearch/product-settings.json
{
  "index": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  },
  "analysis": {
    "tokenizer": {
      "nori_tokenizer": {
        "type": "nori_tokenizer",
        "decompound_mode": "mixed"
      }
    },
    "filter": {
      "pos_filter": {
        "type": "nori_part_of_speech",
        "stoptags": ["E", "J", "SC", "SE", "SF", "SP"]
      }
    },
    "analyzer": {
      "korean_analyzer": {
        "type": "custom",
        "tokenizer": "nori_tokenizer",
        "filter": ["pos_filter", "lowercase"]
      }
    }
  }
}
```

```json
// resources/elasticsearch/product-mappings.json
{
  "properties": {
    "name": {
      "type": "text",
      "analyzer": "korean_analyzer",
      "fields": {
        "keyword": {
          "type": "keyword"
        }
      }
    },
    "description": {
      "type": "text",
      "analyzer": "korean_analyzer"
    }
  }
}
```

### Repository Interface

```java
public interface ProductSearchRepository
    extends ElasticsearchRepository<ProductDocument, String> {

    // 메서드 이름 기반 쿼리
    List<ProductDocument> findByName(String name);

    List<ProductDocument> findByCategory(String category);

    List<ProductDocument> findByPriceBetween(Integer minPrice, Integer maxPrice);

    List<ProductDocument> findByBrandAndIsActiveTrue(String brand);

    Page<ProductDocument> findByNameContaining(String keyword, Pageable pageable);

    // @Query 어노테이션 사용
    @Query("{\"match\": {\"name\": \"?0\"}}")
    List<ProductDocument> searchByName(String name);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}], " +
           "\"filter\": [{\"term\": {\"category\": \"?1\"}}]}}")
    List<ProductDocument> searchByNameAndCategory(String name, String category);

    @Query("{\"range\": {\"price\": {\"gte\": ?0, \"lte\": ?1}}}")
    List<ProductDocument> findByPriceRange(Integer min, Integer max);
}
```

### Service 구현

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductDocument save(ProductDocument product) {
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productSearchRepository.save(product);
    }

    public List<ProductDocument> saveAll(List<ProductDocument> products) {
        products.forEach(p -> {
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
        });
        return (List<ProductDocument>) productSearchRepository.saveAll(products);
    }

    public Optional<ProductDocument> findById(String id) {
        return productSearchRepository.findById(id);
    }

    public void delete(String id) {
        productSearchRepository.deleteById(id);
    }

    public Page<ProductDocument> search(String keyword, Pageable pageable) {
        return productSearchRepository.findByNameContaining(keyword, pageable);
    }
}
```

---

## 3. ElasticsearchClient (Java API Client)

### 복잡한 쿼리 구현

```java
@Service
@RequiredArgsConstructor
public class ProductElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    public SearchResponse<ProductDocument> search(
            String keyword,
            String category,
            Integer minPrice,
            Integer maxPrice,
            int page,
            int size) throws IOException {

        return elasticsearchClient.search(s -> s
            .index("products")
            .query(q -> q
                .bool(b -> {
                    // must: 키워드 검색
                    if (StringUtils.hasText(keyword)) {
                        b.must(m -> m
                            .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("name^3", "description^2", "brand")
                                .type(TextQueryType.BestFields)
                                .fuzziness("AUTO")
                            )
                        );
                    }

                    // filter: 카테고리
                    if (StringUtils.hasText(category)) {
                        b.filter(f -> f
                            .term(t -> t
                                .field("category")
                                .value(category)
                            )
                        );
                    }

                    // filter: 가격 범위
                    if (minPrice != null || maxPrice != null) {
                        b.filter(f -> f
                            .range(r -> {
                                r.field("price");
                                if (minPrice != null) r.gte(JsonData.of(minPrice));
                                if (maxPrice != null) r.lte(JsonData.of(maxPrice));
                                return r;
                            })
                        );
                    }

                    // filter: 활성 상품만
                    b.filter(f -> f
                        .term(t -> t
                            .field("isActive")
                            .value(true)
                        )
                    );

                    return b;
                })
            )
            .from(page * size)
            .size(size)
            .sort(so -> so
                .score(sc -> sc.order(SortOrder.Desc))
            )
            .highlight(h -> h
                .fields("name", hf -> hf.preTags("<em>").postTags("</em>"))
                .fields("description", hf -> hf.preTags("<em>").postTags("</em>"))
            ),
            ProductDocument.class
        );
    }
}
```

### Aggregation 구현

```java
@Service
@RequiredArgsConstructor
public class ProductAggregationService {

    private final ElasticsearchClient elasticsearchClient;

    public Map<String, Object> getProductStatistics(String category) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
            .index("products")
            .size(0)
            .query(q -> q
                .bool(b -> {
                    b.filter(f -> f.term(t -> t.field("isActive").value(true)));
                    if (StringUtils.hasText(category)) {
                        b.filter(f -> f.term(t -> t.field("category").value(category)));
                    }
                    return b;
                })
            )
            .aggregations("price_stats", a -> a
                .stats(st -> st.field("price"))
            )
            .aggregations("by_brand", a -> a
                .terms(t -> t.field("brand").size(10))
                .aggregations("avg_price", sub -> sub
                    .avg(avg -> avg.field("price"))
                )
            )
            .aggregations("price_ranges", a -> a
                .range(r -> r
                    .field("price")
                    .ranges(
                        rg -> rg.key("저가").to("100000"),
                        rg -> rg.key("중가").from("100000").to("500000"),
                        rg -> rg.key("고가").from("500000")
                    )
                )
            )
            .aggregations("by_category", a -> a
                .terms(t -> t.field("category").size(20))
            ),
            Void.class
        );

        return parseAggregations(response.aggregations());
    }

    private Map<String, Object> parseAggregations(Map<String, Aggregate> aggregations) {
        Map<String, Object> result = new HashMap<>();

        // Price Stats
        StatsAggregate priceStats = aggregations.get("price_stats").stats();
        result.put("priceStats", Map.of(
            "min", priceStats.min(),
            "max", priceStats.max(),
            "avg", priceStats.avg(),
            "sum", priceStats.sum(),
            "count", priceStats.count()
        ));

        // By Brand
        List<Map<String, Object>> brands = aggregations.get("by_brand").sterms().buckets()
            .array().stream()
            .map(bucket -> Map.of(
                "brand", bucket.key().stringValue(),
                "count", bucket.docCount(),
                "avgPrice", bucket.aggregations().get("avg_price").avg().value()
            ))
            .collect(Collectors.toList());
        result.put("byBrand", brands);

        // Price Ranges
        List<Map<String, Object>> priceRanges = aggregations.get("price_ranges").range().buckets()
            .array().stream()
            .map(bucket -> Map.of(
                "key", bucket.key(),
                "count", bucket.docCount()
            ))
            .collect(Collectors.toList());
        result.put("priceRanges", priceRanges);

        return result;
    }
}
```

### Bulk Operations

```java
@Service
@RequiredArgsConstructor
public class ProductBulkService {

    private final ElasticsearchClient elasticsearchClient;

    public BulkResponse bulkIndex(List<ProductDocument> products) throws IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (ProductDocument product : products) {
            br.operations(op -> op
                .index(idx -> idx
                    .index("products")
                    .id(product.getId())
                    .document(product)
                )
            );
        }

        return elasticsearchClient.bulk(br.build());
    }

    public BulkResponse bulkUpdate(Map<String, Map<String, Object>> updates) throws IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (Map.Entry<String, Map<String, Object>> entry : updates.entrySet()) {
            br.operations(op -> op
                .update(u -> u
                    .index("products")
                    .id(entry.getKey())
                    .action(a -> a
                        .doc(entry.getValue())
                    )
                )
            );
        }

        return elasticsearchClient.bulk(br.build());
    }

    public BulkResponse bulkDelete(List<String> ids) throws IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (String id : ids) {
            br.operations(op -> op
                .delete(d -> d
                    .index("products")
                    .id(id)
                )
            );
        }

        return elasticsearchClient.bulk(br.build());
    }
}
```

---

## 4. 검색 기능 구현

### 검색 요청/응답 DTO

```java
// Request DTO
@Data
@Builder
public class ProductSearchRequest {
    private String keyword;
    private String category;
    private String brand;
    private Integer minPrice;
    private Integer maxPrice;
    private Float minRating;
    private SortType sortType;
    private Integer page;
    private Integer size;

    public enum SortType {
        RELEVANCE, PRICE_ASC, PRICE_DESC, RATING, NEWEST, SALES
    }
}

// Response DTO
@Data
@Builder
public class ProductSearchResponse {
    private List<ProductSearchItem> items;
    private long totalCount;
    private int totalPages;
    private int currentPage;
    private Map<String, List<FacetItem>> facets;
}

@Data
@Builder
public class ProductSearchItem {
    private String id;
    private String name;
    private String nameHighlight;
    private String description;
    private String descriptionHighlight;
    private String brand;
    private String category;
    private Integer price;
    private Float rating;
    private String imageUrl;
}

@Data
@Builder
public class FacetItem {
    private String key;
    private long count;
}
```

### 검색 서비스

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchFacade {

    private final ElasticsearchClient elasticsearchClient;

    public ProductSearchResponse search(ProductSearchRequest request) {
        try {
            SearchResponse<ProductDocument> response = executeSearch(request);
            return buildResponse(response, request);
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new SearchException("검색 중 오류가 발생했습니다", e);
        }
    }

    private SearchResponse<ProductDocument> executeSearch(ProductSearchRequest request)
            throws IOException {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        return elasticsearchClient.search(s -> s
            .index("products")
            .query(buildQuery(request))
            .from(page * size)
            .size(size)
            .sort(buildSort(request.getSortType()))
            .highlight(buildHighlight())
            .aggregations(buildAggregations()),
            ProductDocument.class
        );
    }

    private Query buildQuery(ProductSearchRequest request) {
        return Query.of(q -> q
            .bool(b -> {
                // must: 키워드 검색
                if (StringUtils.hasText(request.getKeyword())) {
                    b.must(m -> m
                        .multiMatch(mm -> mm
                            .query(request.getKeyword())
                            .fields("name^3", "description^2", "brand")
                            .type(TextQueryType.BestFields)
                            .fuzziness("AUTO")
                        )
                    );
                }

                // filter 조건들
                b.filter(f -> f.term(t -> t.field("isActive").value(true)));

                if (StringUtils.hasText(request.getCategory())) {
                    b.filter(f -> f.term(t -> t.field("category").value(request.getCategory())));
                }

                if (StringUtils.hasText(request.getBrand())) {
                    b.filter(f -> f.term(t -> t.field("brand").value(request.getBrand())));
                }

                if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                    b.filter(f -> f
                        .range(r -> {
                            r.field("price");
                            if (request.getMinPrice() != null) {
                                r.gte(JsonData.of(request.getMinPrice()));
                            }
                            if (request.getMaxPrice() != null) {
                                r.lte(JsonData.of(request.getMaxPrice()));
                            }
                            return r;
                        })
                    );
                }

                if (request.getMinRating() != null) {
                    b.filter(f -> f
                        .range(r -> r
                            .field("rating")
                            .gte(JsonData.of(request.getMinRating()))
                        )
                    );
                }

                return b;
            })
        );
    }

    private List<SortOptions> buildSort(ProductSearchRequest.SortType sortType) {
        if (sortType == null) sortType = ProductSearchRequest.SortType.RELEVANCE;

        return switch (sortType) {
            case PRICE_ASC -> List.of(
                SortOptions.of(so -> so.field(f -> f.field("price").order(SortOrder.Asc)))
            );
            case PRICE_DESC -> List.of(
                SortOptions.of(so -> so.field(f -> f.field("price").order(SortOrder.Desc)))
            );
            case RATING -> List.of(
                SortOptions.of(so -> so.field(f -> f.field("rating").order(SortOrder.Desc)))
            );
            case NEWEST -> List.of(
                SortOptions.of(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );
            case SALES -> List.of(
                SortOptions.of(so -> so.field(f -> f.field("salesCount").order(SortOrder.Desc)))
            );
            default -> List.of(
                SortOptions.of(so -> so.score(sc -> sc.order(SortOrder.Desc)))
            );
        };
    }

    private Highlight buildHighlight() {
        return Highlight.of(h -> h
            .fields("name", hf -> hf.preTags("<em>").postTags("</em>").numberOfFragments(0))
            .fields("description", hf -> hf.preTags("<em>").postTags("</em>").fragmentSize(150))
        );
    }

    private Map<String, Aggregation> buildAggregations() {
        return Map.of(
            "categories", Aggregation.of(a -> a.terms(t -> t.field("category").size(20))),
            "brands", Aggregation.of(a -> a.terms(t -> t.field("brand").size(20))),
            "price_ranges", Aggregation.of(a -> a
                .range(r -> r
                    .field("price")
                    .ranges(
                        rg -> rg.key("~5만원").to("50000"),
                        rg -> rg.key("5~10만원").from("50000").to("100000"),
                        rg -> rg.key("10~30만원").from("100000").to("300000"),
                        rg -> rg.key("30~50만원").from("300000").to("500000"),
                        rg -> rg.key("50만원~").from("500000")
                    )
                )
            ),
            "ratings", Aggregation.of(a -> a
                .range(r -> r
                    .field("rating")
                    .ranges(
                        rg -> rg.key("4점 이상").from("4"),
                        rg -> rg.key("3점 이상").from("3"),
                        rg -> rg.key("2점 이상").from("2")
                    )
                )
            )
        );
    }

    private ProductSearchResponse buildResponse(
            SearchResponse<ProductDocument> response,
            ProductSearchRequest request) {

        List<ProductSearchItem> items = response.hits().hits().stream()
            .map(hit -> {
                ProductDocument doc = hit.source();
                Map<String, List<String>> highlight = hit.highlight();

                return ProductSearchItem.builder()
                    .id(doc.getId())
                    .name(doc.getName())
                    .nameHighlight(getHighlight(highlight, "name", doc.getName()))
                    .description(doc.getDescription())
                    .descriptionHighlight(getHighlight(highlight, "description", doc.getDescription()))
                    .brand(doc.getBrand())
                    .category(doc.getCategory())
                    .price(doc.getPrice())
                    .rating(doc.getRating())
                    .build();
            })
            .collect(Collectors.toList());

        int size = request.getSize() != null ? request.getSize() : 20;
        long totalCount = response.hits().total().value();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, List<FacetItem>> facets = buildFacets(response.aggregations());

        return ProductSearchResponse.builder()
            .items(items)
            .totalCount(totalCount)
            .totalPages(totalPages)
            .currentPage(request.getPage() != null ? request.getPage() : 0)
            .facets(facets)
            .build();
    }

    private String getHighlight(Map<String, List<String>> highlight, String field, String defaultValue) {
        if (highlight != null && highlight.containsKey(field)) {
            return String.join("...", highlight.get(field));
        }
        return defaultValue;
    }

    private Map<String, List<FacetItem>> buildFacets(Map<String, Aggregate> aggregations) {
        Map<String, List<FacetItem>> facets = new HashMap<>();

        // Terms aggregations
        for (String key : List.of("categories", "brands")) {
            if (aggregations.containsKey(key)) {
                List<FacetItem> items = aggregations.get(key).sterms().buckets()
                    .array().stream()
                    .map(b -> FacetItem.builder()
                        .key(b.key().stringValue())
                        .count(b.docCount())
                        .build())
                    .collect(Collectors.toList());
                facets.put(key, items);
            }
        }

        // Range aggregations
        for (String key : List.of("price_ranges", "ratings")) {
            if (aggregations.containsKey(key)) {
                List<FacetItem> items = aggregations.get(key).range().buckets()
                    .array().stream()
                    .map(b -> FacetItem.builder()
                        .key(b.key())
                        .count(b.docCount())
                        .build())
                    .collect(Collectors.toList());
                facets.put(key, items);
            }
        }

        return facets;
    }
}
```

---

## 5. 실전 예제

### Controller

```java
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchFacade productSearchFacade;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        ProductSearchRequest request = ProductSearchRequest.builder()
            .keyword(keyword)
            .category(category)
            .brand(brand)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .minRating(minRating)
            .sortType(parseSortType(sort))
            .page(page)
            .size(size)
            .build();

        ProductSearchResponse response = productSearchFacade.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private ProductSearchRequest.SortType parseSortType(String sort) {
        if (sort == null) return null;
        try {
            return ProductSearchRequest.SortType.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProductSearchRequest.SortType.RELEVANCE;
        }
    }
}
```

### 인덱스 관리 서비스

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexManagementService {

    private final ElasticsearchClient elasticsearchClient;
    private final ResourceLoader resourceLoader;

    public void createIndex(String indexName) throws IOException {
        // 설정 파일 로드
        Resource settingsResource = resourceLoader.getResource(
            "classpath:elasticsearch/" + indexName + "-settings.json");
        Resource mappingsResource = resourceLoader.getResource(
            "classpath:elasticsearch/" + indexName + "-mappings.json");

        String settings = StreamUtils.copyToString(
            settingsResource.getInputStream(), StandardCharsets.UTF_8);
        String mappings = StreamUtils.copyToString(
            mappingsResource.getInputStream(), StandardCharsets.UTF_8);

        CreateIndexRequest request = CreateIndexRequest.of(b -> b
            .index(indexName)
            .settings(s -> s.withJson(new StringReader(settings)))
            .mappings(m -> m.withJson(new StringReader(mappings)))
        );

        CreateIndexResponse response = elasticsearchClient.indices().create(request);
        log.info("Index created: {}, acknowledged: {}", indexName, response.acknowledged());
    }

    public void deleteIndex(String indexName) throws IOException {
        DeleteIndexResponse response = elasticsearchClient.indices()
            .delete(d -> d.index(indexName));
        log.info("Index deleted: {}, acknowledged: {}", indexName, response.acknowledged());
    }

    public boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices()
            .exists(e -> e.index(indexName))
            .value();
    }

    public void reindex(String sourceIndex, String targetIndex) throws IOException {
        ReindexResponse response = elasticsearchClient.reindex(r -> r
            .source(s -> s.index(sourceIndex))
            .dest(d -> d.index(targetIndex))
        );
        log.info("Reindex completed: {} -> {}, took: {}ms",
            sourceIndex, targetIndex, response.took());
    }

    public void updateSettings(String indexName, Map<String, Object> settings)
            throws IOException {
        // 먼저 인덱스 닫기 (일부 설정 변경에 필요)
        elasticsearchClient.indices().close(c -> c.index(indexName));

        // 설정 업데이트
        elasticsearchClient.indices().putSettings(p -> p
            .index(indexName)
            .settings(s -> s
                .numberOfReplicas(String.valueOf(settings.get("number_of_replicas")))
            )
        );

        // 인덱스 다시 열기
        elasticsearchClient.indices().open(o -> o.index(indexName));
    }
}
```

### 데이터 동기화

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncService {

    private final ProductRepository productRepository;  // JPA
    private final ProductSearchRepository productSearchRepository;  // ES
    private final ProductBulkService productBulkService;

    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    public void fullSync() {
        log.info("Starting full sync...");

        int page = 0;
        int size = 1000;
        long totalSynced = 0;

        while (true) {
            Page<Product> products = productRepository.findAll(
                PageRequest.of(page, size)
            );

            if (products.isEmpty()) break;

            List<ProductDocument> documents = products.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());

            try {
                productBulkService.bulkIndex(documents);
                totalSynced += documents.size();
                log.info("Synced {} products (page {})", documents.size(), page);
            } catch (IOException e) {
                log.error("Bulk index failed at page {}", page, e);
            }

            page++;
        }

        log.info("Full sync completed. Total: {} products", totalSynced);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        ProductDocument document = toDocument(event.getProduct());
        productSearchRepository.save(document);
        log.debug("Product indexed: {}", event.getProduct().getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUpdated(ProductUpdatedEvent event) {
        ProductDocument document = toDocument(event.getProduct());
        productSearchRepository.save(document);
        log.debug("Product updated in index: {}", event.getProduct().getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductDeleted(ProductDeletedEvent event) {
        productSearchRepository.deleteById(event.getProductId().toString());
        log.debug("Product removed from index: {}", event.getProductId());
    }

    private ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
            .id(product.getId().toString())
            .name(product.getName())
            .description(product.getDescription())
            .brand(product.getBrand())
            .category(product.getCategory())
            .price(product.getPrice())
            .rating(product.getRating())
            .salesCount(product.getSalesCount())
            .isActive(product.getIsActive())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}
```

---

## 6. 테스트 전략

### Testcontainers 설정

```java
@SpringBootTest
@Testcontainers
class ProductSearchServiceTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
    )
    .withEnv("xpack.security.enabled", "false")
    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    }

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void setUp() {
        productSearchRepository.deleteAll();
    }

    @Test
    void searchByKeyword_shouldReturnMatchingProducts() {
        // given
        ProductDocument product1 = createProduct("삼성 갤럭시 S24", "electronics");
        ProductDocument product2 = createProduct("아이폰 15 프로", "electronics");
        productSearchRepository.saveAll(List.of(product1, product2));

        // Refresh index for immediate search
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        // when
        List<ProductDocument> results = productSearchRepository.findByNameContaining(
            "갤럭시", Pageable.ofSize(10)
        ).getContent();

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).contains("갤럭시");
    }

    private ProductDocument createProduct(String name, String category) {
        return ProductDocument.builder()
            .id(UUID.randomUUID().toString())
            .name(name)
            .category(category)
            .price(100000)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

### 단위 테스트 (Mock)

```java
@ExtendWith(MockitoExtension.class)
class ProductSearchFacadeTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ProductSearchFacade productSearchFacade;

    @Test
    void search_shouldBuildCorrectQuery() throws IOException {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
            .keyword("스마트폰")
            .category("electronics")
            .minPrice(100000)
            .maxPrice(500000)
            .page(0)
            .size(20)
            .build();

        SearchResponse<ProductDocument> mockResponse = createMockResponse();
        when(elasticsearchClient.search(any(Function.class), eq(ProductDocument.class)))
            .thenReturn(mockResponse);

        // when
        ProductSearchResponse response = productSearchFacade.search(request);

        // then
        verify(elasticsearchClient).search(any(Function.class), eq(ProductDocument.class));
        assertThat(response.getItems()).isNotEmpty();
    }

    private SearchResponse<ProductDocument> createMockResponse() {
        // Mock response 생성
        // ...
    }
}
```

---

## 핵심 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│            Spring + Elasticsearch Quick Reference                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  의존성                                                              │
│  └── spring-boot-starter-data-elasticsearch                         │
│                                                                      │
│  Entity 매핑                                                         │
│  ├── @Document(indexName = "...")                                   │
│  ├── @Field(type = FieldType.Text, analyzer = "...")               │
│  └── @Setting, @Mapping으로 외부 JSON 연결                          │
│                                                                      │
│  Repository                                                          │
│  ├── ElasticsearchRepository<T, ID> 상속                            │
│  ├── 메서드명 기반 쿼리: findByNameContaining(...)                   │
│  └── @Query 어노테이션으로 커스텀 쿼리                               │
│                                                                      │
│  ElasticsearchClient (복잡한 쿼리)                                   │
│  ├── elasticsearchClient.search(s -> s...)                          │
│  ├── 빌더 패턴으로 쿼리 구성                                         │
│  └── Aggregation, Highlight, Sort 지원                              │
│                                                                      │
│  데이터 동기화                                                        │
│  ├── @TransactionalEventListener로 실시간 동기화                     │
│  └── @Scheduled로 전체 동기화                                        │
│                                                                      │
│  테스트                                                               │
│  └── Testcontainers + ElasticsearchContainer                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [Query DSL](./es-query-dsl.md) - 검색 쿼리 심화
- [Aggregations](./es-aggregations.md) - 집계 기능 활용
- [Optimization](./es-optimization.md) - 성능 최적화
