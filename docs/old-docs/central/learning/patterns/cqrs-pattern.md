# CQRS (Command Query Responsibility Segregation) 패턴

## 학습 목표
- CQRS의 핵심 개념과 원리 이해
- Command/Query 분리의 장점과 적용 시점 파악
- 읽기/쓰기 모델 분리 전략 학습
- Portal Universe에서의 CQRS 적용 방안 분석

---

## 1. CQRS 개요

### 1.1 전통적인 CRUD vs CQRS

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CRUD vs CQRS                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   전통적인 CRUD:                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   Client ──► [Same Model] ──► Database                              │  │
│   │              Create │                                                │  │
│   │              Read   │  ← 동일한 모델로 읽기/쓰기                      │  │
│   │              Update │                                                │  │
│   │              Delete │                                                │  │
│   │                                                                      │  │
│   │   문제점:                                                             │  │
│   │   • 복잡한 조회 요구사항에 대응 어려움                                 │  │
│   │   • 읽기/쓰기 성능 최적화 충돌                                        │  │
│   │   • 도메인 모델이 조회 요구사항에 오염                                 │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   CQRS:                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │              ┌─────────────┐        ┌─────────────┐                  │  │
│   │   Client ──► │ Write Model │ ──────►│ Write DB    │                  │  │
│   │   (Command)  │ (Domain)    │        │ (PostgreSQL)│                  │  │
│   │              └─────────────┘        └─────────────┘                  │  │
│   │                    │                       │                         │  │
│   │                    │ Sync/Event            │                         │  │
│   │                    ▼                       ▼                         │  │
│   │              ┌─────────────┐        ┌─────────────┐                  │  │
│   │   Client ◄── │ Read Model  │ ◄──────│ Read DB     │                  │  │
│   │   (Query)    │ (DTO/View)  │        │ (MongoDB)   │                  │  │
│   │              └─────────────┘        └─────────────┘                  │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 개념

| 개념 | 설명 |
|------|------|
| **Command** | 상태를 변경하는 명령 (Create, Update, Delete) |
| **Query** | 상태를 조회하는 요청 (Read) |
| **Write Model** | 비즈니스 로직과 유효성 검증에 최적화 |
| **Read Model** | 조회 성능에 최적화된 비정규화 뷰 |
| **Synchronization** | Write → Read 모델 동기화 메커니즘 |

### 1.3 CQRS 적용 수준

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CQRS COMPLEXITY LEVELS                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Level 1: 단순 분리 (Same Database)                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Write Service ──┬──► PostgreSQL                                    │  │
│   │  Read Service  ──┘                                                   │  │
│   │                                                                      │  │
│   │  • 같은 DB, 다른 Repository                                          │  │
│   │  • 읽기용 최적화 쿼리 분리                                            │  │
│   │  • 가장 간단한 형태                                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   Level 2: 분리된 스토어 (Different Databases)                               │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Write Service ──► PostgreSQL ──(Sync)──► MongoDB ◄── Read Service  │  │
│   │                                                                      │  │
│   │  • 목적에 맞는 DB 선택                                                │  │
│   │  • Eventual Consistency                                              │  │
│   │  • 동기화 전략 필요                                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   Level 3: Event Sourcing + CQRS                                            │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Write ──► Event Store ──(Events)──► Read Model (Projection)        │  │
│   │                                                                      │  │
│   │  • 완전한 이벤트 기반 동기화                                          │  │
│   │  • 다양한 Projection 가능                                             │  │
│   │  • 가장 복잡하지만 유연함                                             │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Command 설계

### 2.1 Command 구조

```java
/**
 * Command Marker Interface
 */
public interface Command<R> {
    // R: Command 실행 결과 타입
}

/**
 * 주문 생성 Command
 */
public record CreateOrderCommand(
    String customerId,
    List<OrderItemDto> items,
    String shippingAddress,
    PaymentMethod paymentMethod
) implements Command<String> {

    // 유효성 검증 (불변 객체이므로 생성자에서)
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId is required");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be empty");
        }
        Objects.requireNonNull(shippingAddress, "shippingAddress is required");
    }
}

/**
 * 주문 취소 Command
 */
public record CancelOrderCommand(
    String orderId,
    String reason
) implements Command<Void> {

    public CancelOrderCommand {
        Objects.requireNonNull(orderId, "orderId is required");
        Objects.requireNonNull(reason, "cancellation reason is required");
    }
}

/**
 * 주문 상태 변경 Command
 */
public record UpdateOrderStatusCommand(
    String orderId,
    OrderStatus newStatus,
    String note
) implements Command<Void> {}
```

### 2.2 Command Handler

```java
/**
 * Command Handler Interface
 */
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}

/**
 * 주문 생성 Command Handler
 */
@Service
@RequiredArgsConstructor
public class CreateOrderCommandHandler
        implements CommandHandler<CreateOrderCommand, String> {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public String handle(CreateOrderCommand command) {
        // 1. 재고 확인
        for (OrderItemDto item : command.items()) {
            if (!inventoryService.isAvailable(item.productId(), item.quantity())) {
                throw new CustomBusinessException(
                    ShoppingErrorCode.INSUFFICIENT_STOCK
                );
            }
        }

        // 2. 주문 생성
        Order order = Order.create(
            command.customerId(),
            command.items(),
            command.shippingAddress(),
            command.paymentMethod()
        );

        // 3. 저장
        orderRepository.save(order);

        // 4. 도메인 이벤트 발행
        eventPublisher.publishEvent(new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount()
        ));

        return order.getId();
    }
}

/**
 * 주문 취소 Command Handler
 */
@Service
@RequiredArgsConstructor
public class CancelOrderCommandHandler
        implements CommandHandler<CancelOrderCommand, Void> {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Void handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));

        // 도메인 로직 실행
        order.cancel(command.reason());

        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCancelledEvent(
            order.getId(),
            command.reason()
        ));

        return null;
    }
}
```

### 2.3 Command Bus (Optional)

```java
/**
 * Command Bus - 중앙 집중식 Command 라우팅
 */
@Component
@RequiredArgsConstructor
public class CommandBus {

    private final ApplicationContext applicationContext;
    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Handler 자동 등록
        applicationContext.getBeansOfType(CommandHandler.class)
            .values()
            .forEach(this::registerHandler);
    }

    @SuppressWarnings("unchecked")
    public <C extends Command<R>, R> R dispatch(C command) {
        CommandHandler<C, R> handler = (CommandHandler<C, R>)
            handlers.get(command.getClass());

        if (handler == null) {
            throw new IllegalArgumentException(
                "No handler found for: " + command.getClass().getName()
            );
        }

        return handler.handle(command);
    }

    private void registerHandler(CommandHandler<?, ?> handler) {
        // 리플렉션으로 Command 타입 추출 및 등록
        Type[] interfaces = handler.getClass().getGenericInterfaces();
        // ... 타입 파라미터 추출 로직
    }
}

// 사용 예시
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final CommandBus commandBus;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<String>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        String orderId = commandBus.dispatch(
            new CreateOrderCommand(
                request.getCustomerId(),
                request.getItems(),
                request.getShippingAddress(),
                request.getPaymentMethod()
            )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(orderId));
    }
}
```

---

## 3. Query 설계

### 3.1 Query 구조

```java
/**
 * Query Marker Interface
 */
public interface Query<R> {
    // R: Query 결과 타입
}

/**
 * 주문 상세 조회 Query
 */
public record GetOrderDetailQuery(
    String orderId
) implements Query<OrderDetailDto> {}

/**
 * 주문 목록 조회 Query
 */
public record GetOrderListQuery(
    String customerId,
    OrderStatus status,
    LocalDate fromDate,
    LocalDate toDate,
    int page,
    int size
) implements Query<Page<OrderSummaryDto>> {

    // 기본값 설정
    public GetOrderListQuery {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}

/**
 * 주문 검색 Query
 */
public record SearchOrdersQuery(
    String keyword,
    List<OrderStatus> statuses,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    SortField sortBy,
    SortDirection direction,
    int page,
    int size
) implements Query<Page<OrderSearchResultDto>> {}
```

### 3.2 Query Handler

```java
/**
 * Query Handler Interface
 */
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}

/**
 * 주문 상세 조회 Query Handler
 */
@Service
@RequiredArgsConstructor
public class GetOrderDetailQueryHandler
        implements QueryHandler<GetOrderDetailQuery, OrderDetailDto> {

    private final OrderReadRepository orderReadRepository;

    @Override
    @Transactional(readOnly = true)  // 읽기 전용 트랜잭션
    public OrderDetailDto handle(GetOrderDetailQuery query) {
        return orderReadRepository.findOrderDetail(query.orderId())
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));
    }
}

/**
 * 주문 목록 조회 Query Handler
 */
@Service
@RequiredArgsConstructor
public class GetOrderListQueryHandler
        implements QueryHandler<GetOrderListQuery, Page<OrderSummaryDto>> {

    private final OrderReadRepository orderReadRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> handle(GetOrderListQuery query) {
        return orderReadRepository.findOrderSummaries(
            query.customerId(),
            query.status(),
            query.fromDate(),
            query.toDate(),
            PageRequest.of(query.page(), query.size())
        );
    }
}
```

### 3.3 Read Model (비정규화된 뷰)

```java
/**
 * 주문 상세 DTO - 조회 최적화
 */
@Data
@Builder
public class OrderDetailDto {
    private String orderId;
    private String orderNumber;
    private OrderStatus status;
    private String statusDisplayName;

    // Customer 정보 (조인 없이 포함)
    private String customerId;
    private String customerName;
    private String customerEmail;

    // 상품 목록
    private List<OrderItemDto> items;

    // 금액 정보
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // 배송 정보
    private AddressDto shippingAddress;
    private String trackingNumber;
    private String carrier;

    // 결제 정보
    private PaymentInfoDto payment;

    // 시간 정보
    private LocalDateTime orderedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    // 계산된 필드
    private boolean cancellable;
    private boolean returnable;
    private long daysUntilReturnExpiry;
}

/**
 * 주문 요약 DTO - 목록 조회용
 */
@Data
@Builder
public class OrderSummaryDto {
    private String orderId;
    private String orderNumber;
    private OrderStatus status;

    // 대표 상품 정보
    private String mainProductName;
    private String mainProductImageUrl;
    private int totalItemCount;

    private BigDecimal totalAmount;
    private LocalDateTime orderedAt;
}
```

---

## 4. 읽기/쓰기 모델 분리

### 4.1 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CQRS ARCHITECTURE                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌───────────────────────────────────────────────────────────────────┐    │
│   │                         API Layer                                  │    │
│   │  ┌─────────────────┐              ┌─────────────────┐             │    │
│   │  │ POST /orders    │              │ GET /orders     │             │    │
│   │  │ PUT /orders/:id │              │ GET /orders/:id │             │    │
│   │  │ DELETE /orders  │              │ GET /orders/search │          │    │
│   │  └────────┬────────┘              └────────┬────────┘             │    │
│   │           │                                │                       │    │
│   └───────────┼────────────────────────────────┼───────────────────────┘    │
│               │                                │                             │
│               ▼                                ▼                             │
│   ┌───────────────────────┐      ┌───────────────────────┐                  │
│   │    Command Side       │      │     Query Side        │                  │
│   │  ┌─────────────────┐  │      │  ┌─────────────────┐  │                  │
│   │  │ Command Handler │  │      │  │ Query Handler   │  │                  │
│   │  └────────┬────────┘  │      │  └────────┬────────┘  │                  │
│   │           │           │      │           │           │                  │
│   │  ┌────────▼────────┐  │      │  ┌────────▼────────┐  │                  │
│   │  │ Domain Model    │  │      │  │ Read Repository │  │                  │
│   │  │ (Order Entity)  │  │      │  │ (Query DSL)     │  │                  │
│   │  └────────┬────────┘  │      │  └────────┬────────┘  │                  │
│   │           │           │      │           │           │                  │
│   │  ┌────────▼────────┐  │      │  ┌────────▼────────┐  │                  │
│   │  │ Write Repository│  │      │  │ Read Database   │  │                  │
│   │  └────────┬────────┘  │      │  │ (MongoDB/ES)    │  │                  │
│   └───────────┼───────────┘      └───────────┼───────────┘                  │
│               │                              ▲                               │
│               ▼                              │                               │
│   ┌───────────────────────┐                  │                               │
│   │   Write Database      │──────(Sync)──────┘                               │
│   │   (PostgreSQL)        │                                                  │
│   └───────────────────────┘                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Write Model (도메인 중심)

```java
/**
 * Write Model - Order Aggregate
 * 비즈니스 규칙과 불변식을 보장
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    private ShippingInfo shippingInfo;

    @Embedded
    private PaymentInfo paymentInfo;

    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Factory Method
    public static Order create(String customerId, List<OrderItemDto> items,
                               String shippingAddress, PaymentMethod paymentMethod) {
        Order order = new Order();
        order.id = UUID.randomUUID().toString();
        order.customerId = customerId;
        order.status = OrderStatus.PENDING;
        order.createdAt = LocalDateTime.now();

        // 비즈니스 규칙: 최소 1개 이상의 상품
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        items.forEach(item -> order.addItem(item));
        order.calculateTotal();
        order.shippingInfo = new ShippingInfo(shippingAddress);
        order.paymentInfo = new PaymentInfo(paymentMethod);

        return order;
    }

    // 도메인 로직: 주문 취소
    public void cancel(String reason) {
        // 비즈니스 규칙: 배송 전에만 취소 가능
        if (!isCancellable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED
            );
        }

        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 규칙
    public boolean isCancellable() {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.PAID ||
               status == OrderStatus.CONFIRMED;
    }

    private void calculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

### 4.3 Read Model (조회 최적화)

```java
/**
 * Read Model - MongoDB Document
 * 조회 성능에 최적화된 비정규화 구조
 */
@Document(collection = "order_views")
@Data
@Builder
public class OrderReadModel {

    @Id
    private String orderId;

    private String orderNumber;
    private String customerId;
    private String customerName;      // 비정규화: Customer 정보 포함
    private String customerEmail;

    private String status;
    private String statusDisplayName;

    // 비정규화된 상품 목록
    private List<OrderItemView> items;

    // 대표 상품 (목록 표시용)
    private String mainProductName;
    private String mainProductImageUrl;
    private int totalItemCount;

    // 금액
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // 배송
    private AddressView shippingAddress;
    private String trackingNumber;
    private String carrier;

    // 결제
    private String paymentMethod;
    private String paymentStatus;

    // 시간
    private LocalDateTime orderedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    // 검색/필터용 인덱스 필드
    @Indexed
    private LocalDate orderDate;

    @Indexed
    private BigDecimal amountForRange;  // 금액 범위 검색용

    // 계산된 필드 (읽기 시점에 결정)
    private boolean cancellable;
    private boolean returnable;

    private LocalDateTime lastUpdatedAt;
}

/**
 * Read Repository - 조회 전용
 */
public interface OrderReadRepository extends MongoRepository<OrderReadModel, String> {

    @Query("{ 'customerId': ?0, 'status': ?1 }")
    Page<OrderReadModel> findByCustomerIdAndStatus(
        String customerId, String status, Pageable pageable);

    @Query("{ 'customerName': { $regex: ?0, $options: 'i' } }")
    List<OrderReadModel> searchByCustomerName(String keyword);

    // QueryDSL 또는 Aggregation을 활용한 복잡한 조회
    @Aggregation(pipeline = {
        "{ $match: { 'orderDate': { $gte: ?0, $lte: ?1 } } }",
        "{ $group: { _id: '$status', count: { $sum: 1 }, total: { $sum: '$totalAmount' } } }"
    })
    List<OrderStatusSummary> getStatusSummary(LocalDate from, LocalDate to);
}
```

---

## 5. 동기화 전략

### 5.1 동기화 방식 비교

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SYNCHRONIZATION STRATEGIES                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. 동기식 업데이트 (Same Transaction)                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  @Transactional                                                      │  │
│   │  void createOrder(command) {                                         │  │
│   │      writeRepo.save(order);        // Write DB                       │  │
│   │      readRepo.save(readModel);     // Read DB                        │  │
│   │  }                                                                   │  │
│   │                                                                      │  │
│   │  장점: 강한 일관성                                                    │  │
│   │  단점: 트랜잭션 범위 증가, 성능 저하                                  │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   2. 비동기 이벤트 (Eventual Consistency)                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Write ──► Event ──► Kafka ──► Projection ──► Read DB               │  │
│   │                                                                      │  │
│   │  장점: 느슨한 결합, 확장성                                            │  │
│   │  단점: 일시적 불일치, 복잡성                                          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   3. CDC (Change Data Capture)                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Write DB ──► Debezium ──► Kafka ──► Projection ──► Read DB         │  │
│   │                                                                      │  │
│   │  장점: 애플리케이션 수정 최소화                                       │  │
│   │  단점: 인프라 복잡성, 스키마 의존성                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 이벤트 기반 Projection

```java
/**
 * Order Projection Service
 * 도메인 이벤트를 구독하여 Read Model 업데이트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProjectionService {

    private final OrderReadRepository readRepository;
    private final CustomerClient customerClient;  // Feign Client

    /**
     * 주문 생성 이벤트 처리
     */
    @KafkaListener(topics = "order-events", groupId = "order-projection")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        switch (event) {
            case OrderCreatedEvent e -> createProjection(e);
            case OrderPaidEvent e -> updatePaymentStatus(e);
            case OrderShippedEvent e -> updateShippingStatus(e);
            case OrderCancelledEvent e -> updateCancelledStatus(e);
            default -> log.warn("Unknown event type: {}", event.getClass());
        }
    }

    private void createProjection(OrderCreatedEvent event) {
        // Customer 정보 조회 (비정규화를 위해)
        CustomerInfo customer = customerClient.getCustomer(event.getCustomerId());

        OrderReadModel readModel = OrderReadModel.builder()
            .orderId(event.getOrderId())
            .orderNumber(generateOrderNumber(event))
            .customerId(event.getCustomerId())
            .customerName(customer.getName())
            .customerEmail(customer.getEmail())
            .status(OrderStatus.PENDING.name())
            .statusDisplayName("주문 접수")
            .items(mapToItemViews(event.getItems()))
            .mainProductName(event.getItems().get(0).getProductName())
            .mainProductImageUrl(event.getItems().get(0).getImageUrl())
            .totalItemCount(event.getItems().size())
            .totalAmount(event.getTotalAmount())
            .orderedAt(event.getOccurredAt())
            .orderDate(event.getOccurredAt().toLocalDate())
            .cancellable(true)
            .returnable(false)
            .lastUpdatedAt(LocalDateTime.now())
            .build();

        readRepository.save(readModel);
        log.info("Created order projection: {}", event.getOrderId());
    }

    private void updateShippingStatus(OrderShippedEvent event) {
        readRepository.findById(event.getOrderId())
            .ifPresent(model -> {
                model.setStatus(OrderStatus.SHIPPED.name());
                model.setStatusDisplayName("배송 중");
                model.setTrackingNumber(event.getTrackingNumber());
                model.setCarrier(event.getCarrier());
                model.setShippedAt(event.getOccurredAt());
                model.setCancellable(false);
                model.setReturnable(false);
                model.setLastUpdatedAt(LocalDateTime.now());
                readRepository.save(model);
            });
    }
}
```

### 5.3 Eventual Consistency 처리

```java
/**
 * 읽기 측에서 Eventual Consistency 처리
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderReadRepository readRepository;
    private final OrderWriteRepository writeRepository;  // Fallback용

    /**
     * Read Model에서 조회, 없으면 Write DB에서 조회
     */
    public OrderDetailDto getOrder(String orderId) {
        return readRepository.findById(orderId)
            .map(this::toDetailDto)
            .orElseGet(() -> {
                // Read Model에 없으면 Write DB에서 조회 (Fallback)
                log.warn("Order not found in read model, falling back: {}", orderId);
                return writeRepository.findById(orderId)
                    .map(this::toDetailDtoFromEntity)
                    .orElseThrow(() -> new CustomBusinessException(
                        ShoppingErrorCode.ORDER_NOT_FOUND
                    ));
            });
    }

    /**
     * 최신 데이터가 필요한 경우 Write DB 직접 조회
     */
    public OrderDetailDto getOrderConsistent(String orderId) {
        return writeRepository.findById(orderId)
            .map(this::toDetailDtoFromEntity)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));
    }
}
```

---

## 6. Portal Universe 적용

### 6.1 Shopping Service CQRS 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                 PORTAL UNIVERSE - SHOPPING SERVICE CQRS                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   services/shopping-service/                                                │
│   ├── src/main/java/                                                        │
│   │   └── com/portal/universe/shoppingservice/                              │
│   │       ├── order/                                                        │
│   │       │   ├── command/           # Command 측                           │
│   │       │   │   ├── CreateOrderCommand.java                               │
│   │       │   │   ├── CancelOrderCommand.java                               │
│   │       │   │   └── handler/                                              │
│   │       │   │       ├── CreateOrderCommandHandler.java                    │
│   │       │   │       └── CancelOrderCommandHandler.java                    │
│   │       │   │                                                             │
│   │       │   ├── query/             # Query 측                             │
│   │       │   │   ├── GetOrderDetailQuery.java                              │
│   │       │   │   ├── GetOrderListQuery.java                                │
│   │       │   │   └── handler/                                              │
│   │       │   │       ├── GetOrderDetailQueryHandler.java                   │
│   │       │   │       └── GetOrderListQueryHandler.java                     │
│   │       │   │                                                             │
│   │       │   ├── domain/            # Write Model                          │
│   │       │   │   ├── Order.java                                            │
│   │       │   │   └── OrderItem.java                                        │
│   │       │   │                                                             │
│   │       │   ├── read/              # Read Model                           │
│   │       │   │   ├── OrderReadModel.java                                   │
│   │       │   │   └── OrderProjectionService.java                           │
│   │       │   │                                                             │
│   │       │   └── repository/                                               │
│   │       │       ├── OrderWriteRepository.java      # JPA                  │
│   │       │       └── OrderReadRepository.java       # MongoDB              │
│   │       │                                                                 │
│   │       └── ...                                                           │
│   │                                                                         │
│   └── src/main/resources/                                                   │
│       └── application.yml                                                   │
│                                                                             │
│   데이터베이스:                                                               │
│   ┌─────────────────┐    ┌─────────────────┐                                │
│   │ PostgreSQL      │    │ MongoDB         │                                │
│   │ (Write - orders)│    │ (Read - views)  │                                │
│   └─────────────────┘    └─────────────────┘                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 설정 예시

```yaml
# application.yml
spring:
  # Write DB - PostgreSQL
  datasource:
    url: jdbc:postgresql://localhost:5432/shopping_write
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: orders

  # Read DB - MongoDB
  data:
    mongodb:
      uri: mongodb://localhost:27017/shopping_read
      database: shopping_read

# Kafka
spring.kafka:
  bootstrap-servers: localhost:9092
  consumer:
    group-id: order-projection
    auto-offset-reset: earliest
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### 6.3 Controller 예시

```java
/**
 * Order Controller - Command/Query 분리
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    // Command Handlers
    private final CreateOrderCommandHandler createOrderHandler;
    private final CancelOrderCommandHandler cancelOrderHandler;

    // Query Handlers
    private final GetOrderDetailQueryHandler getOrderDetailHandler;
    private final GetOrderListQueryHandler getOrderListHandler;

    // === Commands ===

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        String orderId = createOrderHandler.handle(
            new CreateOrderCommand(
                request.getCustomerId(),
                request.getItems(),
                request.getShippingAddress(),
                request.getPaymentMethod()
            )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        cancelOrderHandler.handle(
            new CancelOrderCommand(orderId, request.getReason())
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // === Queries ===

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrder(
            @PathVariable String orderId) {

        OrderDetailDto order = getOrderDetailHandler.handle(
            new GetOrderDetailQuery(orderId)
        );

        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderSummaryDto>>> getOrders(
            @RequestParam String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderSummaryDto> orders = getOrderListHandler.handle(
            new GetOrderListQuery(customerId, status, from, to, page, size)
        );

        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
```

---

## 7. 모범 사례

### 7.1 CQRS 적용 판단 기준

| 적용 권장 | 적용 비권장 |
|----------|-----------|
| 읽기/쓰기 비율 차이가 큰 경우 | 단순 CRUD 애플리케이션 |
| 복잡한 조회 요구사항 | 읽기/쓰기 패턴이 유사 |
| 높은 확장성 필요 | 팀이 작고 경험 부족 |
| Event Sourcing 사용 시 | 강한 일관성이 필수 |
| 다양한 조회 뷰 필요 | 단순한 도메인 |

### 7.2 주의사항

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CQRS CONSIDERATIONS                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. Eventual Consistency 처리                                              │
│      • UI에서 적절한 피드백 제공                                             │
│      • "주문이 처리 중입니다" 메시지                                         │
│      • Polling 또는 WebSocket으로 상태 업데이트                              │
│                                                                             │
│   2. 동기화 실패 처리                                                        │
│      • Dead Letter Queue 활용                                               │
│      • 재시도 로직 구현                                                      │
│      • 모니터링 및 알림                                                      │
│                                                                             │
│   3. 데이터 정합성 검증                                                       │
│      • 정기적인 Write/Read 비교                                              │
│      • 불일치 감지 및 자동 복구                                              │
│                                                                             │
│   4. 복잡성 관리                                                             │
│      • 필요한 곳에만 적용                                                    │
│      • 점진적 도입                                                           │
│      • 문서화                                                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 연관 패턴

| 패턴 | 관계 |
|------|------|
| **Event Sourcing** | CQRS의 자연스러운 동반자 |
| **Saga** | Command 실행 결과로 다른 서비스 연계 |
| **Materialized View** | Read Model의 구현 기법 |
| **Event-Driven** | 동기화 메커니즘 |

---

## 9. 참고 자료

- [Martin Fowler - CQRS](https://martinfowler.com/bliki/CQRS.html)
- [Microsoft - CQRS Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [Greg Young - CQRS Documents](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf)
- Portal Universe: `services/shopping-service/` 예제 참조
