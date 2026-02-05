# ADR-004: JWT RBAC 자동 설정 전략

**Status**: Accepted
**Date**: 2026-01-19

## Context
Portal Universe 마이크로서비스 아키텍처에서는 각 서비스(Auth, Blog, Shopping, Notification)가 JWT 기반 인증/인가를 구현해야 합니다. 특히 **JWT 토큰의 `roles` 클레임을 Spring Security의 `GrantedAuthority`로 변환**하는 작업이 모든 서비스에서 동일하게 필요하며, 각 서비스마다 `JwtAuthenticationConverter` 설정 코드가 중복되고 있습니다. 또한 Servlet(Spring MVC)와 Reactive(Spring WebFlux) 환경별로 다른 구현이 필요합니다.

## Decision
Common Library에 Spring Boot Auto-Configuration을 추가하여 JWT RBAC 설정을 자동화합니다.

### 구현 구조
```
services/common-library/src/main/java/.../security/
├── config/
│   └── JwtSecurityAutoConfiguration.java              # Auto-Configuration
└── converter/
    ├── JwtAuthenticationConverterAdapter.java         # Servlet용
    └── ReactiveJwtAuthenticationConverterAdapter.java # Reactive용
```

## Rationale
- **Zero Configuration**: 의존성 추가만으로 자동 적용, 보일러플레이트 제거 (각 서비스 20줄 → 0줄)
- **환경별 자동 감지**: `@ConditionalOnWebApplication`으로 Servlet/Reactive 자동 구분
- **커스터마이징 가능**: `@ConditionalOnMissingBean` 활용, 각 서비스에서 Bean 정의 시 자동 설정 비활성화
- **일관성 보장**: 모든 서비스에서 `roles` 클레임을 동일한 방식으로 처리, 권한 검증 로직 통일
- **유지보수 용이**: JWT 클레임 이름 변경 시 한 곳만 수정

## Trade-offs
✅ **장점**:
- 신규 서비스 개발 속도 향상 (SecurityConfig 작성 불필요)
- 코드 중복 제거 (5개 서비스 × 20줄 = 100줄 → 178줄 재사용 가능 코드)
- Spring Boot 철학 부합 (Convention over Configuration)

⚠️ **단점 및 완화**:
- Common Library와 서비스 간 결합도 증가 → (완화: Semantic Versioning, Breaking Change 시 Migration Guide 제공)
- Auto-Configuration 동작 원리 이해 필요 → (완화: `CLAUDE.md`에 설명 추가, IDE Auto-Configuration 탐색 활용)
- 디버깅 복잡도 증가 → (완화: 로그 레벨 DEBUG 설정 시 Auto-Configuration 로그 확인)

## Implementation
**Auto-Configuration 핵심 로직**:
```java
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationConverter.class)
public class JwtSecurityAutoConfiguration {

  // Servlet 환경
  @Bean
  @ConditionalOnWebApplication(type = SERVLET)
  @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    return JwtAuthenticationConverterAdapter.createDefault();
  }

  // Reactive 환경
  @Bean
  @ConditionalOnWebApplication(type = REACTIVE)
  @ConditionalOnMissingBean(name = "reactiveJwtAuthenticationConverter")
  public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
    return new ReactiveJwtAuthenticationConverterAdapter();
  }
}
```

**기본 권한 변환 규칙**:
- JWT 클레임: `roles` (예: `["ROLE_USER", "ROLE_ADMIN"]`)
- 권한 접두사: `` (빈 문자열, Auth-Service가 이미 `ROLE_` 포함)
- Spring Security `GrantedAuthority`: `SimpleGrantedAuthority("ROLE_USER")`

**서비스별 적용 현황**:
- API Gateway (Reactive): ✅ `ReactiveJwtAuthenticationConverterAdapter`
- Auth/Blog/Shopping/Notification (Servlet): ✅ `JwtAuthenticationConverterAdapter`

## References
- 관련 ADR: [ADR-003: Admin 권한 검증 전략](./ADR-003-authorization-strategy.md)
- Common Library: `/Users/laze/Laze/Project/portal-universe/services/common-library/README.md`
- Spring Security: Method Security 공식 문서

---

📂 상세: [old-docs/central/adr/ADR-004-jwt-rbac-auto-configuration.md](../old-docs/central/adr/ADR-004-jwt-rbac-auto-configuration.md)
