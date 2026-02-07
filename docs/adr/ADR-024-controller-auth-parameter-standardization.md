# ADR-024: Controller 인증 파라미터 표준화

**Status**: Accepted
**Created**: 2026-02-07
**Author**: Laze

## Context

Portal Universe의 각 마이크로서비스는 API Gateway를 통해 JWT 검증을 수행하고, 검증된 사용자 정보를 HTTP 헤더(`X-User-Id`, `X-User-Name`, `X-User-Nickname`)로 전달받는다. 하지만 현재 blog-service를 포함한 여러 서비스에서 Controller 메서드가 이 사용자 정보를 받는 방식이 통일되지 않았다.

**현재 2가지 방식 혼용**:
1. `@AuthenticationPrincipal String userId` - Spring Security의 기본 principal(userId만 추출)
2. `@CurrentUser GatewayUser user` - 커스텀 resolver로 Gateway 헤더 전체를 GatewayUser record로 매핑

**문제점**:
- 같은 Controller 내에서도 메서드별로 다른 방식 사용 (예: PostController의 create는 방식 2, update는 방식 1)
- 신규 개발자가 어느 방식을 따라야 할지 혼란
- 향후 사용자 정보(name, nickname)가 추가로 필요해지면 파라미터 타입 변경 필요

## Decision

**`@CurrentUser GatewayUser`를 표준 인증 파라미터 방식으로 채택**한다.

- userId만 필요한 경우에도 `@CurrentUser GatewayUser user`를 받고 `user.uuid()`로 접근한다.
- `@AuthenticationPrincipal String userId` 방식은 더 이상 사용하지 않는다.
- 기존 코드는 점진적으로 마이그레이션한다.

## Rationale

- **일관성**: 모든 Controller에서 동일한 방식으로 사용자 정보 접근 → 코드 리뷰 및 유지보수 용이
- **확장성**: 현재 userId만 사용하더라도 향후 name, nickname이 필요해지면 파라미터 변경 없이 `user.name()`, `user.nickname()` 추가 가능
- **명시성**: `GatewayUser` 타입으로 이 값이 API Gateway를 거쳐 온 것임을 코드에서 명확히 표현
- **단순성**: `user.uuid()`가 `userId`보다 출처가 명확하고, IDE 자동완성으로 사용 가능한 필드 즉시 확인 가능

## Trade-offs

✅ **장점**:
- 전체 서비스의 Controller 코드 패턴 통일
- 향후 사용자 정보 확장 시 코드 변경 최소화
- 커스텀 어노테이션(`@CurrentUser`)의 존재 이유 명확화 → Spring Security 기본 방식 대신 Gateway 헤더 우선 사용

⚠️ **단점 및 완화**:
- userId만 필요한데 GatewayUser 전체를 받음 → (완화: GatewayUser는 record이므로 메모리 오버헤드 무시 가능, final 필드만 존재)
- 기존 코드 마이그레이션 필요 → (완화: 새 코드부터 적용, 기존 코드는 점진적 교체, `@Deprecated` 주석으로 명시)
- `@AuthenticationPrincipal`이 Spring Security 표준인데 커스텀 방식 사용 → (완화: Gateway 헤더가 principal보다 우선적인 인증 정보 출처임을 ADR로 문서화)

## Implementation

**영향받는 서비스**:
- `blog-service`: PostController, SeriesController, CommentController의 `@AuthenticationPrincipal String userId` → `@CurrentUser GatewayUser user` 교체
- `shopping-service`, `notification-service`: 신규 Controller 작성 시 `@CurrentUser GatewayUser` 사용
- `auth-service`: 자체 인증 서비스이므로 예외 (자체 SecurityContext 사용)

**마이그레이션 단계**:
1. 신규 API 개발 시 `@CurrentUser GatewayUser` 필수 적용
2. 기존 API 수정 시 점진적 교체
3. `@AuthenticationPrincipal String userId`에 `@Deprecated` 주석 (IntelliJ 경고)

**공통 라이브러리 위치**:
- `common-library/src/main/java/com/portal/universe/common/auth/GatewayUser.java`
- `common-library/src/main/java/com/portal/universe/common/auth/CurrentUser.java`
- `common-library/src/main/java/com/portal/universe/common/auth/GatewayUserArgumentResolver.java`

## References

- [ADR-003: Authorization Strategy](./ADR-003-authorization-strategy.md) - API Gateway 인증 흐름
- common-library: `GatewayUser.java`, `CurrentUser.java`, `GatewayUserArgumentResolver.java`
- blog-service 코드 리뷰 피드백 (2026-02-07)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
