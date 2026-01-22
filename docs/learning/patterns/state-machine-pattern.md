# State Machine 패턴

## 학습 목표
- State Machine 패턴의 원리 이해
- 상태 전이 규칙의 중요성
- Portal Universe의 Order/Payment 상태 관리 분석

---

## 1. State Machine 개요

### 1.1 State Machine이란?

State Machine(상태 기계)은 **유한한 상태**와 **상태 간 전이 규칙**을 정의하여 객체의 생명주기를 관리하는 패턴입니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        STATE MACHINE CONCEPT                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   구성 요소:                                                                 │
│                                                                              │
│   ┌────────────┐       Event          ┌────────────┐                        │
│   │   State A  │ ─────────────────────► │   State B  │                        │
│   │            │       (Action)       │            │                        │
│   └────────────┘                      └────────────┘                        │
│                                                                              │
│   • State: 객체가 가질 수 있는 상태 (PENDING, PAID, SHIPPED...)             │
│   • Event: 상태 전이를 트리거하는 이벤트 (결제 완료, 배송 시작...)           │
│   • Transition: 상태 A에서 상태 B로의 전이 규칙                              │
│   • Action: 전이 시 실행되는 로직                                            │
│   • Guard: 전이 가능 여부를 판단하는 조건                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 왜 State Machine을 사용하는가?

| 문제 | State Machine 해결책 |
|------|----------------------|
| 잘못된 상태 전이 방지 | 허용된 전이만 정의 |
| 상태 관련 로직 산재 | 상태 전이를 한 곳에 집중 |
| 비즈니스 규칙 위반 | Guard 조건으로 검증 |
| 복잡한 if-else 분기 | 명확한 전이 정의 |

---

## 2. Order Status State Machine

### 2.1 상태 정의

```java
public enum OrderStatus {

    /**
     * 결제 대기 중 - 주문 생성 후 결제 전
     */
    PENDING("결제 대기 중"),

    /**
     * 결제 완료 - 결제 성공
     */
    PAID("결제 완료"),

    /**
     * 배송 준비 중 - 상품 준비 단계
     */
    PREPARING("배송 준비 중"),

    /**
     * 배송 중 - 상품 발송 완료
     */
    SHIPPED("배송 중"),

    /**
     * 배송 완료 - 고객 수령 완료
     */
    DELIVERED("배송 완료"),

    /**
     * 취소됨 - 주문 취소
     */
    CANCELLED("취소됨"),

    /**
     * 환불됨 - 결제 환불 처리 완료
     */
    REFUNDED("환불됨");
}
```

### 2.2 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ORDER STATUS STATE MACHINE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         ┌─────────┐                                          │
│                         │ PENDING │ ◄─── 주문 생성                           │
│                         └────┬────┘                                          │
│                              │                                               │
│               ┌──────────────┼──────────────┐                                │
│               │              │              │                                │
│               ▼              ▼              │                                │
│         ┌───────────┐  ┌───────────┐        │                                │
│         │ CANCELLED │  │   PAID    │        │ 결제 실패/                     │
│         └───────────┘  └─────┬─────┘        │ 고객 취소                      │
│                              │              │                                │
│                              ▼              │                                │
│                        ┌───────────┐        │                                │
│                        │ PREPARING │────────┘                                │
│                        └─────┬─────┘                                         │
│                              │                                               │
│                              ▼                                               │
│                        ┌───────────┐                                         │
│                        │  SHIPPED  │                                         │
│                        └─────┬─────┘                                         │
│                              │                                               │
│               ┌──────────────┼──────────────┐                                │
│               │              ▼              │                                │
│               │        ┌───────────┐        │                                │
│               │        │ DELIVERED │        │                                │
│               │        └─────┬─────┘        │                                │
│               │              │              │                                │
│               ▼              ▼              ▼                                │
│         ┌───────────────────────────────────────┐                            │
│         │               REFUNDED                │                            │
│         └───────────────────────────────────────┘                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 전이 규칙 구현

```java
public enum OrderStatus {
    PENDING("결제 대기 중"),
    PAID("결제 완료"),
    PREPARING("배송 준비 중"),
    SHIPPED("배송 중"),
    DELIVERED("배송 완료"),
    CANCELLED("취소됨"),
    REFUNDED("환불됨");

    /**
     * 취소 가능 여부
     */
    public boolean isCancellable() {
        return this == PENDING || this == PAID;
    }

    /**
     * 환불 가능 여부
     */
    public boolean isRefundable() {
        return this == PAID || this == PREPARING ||
               this == SHIPPED || this == DELIVERED;
    }

    /**
     * 배송 시작 가능 여부
     */
    public boolean canShip() {
        return this == PREPARING;
    }

    /**
     * 다음 상태로 전이 가능한지 검증
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PAID || target == CANCELLED;
            case PAID -> target == PREPARING || target == CANCELLED ||
                         target == REFUNDED;
            case PREPARING -> target == SHIPPED || target == CANCELLED ||
                              target == REFUNDED;
            case SHIPPED -> target == DELIVERED || target == REFUNDED;
            case DELIVERED -> target == REFUNDED;
            case CANCELLED, REFUNDED -> false;  // 최종 상태
        };
    }
}
```

### 2.4 Order Entity의 상태 전이 메서드

```java
@Entity
public class Order {

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * 결제 완료 처리
     */
    public void markAsPaid() {
        validateTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 배송 준비 시작
     */
    public void startPreparing() {
        validateTransition(OrderStatus.PREPARING);
        this.status = OrderStatus.PREPARING;
    }

    /**
     * 배송 시작
     */
    public void ship(String trackingNumber) {
        validateTransition(OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
        this.shippedAt = LocalDateTime.now();
    }

    /**
     * 배송 완료
     */
    public void deliver() {
        validateTransition(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * 주문 취소
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    /**
     * 환불 처리
     */
    public void refund() {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.ORDER_CANNOT_BE_REFUNDED);
        }
        this.status = OrderStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * 상태 전이 검증
     */
    private void validateTransition(OrderStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new CustomBusinessException(
                ShoppingErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }
}
```

---

## 3. Payment Status State Machine

### 3.1 상태 정의

```java
public enum PaymentStatus {

    /**
     * 대기 중 - 결제 생성됨
     */
    PENDING("대기 중"),

    /**
     * 처리 중 - PG사에 요청 중
     */
    PROCESSING("처리 중"),

    /**
     * 완료 - 결제 성공
     */
    COMPLETED("완료"),

    /**
     * 실패 - 결제 실패
     */
    FAILED("실패"),

    /**
     * 취소됨 - 결제 취소
     */
    CANCELLED("취소"),

    /**
     * 환불됨 - 결제 환불
     */
    REFUNDED("환불");
}
```

### 3.2 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   PAYMENT STATUS STATE MACHINE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         ┌─────────┐                                          │
│                         │ PENDING │ ◄─── 결제 생성                           │
│                         └────┬────┘                                          │
│                              │                                               │
│               ┌──────────────┼──────────────┐                                │
│               │              │              │                                │
│               ▼              ▼              │                                │
│         ┌───────────┐  ┌────────────┐       │                                │
│         │ CANCELLED │  │ PROCESSING │       │ 사용자 취소                    │
│         └───────────┘  └─────┬──────┘       │                                │
│                              │              │                                │
│               ┌──────────────┼──────────────┤                                │
│               │              │              │                                │
│               ▼              ▼              ▼                                │
│         ┌──────────┐   ┌───────────┐  ┌──────────┐                           │
│         │  FAILED  │   │ COMPLETED │  │ CANCELLED│                           │
│         └──────────┘   └─────┬─────┘  └──────────┘                           │
│                              │                                               │
│                              │ 환불 요청                                     │
│                              ▼                                               │
│                        ┌──────────┐                                          │
│                        │ REFUNDED │                                          │
│                        └──────────┘                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Payment Entity 구현

```java
@Entity
public class Payment {

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /**
     * 결제를 처리 중 상태로 변경
     */
    public void startProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new CustomBusinessException(
                ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.PROCESSING;
    }

    /**
     * 결제 완료
     */
    public void complete(String pgTransactionId, String pgResponse) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new CustomBusinessException(
                ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
        this.pgResponse = pgResponse;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail(String failureReason, String pgResponse) {
        if (this.status == PaymentStatus.COMPLETED ||
            this.status == PaymentStatus.REFUNDED) {
            throw new CustomBusinessException(
                ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.pgResponse = pgResponse;
    }

    /**
     * 결제 취소
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
    }

    /**
     * 결제 환불
     */
    public void refund(String pgTransactionId) {
        if (!this.status.isRefundable()) {
            throw new CustomBusinessException(
                ShoppingErrorCode.PAYMENT_REFUND_FAILED);
        }
        this.status = PaymentStatus.REFUNDED;
        this.pgTransactionId = pgTransactionId;
        this.refundedAt = LocalDateTime.now();
    }
}
```

---

## 4. State Machine 설계 원칙

### 4.1 Guard 조건

```java
public enum PaymentStatus {

    /**
     * 취소 가능 여부 (Guard)
     */
    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * 환불 가능 여부 (Guard)
     */
    public boolean isRefundable() {
        return this == COMPLETED;
    }
}
```

### 4.2 불변성 보장

```java
@Entity
public class Order {

    // ❌ 직접 상태 변경 불가
    // this.status = OrderStatus.PAID;

    // ✅ 비즈니스 메서드를 통한 상태 변경
    public void markAsPaid() {
        validateTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
}
```

### 4.3 상태 전이와 부수 효과

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    STATE TRANSITION SIDE EFFECTS                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   상태 전이 시 함께 발생해야 하는 부수 효과:                                 │
│                                                                              │
│   markAsPaid():                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ 1. status = PAID                                                    │   │
│   │ 2. paidAt = LocalDateTime.now()                                     │   │
│   │ 3. [Event] OrderPaidEvent 발행                                      │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   ship():                                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ 1. status = SHIPPED                                                 │   │
│   │ 2. trackingNumber = trackingNumber                                  │   │
│   │ 3. shippedAt = LocalDateTime.now()                                  │   │
│   │ 4. [Event] OrderShippedEvent 발행                                   │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   cancel():                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ 1. status = CANCELLED                                               │   │
│   │ 2. cancelledAt = LocalDateTime.now()                                │   │
│   │ 3. cancelReason = reason                                            │   │
│   │ 4. [Saga] 보상 트랜잭션 트리거                                       │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Order-Payment 상태 연동

### 5.1 상태 동기화

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              ORDER-PAYMENT STATE SYNCHRONIZATION                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Order                           Payment                                    │
│   ┌───────────┐                   ┌───────────┐                              │
│   │  PENDING  │ ◄─────────────────│  PENDING  │                              │
│   └─────┬─────┘                   └─────┬─────┘                              │
│         │                               │                                    │
│         │ 결제 요청                     │                                    │
│         │                               ▼                                    │
│         │                         ┌───────────┐                              │
│         │                         │PROCESSING │                              │
│         │                         └─────┬─────┘                              │
│         │                               │                                    │
│         │         ┌─────────────────────┼─────────────────────┐              │
│         │         │ 성공                │ 실패                │              │
│         │         ▼                     ▼                     ▼              │
│         │   ┌───────────┐         ┌──────────┐          ┌──────────┐        │
│         │   │ COMPLETED │         │  FAILED  │          │CANCELLED │        │
│         │   └─────┬─────┘         └──────────┘          └──────────┘        │
│         │         │                     │                     │              │
│         │         │                     └─────────────────────┘              │
│         │         │                               │                          │
│         │         │                               ▼                          │
│         │         │                     ┌───────────────────────┐            │
│         ▼         ▼                     │   Order → CANCELLED   │            │
│   ┌───────────────────┐                 └───────────────────────┘            │
│   │   Order → PAID    │                                                      │
│   └───────────────────┘                                                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 서비스 레이어 구현

```java
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Override
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        // 1. Order 조회
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new CustomBusinessException(
                    ShoppingErrorCode.ORDER_NOT_FOUND));

        // 2. Payment 생성
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();

        payment = paymentRepository.save(payment);

        try {
            // 3. PG 처리
            payment.startProcessing();
            PgResponse pgResponse = pgClient.process(payment);

            if (pgResponse.isSuccess()) {
                // 4. 결제 성공
                payment.complete(pgResponse.getTransactionId(),
                                 pgResponse.toJson());
                paymentRepository.save(payment);

                // 5. Saga 계속 실행 (Order 상태 변경 포함)
                sagaOrchestrator.completeSagaAfterPayment(order.getOrderNumber());

            } else {
                // 결제 실패
                payment.fail(pgResponse.getMessage(), pgResponse.toJson());
                paymentRepository.save(payment);
            }

        } catch (Exception e) {
            payment.fail(e.getMessage(), null);
            paymentRepository.save(payment);
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_FAILED);
        }

        return PaymentResponse.from(payment);
    }
}
```

---

## 6. 에러 코드 매핑

```java
public enum ShoppingErrorCode implements ErrorCode {

    // Order 상태 관련
    ORDER_NOT_FOUND("S101", "주문을 찾을 수 없습니다"),
    ORDER_CANNOT_BE_CANCELLED("S102", "이 주문은 취소할 수 없습니다"),
    ORDER_CANNOT_BE_REFUNDED("S103", "이 주문은 환불할 수 없습니다"),
    INVALID_ORDER_STATUS_TRANSITION("S104", "잘못된 주문 상태 변경입니다"),

    // Payment 상태 관련
    PAYMENT_NOT_FOUND("S201", "결제 정보를 찾을 수 없습니다"),
    PAYMENT_ALREADY_COMPLETED("S202", "이미 완료된 결제입니다"),
    PAYMENT_CANNOT_BE_CANCELLED("S203", "이 결제는 취소할 수 없습니다"),
    PAYMENT_FAILED("S204", "결제에 실패했습니다"),
    PAYMENT_REFUND_FAILED("S205", "환불에 실패했습니다");
}
```

---

## 7. 테스트 시나리오

### 7.1 정상 흐름 테스트

```java
@Test
void 정상_주문_흐름() {
    // Given
    Order order = Order.builder()
            .userId("user123")
            .build();
    order = orderRepository.save(order);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

    // When: 결제 완료
    order.markAsPaid();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

    // When: 배송 준비
    order.startPreparing();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

    // When: 배송 시작
    order.ship("TRACK-001");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);

    // When: 배송 완료
    order.deliver();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
}
```

### 7.2 잘못된 전이 테스트

```java
@Test
void 잘못된_상태_전이는_예외_발생() {
    // Given: PENDING 상태의 주문
    Order order = Order.builder().userId("user123").build();

    // When & Then: PENDING → DELIVERED 직접 전이 불가
    assertThatThrownBy(() -> order.deliver())
            .isInstanceOf(CustomBusinessException.class)
            .hasMessage("잘못된 주문 상태 변경입니다");
}

@Test
void CANCELLED_상태에서는_더_이상_전이_불가() {
    // Given
    Order order = Order.builder().userId("user123").build();
    order.cancel("Test cancellation");

    // When & Then
    assertThatThrownBy(() -> order.markAsPaid())
            .isInstanceOf(CustomBusinessException.class);
}
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **State** | 객체가 가질 수 있는 유한한 상태 |
| **Transition** | 상태 간 허용된 전이 규칙 |
| **Guard** | 전이 가능 여부를 판단하는 조건 |
| **Action** | 전이 시 실행되는 부수 효과 |
| **validateTransition()** | 전이 전 유효성 검증 |
| **canTransitionTo()** | 허용된 전이인지 확인 |

---

## 9. 설계 결정 이유

### Enum 기반 State Machine

1. **단순성**: 별도 라이브러리 없이 구현
2. **타입 안정성**: 컴파일 타임 검증
3. **가독성**: 상태와 전이 규칙이 명확
4. **성능**: 오버헤드 최소

### 주의사항

1. **상태 폭발**: 상태가 많아지면 관리 어려움
2. **병행 상태**: 여러 상태를 동시에 가질 수 없음
3. **히스토리**: 상태 변경 이력 별도 관리 필요

---

## 다음 학습

- [Saga 패턴](./saga-pattern-deep-dive.md)
- [이벤트 소싱](./event-sourcing.md)
- [주문 도메인 설계](../../services/shopping-service/docs/learning/domain/order-domain.md)
