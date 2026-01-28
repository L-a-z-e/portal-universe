# RBAC 리팩토링 구현 가이드 - API Gateway

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- WebFlux 기반 `JwtAuthenticationFilter`에서 JWT 검증
- `claims.get("roles", String.class)` → 단일 문자열 파싱
- `List.of(new SimpleGrantedAuthority(roles))` → 단일 Authority
- `X-User-Id`, `X-User-Roles` 헤더로 하위 서비스 전달
- SecurityConfig: `/api/shopping/admin/**` → `hasRole("ADMIN")`
- Rate Limiting: IP/User/Composite 기반 (Redis)

## 변경 목표
- JWT v1/v2 dual format 지원 (roles: String → String[])
- `X-User-Memberships` 헤더 추가
- SecurityConfig 경로 확장 (SELLER, SERVICE_ADMIN)
- 복수 Authority 생성

---

## 구현 단계

### Phase 2: JwtAuthenticationFilter 확장

#### roles 파싱 변경
```
Before: String roles = claims.get("roles", String.class);
        List.of(new SimpleGrantedAuthority(roles))

After:  Object rolesClaim = claims.get("roles");
        List<String> rolesList;
        if (rolesClaim instanceof String) {
            rolesList = List.of((String) rolesClaim);           // v1 하위 호환
        } else if (rolesClaim instanceof List) {
            rolesList = (List<String>) rolesClaim;              // v2 배열
        }
        List<SimpleGrantedAuthority> authorities = rolesList.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
```

#### memberships 파싱 (신규)
```
Object membershipsClaim = claims.get("memberships");
String membershipsJson = membershipsClaim != null
    ? objectMapper.writeValueAsString(membershipsClaim)
    : "{}";
```

#### 하위 서비스 헤더 전달 확장
```
Before: X-User-Id: <userId>
        X-User-Roles: <단일 role>

After:  X-User-Id: <userId>
        X-User-Roles: ROLE_USER,ROLE_SELLER          (콤마 구분)
        X-User-Memberships: {"shopping":"PREMIUM"}    (JSON)
```

### Phase 2: SecurityConfig 경로 확장

```
// 기존 (변경 없음)
/auth-service/**, /api/auth/**, /api/users/**          → permitAll
GET /api/blog/**                                        → permitAll
GET /api/shopping/products/**, /api/shopping/categories/** → permitAll

// 변경
/api/shopping/admin/**
    → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")    (기존: hasRole("ADMIN"))

// 신규
/api/blog/admin/**
    → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")

/api/shopping/seller/**
    → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")

/api/admin/**
    → hasRole("SUPER_ADMIN")

// 나머지
anyExchange → authenticated
```

### Phase 5: Rate Limiter 확장 (선택)

Seller 전용 Rate Limiter 추가 고려:
```
sellerRedisRateLimiter: 3/sec, burst 150 (상품 등록 등 빈번한 작업)
```

---

## 영향받는 파일

### 수정 필요
```
src/main/java/.../filter/JwtAuthenticationFilter.java
  - roles 파싱: String → List 지원
  - memberships 파싱 추가
  - X-User-Roles 헤더: 콤마 구분 전달
  - X-User-Memberships 헤더 추가

src/main/java/.../config/SecurityConfig.java
  - 경로별 접근 제어 확장
  - hasRole("ADMIN") → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
  - /api/blog/admin/**, /api/shopping/seller/**, /api/admin/** 추가

src/main/resources/application.yml
  - 신규 라우팅 규칙 추가 (seller 경로)
```

### 변경 없음
```
config/RateLimiterConfig.java          - Phase 5에서 선택적 확장
config/SecurityHeadersFilter.java      - 변경 불필요
config/GlobalForwardedHeadersFilter.java - 변경 불필요
config/GlobalLoggingFilter.java        - 변경 불필요
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] JWT v1 (roles: String) 파싱 → 단일 Authority 생성
- [ ] JWT v2 (roles: String[]) 파싱 → 복수 Authority 생성
- [ ] memberships claim 파싱 → X-User-Memberships 헤더 생성
- [ ] memberships 없는 v1 토큰 → X-User-Memberships: {} 전달

### 통합 테스트
- [ ] SUPER_ADMIN → /api/admin/** 접근 성공
- [ ] SHOPPING_ADMIN → /api/shopping/admin/** 접근 성공
- [ ] SELLER → /api/shopping/seller/** 접근 성공
- [ ] USER → /api/shopping/seller/** 접근 거부 (403)
- [ ] BLOG_ADMIN → /api/blog/admin/** 접근 성공
- [ ] USER → /api/blog/admin/** 접근 거부 (403)

### 하위 호환 테스트
- [ ] 기존 JWT v1 토큰으로 정상 인증
- [ ] 기존 ADMIN 토큰 → /api/shopping/admin/** 접근 (SUPER_ADMIN으로 마이그레이션 후)
