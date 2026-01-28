---
id: ADR-011
title: 계층적 RBAC + 멤버십 기반 인증/인가 시스템
type: adr
status: proposed
created: 2026-01-28
updated: 2026-01-28
author: Laze
tags: [auth, rbac, permission, membership, refactoring]
related:
  - ADR-003
  - ADR-004
  - ADR-010
---

# ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템

## 상태

Proposed

## 날짜

2026-01-28

---

## 컨텍스트

### 현재 시스템 분석

Portal Universe의 인증/인가 시스템은 `ROLE_USER`와 `ROLE_ADMIN` 2단계 Role 체계로 운영되고 있다.

**현재 구조:**

| 항목 | 현재 상태 | 문제점 |
|------|----------|--------|
| Role | `ROLE_USER`, `ROLE_ADMIN` 2개 | 서비스별 관리자, 판매자 구분 불가 |
| JWT claims.roles | 단일 문자열 `"ROLE_USER"` | 다중 역할 불가 |
| Gateway 전달 | `X-User-Roles: ROLE_USER` | 단일 Authority만 전달 |
| Permission | 없음 (Role 직접 체크) | Fine-grained 접근 제어 불가 |
| 멤버십 | 없음 | 기능 차별화 불가 |
| Seller/Buyer | 구분 없음 | 모든 ADMIN이 모든 상품 관리 |

**영향받는 핵심 코드:**

- `services/auth-service/.../user/domain/Role.java` — `USER`, `ADMIN` 2종 enum
- `services/auth-service/.../auth/service/TokenService.java` — `claims.put("roles", user.getRole().getKey())` 단일 문자열
- `services/api-gateway/.../filter/JwtAuthenticationFilter.java` — `List.of(new SimpleGrantedAuthority(roles))` 단일 Authority
- `services/common-library/.../security/filter/GatewayAuthenticationFilter.java` — 동일하게 단일 Authority
- `frontend/portal-shell/src/store/auth.ts` — `role: 'guest' | 'user' | 'admin'`

### Decision Drivers

1. Shopping 서비스에 판매자(Seller) 역할이 필요하다
2. 시스템 전체 관리자와 서비스별 관리자를 분리해야 한다
3. 멤버십 티어에 따른 기능 접근 차별화가 필요하다
4. 새 서비스 추가 시 역할/권한을 쉽게 확장할 수 있어야 한다
5. 기존 시스템과의 하위 호환성을 유지하면서 점진적으로 전환해야 한다

---

## 대안 검토

| 대안 | 장점 | 단점 | 평가 |
|------|------|------|------|
| **A. 기존 유지 (USER/ADMIN)** | 변경 없음, 단순 | 확장성 없음, Seller 구분 불가 | ❌ |
| **B. ABAC만 (Attribute-Based)** | 매우 유연한 정책 | 복잡도 과도, 학습 곡선 가파름, 디버깅 어려움 | ❌ |
| **C. RBAC만 (Role만, Permission 없음)** | 구현 단순 | 세밀한 제어 불가, 멤버십 기능 차별화 어려움 | 🟡 |
| **D. RBAC + Membership 하이브리드** | Role 기반 + Permission + 멤버십 확장성 | 구현 복잡도 중간 | ✅ 채택 |

---

## 결정

**계층적 RBAC + Membership 기반 하이브리드 모델**을 채택한다.

핵심 설계 원칙: **Lean Token, Rich Resolution** — JWT에는 Role과 Membership만 포함하고, Permission은 서비스 레벨에서 Redis 캐시 기반으로 해석한다.

### 1. 계층적 Role 구조

```
                    SUPER_ADMIN
                   /           \
          BLOG_ADMIN        SHOPPING_ADMIN
              |              /          \
              |          SELLER        (BUYER = USER)
              |              \          /
              +-------- USER ---------+
                   (Base Role)
```

| Role Key | 범위 | 설명 |
|----------|------|------|
| `ROLE_USER` | Global | 기본 역할. 모든 가입 사용자에게 자동 부여 |
| `ROLE_SELLER` | Shopping | 상품 등록/수정/관리. 관리자 수동 승인 필요 |
| `ROLE_BLOG_ADMIN` | Blog | 블로그 컨텐츠 관리 |
| `ROLE_SHOPPING_ADMIN` | Shopping | 쇼핑 전체 관리 (주문, 배송, 재고, 상품) |
| `ROLE_SUPER_ADMIN` | Global | 전체 시스템 관리, 역할/권한 부여 가능 |

- 상위 Role은 하위 Role의 모든 Permission을 **상속**한다.
- 한 사용자가 **복수 Role**을 가질 수 있다.
- Seller 역할은 **관리자 수동 승인**으로 부여한다.

### 2. Permission 모델

Permission 명명 규칙: `{service}:{resource}:{action}`

**Action 종류:** `create`, `read`, `read:own`, `update:own`, `delete:own`, `manage`, `*`

**주요 Permission 예시:**

| Permission | 설명 | 부여 대상 |
|-----------|------|----------|
| `blog:post:create` | 게시글 작성 | USER |
| `blog:post:update:own` | 본인 게시글 수정 | USER |
| `blog:post:manage` | 모든 게시글 관리 | BLOG_ADMIN |
| `shopping:product:create` | 상품 등록 | SELLER |
| `shopping:product:update:own` | 본인 상품 수정 | SELLER |
| `shopping:product:manage` | 모든 상품 관리 | SHOPPING_ADMIN |
| `shopping:order:create` | 주문 생성 | USER |
| `shopping:order:manage` | 모든 주문 관리 | SHOPPING_ADMIN |
| `system:role:manage` | 역할 관리 | SUPER_ADMIN |

### 3. JWT Claims 구조 변경

**Before (v1):**
```json
{
  "sub": "uuid",
  "roles": "ROLE_USER",
  "email": "...",
  "nickname": "..."
}
```

**After (v2):**
```json
{
  "sub": "uuid",
  "roles": ["ROLE_USER", "ROLE_SELLER"],
  "memberships": { "shopping": "PREMIUM", "blog": "FREE" },
  "email": "...",
  "nickname": "..."
}
```

- `roles`: String → String[] (배열)
- `memberships`: 신규 추가 (서비스별 티어 Map)
- **하위 호환**: Gateway에서 v1(문자열)/v2(배열) dual format 지원
- **토큰 크기 영향**: ~80바이트 증가 (HTTP 헤더 제한 대비 무시 가능)

### 4. 멤버십 모델

서비스별 4단계 티어: `FREE` → `BASIC` → `PREMIUM` → `VIP`

**Blog:**
| 티어 | 추가 기능 |
|------|----------|
| FREE | 기본 블로그 (게시, 댓글, 시리즈) |
| BASIC | +커스텀 도메인, 시리즈 고급 설정 |
| PREMIUM | +블로그 통계/분석, 예약 발행, 광고 제거 |
| VIP | +우선 노출, 전담 지원, Featured 게시글 |

**Shopping:**
| 티어 | 추가 기능 |
|------|----------|
| FREE | 기본 쇼핑 (구매, 장바구니, 주문) |
| BASIC | +무료 배송, 전용 쿠폰 |
| PREMIUM | +타임딜 조기 접근, 연장 반품, 포인트 2배 |
| VIP | +VIP 전용 딜, 퍼스널 쇼퍼, 우선 CS |

### 5. DB 스키마

```sql
-- Role & Permission
roles (id, role_key, display_name, service_scope, parent_role_id, is_system)
permissions (id, permission_key, service, resource, action)
user_roles (id, user_id, role_id, assigned_by, expires_at)
role_permissions (id, role_id, permission_id)

-- Membership
membership_tiers (id, service_name, tier_key, display_name, price_monthly, price_yearly)
membership_tier_permissions (id, tier_id, permission_id)
user_memberships (id, user_id, service_name, tier_id, status, expires_at)

-- Audit
auth_audit_log (id, event_type, target_user_id, actor_user_id, details, ip_address)
```

### 6. 권한 검증 흐름

```
Client → API Gateway (JWT 검증)
       → X-User-Id, X-User-Roles (콤마 구분), X-User-Memberships (JSON)
       → 각 서비스: EnhancedGatewayAuthenticationFilter
         → PermissionResolver (Redis 캐시)
           → Role + Membership → Permission Set 해석
         → SecurityContext에 Role + Permission Authority 설정
       → SecurityConfig / @PreAuthorize 검증
       → Service 레이어 비즈니스 로직 검증
```

**Permission Resolution 성능:**
| 시나리오 | 소요 시간 |
|----------|:---:|
| Redis 캐시 히트 | < 1ms |
| Redis 캐시 미스 → DB | 5-15ms |
| Redis 장애 → DB Fallback | 10-30ms |

### 7. 이벤트 기반 동기화 (Kafka)

| Topic | 용도 |
|-------|------|
| `auth.role.changed` | 역할 변경 → 캐시 무효화 |
| `auth.permission-mapping.changed` | Role-Permission 매핑 변경 → 캐시 전체 무효화 |
| `auth.membership.changed` | 멤버십 변경 → 캐시 무효화 + 서비스별 로직 |
| `auth.token.revoked` | 강제 토큰 무효화 → Gateway Blacklist |

### 8. Frontend 권한 모델

**Portal Shell (Vue 3):**
- `usePermission` composable: `hasRole()`, `hasAnyRole()`, `isSeller()`, `hasMembershipAtLeast()`
- authAdapter 확장: roles 배열, memberships Map, 편의 메서드

**Shopping Frontend (React 18):**
- `usePermission` hook: Role/Membership 체크 메서드
- `RequireAnyRole`, `RequireMembership` Guard 컴포넌트

**Blog Frontend (Vue 3):**
- 라우터 가드 구현 (현재 미구현 → 추가)

---

## 구현 계획

### Phase 1: DB 스키마 & 데이터 마이그레이션

- roles, permissions 등 8개 테이블 생성
- 기존 `User.role` 데이터를 `user_roles` 테이블로 마이그레이션
- 기존 ADMIN → SUPER_ADMIN으로 매핑
- 모든 사용자에게 FREE 멤버십 자동 생성
- **리스크: 낮음** (DB 추가만, 기존 기능 영향 없음)

### Phase 2: JWT 포맷 + Gateway 마이그레이션

- TokenService: roles를 배열로 생성, memberships 추가
- Gateway JwtAuthenticationFilter: v1/v2 dual format 파싱
- GatewayAuthenticationFilter: 콤마 구분 roles 파싱
- X-User-Memberships 헤더 추가
- **리스크: 중간** (Gateway + 모든 서비스 배포 필요)

### Phase 3: Permission Resolution + Membership API

- common-library에 PermissionResolver 구현 (Redis 캐시)
- EnhancedGatewayAuthenticationFilter
- Membership 관리 API (CRUD)
- Kafka 이벤트 스키마 + Consumer 추가
- **리스크: 중간** (Redis 의존성 추가)

### Phase 4: Frontend 업데이트

- Portal Shell: UserAuthority 확장, usePermission composable
- authAdapter 확장 (roles 배열, memberships, 편의 메서드)
- Shopping Frontend: authStore 타입 변경, usePermission hook
- Blog Frontend: 라우터 가드 구현
- Guard 컴포넌트 확장 (RequireAnyRole, RequireMembership)
- **리스크: 낮음**

### Phase 5: Full RBAC 적용

- SecurityConfig를 SELLER/SERVICE_ADMIN 기반으로 전환
- @PreAuthorize Permission 기반 체크 도입
- User.role enum 필드 완전 제거
- Audit 로깅 강화
- RBAC Admin UI 구현
- **리스크: 중간** (권한 체계 전환)

---

## 리스크 완화

| 리스크 | 완화 전략 |
|--------|----------|
| JWT v1→v2 전환 중 인증 실패 | Gateway에서 dual format 지원, 점진적 전환 |
| Permission 캐시 불일치 | Short TTL (5분) + Kafka 이벤트 기반 즉시 무효화 |
| Redis 장애 | DB Fallback + Circuit Breaker |
| 기존 API 동작 변경 | 하위 호환 유지, deprecated 표시 후 점진적 제거 |
| 배포 순서 의존성 | Phase별 독립 배포 가능하도록 설계 |

---

## 결과

### 긍정적
- 서비스별 관리자 분리로 최소 권한 원칙 적용
- Seller 역할 도입으로 판매자 기능 분리
- 멤버십 기반 기능 차별화로 수익 모델 확보
- Permission 기반 세밀한 접근 제어
- 새 서비스 추가 시 Role/Permission 확장 용이

### 부정적
- 구현 복잡도 증가 (8개 신규 테이블, Redis 의존성)
- 모든 서비스에 걸친 변경 필요
- Permission Resolution 추가 레이턴시 (캐시 미스 시 5-15ms)

---

## 관련 결정

- **ADR-003**: Admin 권한 검증 전략 → 본 ADR에 의해 확장됨
- **ADR-004**: JWT RBAC 자동 설정 전략 → 본 ADR의 Phase 2-3에서 확장
- **ADR-010**: 보안 강화 아키텍처 → 감사 로깅 요구사항 연계

---

## 서비스별 구현 가이드

각 서비스의 구체적 구현 가이드는 해당 모듈 내 문서를 참조:

- `services/auth-service/docs/rbac-refactoring-guide.md`
- `services/api-gateway/docs/rbac-refactoring-guide.md`
- `services/common-library/docs/rbac-refactoring-guide.md`
- `services/shopping-service/docs/rbac-refactoring-guide.md`
- `services/blog-service/docs/rbac-refactoring-guide.md`
- `frontend/portal-shell/docs/rbac-refactoring-guide.md`
- `frontend/shopping-frontend/docs/rbac-refactoring-guide.md`
- `frontend/blog-frontend/docs/rbac-refactoring-guide.md`
