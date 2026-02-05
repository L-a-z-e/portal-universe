# Payment Integration (결제 연동)

## 개요

Shopping Service의 PG(Payment Gateway) 연동 구조와 결제 처리 플로우를 설명합니다.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  PaymentService │────▶│   MockPGClient  │────▶│  (실제 PG사)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│    Payment      │     │   PgResponse    │
│    (Entity)     │     │   (DTO)         │
└─────────────────┘     └─────────────────┘
```

## 핵심 컴포넌트

### 1. Payment Entity

`/payment/domain/Payment.java`

결제 정보를 저장하는 엔티티입니다.

```java
@Entity
@Table(name = "payments")
public class Payment {
    private String paymentNumber;      // PAY-XXXXXXXX 형식
    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private PaymentStatus status;      // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    private PaymentMethod paymentMethod;
    private String pgTransactionId;    // PG사 거래 ID
    private String pgResponse;         // PG사 원본 응답 (JSON)
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
}
```

### 2. PaymentStatus Lifecycle

```
PENDING ──────▶ PROCESSING ──────▶ COMPLETED ──────▶ REFUNDED
    │               │
    │               ▼
    └──────▶    FAILED
    │
    ▼
CANCELLED
```

| Status | 설명 | 취소 가능 | 환불 가능 |
|--------|------|----------|----------|
| PENDING | 결제 생성됨 | O | X |
| PROCESSING | PG사 요청 중 | O | X |
| COMPLETED | 결제 성공 | X | O |
| FAILED | 결제 실패 | X | X |
| CANCELLED | 결제 취소 | X | X |
| REFUNDED | 결제 환불 | X | X |

### 3. MockPGClient

`/payment/pg/MockPGClient.java`

개발/테스트 환경용 Mock PG 클라이언트입니다.

```java
@Component
public class MockPGClient {
    private static final double SUCCESS_RATE = 0.90; // 90% 성공률

    public PgResponse processPayment(String paymentNumber, BigDecimal amount,
                                     PaymentMethod method, String cardNumber) {
        simulateProcessingDelay();  // 100~500ms 랜덤 지연

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

**시뮬레이션 에러 코드:**
- `CARD_DECLINED`: 카드 거절
- `INSUFFICIENT_FUNDS`: 잔액 부족
- `INVALID_CARD`: 유효하지 않은 카드
- `EXPIRED_CARD`: 만료된 카드
- `NETWORK_ERROR`: 네트워크 오류
- `TIMEOUT`: 타임아웃
- `LIMIT_EXCEEDED`: 한도 초과

## 결제 플로우

### 1. 결제 처리 (processPayment)

```java
@Transactional
public PaymentResponse processPayment(String userId, ProcessPaymentRequest request) {
    // 1. 주문 조회 및 검증
    Order order = orderRepository.findByOrderNumberWithItems(request.orderNumber())
            .orElseThrow(() -> new CustomBusinessException(ORDER_NOT_FOUND));

    // 주문자 확인
    if (!order.getUserId().equals(userId)) {
        throw new CustomBusinessException(ORDER_USER_MISMATCH);
    }

    // 주문 상태 확인 (CONFIRMED 상태여야 결제 가능)
    if (order.getStatus() != OrderStatus.CONFIRMED) {
        throw new CustomBusinessException(INVALID_ORDER_STATUS);
    }

    // 2. Payment 엔티티 생성 (쿠폰 적용된 최종 금액)
    Payment payment = Payment.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .amount(order.getFinalAmount())  // 할인 적용 금액
            .paymentMethod(request.paymentMethod())
            .build();

    payment = paymentRepository.save(payment);
    payment.startProcessing();

    // 3. PG사 결제 요청
    PgResponse pgResponse = mockPGClient.processPayment(
            payment.getPaymentNumber(),
            payment.getAmount(),
            payment.getPaymentMethod(),
            request.cardNumber()
    );

    // 4. 결제 결과 처리
    if (pgResponse.success()) {
        payment.complete(pgResponse.transactionId(), pgResponse.rawResponse());

        // 5. 주문 완료 처리 (Saga 나머지 단계)
        try {
            orderService.completeOrderAfterPayment(order.getOrderNumber());
        } catch (Exception e) {
            // 주문 완료 실패 시 결제 환불
            refundPaymentInternal(payment);
            throw new CustomBusinessException(ORDER_CREATION_FAILED);
        }
    } else {
        payment.fail(pgResponse.errorCode() + ": " + pgResponse.message(),
                     pgResponse.rawResponse());
        throw new CustomBusinessException(PAYMENT_PROCESSING_FAILED);
    }

    return PaymentResponse.from(payment);
}
```

### 2. 환불 처리 (refundPayment)

```java
private Payment refundPaymentInternal(Payment payment) {
    if (!payment.getStatus().isRefundable()) {
        throw new CustomBusinessException(PAYMENT_REFUND_FAILED);
    }

    // PG사 환불 요청
    PgResponse pgResponse = mockPGClient.refundPayment(
            payment.getPgTransactionId(),
            payment.getAmount()
    );

    if (pgResponse.success()) {
        payment.refund(pgResponse.transactionId());

        // 주문 환불 처리
        Order order = orderRepository.findByOrderNumber(payment.getOrderNumber())
                .orElse(null);
        if (order != null) {
            order.refund();
        }
    } else {
        throw new CustomBusinessException(PAYMENT_REFUND_FAILED);
    }

    return payment;
}
```

## PgResponse DTO

```java
public record PgResponse(
    boolean success,
    String transactionId,
    String message,
    String errorCode,
    String rawResponse
) {
    public static PgResponse success(String transactionId) {
        return new PgResponse(
            true, transactionId, "Payment processed successfully", null,
            "{\"status\":\"SUCCESS\",\"txId\":\"" + transactionId + "\"}"
        );
    }

    public static PgResponse failure(String errorCode, String message) {
        return new PgResponse(
            false, null, message, errorCode,
            "{\"status\":\"FAILED\",\"errorCode\":\"" + errorCode + "\"}"
        );
    }
}
```

## Webhook 처리 (향후 구현)

실제 PG사 연동 시에는 비동기 웹훅으로 결제 결과를 받습니다.

```java
// 예시: 웹훅 컨트롤러
@RestController
@RequestMapping("/api/v1/payments/webhook")
public class PaymentWebhookController {

    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(@RequestBody TossWebhookRequest request) {
        // 1. 서명 검증
        validateSignature(request);

        // 2. 결제 상태 업데이트
        paymentService.handleWebhook(request.getPaymentKey(), request.getStatus());

        // 3. 200 OK 응답 (재시도 방지)
        return ResponseEntity.ok().build();
    }
}
```

## 보안 고려사항

1. **카드 정보 비저장**: 카드번호는 마스킹 처리, PCI-DSS 준수
2. **금융 정보 로깅**: 금액은 DEBUG 레벨로만 로깅 (프로덕션 비활성화)
3. **멱등성 보장**: paymentNumber로 중복 결제 방지
4. **타임아웃 처리**: PG 요청 시 적절한 타임아웃 설정

## 프로덕션 전환 체크리스트

- [ ] MockPGClient를 실제 PG 클라이언트로 교체
- [ ] Webhook 엔드포인트 구현 및 서명 검증
- [ ] 결제 재시도 로직 구현
- [ ] 부분 환불 지원
- [ ] 결제 취소 시간 제한 설정
- [ ] 모니터링 및 알림 설정

## 관련 파일

- `/payment/service/PaymentServiceImpl.java` - 결제 서비스 구현
- `/payment/pg/MockPGClient.java` - Mock PG 클라이언트
- `/payment/domain/Payment.java` - 결제 엔티티
- `/payment/domain/PaymentStatus.java` - 결제 상태 enum
