# Shopping Seller Service Database Schema

**Database**: PostgreSQL (shopping_seller_db)
**Entity Count**: 11
**Last Updated**: 2026-02-18

> Shopping 서비스 분해 (2026-02-14, ADR-041)로 Seller 도메인이 독립 서비스로 분리되었습니다.

## ERD

```mermaid
erDiagram
    Seller {
        Long id PK
        String userId UK
        String businessName
        String businessNumber
        String representativeName
        String phone
        String email
        String bankName
        String bankAccount
        BigDecimal commissionRate
        String status
        Timestamp createdAt
        Timestamp updatedAt
    }

    Product {
        Long id PK
        Long sellerId FK
        String name
        String description
        BigDecimal price
        BigDecimal discountPrice
        Integer stock
        String imageUrl
        String category
        Boolean featured
        Timestamp createdAt
        Timestamp updatedAt
    }

    ProductImage {
        Long id PK
        Long productId FK
        String imageUrl
        Integer sortOrder
        String altText
        Timestamp createdAt
    }

    Inventory {
        Long id PK
        Long productId FK_UK
        Integer availableQuantity
        Integer reservedQuantity
        Integer totalQuantity
        Long version
        Timestamp createdAt
        Timestamp updatedAt
    }

    StockMovement {
        Long id PK
        Long inventoryId FK
        Long productId
        String movementType
        Integer quantity
        Integer previousAvailable
        Integer afterAvailable
        Integer previousReserved
        Integer afterReserved
        String referenceType
        String referenceId
        String reason
        String performedBy
        Timestamp createdAt
    }

    Coupon {
        Long id PK
        Long sellerId
        String code UK
        String name
        String description
        String discountType
        BigDecimal discountValue
        BigDecimal minimumOrderAmount
        BigDecimal maximumDiscountAmount
        Integer totalQuantity
        Integer issuedQuantity
        String status
        Timestamp startsAt
        Timestamp expiresAt
        Timestamp createdAt
        Timestamp updatedAt
    }

    TimeDeal {
        Long id PK
        Long sellerId
        String name
        String description
        String status
        Timestamp startsAt
        Timestamp endsAt
        Timestamp createdAt
        Timestamp updatedAt
    }

    TimeDealProduct {
        Long id PK
        Long timeDealId FK
        Long productId FK
        BigDecimal dealPrice
        Integer dealQuantity
        Integer soldQuantity
        Integer maxPerUser
    }

    WaitingQueue {
        Long id PK
        String eventType
        Long eventId
        Integer maxCapacity
        Integer entryBatchSize
        Integer entryIntervalSeconds
        Boolean isActive
        Timestamp createdAt
        Timestamp activatedAt
        Timestamp deactivatedAt
    }

    QueueEntry {
        Long id PK
        Long queueId FK
        String userId
        String entryToken UK
        String status
        Timestamp joinedAt
        Timestamp enteredAt
        Timestamp expiredAt
        Timestamp leftAt
    }

    Seller ||--o{ Product : owns
    Product ||--o{ ProductImage : has
    Product ||--o| Inventory : has
    Inventory ||--o{ StockMovement : tracks
    Seller ||--o{ Coupon : creates
    Seller ||--o{ TimeDeal : creates
    TimeDeal ||--o{ TimeDealProduct : contains
    Product ||--o{ TimeDealProduct : "included in"
    WaitingQueue ||--o{ QueueEntry : contains
```

## Entities

| Entity | 설명 | 주요 필드 |
|--------|------|----------|
| Seller | 판매자 정보 | id, userId, businessName, commissionRate, status |
| Product | 상품 | id, sellerId, name, price, discountPrice, featured |
| ProductImage | 상품 다중 이미지 | id, productId, imageUrl, sortOrder |
| Inventory | 재고 | id, productId, availableQuantity, reservedQuantity |
| StockMovement | 재고 이동 이력 | id, inventoryId, movementType, quantity |
| Coupon | 쿠폰 정의 | id, code, discountType, discountValue |
| TimeDeal | 타임딜 이벤트 | id, name, status, startsAt, endsAt |
| TimeDealProduct | 타임딜 상품 | id, timeDealId, productId, dealPrice |
| WaitingQueue | 대기열 설정 | id, eventType, eventId, maxCapacity |
| QueueEntry | 대기열 엔트리 | id, queueId, userId, entryToken, status |

## Relationships

### 판매자-상품
- Seller 1:N Product: 판매자는 여러 상품을 등록
- Product 1:N ProductImage: 상품당 여러 이미지 (ON DELETE CASCADE)
- Product 1:1 Inventory: 상품당 하나의 재고 레코드

### 재고 관리
- Inventory 1:N StockMovement: 모든 재고 변경 이력 추적
- **Optimistic Locking**: `version` 필드로 동시성 제어

### 쿠폰/타임딜
- Seller 1:N Coupon: 판매자별 쿠폰 생성
- Seller 1:N TimeDeal: 판매자별 타임딜 생성
- TimeDeal 1:N TimeDealProduct: 타임딜에 여러 상품 포함

### 대기열
- WaitingQueue 1:N QueueEntry: 대기열에 여러 사용자 참가

## Cross-Service References (ID Only)

| 이 서비스 필드 | 참조 서비스 | 설명 |
|---------------|-----------|------|
| sellers.user_id | Auth Service | 사용자 UUID |
| Internal API | Shopping Service | Saga에서 재고 reserve/deduct/release 호출 |

## Indexes

### 성능 최적화
- `uk_sellers_user_id`: 사용자별 판매자 조회 (UK)
- `idx_products_seller_id`: 판매자별 상품 목록
- `idx_products_category`: 카테고리별 상품 검색
- `uk_inventory_product_id`: 상품별 재고 조회 (UK)
- `uk_coupons_code`: 쿠폰 코드 검증 (UK)
- `idx_time_deals_status`: 활성 타임딜 조회
- `idx_queue_entry_queue_user`: 대기열-사용자별 조회

## 변경 이력

| Date | Change | Author |
|------|--------|--------|
| 2026-02-18 | MySQL → PostgreSQL 전환 (ADR-046) | Laze |
| 2026-02-17 | Product 확장: discountPrice, featured, ProductImage 테이블 | Laze |
| 2026-02-14 | shopping-service에서 Seller 도메인 분리 (ADR-041) | Laze |
