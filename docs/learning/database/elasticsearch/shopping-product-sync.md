# MySQL-Elasticsearch 데이터 동기화

## 개요

Shopping Service는 MySQL을 Primary Database로, Elasticsearch를 검색 엔진으로 사용합니다. 이 문서에서는 두 저장소 간의 데이터 동기화 전략과 구현 방법을 설명합니다.

## 동기화 아키텍처

```
┌─────────────┐                    ┌──────────────────┐
│   MySQL     │                    │  Elasticsearch   │
│  (Source)   │                    │    (Target)      │
└──────┬──────┘                    └────────▲─────────┘
       │                                    │
       │  ┌─────────────────────────────────┤
       │  │                                 │
       ▼  ▼                                 │
┌──────────────┐     ┌───────────┐    ┌─────────────┐
│   Product    │────→│   Kafka   │───→│ ES Indexer  │
│   Service    │     │  (CDC)    │    │  Consumer   │
└──────────────┘     └───────────┘    └─────────────┘
```

## 동기화 전략

### 1. 실시간 동기화 (Event-Driven)

상품 변경 시 이벤트를 발행하여 ES를 업데이트:

```java
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductIndexService productIndexService;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = productRepository.save(request.toEntity());

        // ES 동기화 (비동기)
        productIndexService.indexProduct(product);

        return ProductResponse.from(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(PRODUCT_NOT_FOUND));

        product.update(request);
        productRepository.save(product);

        // ES 동기화 (비동기)
        productIndexService.indexProduct(product);

        return ProductResponse.from(product);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);

        // ES에서 삭제
        productIndexService.deleteProduct(id);
    }
}
```

### 2. ProductIndexService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private static final String INDEX_NAME = "products";

    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    /**
     * 상품을 ES에 인덱싱합니다.
     */
    @Async("searchTaskExecutor")
    public CompletableFuture<Void> indexProduct(Product product) {
        try {
            ProductDocument doc = ProductDocument.from(product);

            esClient.index(IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .id(String.valueOf(product.getId()))
                .document(doc)
            ));

            log.info("Product indexed: id={}", product.getId());
        } catch (IOException e) {
            log.error("Failed to index product: id={}", product.getId(), e);
            // 재시도 큐에 추가
            retryQueue.add(new RetryableIndexTask(product.getId()));
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 상품을 ES에서 삭제합니다.
     */
    @Async("searchTaskExecutor")
    public CompletableFuture<Void> deleteProduct(Long productId) {
        try {
            esClient.delete(DeleteRequest.of(d -> d
                .index(INDEX_NAME)
                .id(String.valueOf(productId))
            ));

            log.info("Product deleted from index: id={}", productId);
        } catch (IOException e) {
            log.error("Failed to delete product from index: id={}", productId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 여러 상품을 일괄 인덱싱합니다.
     */
    public void bulkIndex(List<Product> products) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();

            for (Product product : products) {
                ProductDocument doc = ProductDocument.from(product);
                builder.operations(op -> op
                    .index(idx -> idx
                        .index(INDEX_NAME)
                        .id(String.valueOf(product.getId()))
                        .document(doc)
                    )
                );
            }

            BulkResponse response = esClient.bulk(builder.build());

            if (response.errors()) {
                log.error("Bulk indexing had errors");
                response.items().stream()
                    .filter(item -> item.error() != null)
                    .forEach(item -> log.error("Failed to index: {}", item.error().reason()));
            }

            log.info("Bulk indexed {} products", products.size());
        } catch (IOException e) {
            log.error("Failed to bulk index products", e);
        }
    }
}
```

### 3. ProductDocument (ES용 DTO)

```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;

    public static ProductDocument from(Product product) {
        return ProductDocument.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice().doubleValue())
            .stock(product.getStock())
            .build();
    }
}
```

## 전체 재동기화 (Full Reindex)

### 스케줄러 기반 재동기화

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReindexService {

    private static final int BATCH_SIZE = 1000;

    private final ProductRepository productRepository;
    private final ProductIndexService productIndexService;

    /**
     * 전체 상품을 재인덱싱합니다.
     * 야간에 스케줄러로 실행 권장
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    public void fullReindex() {
        log.info("Starting full product reindex");

        long totalProducts = productRepository.count();
        long processedCount = 0;

        int page = 0;
        Page<Product> productPage;

        do {
            productPage = productRepository.findAll(
                PageRequest.of(page, BATCH_SIZE, Sort.by("id"))
            );

            productIndexService.bulkIndex(productPage.getContent());

            processedCount += productPage.getNumberOfElements();
            log.info("Reindex progress: {}/{}", processedCount, totalProducts);

            page++;
        } while (productPage.hasNext());

        log.info("Full reindex completed: {} products", totalProducts);
    }

    /**
     * 특정 기간 동안 변경된 상품만 재인덱싱합니다.
     */
    public void incrementalReindex(LocalDateTime since) {
        log.info("Starting incremental reindex since: {}", since);

        List<Product> modifiedProducts = productRepository
            .findByUpdatedAtAfter(since);

        if (!modifiedProducts.isEmpty()) {
            productIndexService.bulkIndex(modifiedProducts);
            log.info("Incremental reindex completed: {} products", modifiedProducts.size());
        }
    }
}
```

## CDC (Change Data Capture) 패턴

### Debezium 활용 (권장)

```yaml
# Debezium Connector 설정
{
  "name": "product-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",
    "database.server.name": "shopping",
    "table.include.list": "shopping.products",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "schema-changes.shopping"
  }
}
```

### CDC Event Consumer

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCdcConsumer {

    private final ProductIndexService productIndexService;
    private final ProductRepository productRepository;

    @KafkaListener(topics = "shopping.shopping.products", groupId = "es-indexer-group")
    public void handleProductChange(ConsumerRecord<String, String> record) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            JsonNode after = payload.get("after");
            JsonNode before = payload.get("before");
            String operation = payload.get("op").asText();

            switch (operation) {
                case "c":  // Create
                case "u":  // Update
                    Long productId = after.get("id").asLong();
                    Product product = productRepository.findById(productId)
                        .orElseThrow();
                    productIndexService.indexProduct(product);
                    break;

                case "d":  // Delete
                    Long deletedId = before.get("id").asLong();
                    productIndexService.deleteProduct(deletedId);
                    break;

                default:
                    log.warn("Unknown CDC operation: {}", operation);
            }
        } catch (Exception e) {
            log.error("Failed to process CDC event", e);
            throw new RuntimeException(e);
        }
    }
}
```

## 동기화 상태 확인

### 동기화 검증 서비스

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncValidationService {

    private final ProductRepository productRepository;
    private final ElasticsearchClient esClient;

    /**
     * MySQL과 ES의 데이터 일관성을 검증합니다.
     */
    @Scheduled(cron = "0 0 4 * * *")  // 매일 새벽 4시
    public void validateSync() {
        log.info("Starting sync validation");

        // MySQL 상품 수
        long mysqlCount = productRepository.count();

        // ES 문서 수
        long esCount = esClient.count(CountRequest.of(c -> c
            .index("products")
        )).count();

        if (mysqlCount != esCount) {
            log.warn("Sync mismatch: MySQL={}, ES={}", mysqlCount, esCount);
            alertService.sendAlert("SYNC_MISMATCH",
                String.format("MySQL: %d, ES: %d", mysqlCount, esCount));
        }

        // 샘플링 검증 (최근 변경된 100개)
        List<Product> recentProducts = productRepository
            .findTop100ByOrderByUpdatedAtDesc();

        int missingCount = 0;
        for (Product product : recentProducts) {
            boolean exists = esClient.exists(GetRequest.of(g -> g
                .index("products")
                .id(String.valueOf(product.getId()))
            )).value();

            if (!exists) {
                missingCount++;
                log.warn("Product missing in ES: id={}", product.getId());
            }
        }

        if (missingCount > 0) {
            alertService.sendAlert("SYNC_MISSING",
                String.format("%d products missing in ES", missingCount));
        }

        log.info("Sync validation completed: MySQL={}, ES={}, missing={}",
            mysqlCount, esCount, missingCount);
    }
}
```

## 에러 처리 및 재시도

### 재시도 큐

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableIndexService {

    private final Queue<RetryableIndexTask> retryQueue = new ConcurrentLinkedQueue<>();
    private final ProductRepository productRepository;
    private final ProductIndexService productIndexService;

    @Scheduled(fixedDelay = 60000)  // 1분마다
    public void processRetryQueue() {
        int processedCount = 0;
        int maxProcessPerRun = 100;

        while (!retryQueue.isEmpty() && processedCount < maxProcessPerRun) {
            RetryableIndexTask task = retryQueue.poll();

            if (task.getRetryCount() >= 3) {
                log.error("Max retries exceeded for product: id={}", task.getProductId());
                // DLQ 테이블에 저장
                continue;
            }

            try {
                Product product = productRepository.findById(task.getProductId())
                    .orElse(null);

                if (product != null) {
                    productIndexService.indexProduct(product).get();
                }
            } catch (Exception e) {
                task.incrementRetryCount();
                retryQueue.add(task);
                log.warn("Retry failed for product: id={}, retryCount={}",
                    task.getProductId(), task.getRetryCount());
            }

            processedCount++;
        }
    }
}
```

## Best Practices

### 1. 동기화 전략 선택

| 전략 | 장점 | 단점 | 사용 케이스 |
|------|------|------|------------|
| 실시간 (Event) | 즉시 반영 | 복잡도 증가 | 실시간성 중요 |
| CDC | 정확한 동기화 | 인프라 필요 | 대규모 시스템 |
| 배치 | 단순함 | 지연 발생 | 실시간성 덜 중요 |

### 2. 데이터 일관성

- 최종 일관성(Eventual Consistency) 수용
- 정기적인 검증 작업 수행
- 불일치 발생 시 알림 및 자동 복구

### 3. 성능 최적화

```java
// Bulk API 사용
productIndexService.bulkIndex(products);

// Refresh Interval 조정
// 대량 인덱싱 시 일시적으로 비활성화
esClient.indices().putSettings(PutIndicesSettingsRequest.of(r -> r
    .index("products")
    .settings(s -> s.refreshInterval(t -> t.time("-1")))
));

// 인덱싱 후 수동 refresh
esClient.indices().refresh(RefreshRequest.of(r -> r.index("products")));
```

### 4. 모니터링 포인트

| 메트릭 | 설명 | 임계값 |
|--------|------|--------|
| 동기화 지연 | 이벤트 발생 ~ ES 반영 시간 | < 5초 |
| 문서 수 차이 | MySQL vs ES | 0 |
| 인덱싱 실패율 | 실패 / 전체 | < 0.1% |
| 재시도 큐 크기 | 대기 중인 작업 수 | < 100 |

## 관련 문서

- [Product Index Design](./product-index-design.md) - 인덱스 설계
- [Search Service](./search-service.md) - 검색 서비스 로직
- [Faceted Search](./faceted-search.md) - 필터 검색
