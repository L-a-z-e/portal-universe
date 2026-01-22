# Payment Domain

## 1. 개요

Payment 도메인은 결제 처리를 담당하며, PG(Payment Gateway) 연동과 결제 상태 머신을 관리합니다.

## 2. Entity 구조

### Payment Entity

```java
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_number", columnList = "payment_number", unique = true),
    @Index(name = "idx_payment_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_status", columnList = "status")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 30)
    private String paymentNumber;        // 결제 번호 (PAY-XXXXXXXX)

    @Column(name = "order_id", nullable = false)
    private Long orderId;                // 주문 ID

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;          // 주문 번호

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;               // 사용자 ID

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;           // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;        // 결제 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod; // 결제 수단

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;      // PG사 거래 ID

    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;           // PG사 응답 (JSON)

    @Column(name = "failure_reason", length = 500)
    private String failureReason;        // 실패 사유

    @Column(name = "paid_at")
    private LocalDateTime paidAt;        // 결제 완료 일시

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;    // 환불 일시
}
```

## 3. 결제 상태 머신 (PaymentStatus)

```java
public enum PaymentStatus {
    PENDING("대기 중"),      // 결제 생성됨
    PROCESSING("처리 중"),   // PG사에 요청 중
    COMPLETED("완료"),       // 결제 성공
    FAILED("실패"),          // 결제 실패
    CANCELLED("취소"),       // 결제 취소
    REFUNDED("환불");        // 결제 환불
}
```

### 상태 전이 다이어그램

```
                    ┌─────────────────────────────────────┐
                    │                                     │
                    v                                     │
┌─────────┐    ┌───────────┐    ┌───────────┐    ┌───────┴───┐
│ PENDING │───>│PROCESSING │───>│ COMPLETED │───>│ REFUNDED  │
└────┬────┘    └─────┬─────┘    └───────────┘    └───────────┘
     │               │
     │               v
     │         ┌──────────┐
     └────────>│  FAILED  │
               └──────────┘
     │
     v
┌────────────┐
│ CANCELLED  │
└────────────┘
```

### 상태 전이 규칙

| 현재 상태 | 가능한 전이 | 조건 |
|-----------|-------------|------|
| PENDING | PROCESSING, CANCELLED | - |
| PROCESSING | COMPLETED, FAILED | PG 응답에 따라 |
| COMPLETED | REFUNDED | 환불 요청 시 |
| FAILED | - | 최종 상태 |
| CANCELLED | - | 최종 상태 |
| REFUNDED | - | 최종 상태 |

## 4. 결제 수단 (PaymentMethod)

```java
public enum PaymentMethod {
    CARD("카드"),              // 신용/체크카드
    BANK_TRANSFER("계좌이체"), // 실시간 계좌이체
    VIRTUAL_ACCOUNT("가상계좌"), // 가상계좌
    MOBILE("휴대폰"),          // 휴대폰 결제
    POINTS("포인트")           // 포인트 결제
}
```

## 5. 비즈니스 메서드

### 결제 처리 시작

```java
public void startProcessing() {
    if (this.status != PaymentStatus.PENDING) {
        throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
    }
    this.status = PaymentStatus.PROCESSING;
}
```

### 결제 완료

```java
public void complete(String pgTransactionId, String pgResponse) {
    if (this.status != PaymentStatus.PROCESSING) {
        throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
    }
    this.status = PaymentStatus.COMPLETED;
    this.pgTransactionId = pgTransactionId;
    this.pgResponse = pgResponse;
    this.paidAt = LocalDateTime.now();
}
```

### 결제 실패

```java
public void fail(String failureReason, String pgResponse) {
    if (this.status == PaymentStatus.COMPLETED ||
        this.status == PaymentStatus.REFUNDED) {
        throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_ALREADY_COMPLETED);
    }
    this.status = PaymentStatus.FAILED;
    this.failureReason = failureReason;
    this.pgResponse = pgResponse;
}
```

### 결제 취소/환불

```java
public void cancel(String reason) {
    if (!this.status.isCancellable()) {
        throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
    }
    this.status = PaymentStatus.CANCELLED;
    this.failureReason = reason;
}

public void refund(String pgTransactionId) {
    if (!this.status.isRefundable()) {
        throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_REFUND_FAILED);
    }
    this.status = PaymentStatus.REFUNDED;
    this.pgTransactionId = pgTransactionId;
    this.refundedAt = LocalDateTime.now();
}
```

## 6. PG 연동

### MockPGClient

개발/테스트 환경용 Mock PG 클라이언트:

```java
@Component
public class MockPGClient {
    private static final double SUCCESS_RATE = 0.90; // 90% 성공률

    public PgResponse processPayment(String paymentNumber, BigDecimal amount,
                                      PaymentMethod method, String cardNumber) {
        // 100ms ~ 500ms 랜덤 지연 시뮬레이션
        simulateProcessingDelay();

        if (shouldSucceed()) {
            String transactionId = generateTransactionId();
            return PgResponse.success(transactionId);
        } else {
            String errorCode = generateRandomErrorCode();
            return PgResponse.failure(errorCode, getErrorMessage(errorCode));
        }
    }

    public PgResponse refundPayment(String pgTransactionId, BigDecimal refundAmount) {
        // 환불 처리 로직
    }
}
```

### PG 응답 구조

```java
public record PgResponse(
    boolean success,
    String transactionId,
    String errorCode,
    String message,
    String rawResponse
) {
    public static PgResponse success(String transactionId) { ... }
    public static PgResponse failure(String errorCode, String message) { ... }
}
```

### PG 에러 코드

| 에러 코드 | 설명 |
|-----------|------|
| CARD_DECLINED | 카드 거절 |
| INSUFFICIENT_FUNDS | 잔액 부족 |
| INVALID_CARD | 유효하지 않은 카드 |
| EXPIRED_CARD | 만료된 카드 |
| NETWORK_ERROR | 네트워크 오류 |
| TIMEOUT | 타임아웃 |
| LIMIT_EXCEEDED | 한도 초과 |

## 7. 결제 처리 흐름

```
1. 주문 조회 및 검증
   └─ 주문자 확인
   └─ 주문 상태 확인 (CONFIRMED)
   └─ 기존 결제 여부 확인

2. 결제 생성
   └─ Payment Entity 생성 (status = PENDING)
   └─ 결제 번호 생성 (PAY-XXXXXXXX)

3. 결제 처리
   └─ status = PROCESSING
   └─ PG사 결제 요청

4. 결과 처리
   ├─ 성공: status = COMPLETED, 주문 완료 처리
   └─ 실패: status = FAILED, 재고 해제
```

## 8. Error Codes

| 코드 | 설명 |
|------|------|
| `S301` | PAYMENT_NOT_FOUND - 결제를 찾을 수 없음 |
| `S302` | PAYMENT_ALREADY_COMPLETED - 이미 완료된 결제 |
| `S303` | PAYMENT_ALREADY_CANCELLED - 이미 취소된 결제 |
| `S304` | PAYMENT_PROCESSING_FAILED - 결제 처리 실패 |
| `S305` | PAYMENT_REFUND_FAILED - 환불 실패 |
| `S308` | PAYMENT_TIMEOUT - 결제 타임아웃 |
| `S311` | PG_CONNECTION_ERROR - PG사 연결 오류 |
| `S312` | PAYMENT_CANNOT_BE_CANCELLED - 취소 불가 상태 |
| `S313` | PAYMENT_USER_MISMATCH - 결제 소유자 불일치 |

## 9. 소스 위치

- Entity: `payment/domain/Payment.java`
- Enum: `payment/domain/PaymentStatus.java`, `PaymentMethod.java`
- Repository: `payment/repository/PaymentRepository.java`
- Service: `payment/service/PaymentService.java`, `PaymentServiceImpl.java`
- Controller: `payment/controller/PaymentController.java`
- PG: `payment/pg/MockPGClient.java`, `PgResponse.java`
