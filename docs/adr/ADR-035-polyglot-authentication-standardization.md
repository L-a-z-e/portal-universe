# ADR-035: Polyglot 서비스 인증 표준화

**Status**: Accepted
**Date**: 2026-02-11
**Author**: Laze
**Supersedes**: -

## Context

Portal Universe는 Java(Spring Boot), NestJS, Python(FastAPI) 등 다양한 언어로 구성된 Polyglot 마이크로서비스 아키텍처입니다. API Gateway가 JWT 검증 후 `X-User-*` 헤더를 전파하는 구조이나, 언어별 인증 구현 방식에 차이가 있습니다. Java 서비스는 이중 보호(Gateway JWT 검증 + 서비스 자체 JWT 검증)를 하지만, NestJS와 Python 서비스는 Gateway 헤더만 신뢰하여 Gateway 우회 시 무인증 접근이 가능한 취약점이 존재합니다.

## Decision

**현재 "Gateway 헤더 신뢰 (Gateway-Primary Trust)" 모델을 유지하되, 네트워크 격리로 위험을 완화합니다.**

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 각 서비스에 JWT 검증 추가 (Java 방식 확대) | 완전한 이중 보호, Gateway 우회 방어 | JWKS 엔드포인트 의존, 언어별 검증 로직 중복, 성능 오버헤드 |
| ② Gateway 통과 증명 헤더 (HMAC 서명) | Gateway 우회 방어, JWT 의존 없음, 구현 단순 | 내부 시크릿 키 관리 필요, 추가 구현 비용 |
| ③ Service Mesh mTLS (Istio) | 언어 무관, 인프라 레벨 보안 | K8s 필수, 로컬 개발 복잡, 오버킬 |
| ④ Gateway 헤더 신뢰 + 네트워크 격리 (채택) | 변경 비용 제로, 로컬 개발 편의 | Gateway 우회 가능 (로컬), 네트워크 정책 필수 |

## Rationale

- **내부 네트워크 신뢰 모델**: Docker 네트워크와 K8s ClusterIP로 서비스는 클러스터 내부에서만 통신하며, API Gateway가 유일한 외부 진입점입니다.
- **로컬 개발 편의성 우선**: 로컬 환경에서 Gateway 우회 직접 호출은 개발자 생산성을 위한 의도적 허용입니다. 프로덕션 환경과의 격차는 NetworkPolicy로 해소합니다.
- **Zero Trust 전환 시점 재평가**: 향후 서비스가 외부에 직접 노출되거나 Zero Trust 아키텍처로 전환할 때 대안 ①(JWT 검증) 또는 ②(HMAC 증명)를 재검토합니다.
- **Java 서비스의 이중 검증은 유지**: 기존 구현을 제거하지 않으며, 이는 방어 심층화의 추가 계층으로 작동합니다.

## Trade-offs

✅ **장점**:
- 코드 변경 없이 현재 구조 유지, 구현 비용 제로
- 로컬 개발 환경에서 Gateway 없이도 서비스 직접 호출 가능 (개발 속도 향상)
- Java 서비스는 이미 이중 보호 제공

⚠️ **단점 및 완화**:
- Gateway 우회 시 `X-User-Id` 헤더만 세팅하면 인증 통과 → (완화: Docker/K8s 환경에서는 NetworkPolicy로 서비스 직접 접근 차단, 로컬 환경은 개발자 책임)
- 내부 서비스 간 호출 시 헤더 위조 가능 → (완화: 서비스 간 호출 시 Gateway를 경유하거나, 향후 Service Mesh 도입 검토)
- Zero Trust 원칙 위배 → (완화: 현재는 Perimeter Security 모델, 향후 Zero Trust 전환 시 ADR 개정)

## Implementation

### 1. API Gateway - JWT 검증 (1차 방어선)

**파일**: `services/api-gateway/src/main/java/com/portal/universe/apigateway/filter/JwtAuthenticationFilter.java`

- JWT 서명 검증 (HMAC, Key Rotation 지원)
- 토큰 블랙리스트 체크 (Redis)
- Role Hierarchy resolve → `X-User-Effective-Roles` 계산
- 검증 완료 후 `X-User-Id`, `X-User-Roles`, `X-User-Effective-Roles`, `X-User-Memberships`, `X-User-Nickname`, `X-User-Name` 헤더 추가
- **외부 주입 헤더 strip (Header Injection 방어)**: 113-122행에서 기존 `X-User-*` 헤더를 제거한 후 Gateway가 재설정

### 2. Java 서비스 - 이중 보호 (방어 심층화)

**파일**: `services/common-library/src/main/java/com/portal/universe/commonlibrary/security/`

- **Auto-Configuration**: `config/JwtSecurityAutoConfiguration.java` - Servlet/Reactive 환경 자동 감지, JWT 토큰 자체 검증
- **Converter**: `converter/JwtAuthenticationConverterAdapter.java` - JWT `roles` 클레임 → Spring Security `GrantedAuthority` 변환
- **Context**: `context/AuthUser.java` - Gateway 헤더 기반 사용자 정보, `@CurrentUser` resolver로 Controller 주입

### 3. NestJS prism-service - Gateway 헤더 신뢰 (단일 보호)

**파일**: `services/prism-service/src/common/guards/jwt-auth.guard.ts`

```typescript
// 보안 모델: API Gateway 헤더 신뢰 (Gateway-Primary Trust)
// - 1차 방어: API Gateway가 JWT 검증 완료 후 X-User-* 헤더 전파
// - 2차 방어: Docker/K8s NetworkPolicy로 직접 접근 차단
// - 로컬 환경: 개발 편의를 위해 Gateway 우회 허용 (개발자 책임)
const userId = request.headers['x-user-id'];
if (!userId) {
  throw new UnauthorizedException('User not authenticated');
}
```

- `X-User-Id` 헤더 존재 여부만 확인 (JWT 토큰 검증 없음)
- `X-User-Effective-Roles`, `X-User-Roles` 파싱 (fallback 순서)
- `X-User-Memberships` JSON 파싱 (enriched 구조)

### 4. Python chatbot-service - Gateway 헤더 신뢰 (단일 보호)

**파일**: `services/chatbot-service/app/core/security.py`

```python
def get_current_user_id(x_user_id: str | None = Header(None)) -> str:
    """Gateway에서 JWT 파싱 후 전달하는 X-User-Id 헤더에서 사용자 ID 추출.

    보안 모델: API Gateway 헤더 신뢰 (Gateway-Primary Trust)
    - API Gateway가 JWT 검증을 담당하므로 chatbot-service는 헤더만 신뢰합니다.
    """
    if not x_user_id:
        raise HTTPException(status_code=401, detail="Authentication required")
    return x_user_id
```

- `python-jose[cryptography]` 의존성은 있지만 JWT 검증 미사용
- 역할 기반 권한 확인: `require_admin()` → `X-User-Effective-Roles` / `X-User-Roles` fallback

### 5. 네트워크 격리 (위험 완화)

**Docker Compose (로컬)**:
- 비Java 서비스 포트는 외부 노출하지 않음 (내부 네트워크만)
- 개발자는 `localhost:8085`, `localhost:8086` 직접 호출 가능 (의도된 허용)

**Kubernetes (프로덕션)**:
- NetworkPolicy: API Gateway → 서비스만 허용
- ClusterIP 서비스로 외부 직접 접근 차단
- Ingress는 Gateway만 노출

## References

- [ADR-004: JWT RBAC Auto Configuration](./ADR-004-jwt-rbac-auto-configuration.md)
- [ADR-029: Cross-Cutting Security Layer](./ADR-029-cross-cutting-security-layer.md)
- `services/api-gateway/src/main/java/com/portal/universe/apigateway/filter/JwtAuthenticationFilter.java`
- `services/common-library/src/main/java/com/portal/universe/commonlibrary/security/`

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
