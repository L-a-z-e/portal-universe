# RBAC 리팩토링 구현 가이드 - Shopping Service

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- SecurityConfig: ADMIN만 상품 생성/수정/삭제, 재고/배송 관리
- AdminProductController, AdminOrderController 등: `@PreAuthorize("hasRole('ADMIN')")` 클래스 레벨
- InventoryController, DeliveryController: @PreAuthorize 없음 (SecurityConfig에서만 제어)
- 주문/결제: userId 기반 본인 확인 (Service 레이어)
- Seller/Buyer 구분 없음
- Product 엔티티에 sellerId 없음
- Feign Client로 auth-service/blog-service 통신 (Authorization 헤더 자동 전파)

## 변경 목표
- SELLER 역할 도입: 상품 등록/수정/삭제 (본인 상품만)
- SHOPPING_ADMIN: 모든 상품/주문/배송/재고 관리
- Product에 sellerId 추가: 판매자 소유권 검증
- Seller 전용 API 경로: `/seller/**`
- 멤버십 기반 기능 차별화 (PREMIUM → 타임딜 조기 접근 등)
- SecurityConfig 권한 체계 전면 개편
- @PreAuthorize 일관성 확보

---

## 구현 단계

### Phase 2: GatewayAuthenticationFilter 수신 변경

common-library의 EnhancedGatewayAuthenticationFilter 적용:
- X-User-Roles: 콤마 구분 복수 Authority
- X-User-Memberships: JSON → MembershipContext

### Phase 3: SecurityConfig 변경

```
// 공개 (변경 없음)
GET /products, /products/**              → permitAll
GET /categories, /categories/**          → permitAll
GET /coupons, /time-deals/**            → permitAll

// 인증 필요 (변경 없음)
/cart/**                                → authenticated
POST /orders                            → authenticated
GET /orders, /orders/**                 → authenticated
POST /orders/*/cancel                   → authenticated
POST /payments                          → authenticated
GET /payments/**                        → authenticated

// SELLER + SHOPPING_ADMIN (신규)
POST /products                          → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")
PUT /products/**                        → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")
DELETE /products/**                     → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")
/seller/**                              → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")

// SHOPPING_ADMIN (기존 ADMIN → 변경)
/admin/**                               → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
POST /inventory/**                      → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
PUT /inventory/**                       → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
PUT /deliveries/**                      → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
POST /payments/*/refund                 → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
```

### Phase 3: Product 엔티티 변경

```java
// Product.java에 추가
@Column(name = "seller_id")
private String sellerId;  // 판매자 UUID (SELLER 또는 ADMIN이 등록)

public boolean isOwnedBy(String userId) {
    return this.sellerId != null && this.sellerId.equals(userId);
}
```

### Phase 3: Seller 전용 API

#### SellerProductController (신규)
```
GET /seller/products           → 내 상품 목록
GET /seller/products/{id}      → 내 상품 상세
POST /seller/products          → 상품 등록 (sellerId 자동 설정)
PUT /seller/products/{id}      → 내 상품 수정 (소유권 검증)
DELETE /seller/products/{id}   → 내 상품 삭제 (소유권 검증)
```

#### SellerDashboardController (신규)
```
GET /seller/dashboard          → 판매 현황 요약
GET /seller/orders             → 내 상품 주문 목록
GET /seller/analytics          → 판매 통계
```

### Phase 3: 소유권 검증 로직

```java
// ProductService에 추가
public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, String userId, List<String> roles) {
    Product product = productRepository.findById(productId)
        .orElseThrow(...);

    // SHOPPING_ADMIN/SUPER_ADMIN → 무조건 허용
    if (roles.contains("ROLE_SHOPPING_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")) {
        return updateAndReturn(product, request);
    }

    // SELLER → 본인 상품만
    if (!product.isOwnedBy(userId)) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_OWNED);
    }
    return updateAndReturn(product, request);
}
```

### Phase 3: Membership 기반 기능 분기

```java
// 타임딜 조기 접근 예시
@GetMapping("/time-deals/{id}/early-access")
public ApiResponse<TimeDealResponse> getEarlyAccess(
        @PathVariable Long id,
        @AuthenticationPrincipal String userId) {
    // MembershipContext에서 shopping 티어 확인
    String tier = MembershipContext.getTier("shopping");
    if (!"PREMIUM".equals(tier) && !"VIP".equals(tier)) {
        throw new CustomBusinessException(ShoppingErrorCode.MEMBERSHIP_REQUIRED);
    }
    return ApiResponse.success(timeDealService.getEarlyAccess(id, userId));
}
```

### Phase 5: @PreAuthorize 일관성 확보

모든 Admin 컨트롤러를 `SHOPPING_ADMIN`/`SUPER_ADMIN` 기반으로 전환:
```java
@PreAuthorize("hasAnyRole('SHOPPING_ADMIN', 'SUPER_ADMIN')")
```

Seller 컨트롤러:
```java
@PreAuthorize("hasAnyRole('SELLER', 'SHOPPING_ADMIN', 'SUPER_ADMIN')")
```

### Phase 5: Kafka Consumer (권한 변경 수신)

```java
@KafkaListener(topics = "auth.role.changed")
public void handleRoleChanged(RoleChangedEvent event) {
    // Redis Permission 캐시 무효화
    permissionCacheService.invalidate(event.userId());
}

@KafkaListener(topics = "auth.membership.changed")
public void handleMembershipChanged(MembershipChangedEvent event) {
    if ("shopping".equals(event.serviceName())) {
        permissionCacheService.invalidate(event.userId());
        // 추가: 멤버십별 특수 로직 (예: VIP 전용 쿠폰 발급)
    }
}
```

---

## 영향받는 파일

### 신규 생성
```
src/main/java/.../controller/SellerProductController.java
src/main/java/.../controller/SellerDashboardController.java
src/main/java/.../service/SellerProductService.java
src/main/java/.../service/impl/SellerProductServiceImpl.java
src/main/java/.../consumer/RoleChangeConsumer.java
src/main/java/.../consumer/MembershipChangeConsumer.java
```

### 수정 필요
```
src/main/java/.../common/config/SecurityConfig.java
  - 경로별 접근 제어 전면 개편

src/main/java/.../product/entity/Product.java
  - sellerId 필드 추가

src/main/java/.../product/service/impl/ProductServiceImpl.java
  - 소유권 검증 로직 추가

src/main/java/.../controller/AdminProductController.java
  - @PreAuthorize → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")

src/main/java/.../controller/AdminOrderController.java
  - @PreAuthorize → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")

src/main/java/.../controller/AdminTimeDealController.java
  - @PreAuthorize → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")

src/main/java/.../controller/AdminCouponController.java
  - @PreAuthorize → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")

src/main/java/.../common/exception/ShoppingErrorCode.java
  - PRODUCT_NOT_OWNED, MEMBERSHIP_REQUIRED 등 추가

src/main/resources/db/migration/
  - V__add_seller_id_to_products.sql
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] SELLER: 본인 상품 수정 성공
- [ ] SELLER: 타인 상품 수정 → PRODUCT_NOT_OWNED
- [ ] SHOPPING_ADMIN: 모든 상품 수정 성공
- [ ] USER: 상품 등록 시도 → 403
- [ ] Membership PREMIUM: 타임딜 조기 접근 성공
- [ ] Membership FREE: 타임딜 조기 접근 → MEMBERSHIP_REQUIRED

### 통합 테스트
- [ ] Seller 전용 API 전체 흐름 (상품 CRUD)
- [ ] Admin API: SHOPPING_ADMIN 접근 성공, USER 접근 거부
- [ ] 권한 변경 Kafka 이벤트 수신 → 캐시 무효화

### 하위 호환 테스트
- [ ] 기존 Admin 기능 정상 동작 (SUPER_ADMIN으로 마이그레이션 후)
- [ ] 기존 User 기능 정상 동작 (장바구니, 주문, 결제)
