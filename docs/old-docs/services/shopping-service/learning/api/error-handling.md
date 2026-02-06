# ErrorCode와 예외 처리

## 개요

Portal Universe는 일관된 에러 처리 체계를 통해 클라이언트에게 명확한 에러 정보를 제공합니다. 이 문서에서는 ErrorCode 인터페이스, 커스텀 예외, 그리고 전역 예외 처리 패턴을 설명합니다.

## 에러 처리 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Request                          │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                         │
│  - @Valid 검증 → MethodArgumentNotValidException             │
│  - @Validated 검증 → ConstraintViolationException            │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│  - 비즈니스 검증 → CustomBusinessException                    │
│  - 예외 발생 시 적절한 ErrorCode와 함께 예외 throw             │
└─────────────────────────────┬───────────────────────────────┘
                              │ 예외 발생
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 GlobalExceptionHandler                       │
│  - 예외 타입별 처리                                           │
│  - 일관된 ApiResponse 형식으로 변환                           │
│  - 적절한 HTTP 상태 코드 반환                                  │
└─────────────────────────────────────────────────────────────┘
```

## ErrorCode Interface

### 공통 라이브러리 정의

```java
package com.portal.universe.commonlibrary.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 서비스에서 사용될 오류 코드의 공통 규약입니다.
 */
public interface ErrorCode {
    /**
     * HTTP 상태 코드를 반환합니다.
     */
    HttpStatus getStatus();

    /**
     * 애플리케이션 오류 코드를 반환합니다. (예: "C001", "S001")
     */
    String getCode();

    /**
     * 사용자에게 보여줄 오류 메시지를 반환합니다.
     */
    String getMessage();
}
```

## CommonErrorCode

### 공통 에러 코드

```java
@Getter
public enum CommonErrorCode implements ErrorCode {
    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "Authentication required"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "Access denied"),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "Resource not found"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "Internal server error");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommonErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
```

## ShoppingErrorCode

### 서비스별 에러 코드 체계

```java
/**
 * 쇼핑 서비스 오류 코드
 *
 * 에러코드 체계:
 * - S0XX: Product (S001-S010)
 * - S1XX: Cart (S101-S110)
 * - S2XX: Order (S201-S220)
 * - S3XX: Payment (S301-S315)
 * - S4XX: Inventory (S401-S410)
 * - S5XX: Delivery (S501-S510)
 * - S6XX: Coupon (S601-S615)
 * - S7XX: TimeDeal (S701-S710)
 * - S8XX: Queue (S801-S810)
 * - S9XX: Saga/System (S901-S910)
 * - S10XX: Search (S1001-S1010)
 */
@Getter
public enum ShoppingErrorCode implements ErrorCode {

    // ========================================
    // Product Errors (S0XX)
    // ========================================
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    PRODUCT_ALREADY_EXISTS(HttpStatus.CONFLICT, "S002", "Product with this name already exists"),
    PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "S003", "Product is currently inactive"),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "S004", "Product price must be greater than 0"),
    INVALID_PRODUCT_QUANTITY(HttpStatus.BAD_REQUEST, "S005", "Product quantity must be greater than 0"),

    // ========================================
    // Cart Errors (S1XX)
    // ========================================
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "S102", "Cart item not found"),
    CART_ALREADY_CHECKED_OUT(HttpStatus.BAD_REQUEST, "S103", "Cart has already been checked out"),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "S104", "Cart is empty"),

    // ========================================
    // Order Errors (S2XX)
    // ========================================
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "S201", "Order not found"),
    ORDER_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "S202", "Order has already been cancelled"),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "S203", "Order cannot be cancelled in current status"),
    ORDER_USER_MISMATCH(HttpStatus.FORBIDDEN, "S212", "Order does not belong to current user"),

    // ========================================
    // Payment Errors (S3XX)
    // ========================================
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "S301", "Payment not found"),
    PAYMENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S304", "Payment processing failed"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "S310", "Insufficient balance for payment"),

    // ========================================
    // Inventory Errors (S4XX)
    // ========================================
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "S401", "Inventory not found for product"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "S402", "Insufficient stock available"),
    CONCURRENT_STOCK_MODIFICATION(HttpStatus.CONFLICT, "S408", "Stock was modified by another transaction"),

    // ========================================
    // Search Errors (S10XX)
    // ========================================
    SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S1001", "Search operation failed"),
    INVALID_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "S1002", "Invalid search query");

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

## CustomBusinessException

### 커스텀 비즈니스 예외

```java
package com.portal.universe.commonlibrary.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외를 나타내는 커스텀 예외 클래스입니다.
 */
@Getter
public class CustomBusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomBusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public CustomBusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @Override
    public OrderResponse getOrder(String userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        // 권한 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_USER_MISMATCH);
        }

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userId, String orderNumber, CancelOrderRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new CustomBusinessException(ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new CustomBusinessException(ORDER_USER_MISMATCH);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new CustomBusinessException(ORDER_ALREADY_CANCELLED);
        }

        if (!order.getStatus().isCancellable()) {
            throw new CustomBusinessException(ORDER_CANNOT_BE_CANCELLED);
        }

        order.cancel(request.reason());
        return OrderResponse.from(order);
    }
}
```

## GlobalExceptionHandler

### 전역 예외 처리기

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomBusinessException 처리
     */
    @ExceptionHandler(CustomBusinessException.class)
    protected ResponseEntity<ApiResponse<Object>> handleCustomBusinessException(
            CustomBusinessException e
    ) {
        log.error("Business exception: {}", e.getErrorCode().getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Object> response = ApiResponse.error(
            errorCode.getCode(),
            errorCode.getMessage()
        );
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("Validation failed: {}", e.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(ErrorResponse.FieldError::from)
            .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
            CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
            CommonErrorCode.INVALID_INPUT_VALUE.getMessage(),
            request.getRequestURI(),
            fieldErrors
        );

        return new ResponseEntity<>(
            ApiResponse.errorWithDetails(errorResponse),
            HttpStatus.BAD_REQUEST
        );
    }

    /**
     * @Validated 검증 실패 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation: {}", e.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = e.getConstraintViolations()
            .stream()
            .map(v -> new ErrorResponse.FieldError(
                extractFieldName(v),
                v.getMessage(),
                v.getInvalidValue()
            ))
            .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
            CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
            CommonErrorCode.INVALID_INPUT_VALUE.getMessage(),
            request.getRequestURI(),
            fieldErrors
        );

        return new ResponseEntity<>(
            ApiResponse.errorWithDetails(errorResponse),
            HttpStatus.BAD_REQUEST
        );
    }

    /**
     * JSON 파싱 오류 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.warn("JSON parse error: {}", e.getMessage());
        return new ResponseEntity<>(
            ApiResponse.error(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                "Invalid request body format"
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 리소스 없음 (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(
            NoResourceFoundException e
    ) {
        log.warn("Resource not found: {}", e.getMessage());
        return new ResponseEntity<>(
            ApiResponse.error(
                CommonErrorCode.NOT_FOUND.getCode(),
                CommonErrorCode.NOT_FOUND.getMessage()
            ),
            HttpStatus.NOT_FOUND
        );
    }

    /**
     * 예상치 못한 예외 처리 (500)
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return new ResponseEntity<>(
            ApiResponse.error(
                CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private String extractFieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
```

## ApiResponse 구조

### 성공/실패 통합 응답

```java
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }

    public static <T> ApiResponse<T> errorWithDetails(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, null, errorResponse);
    }
}
```

### ErrorResponse

```java
public record ErrorResponse(
    String code,
    String message,
    String path,
    List<FieldError> errors
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null, null);
    }

    public record FieldError(
        String field,
        String message,
        Object rejectedValue
    ) {
        public static FieldError from(org.springframework.validation.FieldError error) {
            return new FieldError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            );
        }
    }
}
```

## 에러 응답 예시

### 비즈니스 예외

```json
{
  "success": false,
  "error": {
    "code": "S201",
    "message": "Order not found"
  }
}
```

### 유효성 검증 실패

```json
{
  "success": false,
  "error": {
    "code": "C001",
    "message": "Invalid input value",
    "path": "/api/v1/orders",
    "errors": [
      {
        "field": "shippingAddress.receiverName",
        "message": "Receiver name is required",
        "rejectedValue": ""
      }
    ]
  }
}
```

### 서버 오류

```json
{
  "success": false,
  "error": {
    "code": "C999",
    "message": "Internal server error"
  }
}
```

## Best Practices

### 1. 에러 코드 체계화

- 서비스별 prefix 사용 (C: Common, S: Shopping, A: Auth, B: Blog)
- 도메인별 범위 지정 (S001-S010: Product, S101-S110: Cart)
- 새 에러 추가 시 체계 유지

### 2. 적절한 HTTP 상태 코드

| 상황 | 상태 코드 |
|------|----------|
| 리소스 없음 | 404 |
| 유효성 검증 실패 | 400 |
| 인증 필요 | 401 |
| 권한 없음 | 403 |
| 중복/충돌 | 409 |
| 서버 오류 | 500 |

### 3. 로깅

- 비즈니스 예외: WARN 레벨
- 예상치 못한 예외: ERROR 레벨 + 스택 트레이스
- 민감 정보 로깅 금지

### 4. 클라이언트 친화적 메시지

- 기술적 세부사항 숨기기
- 사용자가 이해할 수 있는 메시지
- 해결 방법 제시 (가능한 경우)

## 관련 문서

- [Validation](./validation.md) - Bean Validation
- [RESTful API Design](./restful-api-design.md) - REST 설계 원칙
- [DTO Design](./dto-design.md) - Request/Response DTO
