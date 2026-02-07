# Auth Service API Documentation

> Auth Service API 문서 인덱스

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8081` (로컬) |
| **API Prefix** | `/api/v1` |
| **인증** | JWT Bearer Token |
| **소셜 로그인** | OAuth2 Client (Google, Naver, Kakao) |
| **토큰 형식** | JWT |

---

## API 목록

| API | 설명 | 상태 |
|-----|------|------|
| [Auth API](./auth-api.md) | 인증/인가, 사용자, RBAC, 멤버십, 팔로우, 셀러 종합 API | ✅ Current |

---

## 주요 기능

### 인증 (Authentication)
- JWT 기반 로그인/로그아웃/토큰 갱신
- OAuth2 소셜 로그인 (Google, Naver, Kakao)
- Refresh Token Rotation (HttpOnly Cookie: `portal_refresh_token`)
- 로그인 실패 횟수 제한 및 계정 임시 잠금

### 사용자 (User Management)
- 이메일 기반 회원가입
- 프로필 조회/수정, Username 설정, 비밀번호 변경
- 계정 삭제 (탈퇴)

### 팔로우 (Follow)
- 팔로우/언팔로우 토글
- 팔로워/팔로잉 목록 조회 (페이지네이션)

### RBAC (Role-Based Access Control)
- 역할 부여/회수 (관리자 전용)
- 권한 조회 (역할 + 멤버십 기반)

### 멤버십 (Membership)
- 서비스별 다중 티어 (FREE, PREMIUM, PRO 등)
- 멤버십 변경/취소

### 셀러 (Seller)
- 셀러 신청 워크플로우 (PENDING → APPROVED/REJECTED)
- 관리자 심사

### 보안
- 비밀번호 정책 (대소문자, 숫자, 특수문자, 이력 관리, 만료)
- Stateless JWT (세션 미사용)
- CSRF 비활성화 (API Gateway에서 처리)

---

## 공통 정보

### 토큰 정책

| 항목 | 값 |
|------|-----|
| **Access Token TTL** | 15분 (900초) |
| **Refresh Token TTL** | 7일 |
| **Refresh Token 재사용** | 불가 (Rotation: 매 갱신 시 새 토큰 발급) |
| **Cookie 이름** | `portal_refresh_token` |
| **Cookie 속성** | HttpOnly, Secure (local: false), SameSite=Lax |

### Controller 구성

| 카테고리 | Controllers | 주요 기능 |
|---------|-------------|----------|
| 인증 | AuthController | JWT 로그인/로그아웃, 토큰 갱신, 비밀번호 정책 |
| 사용자 | UserController, ProfileController | 회원가입, 프로필 관리, Username, 비밀번호 변경, 탈퇴 |
| 소셜 | FollowController | 팔로우/언팔로우, 팔로워/팔로잉 목록 |
| RBAC 관리 | RbacAdminController, PermissionController | 역할/권한 CRUD, Dashboard, 감사 로그 |
| 멤버십 | MembershipController, MembershipAdminController | 서비스별 티어, 조회/변경/취소 |
| 셀러 | SellerController, SellerAdminController | 셀러 신청, 심사 워크플로우 |
| 내부 | RoleHierarchyController | Gateway 역할 계층 해석 |

---

## 관련 문서

- [Architecture Overview](../../architecture/auth-service/system-overview.md)
- [ADR-003: Admin 권한 검증 전략](../../adr/ADR-003-authorization-strategy.md)
- [ADR-008: JWT Stateless + Redis](../../adr/ADR-008-jwt-stateless-redis.md)
- [ADR-015: Role Hierarchy 구현](../../adr/ADR-015-role-hierarchy-implementation.md)
- [ADR-021: 역할 기반 멤버십 재구조화](../../adr/ADR-021-role-based-membership-restructure.md)

---

## 마지막 업데이트

| 날짜 | 변경사항 |
|------|----------|
| 2026-02-07 | Controller 수치 제거, 카테고리 요약으로 전환, ADR 링크 수정 |
| 2026-02-06 | 전면 재작성: 코드베이스 기준 정확한 정보로 갱신 |
| 2026-01-18 | 최초 API 문서 작성 (auth-api.md) |

---

**최종 업데이트**: 2026-02-07
