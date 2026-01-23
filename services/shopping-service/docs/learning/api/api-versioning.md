# API 버저닝 전략

## 개요

API 버저닝은 하위 호환성을 유지하면서 API를 발전시키는 핵심 전략입니다. Portal Universe는 **URL Path 버저닝**을 기본 전략으로 채택합니다.

## 버저닝 방식 비교

### 1. URL Path (Portal Universe 채택)

```
GET /api/v1/products
GET /api/v2/products
```

**장점**:
- 직관적이고 명확함
- 캐싱 친화적
- 브라우저에서 쉽게 테스트 가능
- 라우팅이 단순함

**단점**:
- URL이 길어질 수 있음
- 리소스 개념과 충돌할 수 있음

### 2. Header (Accept Header)

```
GET /api/products
Accept: application/vnd.portal.v1+json
```

**장점**:
- URL 깔끔함
- REST 원칙에 더 부합

**단점**:
- 브라우저에서 테스트 어려움
- 캐싱 복잡도 증가

### 3. Query Parameter

```
GET /api/products?version=1
```

**장점**:
- 구현 간단

**단점**:
- 캐싱 문제
- RESTful하지 않음

## Portal Universe 버저닝 규칙

### URL 구조

```
/api/v{major}/resources
```

예시:
```
/api/v1/products
/api/v1/orders
/api/v2/products  (신규 버전)
```

### Controller 구현

```java
// V1 Controller
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductControllerV1 {

    private final ProductServiceV1 productService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseV1>> get(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success(productService.get(id))
        );
    }
}

// V2 Controller (새 기능 추가)
@RestController
@RequestMapping("/api/v2/products")
@RequiredArgsConstructor
public class ProductControllerV2 {

    private final ProductServiceV2 productService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseV2>> get(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success(productService.get(id))
        );
    }

    // V2에서 추가된 엔드포인트
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<ApiResponse<List<ProductResponseV2>>> recommendations(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(productService.getRecommendations(id))
        );
    }
}
```

## 버전 업그레이드 기준

### Major 버전 변경 (v1 -> v2)

다음 경우에 버전을 올립니다:

1. **Breaking Changes**
   - 필드 제거
   - 필드 타입 변경
   - 필수 파라미터 추가
   - 응답 구조 변경

2. **예시**

```java
// V1 Response
public record ProductResponseV1(
    Long id,
    String name,
    BigDecimal price
) {}

// V2 Response (구조 변경)
public record ProductResponseV2(
    Long id,
    String name,
    PriceInfo price,    // 타입 변경 (BigDecimal -> PriceInfo)
    List<String> tags   // 필드 추가
) {
    public record PriceInfo(
        BigDecimal original,
        BigDecimal discounted,
        int discountRate
    ) {}
}
```

### 하위 호환 변경 (버전 유지)

버전을 올리지 않아도 되는 경우:

1. **Optional 필드 추가**
2. **새로운 엔드포인트 추가**
3. **Optional 파라미터 추가**

```java
// 기존 V1에 optional 필드 추가 (OK)
public record ProductResponseV1(
    Long id,
    String name,
    BigDecimal price,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String description  // Optional 필드 추가 (하위 호환)
) {}
```

## 버전 공존 전략

### 1. 서비스 레이어 공유

```java
// 공통 서비스
@Service
@RequiredArgsConstructor
public class ProductServiceCommon {

    private final ProductRepository productRepository;

    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(PRODUCT_NOT_FOUND));
    }
}

// V1 서비스
@Service
@RequiredArgsConstructor
public class ProductServiceV1 {

    private final ProductServiceCommon commonService;

    public ProductResponseV1 get(Long id) {
        Product product = commonService.findById(id);
        return ProductResponseV1.from(product);
    }
}

// V2 서비스
@Service
@RequiredArgsConstructor
public class ProductServiceV2 {

    private final ProductServiceCommon commonService;
    private final RecommendationService recommendationService;

    public ProductResponseV2 get(Long id) {
        Product product = commonService.findById(id);
        return ProductResponseV2.from(product);
    }

    public List<ProductResponseV2> getRecommendations(Long id) {
        return recommendationService.getRecommendations(id);
    }
}
```

### 2. Adapter 패턴

```java
@Component
public class ProductResponseAdapter {

    public ProductResponseV1 toV1(Product product) {
        return new ProductResponseV1(
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }

    public ProductResponseV2 toV2(Product product, List<String> tags) {
        return new ProductResponseV2(
            product.getId(),
            product.getName(),
            new ProductResponseV2.PriceInfo(
                product.getOriginalPrice(),
                product.getDiscountedPrice(),
                product.getDiscountRate()
            ),
            tags
        );
    }
}
```

## 버전 폐기 (Deprecation)

### 폐기 프로세스

```
1. 폐기 예고 (6개월 전)
   - 문서 업데이트
   - 응답 헤더에 경고 추가
   - 클라이언트 알림

2. Sunset 기간 (3개월)
   - API는 동작하지만 경고 강화
   - 마이그레이션 가이드 제공

3. 최종 폐기
   - 410 Gone 응답
   - 신규 버전으로 리다이렉트 안내
```

### Deprecation 헤더

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponseV1>> get(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header("Deprecation", "true")
        .header("Sunset", "Sat, 01 Jul 2025 00:00:00 GMT")
        .header("Link", "</api/v2/products/{id}>; rel=\"successor-version\"")
        .body(ApiResponse.success(productService.get(id)));
}
```

### 폐기 후 응답

```java
@RestController
@RequestMapping("/api/v1/deprecated-resource")
public class DeprecatedController {

    @GetMapping
    public ResponseEntity<ApiResponse<Void>> deprecated() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(ApiResponse.error(
                "DEPRECATED",
                "This API version has been deprecated. Please use /api/v2/resource instead."
            ));
    }
}
```

## 문서화

### OpenAPI (Swagger) 버전별 문서

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
            .group("v1")
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return GroupedOpenApi.builder()
            .group("v2")
            .pathsToMatch("/api/v2/**")
            .build();
    }
}
```

### API 문서 접근

```
/swagger-ui.html?group=v1  # V1 문서
/swagger-ui.html?group=v2  # V2 문서
```

## 클라이언트 마이그레이션 가이드

### 변경 사항 문서화

```markdown
# V1 -> V2 마이그레이션 가이드

## Breaking Changes

### 1. 상품 가격 응답 구조 변경

**V1:**
```json
{ "price": 89000 }
```

**V2:**
```json
{
  "price": {
    "original": 100000,
    "discounted": 89000,
    "discountRate": 11
  }
}
```

### 2. 새로운 필수 헤더

V2부터 `X-Client-Version` 헤더가 필수입니다.

## 마이그레이션 체크리스트

- [ ] 가격 필드 타입 변경 반영
- [ ] X-Client-Version 헤더 추가
- [ ] 신규 tags 필드 처리 로직 추가
```

## Best Practices

1. **최소 2개 버전 유지**: 현재 버전 + 이전 버전
2. **충분한 마이그레이션 기간**: 최소 6개월
3. **명확한 변경 문서**: 모든 Breaking Changes 문서화
4. **자동화된 테스트**: 각 버전별 테스트 스위트 유지
5. **모니터링**: 버전별 사용량 추적

## 관련 문서

- [RESTful API Design](./restful-api-design.md) - REST 설계 원칙
- [API Documentation](./api-documentation.md) - OpenAPI/Swagger
- [DTO Design](./dto-design.md) - Request/Response DTO
