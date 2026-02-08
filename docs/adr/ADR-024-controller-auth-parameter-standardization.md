# ADR-024: Controller 인증 파라미터 표준화

**Status**: Accepted
**Created**: 2026-02-07
**Author**: Laze

## Context

Portal Universe의 각 마이크로서비스는 API Gateway를 통해 JWT 검증을 수행하고, 검증된 사용자 정보를 HTTP 헤더(`X-User-Id`, `X-User-Name`, `X-User-Nickname`)로 전달받는다. 하지만 현재 blog-service를 포함한 여러 서비스에서 Controller 메서드가 이 사용자 정보를 받는 방식이 통일되지 않았다.

**현재 3가지 방식 혼용**:
1. `@AuthenticationPrincipal String userId` - Spring Security의 기본 principal(userId만 추출)
2. `@RequestHeader("X-User-Id") String userId` - 직접 헤더 접근 (notification-service)
3. `@CurrentUser GatewayUser user` - 커스텀 resolver로 Gateway 헤더 전체를 GatewayUser record로 매핑

**문제점**:
- 같은 Controller 내에서도 메서드별로 다른 방식 사용 (예: PostController의 create는 방식 3, update는 방식 1)
- 신규 개발자가 어느 방식을 따라야 할지 혼란
- 향후 사용자 정보(name, nickname)가 추가로 필요해지면 파라미터 타입 변경 필요
- `GatewayUser`라는 이름이 인프라 세부사항(Gateway)을 도메인 코드에 노출(Leaky Abstraction)

## Decision

**`@CurrentUser AuthUser`를 표준 인증 파라미터 방식으로 채택**한다.

### 핵심 결정사항

1. **타입 리네이밍**: `GatewayUser` → `AuthUser`
   - 인프라 세부사항(Gateway) 비노출
   - 인증된 사용자(Authenticated User)라는 의미만 전달
   - 도메인 독립적인 네이밍으로 코드 가독성 향상

2. **단일 표준 채택**:
   - userId만 필요한 경우에도 `@CurrentUser AuthUser user`를 받고 `user.uuid()`로 접근
   - `@AuthenticationPrincipal String userId` 방식 폐기
   - `@RequestHeader("X-User-Id")` 직접 접근 폐기

3. **일괄 마이그레이션**:
   - 모든 Java/Spring Controller에서 즉시 적용
   - 점진적 교체 방식 대신 일괄 변경으로 혼란 제거

4. **`@CurrentUserId` 분리 불채택**:
   - `@CurrentUserId String userId` 별도 어노테이션 제안 검토 결과 → 오버엔지니어링 판단
   - `AuthUser` record는 3개 final 필드만 존재, 메모리/성능 오버헤드 제로
   - 유지보수 복잡도(어노테이션 2개 관리) > 이점(타이핑 절감)

### 적용 범위 (Polyglot 고려)

**Java/Spring 서비스만 적용**:
- blog-service, shopping-service, notification-service

**언어별 예외**:
- **NestJS** (prism-service): `@CurrentUserId()` decorator 유지 (NestJS 관례)
- **Python** (chatbot-service): `Depends()` 패턴 유지 (FastAPI 관례)
- **auth-service**: 자체 인증 로직 유지 (JWT 발급 주체)

## Rationale

- **일관성**: 모든 Java/Spring Controller에서 동일한 방식으로 사용자 정보 접근 → 코드 리뷰 및 유지보수 용이
- **확장성**: 현재 userId만 사용하더라도 향후 name, nickname이 필요해지면 파라미터 변경 없이 `user.name()`, `user.nickname()` 추가 가능
- **명시성**: `AuthUser` 타입으로 인증된 사용자라는 의미를 코드에서 명확히 표현
- **인프라 독립성**: `AuthUser`는 Gateway라는 특정 인프라 구현을 드러내지 않음 → 향후 인증 방식 변경 시에도 도메인 코드 수정 불필요
- **단순성**: `user.uuid()`가 `userId`보다 출처가 명확하고, IDE 자동완성으로 사용 가능한 필드 즉시 확인 가능

## Trade-offs

✅ **장점**:
- 전체 Java/Spring 서비스의 Controller 코드 패턴 통일
- 향후 사용자 정보 확장 시 코드 변경 최소화
- 커스텀 어노테이션(`@CurrentUser`)의 존재 이유 명확화 → Spring Security 기본 방식 대신 Gateway 헤더 우선 사용
- **Leaky Abstraction 제거**: `GatewayUser`는 "Gateway를 통해 온 사용자"라는 인프라 세부사항을 노출했지만, `AuthUser`는 "인증된 사용자"라는 도메인 개념만 전달
- **일괄 마이그레이션**: 점진적 교체 대신 즉시 전환으로 혼용 기간 제거 → 신규 개발자 혼란 방지

⚠️ **단점 및 완화**:
- **userId만 필요한데 AuthUser 전체를 받음**
  - 완화: `AuthUser`는 record이므로 메모리 오버헤드 무시 가능(final 필드 3개만 존재)
  - 대안 분석: `@CurrentUserId String userId` 별도 어노테이션 제안 검토 → 유지보수 비용(어노테이션 2개, resolver 2개 관리) > 이점(타이핑 약간 절감)

- **기존 코드 일괄 마이그레이션 필요**
  - 완화: 영향범위 명확(blog-service 4개, shopping-service 9개, notification-service 1개 Controller)
  - 완화: 단순 파라미터 타입/접근 방식 변경이므로 비즈니스 로직 미영향

- **`@AuthenticationPrincipal`이 Spring Security 표준인데 커스텀 방식 사용**
  - 완화: API Gateway 아키텍처에서는 Gateway 헤더가 principal보다 우선적인 인증 정보 출처임을 ADR로 문서화
  - 완화: NestJS(`@CurrentUserId()`), Python(`Depends()`)도 각 언어 관례를 따르므로 Polyglot 환경에서 자연스러움

## Implementation

### 공통 라이브러리 변경 (common-library)

**파일 변경**:
```
common-library/src/main/java/com/portal/universe/common/security/
├── context/
│   ├── AuthUser.java                      # GatewayUser.java에서 rename
│   ├── CurrentUser.java                   # 변경 없음
│   └── CurrentUserArgumentResolver.java   # AuthUser 참조로 변경
├── filter/
│   └── GatewayAuthenticationFilter.java   # AuthUser 생성으로 변경
└── config/
    └── AuthUserWebConfig.java             # GatewayUserWebConfig.java에서 rename
```

**타입 변경**:
- `record GatewayUser(String uuid, String name, String nickname)` → `record AuthUser(String uuid, String name, String nickname)`
- `@CurrentUser GatewayUser user` → `@CurrentUser AuthUser user`

### 영향받는 서비스 및 Controller

#### 1. blog-service (4개 Controller)
| Controller | 변경 전 | 변경 후 |
|-----------|--------|--------|
| PostController | `@AuthenticationPrincipal String userId` | `@CurrentUser AuthUser user` + `user.uuid()` |
| CommentController | `@AuthenticationPrincipal String userId` | `@CurrentUser AuthUser user` + `user.uuid()` |
| SeriesController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| LikeController | `@AuthenticationPrincipal String userId` | `@CurrentUser AuthUser user` + `user.uuid()` |

#### 2. shopping-service (9개 Controller)
| Controller | 변경 전 | 변경 후 |
|-----------|--------|--------|
| OrderController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| CartController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| DeliveryController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| PaymentController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| CouponController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| TimeDealController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| QueueController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| InventoryController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |
| SearchController | `@CurrentUser GatewayUser user` | `@CurrentUser AuthUser user` |

#### 3. notification-service (1개 Controller)
| Controller | 변경 전 | 변경 후 |
|-----------|--------|--------|
| NotificationController | `@RequestHeader("X-User-Id") String userId` | `@CurrentUser AuthUser user` + `user.uuid()` |

#### 4. 예외 서비스
- **auth-service**: 자체 인증 로직 유지 (JWT 발급 주체)
- **prism-service (NestJS)**: `@CurrentUserId()` decorator 유지
- **chatbot-service (Python)**: `Depends()` 패턴 유지

### 마이그레이션 전략

**일괄 변경 순서**:
1. common-library: `GatewayUser` → `AuthUser` rename, 관련 클래스 업데이트
2. shopping-service: 9개 Controller 일괄 변경 (GatewayUser import 교체만)
3. blog-service: 4개 Controller 일괄 변경 (`@AuthenticationPrincipal` 제거)
4. notification-service: 1개 Controller 변경 (`@RequestHeader` 제거)
5. 빌드 검증 및 테스트 실행

## References

- [ADR-003: Authorization Strategy](./ADR-003-authorization-strategy.md) - API Gateway 인증 흐름
- common-library: `AuthUser.java`, `CurrentUser.java`, `CurrentUserArgumentResolver.java`, `GatewayAuthenticationFilter.java`, `AuthUserWebConfig.java`
- blog-service 코드 리뷰 피드백 (2026-02-07)
- AuthUser 리네이밍 결정 (2026-02-08)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
| 2026-02-08 | AuthUser 리네이밍, 일괄 마이그레이션 결정, Polyglot 범위 추가 | Laze |
