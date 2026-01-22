# Bean Validation

## 개요

Portal Universe는 Jakarta Bean Validation (JSR-380)을 활용하여 API 입력 데이터를 검증합니다. 이 문서에서는 유효성 검증 패턴과 커스텀 검증 구현 방법을 설명합니다.

## 기본 Validation Annotations

### 자주 사용하는 어노테이션

| 어노테이션 | 설명 | 예시 |
|-----------|------|------|
| `@NotNull` | null 불허 | `@NotNull Long id` |
| `@NotBlank` | null, 빈 문자열, 공백만 있는 문자열 불허 | `@NotBlank String name` |
| `@NotEmpty` | null, 빈 컬렉션/배열/문자열 불허 | `@NotEmpty List<Item> items` |
| `@Size` | 크기 제한 | `@Size(min=2, max=100) String name` |
| `@Min` / `@Max` | 숫자 최소/최대값 | `@Min(0) @Max(100) int quantity` |
| `@Positive` | 양수만 허용 | `@Positive BigDecimal price` |
| `@PositiveOrZero` | 0 이상 허용 | `@PositiveOrZero int stock` |
| `@Email` | 이메일 형식 | `@Email String email` |
| `@Pattern` | 정규식 패턴 | `@Pattern(regexp="^[A-Z].*") String code` |

### Record DTO에서 사용

```java
public record AddressRequest(
    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be at most 100 characters")
    String receiverName,

    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20, message = "Receiver phone must be at most 20 characters")
    String receiverPhone,

    @NotBlank(message = "Zip code is required")
    @Size(max = 10, message = "Zip code must be at most 10 characters")
    String zipCode,

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be at most 255 characters")
    String address1,

    @Size(max = 255, message = "Detail address must be at most 255 characters")
    String address2  // Optional - @NotBlank 없음
) {}
```

## Controller에서 Validation

### @Valid 사용

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request  // @Valid로 검증 활성화
    ) {
        OrderResponse order = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order));
    }
}
```

### 중첩 객체 검증

```java
public record CreateOrderRequest(
    @NotNull(message = "Shipping address is required")
    @Valid  // 중첩 객체도 검증
    AddressRequest shippingAddress,

    Long userCouponId
) {}
```

### Path Variable / Request Parameter 검증

```java
@RestController
@RequestMapping("/api/v1/products")
@Validated  // 클래스 레벨에 @Validated 필요
public class ProductController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(
            @PathVariable @Positive(message = "ID must be positive") Long id
    ) {
        // ...
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> list(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) @Max(100) int size
    ) {
        // ...
    }
}
```

## 그룹 기반 Validation

### Validation Groups 정의

```java
public interface ValidationGroups {
    interface Create {}
    interface Update {}
}
```

### 그룹별 검증

```java
public record ProductRequest(
    @NotNull(groups = ValidationGroups.Update.class)
    Long id,

    @NotBlank(message = "Name is required", groups = {
        ValidationGroups.Create.class,
        ValidationGroups.Update.class
    })
    @Size(max = 100)
    String name,

    @NotNull(message = "Price is required", groups = ValidationGroups.Create.class)
    @Positive
    BigDecimal price
) {}
```

### Controller에서 그룹 지정

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> create(
        @Validated(ValidationGroups.Create.class) @RequestBody ProductRequest request
) {
    // ...
}

@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>> update(
        @PathVariable Long id,
        @Validated(ValidationGroups.Update.class) @RequestBody ProductRequest request
) {
    // ...
}
```

## Custom Validator 구현

### 1. 어노테이션 정의

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface PhoneNumber {
    String message() default "Invalid phone number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### 2. Validator 구현

```java
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;  // @NotBlank와 조합해서 사용
        }
        return PHONE_PATTERN.matcher(value).matches();
    }
}
```

### 3. 사용

```java
public record AddressRequest(
    @NotBlank
    String receiverName,

    @NotBlank
    @PhoneNumber
    String receiverPhone,
    // ...
) {}
```

## Cross-Field Validation

### 클래스 레벨 Validator

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, TimeDealCreateRequest> {

    @Override
    public boolean isValid(TimeDealCreateRequest request, ConstraintValidatorContext context) {
        if (request.startAt() == null || request.endAt() == null) {
            return true;
        }
        return request.endAt().isAfter(request.startAt());
    }
}

@ValidDateRange
public record TimeDealCreateRequest(
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    @Positive int discountRate
) {}
```

## Validation 에러 처리

### GlobalExceptionHandler

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
     * @Validated 검증 실패 처리 (Path Variable, Request Param)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation: {}", e.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = e.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldError(
                        extractFieldName(violation),
                        violation.getMessage(),
                        violation.getInvalidValue()
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

    private String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDotIndex = propertyPath.lastIndexOf('.');
        return lastDotIndex >= 0 ? propertyPath.substring(lastDotIndex + 1) : propertyPath;
    }
}
```

### 에러 응답 구조

```java
public record ErrorResponse(
    String code,
    String message,
    String path,
    List<FieldError> errors
) {
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

### 에러 응답 예시

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
      },
      {
        "field": "shippingAddress.zipCode",
        "message": "Zip code must be at most 10 characters",
        "rejectedValue": "12345678901234567890"
      }
    ]
  }
}
```

## Service Layer Validation

### 비즈니스 규칙 검증

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // 비즈니스 규칙 검증
        Cart cart = cartService.getActiveCart(userId);

        if (cart.isEmpty()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
        }

        // 재고 검증
        for (CartItem item : cart.getItems()) {
            if (!inventoryService.hasEnoughStock(item.getProductId(), item.getQuantity())) {
                throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 주문 생성 로직...
    }
}
```

## Best Practices

### 1. 에러 메시지

- 사용자 친화적인 메시지
- 어떤 값이 기대되는지 명시
- i18n 지원 고려

```java
@NotBlank(message = "상품명을 입력해주세요")
@Size(min = 2, max = 100, message = "상품명은 2자 이상 100자 이하로 입력해주세요")
String name;
```

### 2. Validation 위치

| 검증 유형 | 위치 | 예시 |
|----------|------|------|
| 형식 검증 | DTO | 필수 필드, 형식, 길이 |
| 비즈니스 규칙 | Service | 재고 충분, 권한 확인 |
| DB 제약 | Entity/DB | 유니크 제약 |

### 3. Fail Fast

- 가능한 빨리 검증 실패
- 모든 검증 오류 한 번에 반환

### 4. 방어적 프로그래밍

```java
@Override
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // null 체크 (방어적)
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    // 비즈니스 로직...
}
```

## 관련 문서

- [DTO Design](./dto-design.md) - Request/Response DTO
- [Error Handling](./error-handling.md) - 예외 처리
- [RESTful API Design](./restful-api-design.md) - REST 설계 원칙
