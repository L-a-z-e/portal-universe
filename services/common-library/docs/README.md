---
id: DOC-INDEX-COMMON-LIBRARY
title: Common Library 문서 인덱스
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: Portal Universe Team
tags: [common-library, index, documentation]
---

# Common Library - Portal Universe 공유 라이브러리

## 개요

`common-library`는 Portal Universe의 모든 마이크로서비스에서 사용하는 공유 라이브러리입니다.

이 모듈은 **API 응답 표준화**, **예외 처리**, **JWT 보안**, **도메인 이벤트** 등 시스템 전반에서 반복되는 기능을 중앙화하여 관리합니다. 각 마이크로서비스는 이 라이브러리를 의존성으로 추가하여 일관된 구조와 패턴을 유지할 수 있습니다.

## 주요 기능

### 1. 통일된 API 응답 (ApiResponse)
- 모든 REST API의 응답을 일관된 형식으로 표준화
- 성공/실패 구분 및 에러 정보 포함
- JSON 직렬화 시 null 값 제외

**예시:**
```json
{
  "success": true,
  "data": { "id": 1, "name": "Product A" },
  "error": null
}
```

### 2. 계층화된 예외 처리 (Exception Handling)
- **ErrorCode Interface**: 모든 오류 코드의 계약 정의
- **CustomBusinessException**: 서비스 로직의 예측 가능한 예외
- **GlobalExceptionHandler**: 모든 예외를 ApiResponse로 일괄 변환
- **CommonErrorCode**: 전체 서비스가 공유하는 기본 에러 코드

**서비스별 에러코드 접두사:**
| 서비스 | 접두사 | 예시 |
|--------|--------|------|
| Common | C | C001, C002, C003 |
| Auth | A | A001 |
| Blog | B | B001, B002, B003 |
| Shopping | S | S001 |

### 3. JWT 보안 자동 설정 (Spring Security)
- **자동 설정 (Auto-Configuration)**: 의존성만 추가하면 자동 적용
- **Servlet/Reactive 이중 지원**: Spring MVC와 WebFlux 모두 지원
- **역할 기반 접근 제어 (RBAC)**: JWT의 `roles` 클레임을 GrantedAuthority로 변환

### 4. 도메인 이벤트 (Domain Events)
- 마이크로서비스 간 비동기 통신을 위한 표준 이벤트 클래스
- Auth, Shopping 도메인의 주요 이벤트 미리 정의
- Kafka를 통한 이벤트 기반 아키텍처 지원

## 프로젝트 구조

```
common-library/
├── src/main/java/
│   ├── com/portal/universe/commonlibrary/
│   │   ├── response/                      # API 응답
│   │   │   ├── ApiResponse.java           # 통일 응답 래퍼
│   │   │   └── ErrorResponse.java         # 에러 정보 DTO
│   │   ├── exception/                     # 예외 처리
│   │   │   ├── ErrorCode.java             # 인터페이스 (계약)
│   │   │   ├── CommonErrorCode.java       # 공통 에러 코드 Enum
│   │   │   ├── CustomBusinessException.java # 커스텀 예외
│   │   │   └── GlobalExceptionHandler.java # 전역 예외 핸들러
│   │   └── security/                      # JWT 보안
│   │       ├── config/
│   │       │   └── JwtSecurityAutoConfiguration.java  # 자동 설정
│   │       └── converter/
│   │           ├── JwtAuthenticationConverterAdapter.java      # Servlet용
│   │           └── ReactiveJwtAuthenticationConverterAdapter.java # Reactive용
│   └── com/portal/universe/common/
│       └── event/                         # 도메인 이벤트
│           ├── UserSignedUpEvent.java
│           └── shopping/
│               ├── OrderCreatedEvent.java
│               ├── PaymentCompletedEvent.java
│               ├── PaymentFailedEvent.java
│               ├── OrderConfirmedEvent.java
│               ├── OrderCancelledEvent.java
│               ├── InventoryReservedEvent.java
│               └── DeliveryShippedEvent.java
├── src/main/resources/
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── build.gradle
```

## 문서 목록

### 📐 아키텍처 문서
| ID | 문서명 | 설명 | 링크 |
|----|--------|------|------|
| ARCH-001 | Common Library Overview | 전체 아키텍처 설계 및 주요 결정 사항 | [ARCH-001](architecture/ARCH-001-common-library-overview.md) |

**주요 내용:**
- 아키텍처 원칙 (최소 의존성, 자동 설정, 계약 기반 설계)
- 응답 계층, 예외 처리 계층, 보안 계층 구조
- ADR (Architecture Decision Records)
- 도메인 이벤트 상세 명세

### 📚 API 문서
| ID | 문서명 | 설명 | 링크 |
|----|--------|------|------|
| API-001 | Common Library API Reference | 공개 API 및 클래스 상세 명세 | [API-001](api/API-001-common-library.md) |

**주요 내용:**
- ApiResponse<T>, ErrorResponse API
- ErrorCode, CustomBusinessException, GlobalExceptionHandler
- JWT 보안 자동 설정 (JwtSecurityAutoConfiguration)
- 도메인 이벤트 Record 클래스 명세

### 📖 개발 가이드
| ID | 문서명 | 설명 | 링크 |
|----|--------|------|------|
| GUIDE-001 | Common Library Usage Guide | 각 서비스별 사용 방법 및 예제 | [GUIDE-001](guides/GUIDE-001-common-library-usage.md) |

**주요 내용:**
- 설정 방법 (build.gradle, application.yml)
- API 응답 작성 패턴
- 예외 처리 사용법 (ErrorCode 정의, 예외 발생)
- JWT 보안 설정 및 권한 제어
- 이벤트 발행/구독 패턴
- 서비스별 구체적 가이드 (Auth, Shopping, Blog, Notification, API Gateway)

## 빠른 시작

### 1. build.gradle에 의존성 추가

```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
}
```

### 2. application.yml 설정 (JWT 사용 시)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-service:8081
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

### 3. API 응답 활용

**Controller 예시:**
```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
```

### 4. 예외 처리 활용

**ErrorCode Enum 정의:**
```java
public enum ShoppingErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "S002", "Invalid quantity"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "S003", "Insufficient stock");
    // ...
}
```

**Service 예시:**
```java
@Service
public class ProductService {

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByName(request.getName())) {
            throw new CustomBusinessException(ShoppingErrorCode.DUPLICATE_PRODUCT);
        }
        // 비즈니스 로직...
        return new ProductResponse(savedProduct);
    }
}
```

> **주의**: 서비스에서 발생한 모든 CustomBusinessException은 GlobalExceptionHandler에 의해 자동으로 ApiResponse.error()로 변환됩니다.

### 5. 이벤트 발행/구독

**이벤트 발행 (Publisher):**
```java
@Service
public class OrderService {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 주문 생성 로직...

        OrderCreatedEvent event = new OrderCreatedEvent(
            saved.getOrderNumber(),
            saved.getUserId(),
            saved.getTotalAmount(),
            saved.getItemCount(),
            items,
            saved.getCreatedAt()
        );

        kafkaTemplate.send("order-created-events", event);

        return new OrderResponse(saved);
    }
}
```

**이벤트 구독 (Subscriber):**
```java
@Service
public class NotificationService {

    @KafkaListener(topics = "order-created-events")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 주문 확인 이메일 발송 로직...
    }
}
```

## 의존성 관리 전략

common-library는 **최소 의존성 원칙**을 따릅니다:

### Implementation (필수)
- `spring-boot-starter-web` - ApiResponse, ExceptionHandler 기본 기능

### CompileOnly (선택적)
- `spring-boot-starter-security` - JWT 보안 설정
- `spring-security-oauth2-resource-server` - OAuth2 리소스 서버
- `spring-security-oauth2-jose` - JWT 처리
- `spring-boot-starter-webflux` - Reactive 환경 지원

> **이유**: 라이브러리를 사용하는 각 서비스가 필요한 의존성만 선택적으로 포함하도록, compileOnly로 설정하여 버전 충돌을 방지합니다.

## 주요 개념

### ErrorCode 인터페이스 계약

```java
public interface ErrorCode {
    HttpStatus getStatus();   // HTTP 상태 코드 (200, 400, 404, 500 등)
    String getCode();         // 애플리케이션 에러 코드 (C001, A001, B001 등)
    String getMessage();      // 클라이언트에게 보여줄 메시지
}
```

각 마이크로서비스는 이 인터페이스를 구현한 **ErrorCode Enum**을 정의합니다.

### 응답 흐름

```
클라이언트 요청
    ↓
API Controller
    ↓
비즈니스 로직 (Service)
    ↓
[예외 발생 시]
CustomBusinessException(ErrorCode)
    ↓
GlobalExceptionHandler
    ↓
ApiResponse.error(code, message) + HttpStatus
    ↓
클라이언트 응답
```

## 개발자 가이드

### 새로운 ErrorCode 추가

1. 각 서비스의 `[Service]ErrorCode.java` Enum에 정의
2. 앞의 2-3자리는 서비스 코드 사용:
   - **Auth**: A (A001, A002, ...)
   - **Blog**: B (B001, B002, ...)
   - **Shopping**: S (S001, S002, ...)
   - **Common**: C (C001, C002, C003)

### 새로운 Event 추가

1. `common-library`의 `com.portal.universe.common.event` 패키지에 추가
2. Record 클래스로 정의 (불변성 보장)
3. 모든 필드는 직렬화 가능한 타입 사용
4. 아키텍처 문서에 발행 조건과 구독 서비스 명시

## 빌드 및 설치

```bash
cd /Users/laze/Laze/Project/portal-universe
./gradlew :services:common-library:build
```

## 문제 해결

### JWT 토큰이 인식되지 않음

**증상:** 401 Unauthorized

**해결:**
1. application.yml에 JWT 설정 확인
2. issuer-uri와 jwk-set-uri 확인
3. Token의 "iss" (issuer) 클레임 확인

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

자세한 내용은 [사용 가이드](guides/GUIDE-001-common-library-usage.md#문제-해결)를 참조하세요.

## 체크리스트

새 마이크로서비스를 생성할 때 다음을 확인하세요:

- [ ] build.gradle에 common-library 의존성 추가
- [ ] application.yml에 JWT 설정 추가 (필요 시)
- [ ] [Service]ErrorCode Enum 정의
- [ ] 모든 Controller가 ApiResponse<T>로 응답 래핑
- [ ] Service에서 CustomBusinessException 사용
- [ ] Kafka 이벤트 발행/구독 설정 (필요 시)
- [ ] 테스트에서 예외 처리 검증

## 참고 자료

- [Spring Boot Auto-Configuration](https://spring.io/guides/gs/spring-boot-auto-configuration/)
- [Spring Security OAuth2 Resource Server](https://spring.io/projects/spring-security-oauth2-resource-server)
- [Error Handling Patterns by Martin Fowler](https://martinfowler.com/bliki/ErrorHandling.html)
- [Kafka Documentation](https://kafka.apache.org/documentation/)

---

**최종 수정**: 2026-01-18
**버전**: 0.0.1-SNAPSHOT
**유지보수자**: Portal Universe Team
