# Gateway Header Contract

> API Gateway가 JWT 검증 후 downstream 서비스에 전달하는 헤더 규격.
> 모든 서비스(Java, NestJS, Python)는 이 계약을 준수하여 헤더를 파싱해야 한다.

## 보안 모델

```
Client → API Gateway (JWT 검증) → X-User-* 헤더 주입 → Downstream Service
```

- Gateway는 요청 수신 시 기존 `X-User-*` 헤더를 **모두 제거** 후 JWT에서 추출한 값으로 재설정 (Header Injection 방어)
- Downstream 서비스는 `X-User-*` 헤더를 **신뢰**하고 별도 JWT 검증 불필요 (ADR-035, ADR-039)
- 네트워크 격리(Docker/K8s NetworkPolicy)로 Gateway 우회 접근 차단

## 헤더 목록

| 헤더명 | 필수 | 형식 | 설명 |
|--------|------|------|------|
| `X-User-Id` | **필수** | UUID string | 사용자 고유 식별자 |
| `X-User-Effective-Roles` | **필수** | 쉼표 구분 string | Role Hierarchy 상속 포함 유효 역할 |
| `X-User-Roles` | 선택 | 쉼표 구분 string | 원본 역할 (Effective-Roles의 fallback) |
| `X-User-Memberships` | 선택 | JSON string | 멤버십 그룹별 티어 정보 |
| `X-User-Nickname` | 선택 | URL-encoded string | 사용자 닉네임 (UTF-8 인코딩) |
| `X-User-Name` | 선택 | URL-encoded string | 사용자명 (UTF-8 인코딩) |

## 상세 형식

### X-User-Id
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
```
- UUID v4 형식
- 인증된 모든 요청에 포함

### X-User-Effective-Roles
```
X-User-Effective-Roles: ROLE_USER,ROLE_SHOPPING_SELLER,ROLE_ADMIN
```
- 쉼표(`,`) 구분, 공백 없음
- Role Hierarchy 상속을 포함한 전체 역할 목록
  - 예: `ROLE_ADMIN`은 `ROLE_USER`를 포함하므로 둘 다 나열됨
- **파싱 규칙**: `header.split(",").map(s => s.trim()).filter(Boolean)`
- **우선순위**: `X-User-Effective-Roles` 우선, 없으면 `X-User-Roles` 사용

### X-User-Roles
```
X-User-Roles: ROLE_ADMIN,ROLE_SHOPPING_SELLER
```
- 쉼표(`,`) 구분, 공백 없음
- JWT에 직접 저장된 원본 역할 (상속 미포함)
- `X-User-Effective-Roles`의 fallback 용도

### X-User-Memberships
```
X-User-Memberships: {"user:blog":{"tier":"PRO","order":2},"seller:shopping":{"tier":"GOLD","order":3}}
```
- Compact JSON (줄바꿈/들여쓰기 없음)
- 구조:
  ```json
  {
    "<group>:<service>": {
      "tier": "string",
      "order": "number (sort_order, 높을수록 상위 티어)"
    }
  }
  ```
- **파싱 실패 시**: 빈 객체(`{}`)로 fallback, 에러 throw 금지

### X-User-Nickname / X-User-Name
```
X-User-Nickname: %ED%99%8D%EA%B8%B8%EB%8F%99
X-User-Name: laze
```
- UTF-8 URL Encoding (`encodeURIComponent` / `URLEncoder.encode(v, "UTF-8")`)
- 한글, 특수문자 포함 가능
- **디코딩 규칙**: `decodeURIComponent(header)` / `URLDecoder.decode(header, "UTF-8")`

## 서비스별 구현 현황

| 헤더 | Java (common-library) | NestJS (prism-service) | Python (chatbot-service) |
|------|----------------------|----------------------|------------------------|
| X-User-Id | GatewayAuthenticationFilter | JwtAuthGuard | get_current_user_id() |
| X-User-Effective-Roles | GatewayAuthenticationFilter (우선) | JwtAuthGuard (우선) | require_admin() (우선) |
| X-User-Roles | GatewayAuthenticationFilter (fallback) | JwtAuthGuard (fallback) | require_admin() (fallback) |
| X-User-Memberships | MembershipContext | JwtAuthGuard (JSON parse) | **미구현** |
| X-User-Nickname | AuthUser.nickname | 미사용 | 미사용 |
| X-User-Name | AuthUser.name | 미사용 | 미사용 |

## 관련 ADR

- [ADR-035: Polyglot 서비스 인증 표준화](../adr/ADR-035-polyglot-auth-standardization.md)
- [ADR-039: JWT 검증 전략 통일](../adr/ADR-039-unified-jwt-validation.md)

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-13 | 최초 작성 - Gateway 헤더 계약 문서화 |
