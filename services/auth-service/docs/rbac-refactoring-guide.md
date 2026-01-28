# RBAC 리팩토링 구현 가이드 - Auth Service

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)
- [ADR-003: Admin 권한 검증 전략](../../../docs/adr/ADR-003-authorization-strategy.md)
- [ADR-004: JWT RBAC 자동 설정 전략](../../../docs/adr/ADR-004-jwt-rbac-auto-configuration.md)

## 현재 상태
- `Role.java` enum: `USER("ROLE_USER")`, `ADMIN("ROLE_ADMIN")` 2종만 존재
- `User.java` 엔티티에 `private Role role` 단일 필드
- `TokenService.java`에서 `claims.put("roles", user.getRole().getKey())` → 단일 문자열
- JWT Access Token: 15분, Refresh Token: 7일 (Redis 저장)
- OAuth2: Google, Naver, Kakao 지원
- 로그인 실패 추적 및 계정 잠금 (3/5/10회)
- Token Blacklist (Redis)

## 변경 목표
- Role을 enum에서 DB 테이블 기반 동적 관리로 전환
- 복수 Role 지원 (user_roles 조인 테이블)
- Permission 모델 도입 (role_permissions 매핑)
- Membership 도메인 신규 구축 (서비스별 티어)
- JWT claims 확장: roles 배열 + memberships Map
- Seller 승인 워크플로우 구현
- Role/Permission/Membership 관리 API 제공
- Kafka 이벤트 발행 (권한 변경 전파)
- Audit 로깅 (auth_audit_log)

---

## 구현 단계

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

---

## 영향받는 파일

### 신규 생성
```
src/main/java/.../auth/domain/
├── RoleEntity.java
├── PermissionEntity.java
├── UserRoleEntity.java
├── RolePermissionEntity.java
├── MembershipTier.java
├── MembershipTierPermission.java
├── UserMembership.java
├── MembershipStatus.java (enum: ACTIVE, EXPIRED, CANCELLED)
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

### 수정 필요
```
src/main/java/.../user/domain/User.java           - roles 관계 추가, role 필드 deprecated
src/main/java/.../user/domain/Role.java            - enum → deprecated (신규 RoleEntity 사용)
src/main/java/.../auth/service/TokenService.java   - JWT claims 확장 (roles 배열, memberships)
src/main/java/.../auth/security/JwtAuthenticationFilter.java - 복수 Authority 파싱
src/main/java/.../common/config/SecurityConfig.java - 경로별 접근제어 확장
src/main/java/.../user/service/UserService.java    - 회원가입 시 기본 Role/Membership 할당
src/main/java/.../common/exception/AuthErrorCode.java - RBAC 관련 에러코드 추가
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] RoleManagementService: CRUD, Permission 매핑
- [ ] MembershipService: 가입/변경/해지
- [ ] SellerApprovalService: 신청/승인/거부 흐름
- [ ] TokenService: JWT v2 생성 (roles 배열, memberships)
- [ ] TokenService: JWT v1 하위 호환 파싱

### 통합 테스트
- [ ] 회원가입 → ROLE_USER + FREE 멤버십 자동 할당
- [ ] Seller 신청 → 승인 → ROLE_SELLER 부여 → JWT 재발급
- [ ] Role 부여 → Kafka RoleChangedEvent 발행 확인
- [ ] Membership 변경 → Kafka MembershipChangedEvent 발행 확인
- [ ] SUPER_ADMIN만 Role/Permission 관리 API 접근 가능

### 보안 테스트
- [ ] 일반 사용자: Role 관리 API 403
- [ ] SERVICE_ADMIN: 해당 서비스 범위만 관리 가능
- [ ] 시스템 Role (is_system=true) 삭제 시도 → 거부
