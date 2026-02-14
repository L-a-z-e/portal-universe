# Shopping Seller Service

판매자 관리 및 상품 관리를 담당하는 마이크로서비스

## 기술 스택

- Java 17
- Spring Boot 3.5.5
- Spring Cloud 2025.0.0
- MySQL (Port 3307, DB: shopping_seller_db)
- Redis
- Kafka
- Flyway

## 포트

- 8088

## 주요 기능

### 1. 판매자 관리 (Seller)
- 판매자 등록/조회/수정
- 판매자 상태 관리 (PENDING, ACTIVE, SUSPENDED, WITHDRAWN)
- 수수료율 관리

### 2. 상품 관리 (Product)
- 상품 CRUD (판매자별)
- 상품 조회 (공개 API)
- 카테고리별 상품 조회

### 3. 재고 관리 (Inventory)
- 재고 초기화/조회
- 재고 추가 (수동)
- 재고 예약/차감/해제 (Saga 패턴)
- 재고 이동 이력 (StockMovement) 추적
- 낙관적 락 (Optimistic Locking) + 비관적 락 (Pessimistic Locking)

### 4. 쿠폰 관리 (스키마만)
- 쿠폰 발행/조회 (구현 예정)

### 5. 타임딜 관리 (스키마만)
- 타임딜 생성/조회 (구현 예정)

### 6. 대기열 관리 (스키마만)
- 대기열 생성/관리 (구현 예정)

## API 엔드포인트

### 판매자 (Seller)
- `POST /sellers/register` - 판매자 등록
- `GET /sellers/me` - 내 정보 조회
- `PUT /sellers/me` - 내 정보 수정

### 상품 (Product)
- `GET /products` - 전체 상품 조회 (공개)
- `GET /products/{productId}` - 상품 상세 조회 (공개)
- `POST /products` - 상품 등록 (SELLER, ADMIN)
- `PUT /products/{productId}` - 상품 수정 (SELLER 본인, ADMIN)
- `DELETE /products/{productId}` - 상품 삭제 (ADMIN)

### 재고 (Inventory)
- `GET /inventory/{productId}` - 재고 조회
- `PUT /inventory/{productId}/add` - 재고 추가
- `POST /inventory/{productId}` - 재고 초기화

### Internal API (서비스 간 통신)
- `GET /internal/products/{productId}` - 상품 조회
- `GET /internal/products` - 상품 목록 조회
- `POST /internal/inventory/reserve` - 재고 예약
- `POST /internal/inventory/deduct` - 재고 차감
- `POST /internal/inventory/release` - 재고 해제

## 권한 체계

- `ROLE_SELLER` - 판매자 (자신의 상품/재고만)
- `ROLE_SHOPPING_ADMIN` - 쇼핑 관리자
- `ROLE_SUPER_ADMIN` - 슈퍼 관리자

## Kafka 이벤트

### Consumer
- `ORDER_CREATED` - 주문 생성
- `ORDER_CANCELLED` - 주문 취소
- `PAYMENT_COMPLETED` - 결제 완료

### Producer
- `INVENTORY_RESERVED` - 재고 예약 완료

## 실행 방법

### Local 환경
```bash
./gradlew :services:shopping-seller-service:bootRun --args='--spring.profiles.active=local'
```

### 필수 인프라
- MySQL (Port 3307)
- Redis (Port 6379)
- Kafka (Port 9092)

### Swagger UI
http://localhost:8088/swagger-ui.html

## 데이터베이스

### 테이블
- `sellers` - 판매자
- `products` - 상품
- `inventory` - 재고
- `stock_movements` - 재고 이동 이력
- `coupons` - 쿠폰
- `time_deals` - 타임딜
- `time_deal_products` - 타임딜 상품
- `waiting_queues` - 대기열
- `queue_entries` - 대기열 항목

## 에러 코드

### Seller (SL0XX)
- `SL001` - Seller not found
- `SL002` - Seller already registered
- `SL003` - Seller account is suspended
- `SL004` - Seller account is pending approval

### Product (SL1XX)
- `SL101` - Product not found
- `SL102` - Product does not belong to this seller
- `SL103` - Product price must be greater than 0

### Inventory (SL2XX)
- `SL201` - Inventory not found for product
- `SL202` - Insufficient stock available
- `SL203` - Failed to reserve stock
- `SL204` - Failed to release stock
- `SL205` - Failed to deduct stock
- `SL206` - Stock quantity must be non-negative
- `SL207` - Inventory already exists for product
- `SL208` - Stock was modified by another transaction

### Coupon (SL3XX)
- `SL301` ~ `SL307` - Coupon related errors

### TimeDeal (SL4XX)
- `SL401` ~ `SL404` - TimeDeal related errors

### Queue (SL5XX)
- `SL501` - Waiting queue not found

## 참고 사항

- 재고 관리는 낙관적 락(`@Version`)과 비관적 락(`PESSIMISTIC_WRITE`) 조합
- 재고 예약/차감/해제는 데드락 방지를 위해 productId 오름차순 정렬 후 락 획득
- 모든 재고 변경은 `stock_movements` 테이블에 이력 기록
- Internal API는 인증 없이 호출 가능 (서비스 간 통신용)
