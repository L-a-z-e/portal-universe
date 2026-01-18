# Common Library 사용 가이드

## 개요

이 가이드는 각 마이크로서비스에서 common-library를 실제로 사용하는 방법을 설명합니다.

## 목차

- [설정](#설정)
- [API 응답](#api-응답)
- [예외 처리](#예외-처리)
- [JWT 보안](#jwt-보안)
- [이벤트 기반 통신](#이벤트-기반-통신)
- [서비스별 가이드](#서비스별-가이드)

---

## 설정

### 1단계: build.gradle에 의존성 추가

```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
}
```

### 2단계: application.yml 구성

JWT 보안을 사용하는 서비스는 다음 설정을 추가합니다:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-service:8081
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

**각 환경별 설정:**

**로컬 개발:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081
          jwk-set-uri: http://localhost:8081/.well-known/jwks.json
```

**Docker Compose:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-service:8081
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

**Kubernetes:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-service.default.svc.cluster.local:8081
          jwk-set-uri: http://auth-service.default.svc.cluster.local:8081/.well-known/jwks.json
```

### 3단계: GlobalExceptionHandler 활성화

common-library는 자동으로 GlobalExceptionHandler를 등록하므로, 별도의 설정이 필요하지 않습니다.

확인:
```bash
# 애플리케이션 로그에서 다음과 같은 메시지를 확인
# INFO ... :... GlobalExceptionHandler initialized
```

---

## API 응답

### 성공 응답 작성

모든 Controller 메서드는 `ApiResponse<T>`로 감싼 응답을 반환합니다.

#### 데이터 반환

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long id) {
        ProductResponse product = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
```

**응답 예시:**
```json
HTTP/1.1 200 OK

{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Product A",
      "price": 29.99
    }
  ]
}
```

#### null 데이터 반환

`ApiResponse.success(null)`은 JSON에서 `data` 필드를 완전히 제외합니다.

```java
@PostMapping
public ResponseEntity<ApiResponse<Void>> createProduct(
        @RequestBody ProductRequest request) {
    productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(null));
}
```

**응답 예시:**
```json
HTTP/1.1 201 Created

{
  "success": true
}
```

#### 응답 DTO 설계

ResponseDTO는 Lombok `@Getter`로 표시하고, 필드를 `final`로 선언합니다:

```java
@Getter
public class ProductResponse {
    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final LocalDateTime createdAt;
    
    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.createdAt = product.getCreatedAt();
    }
}
```

---

## 예외 처리

### ErrorCode Enum 정의

각 서비스는 자신의 `[Service]ErrorCode` Enum을 정의합니다.

**쇼핑 서비스 예시:**

```java
package com.portal.universe.shoppingservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ShoppingErrorCode implements ErrorCode {
    
    // 상품 관련
    PRODUCT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "S001",
        "Product not found"
    ),
    DUPLICATE_PRODUCT(
        HttpStatus.BAD_REQUEST,
        "S002",
        "Product with same name already exists"
    ),
    INVALID_QUANTITY(
        HttpStatus.BAD_REQUEST,
        "S003",
        "Invalid quantity value"
    ),
    INSUFFICIENT_STOCK(
        HttpStatus.BAD_REQUEST,
        "S004",
        "Insufficient stock for product"
    ),
    
    // 주문 관련
    ORDER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "S005",
        "Order not found"
    ),
    INVALID_ORDER_STATE(
        HttpStatus.BAD_REQUEST,
        "S006",
        "Invalid order state for this operation"
    );
    
    private final HttpStatus status;
    private final String code;
    private final String message;
    
    ShoppingErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
```

### 예외 발생

Service 클래스에서 `CustomBusinessException`을 발생시킵니다.

```java
@Service
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
            .map(ProductResponse::new)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.PRODUCT_NOT_FOUND
            ));
    }
    
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // 중복 검증
        if (productRepository.existsByName(request.getName())) {
            throw new CustomBusinessException(
                ShoppingErrorCode.DUPLICATE_PRODUCT
            );
        }
        
        // 수량 검증
        if (request.getQuantity() <= 0) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INVALID_QUANTITY
            );
        }
        
        // 생성
        Product product = new Product(
            request.getName(),
            request.getPrice(),
            request.getQuantity()
        );
        Product saved = productRepository.save(product);
        
        return new ProductResponse(saved);
    }
    
    @Transactional
    public void updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.PRODUCT_NOT_FOUND
            ));
        
        if (product.getStock() < quantity) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INSUFFICIENT_STOCK
            );
        }
        
        product.decreaseStock(quantity);
    }
}
```

### 예외 처리 결과

발생된 예외는 자동으로 처리되어 API 응답으로 변환됩니다.

**404 응답:**
```json
HTTP/1.1 404 Not Found

{
  "success": false,
  "error": {
    "code": "S001",
    "message": "Product not found"
  }
}
```

**400 응답:**
```json
HTTP/1.1 400 Bad Request

{
  "success": false,
  "error": {
    "code": "S004",
    "message": "Insufficient stock for product"
  }
}
```

### 예외 로깅

GlobalExceptionHandler가 자동으로 로깅하므로, 서비스 개발자는 로깅을 별도로 처리할 필요가 없습니다.

```
[ERROR] handleCustomBusinessException: Insufficient stock for product
org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver: ...
```

---

## JWT 보안

### 자동 설정 확인

common-library를 추가하면 JWT 보안이 자동으로 활성화됩니다.

```
spring-boot-starter-security 감지
    ↓
JwtSecurityAutoConfiguration 자동 로드
    ↓
JwtAuthenticationConverter Bean 등록
```

**로그 확인:**
```
INFO ... : Registering JwtAuthenticationConverter...
```

### Spring Security 설정 (선택적 커스터마이징)

기본 설정으로 충분하면 별도 설정이 불필요합니다. 필요한 경우 다음과 같이 커스터마이징할 수 있습니다:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    // 기본 설정 사용하면 이 메서드는 생략 가능
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // 기본 converter 사용 (common-library에서 제공)
                )
            );
        
        return http.build();
    }
}
```

### 권한 기반 접근 제어

Spring Security의 `@PreAuthorize`, `@Secured` 등을 사용합니다.

```java
@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    
    // ROLE_ADMIN 권한만 접근 가능
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestBody ProductRequest request) {
        // ...
    }
    
    // ROLE_USER 또는 ROLE_ADMIN 권한 필요
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long id) {
        // ...
    }
}
```

### 현재 사용자 정보 접근

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(
        @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    List<String> roles = jwt.getClaimAsStringList("roles");
    
    // ...
    
    return ResponseEntity.ok(ApiResponse.success(...));
}
```

---

## 이벤트 기반 통신

### 이벤트 발행 (Publisher)

Kafka를 사용하여 이벤트를 발행합니다.

**예시: 주문 생성 이벤트 발행**

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 주문 생성
        Order order = new Order(request.getUserId());
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            order.addItem(
                itemRequest.getProductId(),
                itemRequest.getProductName(),
                itemRequest.getQuantity(),
                itemRequest.getPrice()
            );
        }
        
        Order saved = orderRepository.save(order);
        
        // 2. 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
            saved.getOrderNumber(),
            saved.getUserId(),
            saved.getTotalAmount(),
            saved.getItemCount(),
            saved.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderItemInfo(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice()
                ))
                .toList(),
            saved.getCreatedAt()
        );
        
        kafkaTemplate.send("order-created-events", event);
        
        // 3. 응답 반환
        return new OrderResponse(saved);
    }
}
```

### 이벤트 구독 (Subscriber)

```java
@Service
public class NotificationService {
    
    private final EmailService emailService;
    
    @KafkaListener(topics = "order-created-events")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 주문 확인 이메일 발송
        String message = String.format(
            "Your order %s has been created with total amount: $%s",
            event.orderNumber(),
            event.totalAmount()
        );
        
        emailService.sendEmail(
            getUserEmail(event.userId()),
            "Order Confirmation",
            message
        );
    }
    
    @KafkaListener(topics = "payment-completed-events")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        // 결제 영수증 발송
        String message = String.format(
            "Payment of $%s has been completed for order %s",
            event.amount(),
            event.orderNumber()
        );
        
        emailService.sendEmail(
            getUserEmail(event.userId()),
            "Payment Confirmation",
            message
        );
    }
}
```

### 이벤트 구독 (Inventory Service)

```java
@Service
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    
    @KafkaListener(topics = "order-created-events")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 재고 예약
        for (OrderCreatedEvent.OrderItemInfo item : event.items()) {
            Inventory inventory = inventoryRepository
                .findByProductId(item.productId())
                .orElseThrow();
            
            if (inventory.getAvailableStock() < item.quantity()) {
                throw new IllegalArgumentException("Insufficient stock");
            }
            
            inventory.reserveStock(item.quantity());
            inventoryRepository.save(inventory);
        }
    }
    
    @KafkaListener(topics = "order-cancelled-events")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // 재고 해제 (구현 필요)
    }
}
```

### Kafka 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
    consumer:
      bootstrap-servers: localhost:9092
      group-id: notification-service
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.type.mapping: "orderCreatedEvent:com.portal.universe.common.event.shopping.OrderCreatedEvent,paymentCompletedEvent:com.portal.universe.common.event.shopping.PaymentCompletedEvent"
```

---

## 서비스별 가이드

### 1. Auth Service (인증 서비스)

#### 사용하는 기능
- ApiResponse (응답 표준화)
- ErrorCode 및 CustomBusinessException
- UserSignedUpEvent 발행

#### build.gradle
```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.kafka:spring-kafka'
}
```

#### 주요 클래스

```java
@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, UserSignedUpEvent> kafkaTemplate;
    
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        // 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomBusinessException(
                AuthErrorCode.DUPLICATE_EMAIL
            );
        }
        
        // 사용자 생성
        User user = new User(
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getName()
        );
        User saved = userRepository.save(user);
        
        // 이벤트 발행
        UserSignedUpEvent event = new UserSignedUpEvent(
            saved.getId(),
            saved.getEmail(),
            saved.getName()
        );
        kafkaTemplate.send("user-signup-events", event);
        
        return new SignUpResponse(saved.getId(), saved.getEmail());
    }
}
```

---

### 2. Shopping Service (쇼핑 서비스)

#### 사용하는 기능
- ApiResponse (응답 표준화)
- ErrorCode 및 CustomBusinessException
- 모든 Shopping 도메인 이벤트 발행
- UserSignedUpEvent 구독

#### build.gradle
```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.kafka:spring-kafka'
}
```

#### 주요 클래스

```java
@Service
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
            .map(ProductResponse::new)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.PRODUCT_NOT_FOUND
            ));
    }
}

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 주문 생성...
        
        // 이벤트 발행
        kafkaTemplate.send("order-created-events", orderCreatedEvent);
        
        return new OrderResponse(saved);
    }
}
```

---

### 3. Blog Service (블로그 서비스)

#### 사용하는 기능
- ApiResponse (응답 표준화)
- ErrorCode 및 CustomBusinessException
- JWT 보안 (댓글 작성자 인증)

#### build.gradle
```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
    implementation 'org.springframework.boot:spring-boot-starter-security'
}
```

#### 주요 클래스

```java
@Service
public class PostService {
    
    private final PostRepository postRepository;
    
    @Transactional
    public PostResponse createPost(PostRequest request) {
        if (postRepository.existsByTitle(request.getTitle())) {
            throw new CustomBusinessException(
                BlogErrorCode.DUPLICATE_TITLE
            );
        }
        
        Post post = new Post(request.getTitle(), request.getContent());
        Post saved = postRepository.save(post);
        
        return new PostResponse(saved);
    }
}
```

---

### 4. Notification Service (알림 서비스)

#### 사용하는 기능
- 모든 도메인 이벤트 구독
- ApiResponse (상태 응답)
- 에러 처리

#### build.gradle
```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
    implementation 'org.springframework.kafka:spring-kafka'
}
```

#### 주요 클래스

```java
@Service
public class EventListenerService {
    
    private final EmailService emailService;
    
    @KafkaListener(topics = "user-signup-events")
    public void handleUserSignedUp(UserSignedUpEvent event) {
        emailService.sendWelcomeEmail(event.email(), event.name());
    }
    
    @KafkaListener(topics = "order-created-events")
    public void handleOrderCreated(OrderCreatedEvent event) {
        emailService.sendOrderConfirmation(event.userId(), event.orderNumber());
    }
    
    @KafkaListener(topics = "payment-completed-events")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        emailService.sendPaymentConfirmation(event.userId(), event.paymentNumber());
    }
}
```

---

### 5. API Gateway (게이트웨이)

#### 사용하는 기능
- JWT 보안 (Reactive)
- 요청 라우팅 및 인증
- 응답 처리

#### build.gradle
```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-security'
}
```

#### 주요 설정

```java
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .uri("http://auth-service:8081"))
            .route("shopping-service", r -> r
                .path("/api/v1/shopping/**")
                .uri("http://shopping-service:8083"))
            .route("blog-service", r -> r
                .path("/api/v1/blog/**")
                .uri("http://blog-service:8082"))
            .build();
    }
}
```

---

## 체크리스트

새 마이크로서비스를 생성할 때 다음을 확인하세요:

- [ ] build.gradle에 common-library 의존성 추가
- [ ] application.yml에 JWT 설정 추가 (필요 시)
- [ ] [Service]ErrorCode Enum 정의
- [ ] 모든 Controller가 ApiResponse<T>로 응답 래핑
- [ ] Service에서 CustomBusinessException 사용
- [ ] Kafka 이벤트 발행/구독 설정 (필요 시)
- [ ] 테스트에서 예외 처리 검증

---

## 문제 해결

### JWT 토큰이 인식되지 않음

**증상:** 401 Unauthorized

**해결:**
1. application.yml에 JWT 설정 확인
2. issuer-uri와 jwk-set-uri 확인
3. Token의 "iss" (issuer) 클레임 확인

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-service:8081  # 정확한 주소 확인
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

### CustomBusinessException이 처리되지 않음

**증상:** 500 error with stack trace

**해결:**
1. common-library 의존성 확인
2. GlobalExceptionHandler가 등록되었는지 로그 확인
3. 패키지 경로 확인: `com.portal.universe.commonlibrary.exception`

### Kafka 이벤트가 역직렬화되지 않음

**증상:** JsonMappingException

**해결:**
```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.type.mapping: |
          orderCreatedEvent:com.portal.universe.common.event.shopping.OrderCreatedEvent,
          paymentCompletedEvent:com.portal.universe.common.event.shopping.PaymentCompletedEvent
```

---

**최종 수정:** 2026-01-18
**버전:** 1.0
