# ADR-022: auth-service 도메인 경계 재정의

**Status**: Accepted (Decision 1), Proposed (Decision 2)
**Date**: 2026-02-07

## Context

auth-service 코드 리뷰 결과, 도메인 경계에 두 가지 구조적 문제를 발견했습니다:

### 1. UserController / ProfileController 중복

두 컨트롤러가 동일한 사용자 도메인을 서로 다른 URL 체계로 노출하여, 프로필 조회·수정·비밀번호 변경이 완전히 중복되어 있습니다.

**현재 중복 현황:**
- `GET /users/me` ↔ `GET /profile/me` (프로필 조회)
- `PUT /users/me/profile` ↔ `PATCH /profile` (프로필 수정)
- `PUT /users/me/password` ↔ `POST /profile/password` (비밀번호 변경)

**ProfileController에만 존재하는 endpoint:**
- `DELETE /profile/account` (회원 탈퇴)

**UserController 전체 endpoints (7개):**
- POST /api/v1/users/signup
- GET /api/v1/users/{username}
- GET /api/v1/users/me
- PUT /api/v1/users/me/profile
- POST /api/v1/users/me/username
- GET /api/v1/users/check-username/{username}
- PUT /api/v1/users/me/password

**ProfileController 전체 endpoints (4개):**
- GET /api/v1/profile/me
- PATCH /api/v1/profile
- POST /api/v1/profile/password
- DELETE /api/v1/profile/account

### 2. Seller 도메인 배치

판매자 신청/승인 워크플로우가 auth-service에 위치하지만, 비즈니스 로직상 shopping-service에 더 적합합니다. 현재 Product 엔티티에 sellerId 필드조차 없어 Seller-Product 연결이 미구현 상태입니다.

**Seller 관련 코드 (auth-service 내):**
- **SellerController**: POST /api/v1/seller/apply, GET /api/v1/seller/application
- **SellerAdminController**: GET /api/v1/admin/seller/applications/pending, GET /api/v1/admin/seller/applications, POST /api/v1/admin/seller/applications/{applicationId}/review
- **SellerApplicationService**: 신청 제출, 심사(승인/거절), 승인 시 ROLE_SHOPPING_SELLER 할당 + seller:shopping BRONZE 멤버십 자동 생성
- **SellerApplication 엔티티 및 Repository**

## Decision

두 개의 결정으로 구성됩니다:

### Decision 1: User/Profile Controller 통합 (Accepted)

ProfileController 기능을 UserController `/users/me` 하위로 통합하고, ProfileController를 deprecate → 제거합니다.

**통합 후 최종 구조:**
```
POST   /api/v1/users/signup                         # 회원가입
GET    /api/v1/users/me                             # 내 프로필 조회
PATCH  /api/v1/users/me                             # 프로필 수정 (+ 새 Access Token 반환)
PUT    /api/v1/users/me/password                    # 비밀번호 변경
DELETE /api/v1/users/me                             # 회원 탈퇴
POST   /api/v1/users/me/username                    # Username 설정 (1회)
GET    /api/v1/users/{username}                     # 공개 프로필 조회
GET    /api/v1/users/check-username/{username}     # Username 중복 확인
```

### Decision 2: Seller 도메인 shopping-service 이전 (Proposed)

Seller 신청/승인 워크플로우를 shopping-service로 이전합니다. 단, 즉시 실행이 아닌 계획 수립 단계입니다.

## Rationale

### Decision 1 근거

- **REST 원칙**: Profile은 User의 속성이지 독립 리소스가 아닙니다. `/users/me` 하위가 자연스럽습니다.
- **주요 서비스 패턴**: GitHub (`/user`), Twitter/X (`/users/me`) 등 통합 패턴을 사용합니다.
- **중복 제거**: 3개 endpoint가 완전 중복 → 클라이언트 혼란, 유지보수 비용 2배
- **자연스러운 확장**: ProfileController의 유일한 고유 기능(회원 탈퇴)은 `DELETE /users/me`로 자연스럽게 이동 가능
- **HTTP Method 정리**: 프로필 수정은 PATCH(부분 수정)로 통일, 비밀번호 변경은 PUT(전체 교체)로 통일

### Decision 2 근거

- **도메인 경계**: Seller 비즈니스 도메인(사업자 정보, 정산, 상품 관리)은 shopping 컨텍스트입니다.
- **auth-service 책임**: 인증/인가이지, 비즈니스 워크플로우가 아닙니다.
- **자연스러운 연결**: 현재 Product.sellerId가 없어 Seller-Product 관계 미구현 → shopping-service로 이전 시 자연스럽게 연결 가능
- **일관된 패턴**: 승인 시 역할/멤버십 할당은 Kafka 이벤트로 분리 가능 (ADR-021 패턴과 일관)

## Trade-offs

### Decision 1

✅ **장점**:
- **단일 진입점**: 사용자 관련 API가 `/users` 하위로 일관됩니다.
- **코드 중복 제거**: 3개 중복 endpoint 및 관련 서비스 로직 제거
- **클라이언트 단순화**: 프론트엔드에서 User/Profile 구분 없이 단일 API 사용

⚠️ **단점 및 완화**:
- 기존 `/profile/*` API를 사용하는 클라이언트 마이그레이션 필요 → **(완화: 프론트엔드가 모노레포 내에 있으므로 동시 변경 가능)**
- ProfileController의 토큰 갱신 로직(프로필 수정 시 새 Access Token 반환)을 UserController로 이전 필요 → **(완화: 서비스 레이어 로직이므로 Controller만 변경)**

### Decision 2

✅ **장점**:
- **도메인 경계 명확화**: auth = 인증/인가, shopping = 판매자 비즈니스
- **Seller-Product 연결**: shopping-service 내에서 자연스럽게 sellerId 추가 가능
- **서비스 간 결합도 감소**: auth-service가 Shopping 도메인 지식에 의존하지 않음

⚠️ **단점 및 완화**:
- 마이그레이션 복잡도: DB 테이블 이전, API 경로 변경, 이벤트 기반 역할 할당 구현 필요 → **(완화: Proposed 상태로 충분한 계획 후 실행)**
- 역할 할당의 비동기화: 승인 즉시 역할 부여 대신 Kafka 이벤트 기반 → **(완화: 이벤트 발행 + 폴링 확인 패턴으로 안정성 확보)**

## Implementation

### Decision 1 구현 계획

1. UserController에 `DELETE /users/me` 추가 (ProfileController의 회원 탈퇴 로직 이전)
2. UserController의 `PUT /users/me/profile` → `PATCH /users/me`로 변경 (토큰 갱신 로직 포함)
3. ProfileController의 모든 endpoint에 `@Deprecated` 추가 후 제거
4. 프론트엔드(portal-shell) API 호출 경로 일괄 변경
5. API 문서 업데이트

### Decision 2 이전 시 필요 작업 (Proposed)

1. SellerApplication 엔티티/테이블 → shopping-service DB로 이전
2. SellerController, SellerAdminController → shopping-service로 이동
3. 승인 시 역할/멤버십 할당은 Kafka 이벤트로 auth-service에 요청
   - shopping-service: `SellerApprovedEvent` 발행
   - auth-service: 이벤트 소비 → ROLE_SHOPPING_SELLER 할당 + seller:shopping 멤버십 생성
4. Product 엔티티에 `sellerId` 필드 추가
5. API Gateway 라우팅 변경

## References

- [ADR-021: 역할+서비스 복합 멤버십 재구조화](./ADR-021-role-based-membership-restructure.md) — Seller 멤버십 그룹 정의
- UserController: `services/auth-service/src/main/java/com/portal/universe/authservice/user/controller/UserController.java`
- ProfileController: `services/auth-service/src/main/java/com/portal/universe/authservice/user/controller/ProfileController.java`
- SellerController: `services/auth-service/src/main/java/com/portal/universe/authservice/auth/controller/SellerController.java`
- SellerAdminController: `services/auth-service/src/main/java/com/portal/universe/authservice/auth/controller/SellerAdminController.java`
- SellerApplicationService: `services/auth-service/src/main/java/com/portal/universe/authservice/auth/service/SellerApplicationService.java`
- Product: `services/shopping-service/src/main/java/com/portal/universe/shoppingservice/product/domain/Product.java`

---

### 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
