# ADR-037: NestJS(Prism) 서비스 장기 스택 전략

**Status**: Proposed
**Date**: 2026-02-11
**Author**: Laze
**Supersedes**: -

## Context

Portal Universe는 Java/Spring 중심의 마이크로서비스 플랫폼으로, prism-service(NestJS, ~8,568 LOC)가 유일한 비Java 백엔드 서비스다. ADR-017에서 "TypeScript로 AI SDK 통합 용이"를 근거로 NestJS를 선택했으나, Java에서도 OpenAI/Anthropic/Ollama를 REST API로 동등하게 호출 가능하다. 현재 Java 5개 서비스는 `common-library`(JWT, ApiResponse, 예외 처리, 감사 로그), Gradle 멀티모듈(events 모듈 공유), Logback JSON 로깅, Flyway 마이그레이션을 공유하지만, prism-service는 이러한 플랫폼 투자를 전혀 활용하지 못하고 있다. ADR-033(관찰성), ADR-034(CI/CD), ADR-036(DB 마이그레이션) 등으로 격차를 해소할 수 있으나, 장기적으로 NestJS를 유지할 것인지 Java로 전환할 것인지 결정이 필요하다.

## Decision

**NestJS를 유지하되, ADR-033~036의 플랫폼 계층 보강을 전제로 한다.** 8,568 LOC 재작성 비용 대비, 플랫폼 격차를 줄이는 것이 효율적이다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① NestJS 유지 + 플랫폼 보강 (채택) | 기존 코드 보존, AI SDK 타입 지원 활용, Polyglot 학습 가치 | 플랫폼 보강 비용 (ADR-033~036 구현), Java와 완전 동등 어려움 |
| ② Java/Spring 점진 전환 | common-library 활용, Gradle 통합, Flyway 마이그레이션, 단일 스택 운영 효율 | 8,568 LOC 재작성 비용 (수주~수월), 전환 중 두 버전 유지 |
| ③ NestJS 표준 스택 격상 | TypeScript 풀스택 (Frontend + Backend) | Java 플랫폼 투자 무력화, 5개 서비스와 불일치 확대 |

## Rationale

- **재작성 비용 회피**: 8,568줄 TypeScript를 Java로 전환 시 수주~수월 소요, 전환 중 기능 동결, 테스트 재작성, 리스크 증가
- **플랫폼 격차 해소 가능**: ADR-033(Winston JSON 로그, OTel 추적, nestjs-prom 메트릭), ADR-034(prism-ci.yml, docker.yml matrix 추가), ADR-036(TypeORM Migration CLI)으로 주요 격차를 70-80% 해소 가능
- **AI SDK 타입 지원**: `@anthropic-ai/sdk`, `openai`는 TypeScript First이므로 자동완성, 타입 체크, 스트리밍 처리가 자연스러움
- **Polyglot 학습 가치**: 단일 스택보다 다양한 기술 스택 경험이 아키텍처 설계 역량 향상에 기여
- **Java로 전환해도 이점 제한적**: common-library의 JWT, ApiResponse 등은 프레임워크 독립적이지 않아 NestJS에 그대로 사용 불가 (각 프레임워크에 맞춰 재구현 필요)
- **전환 재검토 트리거 명확**: common-library의 새 기능을 prism-service가 지속적으로 재구현해야 하는 상황 발생 시 전환 재평가

## Trade-offs

✅ **장점**:
- 8,568 LOC 보존으로 수주~수월의 재작성 비용 절감
- 33개 API, 5개 DB 테이블, E2E 테스트의 재작성/검증 불필요
- AI SDK의 풍부한 TypeScript 타입 지원 계속 활용
- Prism-frontend(React)와의 TypeScript 풀스택 시너지
- Polyglot 환경에서의 아키텍처 설계 경험 축적

⚠️ **단점 및 완화**:
- Java와 완전 동등한 플랫폼 수준 달성 어려움 → (완화: ADR-033~036으로 운영에 필수적인 관찰성, CI/CD, DB 마이그레이션은 달성. JWT 보안 등 고급 기능은 NestJS Guard/Interceptor로 자체 구현)
- 플랫폼 보강 비용 (Winston, OTel, nestjs-prom 설정) → (완화: 약 300-400 LOC 추가, 8,568 LOC 재작성 대비 5% 미만 비용)
- common-library의 새 기능을 prism에서 재구현 필요 → (완화: 현재 common-library 기능(exception, response, security, util 4개 패키지)이 안정 상태, 신규 기능 추가 빈도 낮음. 재구현 비용이 누적되면 전환 재검토)
- 팀 내 NestJS 전문성이 Java 대비 낮을 수 있음 → (완화: NestJS는 Spring과 유사한 DI, Decorator 구조로 Java 개발자 학습 곡선 완만. 1-2주 내 생산성 확보 가능)

## Implementation

이 ADR은 전략 결정이므로 직접 구현 파일은 없으며, 다음 ADR의 구현에 의존한다:

- **ADR-033 구현 결과**: `services/prism-service/src/config/logger.config.ts` (Winston JSON), `src/main.ts` (OTel SDK), `src/app.module.ts` (PrometheusModule)
- **ADR-034 구현 결과**: `.github/workflows/prism-ci.yml`, `.github/workflows/docker.yml` (matrix 수정)
- **ADR-036 구현 결과**: `services/prism-service/src/config/typeorm.config.ts`, `src/migrations/`, `package.json` (migration scripts)
- **ADR-035 검토 결과**: (향후 결정 시 추가)

### 전환 재검토 기준

다음 조건 중 하나라도 발생 시 Java 전환을 재평가한다:

1. common-library에 분기당 2회 이상 신규 기능이 추가되어, prism-service에서 지속적으로 재구현 부담 발생
2. Prism 서비스가 Kafka 이벤트 기반 통신을 Java 서비스와 동등한 수준으로 구현해야 하는 요구사항 발생 (현재는 독립 서비스)
3. 팀 내 NestJS 유지보수 전문성 부족으로 장애 대응 지연이 반복 (분기당 2회 이상)

## References

- [ADR-017: Prism Basic Implementation](./ADR-017-prism-basic-implementation.md) - 최초 NestJS 선택 근거
- [ADR-033: Polyglot Observability Strategy](./ADR-033-polyglot-observability-strategy.md) - NestJS 관찰성 통합 방안
- [ADR-034: Non-Java CI/CD Integration](./ADR-034-non-java-cicd-integration.md) - prism-service CI/CD 파이프라인
- [ADR-036: Prism DB Migration Strategy](./ADR-036-prism-db-migration-strategy.md) - TypeORM Migration CLI 채택
- `services/prism-service/` - 전체 코드베이스 (87개 TS 파일, 8,568 LOC)
- `services/common-library/src/main/java/com/portal/universe/commonlibrary/` - Java 공유 라이브러리 (exception, response, security, util)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
