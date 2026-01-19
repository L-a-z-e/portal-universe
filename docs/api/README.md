# API Documentation Index

Portal Universe의 모든 API 명세 문서 목록입니다.

## 📋 목차

- [보안 & 인증 API](#보안--인증-api)
- [Shopping Service API](#shopping-service-api)
- [문서 작성 가이드](#문서-작성-가이드)

---

## 🔐 보안 & 인증 API

### [Security & Authentication API Reference](./security-api-reference.md)
- **서비스**: Auth Service
- **Base URL**: `http://localhost:8080`
- **프로토콜**: OAuth2 / OpenID Connect 1.0
- **토큰 타입**: JWT (JSON Web Token)

**주요 엔드포인트**:
- OAuth2 Authorization (`/oauth2/authorize`)
- Token 발급 (`/oauth2/token`)
- Token 검증 (`/oauth2/introspect`)
- Token 폐기 (`/oauth2/revoke`)
- JWK Set (`/oauth2/jwks`)
- OpenID Discovery (`/.well-known/openid-configuration`)
- 회원가입 (`POST /api/users/signup`)
- 내 정보 조회 (`GET /api/users/me`)

**특징**:
- ✅ PKCE 필수 (Public Client)
- ✅ Access Token TTL: 2분
- ✅ Refresh Token TTL: 7일
- ✅ RS256 서명 알고리즘

---

## 🛒 Shopping Service API

### [Shopping Service API Reference](./shopping-api-reference.md)
- **서비스**: Shopping Service
- **Base URL**: `http://localhost:8080/api/shopping`
- **프로토콜**: REST API (JSON)
- **인증**: JWT Bearer Token

**주요 엔드포인트**:
- 상품 관리 (Admin)
  - `POST /admin/products` - 상품 생성
  - `PUT /admin/products/{productId}` - 상품 수정
  - `DELETE /admin/products/{productId}` - 상품 삭제
  - `PATCH /admin/products/{productId}/stock` - 재고 수정
- 상품 조회 (Public)
  - `GET /products` - 상품 목록
  - `GET /products/{productId}` - 상품 상세
  - `GET /products/{productId}/with-reviews` - 상품 + 리뷰

**에러 코드**: S001 ~ S010

---

### [Coupon API](./coupon-api.md)
- **서비스**: Shopping Service
- **Base URL**: `http://localhost:8080/api/shopping/coupons`

**주요 엔드포인트**:
- `GET /coupons` - 발급 가능 쿠폰 목록
- `GET /coupons/{couponId}` - 쿠폰 상세
- `POST /coupons/{couponId}/issue` - 선착순 쿠폰 발급
- `GET /coupons/my` - 내 쿠폰 목록
- `GET /coupons/my/available` - 사용 가능한 내 쿠폰

**Admin 전용**:
- `POST /admin/coupons` - 쿠폰 생성
- `DELETE /admin/coupons/{couponId}` - 쿠폰 비활성화

**특징**:
- ✅ Redis 기반 선착순 발급
- ✅ 중복 발급 방지
- ✅ 재고 동시성 제어

---

### [TimeDeal API](./timedeal-api.md)
- **서비스**: Shopping Service
- **Base URL**: `http://localhost:8080/api/shopping/time-deals`

**주요 엔드포인트**:
- `GET /time-deals` - 진행중 타임딜 목록
- `GET /time-deals/{timeDealId}` - 타임딜 상세
- `POST /time-deals/purchase` - 타임딜 구매
- `GET /time-deals/my/purchases` - 내 구매 내역

**Admin 전용**:
- `POST /admin/time-deals` - 타임딜 생성
- `DELETE /admin/time-deals/{timeDealId}` - 타임딜 취소

**특징**:
- ✅ 시간 제한 특가 상품
- ✅ 재고 동시성 제어
- ✅ 구매 제한 (1인 1개)

---

### [Admin Products API](./admin-products-api.md)
- **서비스**: Shopping Service
- **Base URL**: `http://localhost:8080/api/shopping/admin/products`
- **권한**: ADMIN

**참고**: 현재 이 문서는 [Shopping Service API Reference](./shopping-api-reference.md)에 통합되었습니다.

---

## 📝 문서 작성 가이드

### API 문서 구조

모든 API 문서는 다음 구조를 따라야 합니다:

1. **목차**
2. **개요**
   - Base URL
   - Scope
   - Protocol
   - Auth
3. **공통 응답 형식**
   - 성공 응답
   - 에러 응답
   - HTTP 상태 코드
4. **에러 코드**
   - 서비스별 에러 코드
   - 공통 에러 코드
5. **API 상세**
   - Endpoint 및 Method
   - 권한 요구사항
   - Request (Headers, Path Parameters, Query Parameters, Body)
   - Response (성공/실패)
   - cURL 예시
   - 추가 예시 (JavaScript 등)

### 명명 규칙

| 항목 | 규칙 | 예시 |
|------|------|------|
| 파일명 | `{service}-api-reference.md` | `security-api-reference.md` |
| HTTP Method | 대문자 | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` |
| Endpoint | 소문자, kebab-case | `/api/users/me`, `/api/shopping/products` |
| 에러 코드 | 서비스별 prefix + 3자리 숫자 | `A001` (Auth), `S001` (Shopping), `C001` (Common) |

### 에러 코드 범위

| 서비스 | Prefix | 범위 | 예시 |
|--------|--------|------|------|
| Common | C | C001 ~ C099 | C001, C002, C003 |
| Auth | A | A001 ~ A099 | A001 |
| Blog | B | B001 ~ B099 | B001, B002, B003 |
| Shopping | S | S001 ~ S099 | S001 ~ S010 |
| Notification | N | N001 ~ N099 | (추후 정의) |

### 참고 문서

- [Error Handling Rules](../../.claude/rules/error-handling.md)
- [Backend Patterns](../../.claude/rules/backend-patterns.md)

---

## 📊 문서 현황

| 문서 | 서비스 | 상태 | 마지막 업데이트 |
|------|--------|------|------------------|
| security-api-reference.md | Auth | ✅ 완료 | 2026-01-19 |
| shopping-api-reference.md | Shopping | ✅ 완료 | 2026-01-17 |
| coupon-api.md | Shopping | ✅ 완료 | 2026-01-17 |
| timedeal-api.md | Shopping | ✅ 완료 | 2026-01-17 |
| admin-products-api.md | Shopping | 🔄 통합됨 | 2026-01-17 |

---

## 🔗 관련 문서

- [Architecture Overview](../architecture/system-architecture.md)
- [Service Communication](../architecture/service-communication.md)
- [ADR-002: API Gateway Pattern](../adr/ADR-002-api-gateway-pattern.md)
- [PRD-002: Shopping Service](../prd/PRD-002-shopping-service.md)

---

## 📞 Contact

API 문서에 대한 질문이나 개선 사항은 Issue를 통해 제안해주세요.
