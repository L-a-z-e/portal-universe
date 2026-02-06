# Architecture Documentation

> API Gateway의 아키텍처 문서 목록

---

## 문서 목록

| ID | 제목 | 상태 | 최종 업데이트 |
|----|------|------|--------------|
| [arch-system-overview](./system-overview.md) | System Overview | ✅ Current | 2026-02-06 |
| [arch-data-flow](./data-flow.md) | Data Flow | ✅ Current | 2026-02-06 |

---

## 문서 설명

### [System Overview](./system-overview.md)
API Gateway의 전체 시스템 구조를 설명합니다.

**포함 내용**:
- High-Level Architecture (7개 백엔드 서비스, Redis, 필터 체인)
- 핵심 컴포넌트 12개 상세 (SecurityConfig, JwtAuthenticationFilter, RateLimiterConfig 등)
- 필터 체인 실행 순서 (Request 9단계, Response 5단계)
- 보안 모델 (HMAC-SHA256 JWT, RBAC, Token Blacklist, Header Sanitization)
- 라우팅 규칙 (27개 라우트, 경로 변환, 환경별 URL)
- 기술 스택 (build.gradle 기반 실제 의존성)
- Circuit Breaker 설정 (5개 인스턴스, 서비스별 타임아웃)
- 배포 및 운영 (local/docker/kubernetes 프로필)
- 트러블슈팅 (CORS, JWT, Rate Limit, CB)

---

### [Data Flow](./data-flow.md)
API Gateway의 주요 데이터 흐름을 Mermaid sequence diagram으로 설명합니다.

**포함 내용**:
- 인증된 API 요청 전체 흐름 (필터 체인 통과 과정)
- JWT 토큰 검증 상세 흐름 (kid 추출 → 키 조회 → HMAC 검증 → blacklist → claims)
- 공개 경로 요청 흐름 (permitAll vs permitAllGet vs skipJwtParsing 차이)
- Rate Limiting 흐름 (KeyResolver → Redis Token Bucket → Allow/429)
- Circuit Breaker 상태 전이 (CLOSED → OPEN → HALF_OPEN)
- Health Aggregation 흐름 (병렬 WebClient → K8s enrichment)

---

## 읽는 순서 (추천)

1. **신규 팀원 온보딩**:
   ```
   System Overview (섹션 1-3) → Data Flow (섹션 1) → API 문서들
   ```

2. **보안 구조 이해**:
   ```
   System Overview (섹션 4.2, 6) → Data Flow (섹션 2, 3) → Security API 문서
   ```

3. **Rate Limiting 이해**:
   ```
   System Overview (섹션 4.5) → Data Flow (섹션 4) → Rate Limiting API 문서
   ```

4. **장애 대응**:
   ```
   System Overview (섹션 9, 11) → Data Flow (섹션 5) → Resilience API 문서
   ```

5. **운영/모니터링**:
   ```
   System Overview (섹션 4.9, 10) → Data Flow (섹션 6) → Health Monitoring API 문서
   ```

---

## 관련 문서

### API 문서
- [API Gateway README](../../api/api-gateway/README.md) - API 문서 인덱스
- [Routing Specification](../../api/api-gateway/routing-specification.md) - 라우팅 규칙 상세
- [Security & Authentication](../../api/api-gateway/security-authentication.md) - JWT 인증 상세
- [Rate Limiting](../../api/api-gateway/rate-limiting.md) - Rate Limiting 설정 상세
- [Resilience](../../api/api-gateway/resilience.md) - Circuit Breaker 상세
- [Error Reference](../../api/api-gateway/error-reference.md) - 에러 코드 레퍼런스
- [Health Monitoring](../../api/api-gateway/health-monitoring.md) - Health Check 상세

### 시스템 아키텍처
- [Auth Service Architecture](../auth-service/system-overview.md) - JWT 발급 서비스
- [Shopping Service Architecture](../shopping-service/system-overview.md) - 쇼핑 서비스

### 트러블슈팅
- [TS-20260129-005: React Error #321](../../troubleshooting/2026/01/TS-20260129-005-react-error-321-module-federation.md)

---

## 문서 작성 규칙

새로운 아키텍처 문서를 추가할 때:

1. **파일명**: `[kebab-case].md` (예: `cache-strategy.md`)
2. **메타데이터**: 필수 YAML frontmatter 포함 (`id`, `title`, `type`, `status`, `created`, `updated`, `author`, `tags`, `related`)
3. **다이어그램**: Mermaid 사용 권장
4. **README 업데이트**: 이 인덱스 파일에 문서 추가
5. **관련 문서 링크**: 양방향 링크 유지
6. **코드 검증**: 문서에 언급된 모든 클래스/설정이 코드에 존재하는지 확인

---

**최종 업데이트**: 2026-02-06
