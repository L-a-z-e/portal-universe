# Portal Universe Phase 1: E-commerce Implementation Plan

## Overview

shopping-service를 풀 이커머스 플랫폼으로 확장합니다. 장바구니, 주문, 결제, 재고 관리, 배송 추적 기능을 Saga 패턴과 Kafka 이벤트 아키텍처로 구현합니다.

---

## 1. Current State Analysis

**현재 shopping-service 구조:**
```
shoppingservice/
├── controller/ProductController.java, ShoppingTestController.java
├── service/ProductService.java, ProductServiceImpl.java
├── domain/Product.java (id, name, description, price, stock)
├── dto/ProductResponse.java, ProductCreateRequest.java, ...
├── repository/ProductRepository.java
├── exception/ShoppingErrorCode.java (S001 only)
└── config/SecurityConfig.java, FeignClientConfig.java
```

**문제점:**
- Product.stock 필드로 재고 관리 (동시성 제어 없음)
- Error code 1개만 존재
- Flat 패키지 구조 (확장성 낮음)
- Kafka 미연동

---

## 2. Target Architecture

### 2.1 Package Structure (Domain-Based)
```
shoppingservice/
├── common/
│   ├── config/{SecurityConfig, KafkaConfig, JpaConfig}
│   ├── exception/ShoppingErrorCode.java
│   └── domain/Address.java (Embeddable)
├── product/
│   ├── domain/Product.java
│   ├── repository/ProductRepository.java
│   ├── service/{ProductService, ProductServiceImpl}
│   └── controller/ProductController.java
├── inventory/
│   ├── domain/{Inventory, StockMovement, MovementType}
│   ├── repository/{InventoryRepository, StockMovementRepository}
│   ├── service/{InventoryService, InventoryServiceImpl}
│   └── controller/InventoryController.java
├── cart/
│   ├── domain/{Cart, CartItem, CartStatus}
│   ├── repository/{CartRepository, CartItemRepository}
│   ├── service/{CartService, CartServiceImpl}
│   └── controller/CartController.java
├── order/
│   ├── domain/{Order, OrderItem, OrderStatus}
│   ├── repository/{OrderRepository, OrderItemRepository}
│   ├── service/{OrderService, OrderServiceImpl}
│   ├── controller/OrderController.java
│   └── saga/{OrderSagaOrchestrator, SagaState, SagaStep, SagaStatus}
├── payment/
│   ├── domain/{Payment, PaymentStatus, PaymentMethod}
│   ├── repository/PaymentRepository.java
│   ├── service/{PaymentService, PaymentServiceImpl}
│   ├── controller/PaymentController.java
│   └── pg/MockPGClient.java
├── delivery/
│   ├── domain/{Delivery, DeliveryHistory, DeliveryStatus}
│   ├── repository/DeliveryRepository.java
│   ├── service/{DeliveryService, DeliveryServiceImpl}
│   └── controller/DeliveryController.java
└── event/
    └── ShoppingEventPublisher.java
```

### 2.2 Key Entities

| Entity | Key Fields | Notes |
|--------|-----------|-------|
| Inventory | productId, available, reserved, total, version | Pessimistic Lock |
| StockMovement | inventoryId, type, quantity, referenceId | Audit trail |
| Cart | userId, status, items | One active per user |
| CartItem | cartId, productId, productName, price, quantity | Price snapshot |
| Order | orderNumber, userId, status, totalAmount, address | UUID-based number |
| OrderItem | orderId, productId, productName, price, quantity | Price snapshot |
| Payment | paymentNumber, orderId, amount, status, pgTxId | Mock PG |
| Delivery | trackingNumber, orderId, status, carrier | Auto-generated |
| DeliveryHistory | deliveryId, status, location, description | Status log |
| SagaState | sagaId, orderId, currentStep, status | Orchestration |

---

## 3. Implementation Steps

### Step 1: Foundation Setup

**Files to create/modify:**

1. **build.gradle** - Add dependencies
```groovy
// Add to services/shopping-service/build.gradle
implementation 'org.springframework.kafka:spring-kafka'
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

2. **JpaConfig.java** - Enable auditing
```
services/shopping-service/src/main/java/.../common/config/JpaConfig.java
- @EnableJpaAuditing
```

3. **KafkaConfig.java** - Producer + Topics
```
services/shopping-service/src/main/java/.../common/config/KafkaConfig.java
- Topics: shopping.order.created/confirmed/cancelled, shopping.payment.completed/failed, shopping.inventory.reserved, shopping.delivery.shipped
```

4. **ShoppingErrorCode.java** - Expand error codes
```
services/shopping-service/src/main/java/.../common/exception/ShoppingErrorCode.java
- S0XX: Product (S001-S010)
- S1XX: Cart (S101-S110)
- S2XX: Order (S201-S220)
- S3XX: Payment (S301-S315)
- S4XX: Inventory (S401-S410)
- S5XX: Delivery (S501-S510)
```

### Step 2: Inventory Domain

**Files to create:**

1. **Inventory.java**
```
services/shopping-service/src/main/java/.../inventory/domain/Inventory.java
- Fields: id, productId, available, reserved, total, version, createdAt, updatedAt
- Methods: reserve(qty), deduct(qty), release(qty), addStock(qty)
- @Version for optimistic lock backup
```

2. **StockMovement.java**
```
services/shopping-service/src/main/java/.../inventory/domain/StockMovement.java
- Fields: id, inventoryId, productId, type, quantity, previousAvailable, afterAvailable, referenceType, referenceId, reason, performedBy, createdAt
```

3. **MovementType.java**
```
services/shopping-service/src/main/java/.../inventory/domain/MovementType.java
- INBOUND, RESERVE, RELEASE, DEDUCT, RETURN, ADJUSTMENT, INITIAL
```

4. **InventoryRepository.java**
```
services/shopping-service/src/main/java/.../inventory/repository/InventoryRepository.java
- findByProductId(Long)
- @Lock(PESSIMISTIC_WRITE) findByProductIdWithLock(Long) - 3s timeout
- @Lock(PESSIMISTIC_WRITE) findByProductIdsWithLock(List<Long>) - ORDER BY id (deadlock prevention)
```

5. **InventoryService.java / InventoryServiceImpl.java**
```
services/shopping-service/src/main/java/.../inventory/service/
- getInventory(productId)
- initializeInventory(productId, initialStock)
- reserveStock(productId, quantity, refType, refId, userId)
- reserveStockBatch(Map<productId, quantity>, refType, refId, userId)
- deductStock(productId, quantity, refType, refId, userId)
- releaseStock(productId, quantity, refType, refId, userId)
```

6. **InventoryController.java**
```
services/shopping-service/src/main/java/.../inventory/controller/InventoryController.java
- GET /api/shopping/inventory/{productId}
- PUT /api/shopping/inventory/{productId} (Admin)
```

### Step 3: Cart Domain

**Files to create:**

1. **Cart.java, CartItem.java, CartStatus.java**
```
services/shopping-service/src/main/java/.../cart/domain/
- Cart: id, userId, status, items, createdAt, updatedAt
- CartItem: id, cart, productId, productName, price, quantity, addedAt
- CartStatus: ACTIVE, CHECKED_OUT, ABANDONED, MERGED
```

2. **CartRepository.java, CartItemRepository.java**
```
services/shopping-service/src/main/java/.../cart/repository/
- findByUserIdAndStatus(userId, status)
- findByUserIdAndStatusWithItems(userId, status) - fetch join
```

3. **CartService.java / CartServiceImpl.java**
```
services/shopping-service/src/main/java/.../cart/service/
- getCart(userId)
- addItem(userId, request)
- updateItemQuantity(userId, itemId, request)
- removeItem(userId, itemId)
- clearCart(userId)
- checkout(userId) → returns orderNumber
```

4. **CartController.java**
```
services/shopping-service/src/main/java/.../cart/controller/CartController.java
- GET /api/shopping/cart
- POST /api/shopping/cart/items
- PUT /api/shopping/cart/items/{itemId}
- DELETE /api/shopping/cart/items/{itemId}
- POST /api/shopping/cart/checkout
```

### Step 4: Order Domain + Saga

**Files to create:**

1. **Order.java, OrderItem.java, OrderStatus.java, Address.java**
```
services/shopping-service/src/main/java/.../order/domain/
- Order: id, orderNumber, userId, status, totalAmount, shippingAddress(Embedded), items, payment, createdAt, updatedAt
- OrderItem: id, order, productId, productName, price, quantity, subtotal
- Address: receiverName, phone, zipCode, address1, address2
- OrderStatus: PENDING, CONFIRMED, PAID, SHIPPING, DELIVERED, CANCELLED, REFUNDED
```

2. **OrderRepository.java**
```
services/shopping-service/src/main/java/.../order/repository/OrderRepository.java
- findByOrderNumber(orderNumber)
- findByOrderNumberWithItems(orderNumber) - fetch join
- findByUserIdOrderByCreatedAtDesc(userId, pageable)
```

3. **SagaState.java, SagaStep.java, SagaStatus.java**
```
services/shopping-service/src/main/java/.../order/saga/
- SagaState: sagaId, orderId, currentStep, status, startedAt, completedAt, lastErrorMessage, stepExecutions, compensationAttempts
- SagaStep: RESERVE_INVENTORY, PROCESS_PAYMENT, DEDUCT_INVENTORY, CREATE_DELIVERY, CONFIRM_ORDER
- SagaStatus: STARTED, COMPLETED, COMPENSATING, FAILED, COMPENSATION_FAILED
```

4. **OrderSagaOrchestrator.java**
```
services/shopping-service/src/main/java/.../order/saga/OrderSagaOrchestrator.java
- executeSaga(order): Reserve → Payment → Deduct → Delivery → Confirm
- compensate(sagaState, errorMessage): Reverse order compensation
```

5. **OrderService.java / OrderServiceImpl.java**
```
services/shopping-service/src/main/java/.../order/service/
- createOrder(userId, request) - calls saga
- getOrder(orderNumber)
- getUserOrders(userId, pageable)
- cancelOrder(userId, orderNumber)
```

6. **OrderController.java**
```
services/shopping-service/src/main/java/.../order/controller/OrderController.java
- POST /api/shopping/orders
- GET /api/shopping/orders
- GET /api/shopping/orders/{orderNumber}
- POST /api/shopping/orders/{orderNumber}/cancel
```

### Step 5: Payment Domain

**Files to create:**

1. **Payment.java, PaymentStatus.java, PaymentMethod.java**
```
services/shopping-service/src/main/java/.../payment/domain/
- Payment: id, paymentNumber, order, amount, status, paymentMethod, pgTransactionId, pgResponse, failureReason, paidAt, refundedAt, createdAt, updatedAt
- PaymentStatus: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
- PaymentMethod: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE, POINTS
```

2. **MockPGClient.java**
```
services/shopping-service/src/main/java/.../payment/pg/MockPGClient.java
- processPayment(paymentNumber, amount, method, details) → PgResponse
- cancelPayment(pgTransactionId) → PgResponse
- refundPayment(pgTransactionId, amount) → PgResponse
- 90% success rate simulation
```

3. **PaymentService.java / PaymentServiceImpl.java**
```
services/shopping-service/src/main/java/.../payment/service/
- processPayment(userId, request)
- getPayment(paymentNumber)
- cancelPayment(userId, paymentNumber)
- refundPayment(paymentNumber) - Admin
```

4. **PaymentController.java**
```
services/shopping-service/src/main/java/.../payment/controller/PaymentController.java
- POST /api/shopping/payments
- GET /api/shopping/payments/{paymentNumber}
- POST /api/shopping/payments/{paymentNumber}/cancel
```

### Step 6: Delivery Domain

**Files to create:**

1. **Delivery.java, DeliveryHistory.java, DeliveryStatus.java**
```
services/shopping-service/src/main/java/.../delivery/domain/
- Delivery: id, trackingNumber, orderId, status, carrier, shippingAddress, estimatedDeliveryDate, actualDeliveryDate, histories, createdAt, updatedAt
- DeliveryHistory: id, delivery, status, location, description, createdAt
- DeliveryStatus: PREPARING, SHIPPED, IN_TRANSIT, DELIVERED, RETURNED, CANCELLED
```

2. **DeliveryRepository.java**
```
services/shopping-service/src/main/java/.../delivery/repository/DeliveryRepository.java
- findByTrackingNumber(trackingNumber)
- findByOrderId(orderId)
- findByTrackingNumberWithHistories(trackingNumber) - fetch join
```

3. **DeliveryService.java / DeliveryServiceImpl.java**
```
services/shopping-service/src/main/java/.../delivery/service/
- createDelivery(order)
- getDeliveryByTrackingNumber(trackingNumber)
- updateDeliveryStatus(trackingNumber, request) - Admin
- cancelDelivery(orderId) - for saga compensation
```

4. **DeliveryController.java**
```
services/shopping-service/src/main/java/.../delivery/controller/DeliveryController.java
- GET /api/shopping/deliveries/{trackingNumber}
- PUT /api/shopping/deliveries/{trackingNumber}/status (Admin)
```

### Step 7: Event Infrastructure

**Files to create:**

1. **Event DTOs in common-library**
```
services/common-library/src/main/java/.../event/shopping/
- OrderCreatedEvent.java
- OrderConfirmedEvent.java
- OrderCancelledEvent.java
- PaymentCompletedEvent.java
- PaymentFailedEvent.java
- InventoryReservedEvent.java
- DeliveryShippedEvent.java
```

2. **ShoppingEventPublisher.java**
```
services/shopping-service/src/main/java/.../event/ShoppingEventPublisher.java
- publishOrderCreated(event)
- publishOrderConfirmed(event)
- publishOrderCancelled(event)
- publishPaymentCompleted(event)
- publishPaymentFailed(event)
- publishInventoryReserved(event)
- publishDeliveryShipped(event)
```

### Step 8: Database Migrations

**Files to create:**
```
services/shopping-service/src/main/resources/db/migration/
- V1__Initial_products_schema.sql
- V2__Create_inventory_tables.sql
- V3__Migrate_stock_to_inventory.sql
- V4__Create_cart_tables.sql
- V5__Create_order_tables.sql
- V6__Create_payment_tables.sql
- V7__Create_delivery_tables.sql
- V8__Create_saga_tables.sql
```

---

## 4. Critical Files Summary

| Priority | File Path | Purpose |
|----------|-----------|---------|
| 1 | `services/shopping-service/build.gradle` | Add Kafka, Flyway deps |
| 2 | `.../inventory/repository/InventoryRepository.java` | Pessimistic Lock queries |
| 3 | `.../inventory/service/InventoryServiceImpl.java` | Stock reserve/deduct/release |
| 4 | `.../order/saga/OrderSagaOrchestrator.java` | Saga orchestration |
| 5 | `.../order/saga/SagaState.java` | Saga state tracking |
| 6 | `.../event/ShoppingEventPublisher.java` | Kafka event publishing |
| 7 | `.../common/exception/ShoppingErrorCode.java` | Extended error codes |
| 8 | `docker-compose.yml` | Add kafka dependency to shopping-service |

---

## 5. Verification Steps

### 5.1 Unit Tests
```bash
./gradlew :services:shopping-service:test
```

### 5.2 Integration Tests (TestContainers)
```bash
./gradlew :services:shopping-service:test --tests "*IntegrationTest"
```

### 5.3 Concurrent Stock Test
```java
@Test
void testConcurrentReservation() {
    // 10 available stock, 15 concurrent requests
    // Expected: 10 success, 5 fail with INSUFFICIENT_STOCK
}
```

### 5.4 E2E Flow Test
```bash
# 1. Start services
docker-compose up -d

# 2. Add to cart
curl -X POST http://localhost:8080/api/v1/shopping/cart/items \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'

# 3. Create order
curl -X POST http://localhost:8080/api/v1/shopping/orders \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddress": {...}}'

# 4. Process payment
curl -X POST http://localhost:8080/api/v1/shopping/payments \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"orderNumber": "ORD-...", "paymentMethod": "CARD"}'

# 5. Check delivery
curl http://localhost:8080/api/v1/shopping/deliveries/{trackingNumber}
```

### 5.5 Saga Compensation Test
```bash
# Trigger payment failure (Mock PG has 10% failure rate)
# Verify: Inventory released, Order cancelled, OrderCancelledEvent published
```

---

## 6. Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Saga Pattern | Orchestration | Central control, easier debugging, clear compensation flow |
| Inventory Lock | Pessimistic (Phase 1) | Data consistency priority, simpler implementation |
| Price Storage | Denormalized in CartItem/OrderItem | Preserve point-in-time price, performance, fault isolation |
| Event System | Kafka | Loose coupling, async processing, audit log |
| OrderNumber | UUID-based (ORD-YYYYMMDD-UUID8) | Human-readable, unique, sortable |
| DB Migration | Flyway | Version control, rollback support |

---

## 7. Implementation Order

1. **Foundation** → build.gradle, JpaConfig, KafkaConfig, ShoppingErrorCode
2. **Inventory** → Entity, Repository (Pessimistic Lock), Service, Controller
3. **Cart** → Entity, Repository, Service, Controller
4. **Order + Saga** → Entity, SagaState, SagaOrchestrator, Service, Controller
5. **Payment** → Entity, MockPGClient, Service, Controller
6. **Delivery** → Entity, Repository, Service, Controller
7. **Events** → common-library DTOs, ShoppingEventPublisher
8. **Migration** → Flyway scripts
9. **Testing** → Unit, Integration, E2E
