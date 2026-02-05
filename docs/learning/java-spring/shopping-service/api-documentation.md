# API 문서화 (OpenAPI/Swagger)

## 개요

Portal Universe는 OpenAPI 3.0 (Swagger)을 활용하여 API를 문서화합니다. 이 문서에서는 SpringDoc을 사용한 API 문서 자동 생성 방법을 설명합니다.

## 의존성 설정

### build.gradle

```groovy
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
}
```

## OpenAPI 설정

### SpringDoc Configuration

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Shopping Service API")
                .description("Portal Universe Shopping Service REST API Documentation")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Portal Universe Team")
                    .email("dev@portal-universe.com")
                )
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")
                )
            )
            .externalDocs(new ExternalDocumentation()
                .description("GitHub Repository")
                .url("https://github.com/portal-universe")
            )
            .servers(List.of(
                new Server().url("http://localhost:8082").description("Local Development"),
                new Server().url("https://api.portal-universe.com").description("Production")
            ));
    }
}
```

### application.yml 설정

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: alpha
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

## 버전별 문서 분리

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
            .group("v1")
            .displayName("API V1")
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return GroupedOpenApi.builder()
            .group("v2")
            .displayName("API V2")
            .pathsToMatch("/api/v2/**")
            .build();
    }
}
```

접근 URL:
- `/swagger-ui.html?group=v1`
- `/swagger-ui.html?group=v2`

## Controller 문서화

### 기본 어노테이션

```java
@Tag(name = "Orders", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
        summary = "주문 생성",
        description = "장바구니의 상품으로 새 주문을 생성합니다. 재고 예약이 함께 진행됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "주문 생성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponseWrapper.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (유효성 검증 실패)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseWrapper.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "재고 부족",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseWrapper.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @RequestBody @Valid CreateOrderRequest request
    ) {
        OrderResponse order = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(order));
    }

    @Operation(
        summary = "주문 상세 조회",
        description = "주문 번호로 주문 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> get(
            @Parameter(description = "주문 번호", example = "ORD-20250122-001")
            @PathVariable String orderNumber
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(orderService.get(orderNumber))
        );
    }

    @Operation(
        summary = "주문 목록 조회",
        description = "사용자의 주문 목록을 페이징하여 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "정렬 기준 (예: createdAt,desc)")
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
            ApiResponse.success(orderService.list(pageable))
        );
    }

    @Operation(
        summary = "주문 취소",
        description = "주문을 취소합니다. 결제 전 주문만 취소 가능합니다."
    )
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable String orderNumber,
            @RequestBody @Valid CancelOrderRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(orderService.cancel(orderNumber, request))
        );
    }
}
```

## DTO 문서화

### Request DTO

```java
@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
    @Schema(description = "배송 주소", required = true)
    @NotNull @Valid
    AddressRequest shippingAddress,

    @Schema(description = "적용할 쿠폰 ID", example = "123", nullable = true)
    Long userCouponId
) {}

@Schema(description = "배송 주소")
public record AddressRequest(
    @Schema(description = "수령인 이름", example = "홍길동", required = true)
    @NotBlank
    @Size(max = 100)
    String receiverName,

    @Schema(description = "수령인 연락처", example = "010-1234-5678", required = true)
    @NotBlank
    @Size(max = 20)
    String receiverPhone,

    @Schema(description = "우편번호", example = "12345", required = true)
    @NotBlank
    @Size(max = 10)
    String zipCode,

    @Schema(description = "기본 주소", example = "서울시 강남구 테헤란로 123", required = true)
    @NotBlank
    @Size(max = 255)
    String address1,

    @Schema(description = "상세 주소", example = "5층 501호", nullable = true)
    @Size(max = 255)
    String address2
) {}
```

### Response DTO

```java
@Schema(description = "주문 응답")
public record OrderResponse(
    @Schema(description = "주문 ID", example = "1")
    Long id,

    @Schema(description = "주문 번호", example = "ORD-20250122-001")
    String orderNumber,

    @Schema(description = "사용자 ID", example = "user-123")
    String userId,

    @Schema(description = "주문 상태", example = "PENDING")
    OrderStatus status,

    @Schema(description = "주문 상태 설명", example = "결제 대기 중")
    String statusDescription,

    @Schema(description = "주문 항목 목록")
    List<OrderItemResponse> items,

    @Schema(description = "총 금액", example = "150000")
    BigDecimal totalAmount,

    @Schema(description = "주문 생성 일시")
    LocalDateTime createdAt
) {}
```

### Enum 문서화

```java
@Schema(description = "주문 상태")
public enum OrderStatus {
    @Schema(description = "결제 대기")
    PENDING,

    @Schema(description = "결제 완료")
    PAID,

    @Schema(description = "배송 준비")
    PREPARING,

    @Schema(description = "배송 중")
    SHIPPING,

    @Schema(description = "배송 완료")
    DELIVERED,

    @Schema(description = "취소됨")
    CANCELLED
}
```

## ApiResponse Wrapper 문서화

Swagger에서 제네릭 타입을 정확히 표현하기 위한 Wrapper 클래스:

```java
// 성공 응답 예시용 Wrapper
public class OrderResponseWrapper extends ApiResponse<OrderResponse> {}
public class OrderListResponseWrapper extends ApiResponse<Page<OrderResponse>> {}

// 에러 응답 예시용 Wrapper
public class ErrorResponseWrapper extends ApiResponse<Void> {}
```

## 인증 문서화

### Security Scheme 설정

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(...)
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .name("Authorization")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 입력하세요. (Bearer prefix 자동 추가)")
                )
            );
    }
}
```

### 엔드포인트별 보안 설정

```java
// 인증 필요 없는 엔드포인트
@Operation(summary = "상품 목록 조회", security = {})
@GetMapping
public ResponseEntity<ApiResponse<List<ProductResponse>>> list() { ... }

// 인증 필요한 엔드포인트 (기본값 사용)
@Operation(summary = "주문 생성")
@PostMapping
public ResponseEntity<ApiResponse<OrderResponse>> create(...) { ... }
```

## 예시 데이터

### @ExampleObject 사용

```java
@Operation(summary = "주문 생성")
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    content = @Content(
        examples = {
            @ExampleObject(
                name = "기본 주문",
                summary = "배송지 정보만 포함한 기본 주문",
                value = """
                    {
                      "shippingAddress": {
                        "receiverName": "홍길동",
                        "receiverPhone": "010-1234-5678",
                        "zipCode": "12345",
                        "address1": "서울시 강남구 테헤란로 123",
                        "address2": "5층 501호"
                      }
                    }
                    """
            ),
            @ExampleObject(
                name = "쿠폰 적용 주문",
                summary = "쿠폰을 적용한 주문",
                value = """
                    {
                      "shippingAddress": {
                        "receiverName": "홍길동",
                        "receiverPhone": "010-1234-5678",
                        "zipCode": "12345",
                        "address1": "서울시 강남구 테헤란로 123"
                      },
                      "userCouponId": 123
                    }
                    """
            )
        }
    )
)
@PostMapping
public ResponseEntity<ApiResponse<OrderResponse>> create(
        @RequestBody @Valid CreateOrderRequest request
) { ... }
```

## 응답 예시

```java
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "orderNumber": "ORD-20250122-001",
                        "status": "PENDING",
                        "totalAmount": 150000,
                        "items": [
                          {
                            "productId": 101,
                            "productName": "무선 이어폰",
                            "quantity": 1,
                            "price": 89000
                          }
                        ]
                      }
                    }
                    """
            )
        )
    )
})
```

## 접근 URL

| URL | 설명 |
|-----|------|
| `/swagger-ui.html` | Swagger UI |
| `/api-docs` | OpenAPI JSON |
| `/api-docs.yaml` | OpenAPI YAML |

## 운영 환경 설정

### 프로덕션에서 비활성화

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

### 접근 제한

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            // Swagger는 내부 IP만 허용
            .requestMatchers("/swagger-ui/**", "/api-docs/**")
                .hasIpAddress("10.0.0.0/8")
            .anyRequest().permitAll()
        );
        return http.build();
    }
}
```

## Best Practices

### 1. 설명 작성

- 모든 엔드포인트에 summary와 description 작성
- 비즈니스 용어 사용
- 제한 사항이나 주의 사항 명시

### 2. 예시 데이터

- 실제 사용 가능한 예시 제공
- 다양한 시나리오 커버
- 필수/선택 필드 구분

### 3. 에러 응답

- 가능한 모든 에러 코드 문서화
- 에러 상황 설명

### 4. 버전 관리

- API 버전별 문서 분리
- 변경 이력 명시

## 관련 문서

- [RESTful API Design](./restful-api-design.md) - REST 설계 원칙
- [API Versioning](./api-versioning.md) - 버저닝 전략
- [Error Handling](./error-handling.md) - 예외 처리
