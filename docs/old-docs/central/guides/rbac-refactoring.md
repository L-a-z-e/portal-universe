---
id: rbac-refactoring
title: RBAC 리팩토링 구현 가이드 (전체 서비스)
type: guide
status: current
created: 2026-01-28
updated: 2026-01-30
author: Portal Universe Team
tags: [rbac, auth, security, refactoring, guide]
---

# RBAC 리팩토링 구현 가이드

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../adr/ADR-011-hierarchical-rbac-membership-system.md)
- [ADR-003: Admin 권한 검증 전략](../adr/ADR-003-authorization-strategy.md)
- [ADR-004: JWT RBAC 자동 설정 전략](../adr/ADR-004-jwt-rbac-auto-configuration.md)

## 목차

- [개요](#개요)
- [Auth Service](#auth-service)
- [Common Library](#common-library)
- [API Gateway](#api-gateway)
- [Blog Service](#blog-service)
- [Shopping Service](#shopping-service)
- [Portal Shell (Host)](#portal-shell-host)
- [Blog Frontend (Vue)](#blog-frontend-vue)
- [Shopping Frontend (React)](#shopping-frontend-react)

---

## 개요

이 문서는 Portal Universe 전체 서비스에 걸친 RBAC(Role-Based Access Control) 리팩토링 계획을 통합 정리합니다. 기존 단일 Role enum 기반에서 DB 테이블 기반 동적 Role, Permission, Membership 시스템으로 전환합니다.

### Phase 구성

| Phase | 내용 | 서비스 |
|-------|------|--------|
| Phase 1 | DB 스키마 & 엔티티 모델 | Auth Service |
| Phase 2 | JWT Claims 확장, Filter 변경 | Auth, Common Library, API Gateway |
| Phase 3 | 관리 API, SecurityConfig 개편 | Auth, Blog, Shopping, Common Library |
| Phase 4 | 프론트엔드 적용 | Portal Shell, Blog FE, Shopping FE |
| Phase 5 | 일관성 확보, Kafka 이벤트 | 전체 서비스 |

---

## Auth Service

### 현재 상태
- `Role.java` enum: `USER("ROLE_USER")`, `ADMIN("ROLE_ADMIN")` 2종만 존재
- `User.java` 엔티티에 `private Role role` 단일 필드
- `TokenService.java`에서 `claims.put("roles", user.getRole().getKey())` → 단일 문자열
- JWT Access Token: 15분, Refresh Token: 7일 (Redis 저장)
- OAuth2: Google, Naver, Kakao 지원
- 로그인 실패 추적 및 계정 잠금 (3/5/10회)
- Token Blacklist (Redis)

### 변경 목표
- Role을 enum에서 DB 테이블 기반 동적 관리로 전환
- 복수 Role 지원 (user_roles 조인 테이블)
- Permission 모델 도입 (role_permissions 매핑)
- Membership 도메인 신규 구축 (서비스별 티어)
- JWT claims 확장: roles 배열 + memberships Map
- Seller 승인 워크플로우 구현
- Role/Permission/Membership 관리 API 제공
- Kafka 이벤트 발행 (권한 변경 전파)
- Audit 로깅 (auth_audit_log)

### Phase 1: DB 스키마 & 엔티티 모델

#### 신규 테이블 (Flyway 마이그레이션)
```
roles (id, role_key, display_name, description, service_scope, parent_role_id, is_system, is_active)
permissions (id, permission_key, service, resource, action, description, is_active)
user_roles (id, user_id, role_id, assigned_by, assigned_at, expires_at)
role_permissions (id, role_id, permission_id)
membership_tiers (id, service_name, tier_key, display_name, price_monthly, price_yearly, sort_order)
membership_tier_permissions (id, tier_id, permission_id)
user_memberships (id, user_id, service_name, tier_id, status, started_at, expires_at, auto_renew)
auth_audit_log (id, event_type, target_user_id, actor_user_id, details, ip_address, created_at)
```

#### 초기 데이터 마이그레이션
- 기존 `users.role = 'USER'` → `user_roles`에 `ROLE_USER` 삽입
- 기존 `users.role = 'ADMIN'` → `user_roles`에 `ROLE_USER` + `ROLE_SUPER_ADMIN` 삽입
- 모든 사용자에게 blog:FREE, shopping:FREE 멤버십 자동 생성
- 기존 `User.role` 필드는 deprecated 유지 (Phase 5에서 제거)

#### 신규 엔티티
- `Role.java` (entity, enum → JPA Entity 전환)
- `Permission.java` (entity)
- `UserRole.java` (entity, 조인 테이블)
- `RolePermission.java` (entity)
- `MembershipTier.java` (entity)
- `MembershipTierPermission.java` (entity)
- `UserMembership.java` (entity)
- `AuthAuditLog.java` (entity)

### Phase 2: TokenService 변경

#### JWT Claims 확장
```
Before: { "sub": "uuid", "roles": "ROLE_USER", ... }
After:  { "sub": "uuid", "roles": ["ROLE_USER", "ROLE_SELLER"], "memberships": {"shopping": "PREMIUM"}, ... }
```

- `TokenService.generateAccessToken()`: user_roles 조회 → roles 배열 생성, user_memberships 조회 → memberships Map 생성
- `TokenService.validateAccessToken()`: claims.get("roles") 파싱 로직 변경 (String/List 양쪽 지원)
- `JwtAuthenticationFilter`: 복수 Authority 생성 (`List.of()` → `stream().map()`)

### Phase 3: Role/Permission/Membership 관리 API

#### Role 관리 (SUPER_ADMIN 전용)
- `GET /api/roles` - 전체 Role 목록
- `POST /api/roles` - Role 생성
- `PUT /api/roles/{id}` - Role 수정
- `DELETE /api/roles/{id}` - Role 비활성화 (시스템 Role 삭제 불가)
- `PUT /api/roles/{id}/permissions` - Permission 매핑 수정
- `POST /api/users/{userId}/roles` - 사용자에게 Role 부여
- `DELETE /api/users/{userId}/roles/{roleId}` - 사용자 Role 회수

#### Permission 관리 (SUPER_ADMIN 전용)
- `GET /api/permissions` - 전체 Permission 목록
- `GET /api/permissions?service=shopping` - 서비스별 필터

#### Membership 관리
- `GET /api/memberships/tiers` - 멤버십 티어 목록 (공개)
- `GET /api/memberships/tiers?service=shopping` - 서비스별 필터
- `GET /api/users/me/memberships` - 내 멤버십 조회
- `POST /api/users/me/memberships` - 멤버십 가입/변경
- `DELETE /api/users/me/memberships/{service}` - 멤버십 해지

#### Seller 승인 (관리자 수동 승인)
- `POST /api/seller/apply` - Seller 신청 (사용자)
- `GET /api/admin/seller-applications` - 신청 목록 (SHOPPING_ADMIN/SUPER_ADMIN)
- `PUT /api/admin/seller-applications/{id}/approve` - 승인
- `PUT /api/admin/seller-applications/{id}/reject` - 거부

### Phase 5: Kafka 이벤트 발행

#### 신규 토픽 & 이벤트
- `auth.role.changed` → RoleChangedEvent (userId, action, roleKey, currentRoles, changedBy)
- `auth.permission-mapping.changed` → PermissionMappingChangedEvent (roleKey, action, permissionKey)
- `auth.membership.changed` → MembershipChangedEvent (userId, serviceName, previousTier, newTier, action)
- `auth.token.revoked` → TokenRevokedEvent (userId, reason, revokedBy)

#### 기존 토픽 확장
- `user-signup` → UserSignedUpEvent에 `roles`, `defaultMemberships` 필드 추가

### 영향받는 파일

<details>
<summary>신규 생성</summary>

```
src/main/java/.../auth/domain/
├── RoleEntity.java
├── PermissionEntity.java
├── UserRoleEntity.java
├── RolePermissionEntity.java
├── MembershipTier.java
├── MembershipTierPermission.java
├── UserMembership.java
├── MembershipStatus.java (enum)
├── AuthAuditLog.java
└── AuditEventType.java (enum)

src/main/java/.../auth/repository/
├── RoleEntityRepository.java
├── PermissionRepository.java
├── UserRoleRepository.java
├── MembershipTierRepository.java
├── UserMembershipRepository.java
└── AuthAuditLogRepository.java

src/main/java/.../auth/service/
├── RoleManagementService.java
├── PermissionService.java
├── MembershipService.java
├── SellerApprovalService.java
└── AuditService.java

src/main/java/.../auth/controller/
├── RoleManagementController.java
├── PermissionController.java
├── MembershipController.java
└── SellerApprovalController.java

src/main/java/.../auth/dto/
├── RoleRequest.java, RoleResponse.java
├── PermissionResponse.java
├── MembershipRequest.java, MembershipResponse.java
├── SellerApplicationRequest.java, SellerApplicationResponse.java
└── UserRoleAssignRequest.java

src/main/java/.../auth/event/
├── RoleChangedEvent.java
├── PermissionMappingChangedEvent.java
├── MembershipChangedEvent.java
└── TokenRevokedEvent.java

src/main/resources/db/migration/
├── V20260128_001__create_rbac_tables.sql
├── V20260128_002__seed_roles_permissions.sql
└── V20260128_003__migrate_existing_users.sql
```
</details>

<details>
<summary>수정 필요</summary>

```
src/main/java/.../user/domain/User.java           - roles 관계 추가, role 필드 deprecated
src/main/java/.../user/domain/Role.java            - enum → deprecated (신규 RoleEntity 사용)
src/main/java/.../auth/service/TokenService.java   - JWT claims 확장 (roles 배열, memberships)
src/main/java/.../auth/security/JwtAuthenticationFilter.java - 복수 Authority 파싱
src/main/java/.../common/config/SecurityConfig.java - 경로별 접근제어 확장
src/main/java/.../user/service/UserService.java    - 회원가입 시 기본 Role/Membership 할당
src/main/java/.../common/exception/AuthErrorCode.java - RBAC 관련 에러코드 추가
```
</details>

### 테스트 체크리스트

- [ ] RoleManagementService: CRUD, Permission 매핑
- [ ] MembershipService: 가입/변경/해지
- [ ] SellerApprovalService: 신청/승인/거부 흐름
- [ ] TokenService: JWT v2 생성 (roles 배열, memberships)
- [ ] TokenService: JWT v1 하위 호환 파싱
- [ ] 회원가입 → ROLE_USER + FREE 멤버십 자동 할당
- [ ] Seller 신청 → 승인 → ROLE_SELLER 부여 → JWT 재발급
- [ ] Role 부여 → Kafka RoleChangedEvent 발행 확인
- [ ] SUPER_ADMIN만 Role/Permission 관리 API 접근 가능
- [ ] 시스템 Role (is_system=true) 삭제 시도 → 거부

---

## Common Library

### 현재 상태
- `GatewayAuthenticationFilter`: X-User-Id, X-User-Roles 헤더 → SecurityContext 설정
- `List.of(new SimpleGrantedAuthority(roles))` → 단일 Authority
- `JwtAuthenticationConverterAdapter` / `ReactiveJwtAuthenticationConverterAdapter`
- 공통 ErrorCode: `UNAUTHORIZED (C005)`, `FORBIDDEN (C004)`
- Kafka 이벤트 DTO: `UserSignedUpEvent`

### 변경 목표
- `GatewayAuthenticationFilter` 확장: 복수 Authority + Membership 컨텍스트
- `PermissionResolver` 신규 구현: Role+Membership → Permission Set (Redis 캐시)
- 공통 Permission 관련 인터페이스/어노테이션
- Kafka 이벤트 DTO 추가
- 공통 ErrorCode 확장

### Phase 2: GatewayAuthenticationFilter 확장

#### 복수 Authority 지원
```
Before: X-User-Roles: ROLE_USER
        → List.of(new SimpleGrantedAuthority(roles))

After:  X-User-Roles: ROLE_USER,ROLE_SELLER
        → Arrays.stream(roles.split(","))
              .map(String::trim)
              .map(SimpleGrantedAuthority::new)
              .toList()
```

#### Membership 컨텍스트 설정
```
X-User-Memberships: {"shopping":"PREMIUM","blog":"FREE"}
→ MembershipContext.set(parsedMap)
→ ThreadLocal 또는 RequestAttribute로 전달
```

### Phase 3: PermissionResolver 구현

#### 핵심 인터페이스
```java
public interface PermissionResolver {
    Set<String> resolvePermissions(List<String> roles, Map<String, String> memberships);
    boolean hasPermission(String permissionKey);
    boolean hasAnyPermission(String... permissionKeys);
}
```

#### Redis 캐시 전략
```
Key: perm:{sha256(roles_sorted)}:{sha256(memberships_sorted)}
Value: Set<String> (Permission keys)
TTL: 300초 (5분)

Cache Miss → DB 조회 → 캐시 저장
Redis 장애 → DB Fallback + Circuit Breaker
```

#### SUPER_ADMIN 바이패스
```
if roles.contains("ROLE_SUPER_ADMIN"):
    return WILDCARD → 모든 Permission 체크 true
```

### Phase 3: Kafka 이벤트 DTO 추가

#### 신규 이벤트
```java
public record RoleChangedEvent(
    String eventId, String userId, String action, String roleKey,
    List<String> currentRoles, String changedBy, Instant timestamp
) {}

public record PermissionMappingChangedEvent(
    String eventId, String roleKey, String action, String permissionKey,
    String changedBy, Instant timestamp
) {}

public record MembershipChangedEvent(
    String eventId, String userId, String serviceName,
    String previousTier, String newTier, String action,
    Instant expiresAt, Instant timestamp
) {}

public record TokenRevokedEvent(
    String eventId, String userId, String reason,
    String revokedBy, Instant timestamp
) {}
```

#### 기존 이벤트 확장
```java
// UserSignedUpEvent에 추가
List<String> roles,
Map<String, String> defaultMemberships // {"blog":"FREE","shopping":"FREE"}
```

### Phase 3: 공통 Permission 어노테이션 (선택)

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();               // e.g., "shopping:product:create"
    LogicType logic() default LogicType.ANY;
}
```

### Phase 3: ErrorCode 확장

```java
ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "C010", "Role not found"),
PERMISSION_DENIED(HttpStatus.FORBIDDEN, "C011", "Permission denied"),
MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "C012", "Membership not found"),
MEMBERSHIP_EXPIRED(HttpStatus.FORBIDDEN, "C013", "Membership expired"),
SELLER_APPLICATION_PENDING(HttpStatus.CONFLICT, "C014", "Seller application already pending"),
SELLER_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "C015", "Seller application not found"),
```

### 영향받는 파일

<details>
<summary>신규 생성</summary>

```
src/main/java/.../security/filter/EnhancedGatewayAuthenticationFilter.java
src/main/java/.../security/permission/PermissionResolver.java
src/main/java/.../security/permission/RedisPermissionResolver.java
src/main/java/.../security/permission/MembershipContext.java
src/main/java/.../security/annotation/RequirePermission.java (선택)
src/main/java/.../security/annotation/RequirePermissionAspect.java (선택)
src/main/java/.../event/RoleChangedEvent.java
src/main/java/.../event/PermissionMappingChangedEvent.java
src/main/java/.../event/MembershipChangedEvent.java
src/main/java/.../event/TokenRevokedEvent.java
```
</details>

<details>
<summary>수정 필요</summary>

```
src/main/java/.../security/filter/GatewayAuthenticationFilter.java - 복수 Authority, Memberships 파싱
src/main/java/.../exception/CommonErrorCode.java - RBAC 관련 에러코드 추가
src/main/java/.../event/UserSignedUpEvent.java - roles, defaultMemberships 추가
```
</details>

### 테스트 체크리스트

- [ ] GatewayAuthenticationFilter: 복수 roles 파싱 (콤마 구분)
- [ ] GatewayAuthenticationFilter: memberships JSON 파싱
- [ ] GatewayAuthenticationFilter: 빈 roles/memberships 처리
- [ ] PermissionResolver: Role → Permission 매핑
- [ ] PermissionResolver: Role + Membership → 합산 Permission
- [ ] PermissionResolver: SUPER_ADMIN 와일드카드
- [ ] PermissionResolver: Redis 캐시 히트/미스
- [ ] PermissionResolver: Redis 장애 시 DB Fallback
- [ ] Kafka 이벤트 DTO 직렬화/역직렬화

---

## API Gateway

### 현재 상태
- WebFlux 기반 `JwtAuthenticationFilter`에서 JWT 검증
- `claims.get("roles", String.class)` → 단일 문자열 파싱
- `List.of(new SimpleGrantedAuthority(roles))` → 단일 Authority
- `X-User-Id`, `X-User-Roles` 헤더로 하위 서비스 전달
- SecurityConfig: `/api/shopping/admin/**` → `hasRole("ADMIN")`
- Rate Limiting: IP/User/Composite 기반 (Redis)

### 변경 목표
- JWT v1/v2 dual format 지원 (roles: String → String[])
- `X-User-Memberships` 헤더 추가
- SecurityConfig 경로 확장 (SELLER, SERVICE_ADMIN)
- 복수 Authority 생성

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
/api/blog/admin/**      → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")
/api/shopping/seller/** → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")
/api/admin/**           → hasRole("SUPER_ADMIN")

// 나머지
anyExchange → authenticated
```

### Phase 5: Rate Limiter 확장 (선택)

Seller 전용 Rate Limiter 추가 고려:
```
sellerRedisRateLimiter: 3/sec, burst 150 (상품 등록 등 빈번한 작업)
```

### 영향받는 파일

```
수정 필요:
  src/main/java/.../filter/JwtAuthenticationFilter.java - roles/memberships 파싱
  src/main/java/.../config/SecurityConfig.java - 경로별 접근 제어 확장
  src/main/resources/application.yml - 신규 라우팅 규칙

변경 없음:
  config/RateLimiterConfig.java, SecurityHeadersFilter.java,
  GlobalForwardedHeadersFilter.java, GlobalLoggingFilter.java
```

### 테스트 체크리스트

- [ ] JWT v1 (roles: String) 파싱 → 단일 Authority 생성
- [ ] JWT v2 (roles: String[]) 파싱 → 복수 Authority 생성
- [ ] memberships claim 파싱 → X-User-Memberships 헤더 생성
- [ ] memberships 없는 v1 토큰 → X-User-Memberships: {} 전달
- [ ] SUPER_ADMIN → /api/admin/** 접근 성공
- [ ] SHOPPING_ADMIN → /api/shopping/admin/** 접근 성공
- [ ] SELLER → /api/shopping/seller/** 접근 성공
- [ ] USER → /api/shopping/seller/** 접근 거부 (403)
- [ ] BLOG_ADMIN → /api/blog/admin/** 접근 성공
- [ ] 기존 JWT v1 토큰으로 정상 인증

---

## Blog Service

### 현재 상태
- SecurityConfig: GET 공개, POST/PUT/DELETE authenticated, 파일 삭제만 `hasRole("ADMIN")`
- @PreAuthorize: FileController에만 사용
- 소유권 검증: PostServiceImpl, CommentService, SeriesService에서 authorId 비교
- Admin 전용 API: `/posts/all` 존재하나 권한 검증 부재
- MongoDB 사용 (Post, Comment, Series 문서)

### 변경 목표
- BLOG_ADMIN 역할 도입: 모든 게시물/댓글/시리즈 관리
- SUPER_ADMIN: 전체 관리 권한
- Admin 전용 API 경로 정리: `/admin/**`
- @PreAuthorize 일관성 확보
- Membership 기반 기능 분기 (PREMIUM → 커스텀 테마, 통계 대시보드 등)

### Phase 3: SecurityConfig 변경

```
// 공개 (변경 없음)
GET /posts, /posts/**, /tags, /categories       → permitAll

// 인증 필요 (변경 없음)
POST/PUT/DELETE /posts/**, /comments/**         → authenticated
POST /file/upload, /series/**                    → authenticated

// BLOG_ADMIN (신규)
/admin/**                                        → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")
DELETE /file/delete                              → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")
```

### Phase 3: Admin 전용 API 정리

#### AdminPostController (신규)
```
GET /admin/posts              → 전체 게시물 목록 (필터/검색)
GET /admin/posts/{id}         → 게시물 상세 (비공개 포함)
DELETE /admin/posts/{id}      → 게시물 강제 삭제
PUT /admin/posts/{id}/status  → 게시물 상태 변경
```

#### AdminCommentController (신규)
```
GET /admin/comments           → 전체 댓글 목록
DELETE /admin/comments/{id}   → 댓글 강제 삭제
```

#### AdminDashboardController (신규)
```
GET /admin/dashboard          → 블로그 통계 요약
```

### Phase 3: 소유권 검증 + ADMIN 바이패스

```java
// PostServiceImpl 등에서
if (roles.contains("ROLE_BLOG_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")) {
    return updateAndReturn(post, request);  // 무조건 허용
}
if (!post.getAuthorId().equals(userId)) {
    throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
}
```

동일 패턴을 CommentService, SeriesService에도 적용.

### Phase 3: Membership 기반 기능 분기

멤버십별 차별화 기능:
- FREE: 기본 게시물 작성, 10개 이미지 업로드
- BASIC: 시리즈 무제한, 50개 이미지 업로드
- PREMIUM: 커스텀 테마, 통계 대시보드, 무제한 이미지
- VIP: 추천 블로거 배지, 우선 검색 노출

### Phase 5: Kafka Consumer (권한 변경 수신)

```java
@KafkaListener(topics = "auth.role.changed")
public void handleRoleChanged(RoleChangedEvent event) { /* 캐시 무효화 */ }

@KafkaListener(topics = "auth.membership.changed")
public void handleMembershipChanged(MembershipChangedEvent event) {
    if ("blog".equals(event.serviceName())) { /* 멤버십 캐시 갱신 */ }
}
```

### 영향받는 파일

<details>
<summary>신규 생성</summary>

```
src/main/java/.../controller/AdminPostController.java
src/main/java/.../controller/AdminCommentController.java
src/main/java/.../controller/AdminDashboardController.java
src/main/java/.../service/AdminPostService.java
src/main/java/.../service/AdminCommentService.java
src/main/java/.../service/BlogAnalyticsService.java
src/main/java/.../consumer/RoleChangeConsumer.java
src/main/java/.../consumer/MembershipChangeConsumer.java
```
</details>

<details>
<summary>수정 필요</summary>

```
SecurityConfig.java - 경로별 접근 제어 개편
PostServiceImpl.java - ADMIN 바이패스 로직
CommentService.java - ADMIN 바이패스 로직
SeriesService.java - ADMIN 바이패스 로직
FileController.java - @PreAuthorize 변경
PostController.java - /posts/all → /admin/posts 이동
BlogErrorCode.java - MEMBERSHIP_REQUIRED 등 추가
```
</details>

### 테스트 체크리스트

- [ ] BLOG_ADMIN: 타인 게시물 수정/삭제 성공
- [ ] USER: 본인만 수정, 타인 → 403
- [ ] SUPER_ADMIN: 모든 게시물 관리
- [ ] Membership PREMIUM: 통계 대시보드 접근
- [ ] Membership FREE: 통계 대시보드 → MEMBERSHIP_REQUIRED
- [ ] Admin API 전체 흐름, USER 접근 거부
- [ ] 기존 게시물/댓글/시리즈 CRUD 하위 호환

---

## Shopping Service

### 현재 상태
- SecurityConfig: ADMIN만 상품 생성/수정/삭제, 재고/배송 관리
- AdminProductController 등: `@PreAuthorize("hasRole('ADMIN')")` 클래스 레벨
- Seller/Buyer 구분 없음, Product에 sellerId 없음
- Feign Client로 auth-service/blog-service 통신

### 변경 목표
- SELLER 역할 도입: 상품 등록/수정/삭제 (본인 상품만)
- SHOPPING_ADMIN: 모든 상품/주문/배송/재고 관리
- Product에 sellerId 추가: 판매자 소유권 검증
- Seller 전용 API 경로: `/seller/**`
- 멤버십 기반 기능 차별화

### Phase 3: SecurityConfig 변경

```
// SELLER + SHOPPING_ADMIN (신규)
POST/PUT/DELETE /products/**    → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")
/seller/**                      → hasAnyRole("SELLER", "SHOPPING_ADMIN", "SUPER_ADMIN")

// SHOPPING_ADMIN (기존 ADMIN → 변경)
/admin/**, /inventory/**, /deliveries/** → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
POST /payments/*/refund                  → hasAnyRole("SHOPPING_ADMIN", "SUPER_ADMIN")
```

### Phase 3: Product 엔티티 변경

```java
@Column(name = "seller_id")
private String sellerId;  // 판매자 UUID

public boolean isOwnedBy(String userId) {
    return this.sellerId != null && this.sellerId.equals(userId);
}
```

### Phase 3: Seller 전용 API

#### SellerProductController (신규)
```
GET /seller/products           → 내 상품 목록
POST /seller/products          → 상품 등록 (sellerId 자동 설정)
PUT /seller/products/{id}      → 내 상품 수정 (소유권 검증)
DELETE /seller/products/{id}   → 내 상품 삭제
```

#### SellerDashboardController (신규)
```
GET /seller/dashboard          → 판매 현황 요약
GET /seller/orders             → 내 상품 주문 목록
GET /seller/analytics          → 판매 통계
```

### Phase 3: Membership 기반 기능 분기

타임딜 조기 접근 예시 (PREMIUM/VIP만):
```java
String tier = MembershipContext.getTier("shopping");
if (!"PREMIUM".equals(tier) && !"VIP".equals(tier)) {
    throw new CustomBusinessException(ShoppingErrorCode.MEMBERSHIP_REQUIRED);
}
```

### Phase 5: Kafka Consumer

```java
@KafkaListener(topics = "auth.role.changed")
public void handleRoleChanged(RoleChangedEvent event) { /* Permission 캐시 무효화 */ }

@KafkaListener(topics = "auth.membership.changed")
public void handleMembershipChanged(MembershipChangedEvent event) {
    if ("shopping".equals(event.serviceName())) { /* VIP 전용 쿠폰 발급 등 */ }
}
```

### 영향받는 파일

<details>
<summary>신규 생성</summary>

```
src/main/java/.../controller/SellerProductController.java
src/main/java/.../controller/SellerDashboardController.java
src/main/java/.../service/SellerProductService.java
src/main/java/.../consumer/RoleChangeConsumer.java
src/main/java/.../consumer/MembershipChangeConsumer.java
```
</details>

<details>
<summary>수정 필요</summary>

```
SecurityConfig.java - 경로별 접근 제어 전면 개편
Product.java - sellerId 추가
ProductServiceImpl.java - 소유권 검증
AdminProductController.java - @PreAuthorize 변경
AdminOrderController.java - @PreAuthorize 변경
AdminTimeDealController.java - @PreAuthorize 변경
AdminCouponController.java - @PreAuthorize 변경
ShoppingErrorCode.java - PRODUCT_NOT_OWNED, MEMBERSHIP_REQUIRED 추가
db/migration/ - V__add_seller_id_to_products.sql
```
</details>

### 테스트 체크리스트

- [ ] SELLER: 본인 상품 수정 성공, 타인 상품 → PRODUCT_NOT_OWNED
- [ ] SHOPPING_ADMIN: 모든 상품 수정 성공
- [ ] USER: 상품 등록 → 403
- [ ] Membership PREMIUM: 타임딜 조기 접근
- [ ] Seller 전용 API 전체 흐름
- [ ] 기존 Admin/User 기능 하위 호환

---

## Portal Shell (Host)

### 현재 상태
- Pinia authStore: `user.authority.roles` 배열로 역할 저장
- `hasRole(role)`: 단일 역할 확인 메서드
- `isAdmin`: `hasRole('ROLE_ADMIN')` computed
- authService: JWT 파싱 시 `payload.roles` → String/Array 양쪽 지원
- storeAdapter: Module Federation으로 authStore를 Remote 앱에 노출
- router: `meta.requiresAuth` 정의되어 있으나 navigation guard 미구현

### 변경 목표
- 복수 Role 지원 강화 (SELLER, BLOG_ADMIN, SHOPPING_ADMIN 등)
- Membership 상태 저장 및 노출
- Navigation Guard 구현 (역할 기반 라우트 보호)
- storeAdapter 확장: memberships, hasAnyRole 노출
- 역할 기반 UI 분기 (메뉴, 네비게이션)

### Phase 4: Auth Store 확장

```typescript
// 신규 메서드
const hasAnyRole = (...roles: string[]): boolean => {
  return roles.some(role => hasRole(role))
}

const getMembershipTier = (service: string): string => {
  return user.value?.authority.memberships?.[service] || 'FREE'
}

const hasMembershipTier = (service: string, minTier: string): boolean => {
  const tierOrder = ['FREE', 'BASIC', 'PREMIUM', 'VIP']
  return tierOrder.indexOf(getMembershipTier(service)) >= tierOrder.indexOf(minTier)
}

// computed
const isAdmin = computed(() => hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN'))
const isBlogAdmin = computed(() => hasAnyRole('ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN'))
const isShoppingAdmin = computed(() => hasAnyRole('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN'))
const isSeller = computed(() => hasAnyRole('ROLE_SELLER', 'ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN'))
```

### Phase 4: authService JWT 파싱 확장

```typescript
// JWT payload에서 memberships 추출 추가
memberships: payload.memberships || {}
```

### Phase 4: Navigation Guard 구현

```typescript
router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  if (to.meta.requiredRoles) {
    const roles = to.meta.requiredRoles as string[]
    if (!authStore.hasAnyRole(...roles)) {
      return next({ name: 'Forbidden' })
    }
  }
  next()
})
```

### Phase 4: Store Adapter 확장

```typescript
// Remote 앱에 노출할 API 확장
authAdapter = {
  getState: () => ({ ...기존..., isBlogAdmin, isShoppingAdmin, isSeller, memberships }),
  hasRole, hasAnyRole, getMembershipTier
}
```

### 영향받는 파일

```
수정: types/auth.ts, store/auth.ts, services/authService.ts,
      store/storeAdapter.ts, router/index.ts, AppNavigation.vue
신규: views/ForbiddenPage.vue, composables/usePermission.ts (선택)
```

### 테스트 체크리스트

- [ ] hasAnyRole: 복수 역할 중 하나라도 매칭
- [ ] getMembershipTier/hasMembershipTier 정상
- [ ] JWT v1/v2 파싱 호환
- [ ] Navigation guard: 미인증 → 로그인, 역할 부족 → 403
- [ ] storeAdapter: Remote 앱에서 hasAnyRole/memberships 정상 호출
- [ ] 기존 isAdmin 동작 유지

---

## Blog Frontend (Vue)

### 현재 상태
- Portal Shell의 authStore를 Module Federation으로 import
- 자체 auth store 없음 (Host에 의존)
- router: `meta.requiresAuth` 정의만 존재, navigation guard 미구현
- embedded (Memory History) / standalone (Web History) 듀얼 모드

### 변경 목표
- Portal Shell의 확장된 authStore 활용
- Navigation Guard 구현 (역할 기반 라우트 보호)
- Blog Admin 라우트 추가
- Membership 기반 UI 분기
- Federation 타입 정의 업데이트

### Phase 4: Federation 타입 정의 확장

```typescript
declare module 'portal/stores' {
  export const useAuthStore: () => {
    // 기존 + 신규
    isBlogAdmin: ComputedRef<boolean>
    isShoppingAdmin: ComputedRef<boolean>
    isSeller: ComputedRef<boolean>
    hasAnyRole: (...roles: string[]) => boolean
    getMembershipTier: (service: string) => string
  }
}
```

### Phase 4: Navigation Guard 구현

```typescript
// embedded mode
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isAuthenticated.value) {
    window.parent?.postMessage({ type: 'REQUIRE_AUTH', path: to.fullPath }, '*')
    return next(false)
  }
  if (to.meta.requiredRoles) {
    if (!authStore.hasAnyRole(...(to.meta.requiredRoles as string[]))) {
      return next({ name: 'Forbidden' })
    }
  }
  next()
})
```

### Phase 4: 권한 체크 Composable

```typescript
// composables/usePermission.ts
export function usePermission() {
  // Portal authStore 래핑
  return { isAuthenticated, isBlogAdmin, isAdmin, hasRole, hasAnyRole,
           getMembershipTier, blogTier, isPremium }
}
```

### Phase 4: Admin 라우트 & Membership UI

- `/admin` 라우트: `requiredRoles: ['ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN']`
- PREMIUM 이상: 통계 대시보드, 커스텀 테마
- Admin: 타인 게시물 수정/삭제 가능, 상태 변경 버튼

### 영향받는 파일

```
수정: types/federation.d.ts, router/index.ts, PostDetailPage.vue, PostListPage.vue, MyPage.vue
신규: composables/usePermission.ts, views/admin/(Layout|Dashboard|Posts|Comments).vue,
      views/ForbiddenPage.vue, components/common/MembershipUpgradePrompt.vue
```

### 테스트 체크리스트

- [ ] usePermission: Portal authStore 연동 + Federation 실패 시 기본값
- [ ] Navigation guard: embedded/standalone 모드
- [ ] Admin 라우트: BLOG_ADMIN 접근, USER 차단
- [ ] Membership UI: PREMIUM → 통계, FREE → 업그레이드 유도
- [ ] 기존 기능 하위 호환

---

## Shopping Frontend (React)

### 현재 상태
- Zustand authStore: `User.role` 단일 문자열 ('guest' | 'user' | 'admin')
- RequireAuth/RequireRole 컴포넌트로 라우트 보호
- AdminWrapper: RequireAuth + RequireRole('admin')
- Portal Shell authAdapter를 통해 인증 상태 동기화 (embedded mode)
- Standalone 모드 지원

### 변경 목표
- User 타입: 단일 role → roles 배열 + memberships Map
- RequireRole 확장: 복수 역할 검증
- Seller 전용 라우트 및 UI 추가
- Membership 기반 UI 분기
- Portal Shell authAdapter 변경 반영

### Phase 4: User 타입 & AuthStore 확장

```typescript
interface User {
  id: string; email: string; name: string;
  roles: string[]                       // 변경: string → string[]
  memberships: Record<string, string>   // 신규
}

// 신규 메서드
hasRole, hasAnyRole, getMembershipTier, isSeller, isShoppingAdmin
```

### Phase 4: RequireRole 컴포넌트 수정

```tsx
interface RequireRoleProps {
  roles: string[]
  mode?: 'any' | 'all'  // 신규
  children: React.ReactNode
  redirectTo?: string
}
```

### Phase 4: RequireMembership 컴포넌트 (신규)

```tsx
<RequireMembership service="shopping" minTier="PREMIUM">
  <EarlyAccessTimeDealSection />
</RequireMembership>
```

### Phase 4: Seller 라우트

```tsx
// SellerWrapper: RequireAuth + RequireRole(['seller', 'shopping_admin', 'super_admin'])
// /seller: Dashboard, Products, Orders, Analytics
```

### Phase 4: AdminWrapper 수정

```tsx
// roles 변경: 'admin' → ['shopping_admin', 'super_admin']
```

### 영향받는 파일

```
수정: stores/authStore.ts, components/guards/RequireRole.tsx,
      router/index.tsx, types/federation.d.ts
신규: components/guards/RequireMembership.tsx,
      pages/seller/(Layout|Dashboard|Products|Orders|Analytics).tsx,
      pages/error/ForbiddenPage.tsx,
      components/common/MembershipUpgradePrompt.tsx
```

### 테스트 체크리스트

- [ ] hasRole/hasAnyRole: 정규화 및 복수 매칭
- [ ] getMembershipTier: 서비스별 티어, 미등록 시 FREE
- [ ] RequireRole: mode='any'/'all' 동작
- [ ] RequireMembership: 티어 순서 비교
- [ ] Portal Sync: embedded 모드 roles/memberships 동기화
- [ ] AdminWrapper/SellerWrapper 접근 제어
- [ ] 기존 기능 하위 호환
