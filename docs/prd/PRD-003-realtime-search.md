# PRD-003: 실시간 기능 + 검색

## 1. 개요

### 1.1 목적
실시간 사용자 경험과 고급 검색 기능을 제공하여 서비스 품질 향상

### 1.2 배경
- Phase 1~2에서 이커머스 핵심 기능과 동시성 제어 완료
- 사용자에게 실시간 피드백 (알림, 재고 현황) 제공 필요
- 상품/블로그 통합 검색으로 사용자 경험 개선

### 1.3 범위
| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 알림 센터 | 실시간 알림 푸시 | P0 |
| 실시간 재고 | 재고 변동 실시간 표시 | P1 |
| 통합 검색 | 상품/블로그 전문 검색 | P0 |
| 자동완성 | 검색어 추천 | P1 |

---

## 2. 기술 스택

### 2.1 신규 인프라
| 기술 | 버전 | 용도 |
|------|------|------|
| Elasticsearch | 8.x | 전문 검색, 자동완성 |
| WebSocket (STOMP) | - | 실시간 양방향 통신 |
| SSE | - | 서버 → 클라이언트 단방향 푸시 |
| Redis Pub/Sub | 7.x | 실시간 이벤트 브로드캐스트 |

### 2.2 기존 인프라 활용
- Kafka: 알림 이벤트 발행
- MySQL/MongoDB: 알림 저장, 검색 원본 데이터
- notification-service: 알림 처리 확장

---

## 3. 알림 센터

### 3.1 요구사항

#### 기능 요구사항
- 주문 상태 변경 시 실시간 알림
- 배송 상태 변경 시 실시간 알림
- 쿠폰 발급/만료 알림
- 타임딜 시작 알림
- 읽음/안읽음 상태 관리
- 알림 목록 조회 (페이징)

#### 비기능 요구사항
- 알림 전달 지연 < 1초
- 10,000+ 동시 접속 지원
- 오프라인 사용자 알림 저장

### 3.2 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        알림 흐름                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [이벤트 발생]                                                   │
│       │                                                         │
│       ▼                                                         │
│  [Kafka] ──► [notification-service] ──► [알림 저장 (MySQL)]      │
│                      │                                          │
│                      ▼                                          │
│              [Redis Pub/Sub] ──► [WebSocket/SSE] ──► [클라이언트] │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 도메인 설계

```
notification/
├── domain/
│   ├── Notification.java           # 알림 엔티티
│   ├── NotificationType.java       # 알림 유형
│   └── NotificationStatus.java     # READ, UNREAD
├── repository/
│   └── NotificationRepository.java
├── service/
│   ├── NotificationService.java
│   └── NotificationPushService.java
├── controller/
│   ├── NotificationController.java
│   └── NotificationWebSocketController.java
├── consumer/
│   └── NotificationEventConsumer.java
└── dto/
    ├── NotificationResponse.java
    └── NotificationEvent.java
```

### 3.4 엔티티 설계

#### Notification
```java
@Entity
@Table(indexes = {
    @Index(name = "idx_notification_user_status", columnList = "user_id, status"),
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
public class Notification {
    @Id @GeneratedValue
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;          // ORDER, DELIVERY, COUPON, TIMEDEAL, SYSTEM

    private String title;
    private String message;
    private String link;                    // 클릭 시 이동 URL

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;      // UNREAD, READ

    private String referenceId;             // 관련 엔티티 ID (주문번호 등)
    private String referenceType;           // ORDER, DELIVERY, COUPON 등

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
```

#### NotificationType
```java
public enum NotificationType {
    // 주문 관련
    ORDER_CREATED("주문이 접수되었습니다"),
    ORDER_CONFIRMED("주문이 확정되었습니다"),
    ORDER_CANCELLED("주문이 취소되었습니다"),

    // 배송 관련
    DELIVERY_STARTED("상품이 발송되었습니다"),
    DELIVERY_IN_TRANSIT("상품이 배송 중입니다"),
    DELIVERY_COMPLETED("상품이 배송 완료되었습니다"),

    // 결제 관련
    PAYMENT_COMPLETED("결제가 완료되었습니다"),
    PAYMENT_FAILED("결제가 실패했습니다"),
    REFUND_COMPLETED("환불이 완료되었습니다"),

    // 쿠폰 관련
    COUPON_ISSUED("쿠폰이 발급되었습니다"),
    COUPON_EXPIRING("쿠폰이 곧 만료됩니다"),

    // 타임딜 관련
    TIMEDEAL_STARTING("타임딜이 곧 시작됩니다"),
    TIMEDEAL_STARTED("타임딜이 시작되었습니다"),

    // 시스템
    SYSTEM("시스템 알림");

    private final String defaultMessage;
}
```

### 3.5 Kafka Consumer

```java
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;

    @KafkaListener(topics = "shopping.order.created", groupId = "notification-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        Notification notification = notificationService.create(
            event.getUserId(),
            NotificationType.ORDER_CREATED,
            "주문 접수",
            String.format("주문번호 %s가 접수되었습니다.", event.getOrderNumber()),
            "/orders/" + event.getOrderNumber(),
            event.getOrderNumber(),
            "ORDER"
        );

        pushService.push(notification);
    }

    @KafkaListener(topics = "shopping.delivery.shipped", groupId = "notification-group")
    public void handleDeliveryShipped(DeliveryShippedEvent event) {
        Notification notification = notificationService.create(
            event.getUserId(),
            NotificationType.DELIVERY_STARTED,
            "배송 시작",
            String.format("주문하신 상품이 발송되었습니다. 운송장번호: %s", event.getTrackingNumber()),
            "/deliveries/" + event.getTrackingNumber(),
            event.getTrackingNumber(),
            "DELIVERY"
        );

        pushService.push(notification);
    }

    // ... 기타 이벤트 핸들러
}
```

### 3.6 WebSocket 설정 (STOMP)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 구독 경로
        registry.enableSimpleBroker("/topic", "/queue");
        // 클라이언트 발행 경로
        registry.setApplicationDestinationPrefixes("/app");
        // 사용자별 구독
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}
```

### 3.7 실시간 푸시 서비스

```java
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void push(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);

        // 1. WebSocket으로 직접 전송 시도
        messagingTemplate.convertAndSendToUser(
            notification.getUserId().toString(),
            "/queue/notifications",
            response
        );

        // 2. Redis Pub/Sub으로도 발행 (다중 인스턴스 지원)
        String channel = "notification:" + notification.getUserId();
        redisTemplate.convertAndSend(channel, toJson(response));
    }

    public void pushToAll(NotificationResponse response) {
        messagingTemplate.convertAndSend("/topic/notifications", response);
    }
}
```

### 3.8 Redis Pub/Sub Listener (다중 인스턴스)

```java
@Component
@RequiredArgsConstructor
public class NotificationRedisListener {

    private final SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    public void subscribe() {
        // 패턴 구독: notification:*
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.pSubscribe((message, pattern) -> {
                String channel = new String(message.getChannel());
                String userId = channel.split(":")[1];
                String payload = new String(message.getBody());

                messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    payload
                );
            }, "notification:*".getBytes());
            return null;
        });
    }
}
```

### 3.9 API 설계

```
# 알림 조회
GET    /api/notifications                  # 내 알림 목록 (페이징)
GET    /api/notifications/unread/count     # 안읽은 알림 수
PUT    /api/notifications/{id}/read        # 알림 읽음 처리
PUT    /api/notifications/read-all         # 전체 읽음 처리
DELETE /api/notifications/{id}             # 알림 삭제

# WebSocket
WS     /ws/notifications                   # WebSocket 연결
SUB    /user/queue/notifications           # 개인 알림 구독
SUB    /topic/notifications                # 전체 알림 구독 (공지 등)
```

### 3.10 프론트엔드 연동

```typescript
// Vue 3 Composable
export function useNotifications() {
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  let stompClient: Client | null = null

  const connect = (userId: string) => {
    stompClient = new Client({
      brokerURL: 'ws://localhost:8084/ws/notifications',
      onConnect: () => {
        // 개인 알림 구독
        stompClient?.subscribe(`/user/${userId}/queue/notifications`, (message) => {
          const notification = JSON.parse(message.body)
          notifications.value.unshift(notification)
          unreadCount.value++
          showToast(notification)
        })
      }
    })
    stompClient.activate()
  }

  const disconnect = () => {
    stompClient?.deactivate()
  }

  return { notifications, unreadCount, connect, disconnect }
}
```

---

## 4. 실시간 재고 현황

### 4.1 요구사항

#### 기능 요구사항
- 상품 상세 페이지에서 실시간 재고 표시
- 재고 변동 시 즉시 업데이트
- "N개 남음" 표시 (임계값 이하일 때)
- 품절 시 즉시 버튼 비활성화

#### 비기능 요구사항
- 재고 변동 반영 < 500ms
- 페이지당 최대 50개 상품 실시간 업데이트

### 4.2 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     실시간 재고 흐름                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [재고 변동] ──► [Kafka: inventory.changed]                      │
│                        │                                        │
│                        ▼                                        │
│              [Redis Pub/Sub] ──► [SSE] ──► [클라이언트]           │
│                        │                                        │
│                        ▼                                        │
│              [Redis Cache 업데이트]                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 재고 변동 이벤트 발행

```java
// InventoryServiceImpl.java
@Transactional
public void reserveStock(Long productId, int quantity, ...) {
    // ... 재고 예약 로직

    // 재고 변동 이벤트 발행
    kafkaTemplate.send("inventory.changed", InventoryChangedEvent.builder()
        .productId(productId)
        .available(inventory.getAvailable())
        .reserved(inventory.getReserved())
        .timestamp(LocalDateTime.now())
        .build()
    );
}
```

### 4.4 SSE Controller

```java
@RestController
@RequestMapping("/api/shopping/inventory")
@RequiredArgsConstructor
public class InventoryStreamController {

    private final InventoryStreamService streamService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<InventoryUpdate>> streamInventory(
            @RequestParam List<Long> productIds) {

        return streamService.subscribe(productIds)
            .map(update -> ServerSentEvent.<InventoryUpdate>builder()
                .id(String.valueOf(update.getProductId()))
                .event("inventory-update")
                .data(update)
                .build()
            );
    }
}
```

### 4.5 재고 스트림 서비스

```java
@Service
@RequiredArgsConstructor
public class InventoryStreamService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Flux<InventoryUpdate> subscribe(List<Long> productIds) {
        List<String> channels = productIds.stream()
            .map(id -> "inventory:" + id)
            .toList();

        return reactiveRedisTemplate.listenToChannels(channels.toArray(new String[0]))
            .map(message -> parseInventoryUpdate(message.getMessage()));
    }
}
```

### 4.6 프론트엔드 연동

```typescript
// useInventoryStream.ts
export function useInventoryStream(productIds: Ref<number[]>) {
  const inventory = ref<Map<number, InventoryUpdate>>(new Map())
  let eventSource: EventSource | null = null

  const connect = () => {
    const ids = productIds.value.join(',')
    eventSource = new EventSource(`/api/shopping/inventory/stream?productIds=${ids}`)

    eventSource.addEventListener('inventory-update', (event) => {
      const update: InventoryUpdate = JSON.parse(event.data)
      inventory.value.set(update.productId, update)
    })
  }

  const disconnect = () => {
    eventSource?.close()
  }

  return { inventory, connect, disconnect }
}
```

---

## 5. Elasticsearch 통합 검색

### 5.1 요구사항

#### 기능 요구사항
- 상품 검색 (이름, 설명, 카테고리)
- 블로그 검색 (제목, 내용, 태그)
- 통합 검색 (상품 + 블로그)
- 필터링 (가격대, 카테고리, 날짜)
- 정렬 (관련도, 가격, 최신순)
- 하이라이팅 (검색어 강조)

#### 비기능 요구사항
- 검색 응답 시간 < 200ms
- 한글 형태소 분석 지원
- 오타 교정 (fuzzy search)

### 5.2 인프라 설정

#### docker-compose.yml
```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
  container_name: elasticsearch
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
  ports:
    - "9200:9200"
  volumes:
    - es-data:/usr/share/elasticsearch/data
  networks:
    - portal-universe-net

kibana:
  image: docker.elastic.co/kibana/kibana:8.11.0
  container_name: kibana
  ports:
    - "5601:5601"
  environment:
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
  depends_on:
    - elasticsearch
  networks:
    - portal-universe-net
```

### 5.3 인덱스 설계

#### 상품 인덱스 (products)
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
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": { "type": "keyword" },
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
      "category": { "type": "keyword" },
      "price": { "type": "double" },
      "stock": { "type": "integer" },
      "imageUrl": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

#### 블로그 인덱스 (posts)
```json
{
  "settings": {
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
      "id": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": {
            "type": "completion",
            "analyzer": "korean"
          }
        }
      },
      "content": {
        "type": "text",
        "analyzer": "korean"
      },
      "summary": {
        "type": "text",
        "analyzer": "korean"
      },
      "tags": { "type": "keyword" },
      "authorId": { "type": "keyword" },
      "authorName": { "type": "keyword" },
      "viewCount": { "type": "integer" },
      "likeCount": { "type": "integer" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

### 5.4 도메인 설계

```
search/
├── config/
│   └── ElasticsearchConfig.java
├── document/
│   ├── ProductDocument.java
│   └── PostDocument.java
├── repository/
│   ├── ProductSearchRepository.java
│   └── PostSearchRepository.java
├── service/
│   ├── SearchService.java
│   ├── ProductIndexService.java
│   └── PostIndexService.java
├── controller/
│   └── SearchController.java
├── sync/
│   ├── ProductSyncConsumer.java      # Kafka → ES 동기화
│   └── PostSyncConsumer.java
└── dto/
    ├── SearchRequest.java
    ├── SearchResponse.java
    ├── ProductSearchResult.java
    └── PostSearchResult.java
```

### 5.5 검색 서비스

```java
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient esClient;

    public SearchResponse<ProductSearchResult> searchProducts(ProductSearchRequest request) {
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index("products")
            .query(q -> q
                .bool(b -> {
                    // 검색어
                    if (StringUtils.hasText(request.getKeyword())) {
                        b.must(m -> m
                            .multiMatch(mm -> mm
                                .query(request.getKeyword())
                                .fields("name^3", "description", "category")
                                .fuzziness("AUTO")
                            )
                        );
                    }

                    // 카테고리 필터
                    if (StringUtils.hasText(request.getCategory())) {
                        b.filter(f -> f
                            .term(t -> t.field("category").value(request.getCategory()))
                        );
                    }

                    // 가격 범위 필터
                    if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                        b.filter(f -> f
                            .range(r -> {
                                r.field("price");
                                if (request.getMinPrice() != null) r.gte(JsonData.of(request.getMinPrice()));
                                if (request.getMaxPrice() != null) r.lte(JsonData.of(request.getMaxPrice()));
                                return r;
                            })
                        );
                    }

                    return b;
                })
            )
            .highlight(h -> h
                .fields("name", f -> f.preTags("<em>").postTags("</em>"))
                .fields("description", f -> f.preTags("<em>").postTags("</em>"))
            )
            .from(request.getPage() * request.getSize())
            .size(request.getSize())
            .sort(buildSort(request.getSort()))
        );

        SearchResponse<ProductDocument> response = esClient.search(searchRequest, ProductDocument.class);

        return mapToSearchResponse(response);
    }

    public UnifiedSearchResponse searchAll(String keyword, int page, int size) {
        // 상품과 블로그 동시 검색
        List<ProductSearchResult> products = searchProducts(
            ProductSearchRequest.of(keyword, page, size / 2)
        ).getResults();

        List<PostSearchResult> posts = searchPosts(
            PostSearchRequest.of(keyword, page, size / 2)
        ).getResults();

        return UnifiedSearchResponse.of(products, posts);
    }
}
```

### 5.6 데이터 동기화 (Kafka Consumer)

```java
@Component
@RequiredArgsConstructor
public class ProductSyncConsumer {

    private final ProductIndexService indexService;

    @KafkaListener(topics = "product.created", groupId = "search-sync")
    public void handleProductCreated(ProductCreatedEvent event) {
        indexService.index(event.getProduct());
    }

    @KafkaListener(topics = "product.updated", groupId = "search-sync")
    public void handleProductUpdated(ProductUpdatedEvent event) {
        indexService.update(event.getProduct());
    }

    @KafkaListener(topics = "product.deleted", groupId = "search-sync")
    public void handleProductDeleted(ProductDeletedEvent event) {
        indexService.delete(event.getProductId());
    }

    @KafkaListener(topics = "inventory.changed", groupId = "search-sync")
    public void handleInventoryChanged(InventoryChangedEvent event) {
        indexService.updateStock(event.getProductId(), event.getAvailable());
    }
}
```

### 5.7 API 설계

```
# 상품 검색
GET /api/search/products?keyword=키워드&category=전자제품&minPrice=10000&maxPrice=100000&sort=price_asc&page=0&size=20

# 블로그 검색
GET /api/search/posts?keyword=키워드&tags=java,spring&sort=relevance&page=0&size=20

# 통합 검색
GET /api/search?keyword=키워드&page=0&size=20

# 자동완성
GET /api/search/suggest?keyword=아이&type=product
GET /api/search/suggest?keyword=스프&type=post
```

---

## 6. 검색어 자동완성

### 6.1 요구사항

#### 기능 요구사항
- 입력 중 실시간 추천
- 상품명/블로그 제목 기반 추천
- 인기 검색어 추천
- 최근 검색어 표시

#### 비기능 요구사항
- 응답 시간 < 50ms
- 디바운싱 적용 (300ms)

### 6.2 Elasticsearch Completion Suggester

```java
@Service
@RequiredArgsConstructor
public class SuggestService {

    private final ElasticsearchClient esClient;
    private final RedisTemplate<String, String> redisTemplate;

    public List<String> suggest(String keyword, String type, int size) {
        String index = "product".equals(type) ? "products" : "posts";
        String field = "product".equals(type) ? "name.suggest" : "title.suggest";

        SearchRequest request = SearchRequest.of(s -> s
            .index(index)
            .suggest(su -> su
                .suggesters("suggestions", sg -> sg
                    .prefix(keyword)
                    .completion(c -> c
                        .field(field)
                        .size(size)
                        .skipDuplicates(true)
                        .fuzzy(f -> f.fuzziness("AUTO"))
                    )
                )
            )
        );

        SearchResponse<Void> response = esClient.search(request, Void.class);

        return response.suggest().get("suggestions").stream()
            .flatMap(s -> s.completion().options().stream())
            .map(o -> o.text())
            .toList();
    }

    // 인기 검색어 (Redis Sorted Set)
    public List<String> getPopularKeywords(int size) {
        return redisTemplate.opsForZSet()
            .reverseRange("search:popular", 0, size - 1)
            .stream().toList();
    }

    // 검색어 카운트 증가
    public void incrementSearchCount(String keyword) {
        redisTemplate.opsForZSet().incrementScore("search:popular", keyword, 1);
    }

    // 최근 검색어 (사용자별)
    public List<String> getRecentKeywords(Long userId, int size) {
        String key = "search:recent:" + userId;
        return redisTemplate.opsForList().range(key, 0, size - 1);
    }

    public void addRecentKeyword(Long userId, String keyword) {
        String key = "search:recent:" + userId;
        redisTemplate.opsForList().remove(key, 0, keyword);  // 중복 제거
        redisTemplate.opsForList().leftPush(key, keyword);
        redisTemplate.opsForList().trim(key, 0, 19);  // 최대 20개 유지
    }
}
```

### 6.3 API 설계

```
# 자동완성
GET /api/search/suggest?keyword=아이폰&type=product&size=5

# 인기 검색어
GET /api/search/popular?size=10

# 최근 검색어
GET /api/search/recent?size=10
DELETE /api/search/recent/{keyword}
DELETE /api/search/recent  # 전체 삭제
```

### 6.4 프론트엔드 연동

```typescript
// useSearch.ts
export function useSearch() {
  const suggestions = ref<string[]>([])
  const popularKeywords = ref<string[]>([])
  const recentKeywords = ref<string[]>([])

  // 디바운싱된 자동완성
  const fetchSuggestions = useDebounceFn(async (keyword: string) => {
    if (keyword.length < 2) {
      suggestions.value = []
      return
    }
    const { data } = await api.get('/search/suggest', {
      params: { keyword, type: 'product', size: 5 }
    })
    suggestions.value = data
  }, 300)

  const search = async (keyword: string) => {
    // 검색 수행
    const results = await api.get('/search', { params: { keyword } })

    // 최근 검색어에 추가
    await api.post('/search/recent', { keyword })

    return results
  }

  return { suggestions, popularKeywords, recentKeywords, fetchSuggestions, search }
}
```

---

## 7. 에러 코드 (Phase 3)

```java
// 알림 관련 (S9XX)
NOTIFICATION_NOT_FOUND(NOT_FOUND, "S901", "알림을 찾을 수 없습니다"),
NOTIFICATION_ACCESS_DENIED(FORBIDDEN, "S902", "알림 접근 권한이 없습니다"),
WEBSOCKET_CONNECTION_FAILED(INTERNAL_SERVER_ERROR, "S903", "WebSocket 연결에 실패했습니다"),

// 검색 관련 (S10XX)
SEARCH_FAILED(INTERNAL_SERVER_ERROR, "S1001", "검색 중 오류가 발생했습니다"),
INVALID_SEARCH_QUERY(BAD_REQUEST, "S1002", "유효하지 않은 검색어입니다"),
INDEX_NOT_FOUND(INTERNAL_SERVER_ERROR, "S1003", "검색 인덱스를 찾을 수 없습니다"),
SUGGEST_FAILED(INTERNAL_SERVER_ERROR, "S1004", "자동완성 조회에 실패했습니다"),
```

---

## 8. 데이터베이스 마이그레이션

### 8.1 알림 테이블

```sql
-- V11__Create_notification_tables.sql

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME,
    INDEX idx_notification_user_status (user_id, status),
    INDEX idx_notification_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.2 검색 관련 테이블 (선택적)

```sql
-- V12__Create_search_tables.sql

-- 인기 검색어 저장 (백업용, Redis 장애 시)
CREATE TABLE popular_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL UNIQUE,
    search_count BIGINT DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_keyword_count (search_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 9. 인프라 설정 요약

### 9.1 docker-compose.yml 추가 사항

```yaml
services:
  # ... 기존 서비스

  redis:
    # Phase 2에서 이미 추가됨

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    networks:
      - portal-universe-net
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]
      interval: 30s
      timeout: 10s
      retries: 5

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - portal-universe-net

volumes:
  es-data:
```

### 9.2 application.yml 추가 설정

```yaml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://localhost:9200}

  # WebSocket (STOMP)
  websocket:
    allowed-origins: "*"
```

---

## 10. 테스트 계획

### 10.1 단위 테스트
- NotificationService 테스트
- SearchService 테스트
- SuggestService 테스트

### 10.2 통합 테스트 (TestContainers)
```java
@Test
void 알림_생성_및_WebSocket_전송_테스트() {
    // given: 사용자 WebSocket 연결
    // when: 주문 생성 이벤트 발생
    // then: 알림 저장 + WebSocket 메시지 수신
}

@Test
void 상품_검색_테스트() {
    // given: ES에 상품 인덱싱
    // when: 검색 쿼리 실행
    // then: 관련도 높은 순으로 결과 반환
}
```

### 10.3 부하 테스트
```javascript
// k6 - WebSocket 연결 부하 테스트
export default function() {
    const ws = new WebSocket('ws://localhost:8084/ws/notifications');
    ws.onopen = () => {
        ws.send(JSON.stringify({ type: 'SUBSCRIBE' }));
    };
    sleep(60);  // 1분간 연결 유지
    ws.close();
}

export const options = {
    vus: 5000,  // 동시 WebSocket 연결
    duration: '5m',
};
```

---

## 11. 구현 순서

| 순서 | 작업 | 예상 기간 |
|------|------|----------|
| 1 | Elasticsearch 인프라 설정 | 0.5일 |
| 2 | 알림 도메인 구현 | 1.5일 |
| 3 | WebSocket/STOMP 설정 | 1일 |
| 4 | Kafka → 알림 연동 | 1일 |
| 5 | 실시간 재고 SSE 구현 | 1일 |
| 6 | ES 인덱스 설계 + 매핑 | 0.5일 |
| 7 | 상품 검색 서비스 | 1.5일 |
| 8 | 블로그 검색 서비스 | 1일 |
| 9 | 통합 검색 + 필터/정렬 | 1일 |
| 10 | 자동완성 구현 | 1일 |
| 11 | 인기/최근 검색어 | 0.5일 |
| 12 | 데이터 동기화 (Kafka → ES) | 1일 |
| 13 | 프론트엔드 연동 | 2일 |
| 14 | 테스트 + 최적화 | 2일 |
| **총계** | | **~15일** |

---

## 12. 참고 자료

- [Spring WebSocket STOMP](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)
- [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [Elasticsearch Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Nori Korean Analyzer](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-nori.html)
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/)
