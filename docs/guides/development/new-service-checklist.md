---
id: new-service-checklist
title: Polyglot 신규 서비스 추가 체크리스트
type: guide
status: current
created: 2026-02-14
updated: 2026-02-14
author: Laze
tags: [polyglot, checklist, new-service, onboarding]
---

# Polyglot 신규 서비스 추가 체크리스트

**난이도**: ⭐⭐⭐ | **카테고리**: Development

> 언어/프레임워크에 관계없이 신규 서비스가 준수해야 할 cross-cutting concern 체크리스트.
> Java 상세 구현은 [new-service-guide.md](new-service-guide.md) 참조.

---

## 이 문서의 위치

```
이 체크리스트 (상위 개요, 스택 무관)
  └── new-service-guide.md (Java/Spring 상세 구현)
  └── Polyglot Overview (cross-cutting concern 매트릭스)
```

---

## 1. 서비스 설계

- [ ] 포트 번호 할당 (기존 포트와 충돌 없는지 확인)
- [ ] 기술 스택 선택 및 근거 문서화 (ADR 필요 시 작성)
- [ ] DB 선택 (MySQL / PostgreSQL / MongoDB / Stateless)
- [ ] API Gateway 경로 prefix 결정 (`/api/{service-name}/**`)

**참고**: [Polyglot Overview - 서비스별 기술 스택](../../architecture/system/polyglot-overview.md)

---

## 2. API 계약 준수

- [ ] 모든 응답을 `ApiResponse` wrapper로 감싸기 ([api-response.schema.json](../../contracts/api-response.schema.json))
- [ ] 에러 응답도 동일 구조 사용 ([error-response.schema.json](../../contracts/error-response.schema.json))
- [ ] 서비스 에러 코드 prefix 할당받기 ([error-codes.md](../../contracts/error-codes.md))
- [ ] 페이지네이션은 `PageResponse` 스키마 준수 ([page-response.schema.json](../../contracts/page-response.schema.json))
- [ ] GlobalExceptionHandler 구현 (스택별 패턴 참조)

**스택별 구현 참조**:
| 스택 | 참조 |
|------|------|
| Java/Spring | `common-lib` 의존성 추가 → 자동 적용 |
| NestJS | `prism-service`의 `AllExceptionsFilter`, `ApiResponseInterceptor` |
| Python | `chatbot-service`의 `exception_handlers.py`, `api_response.py` |

---

## 3. 인증/인가

- [ ] Gateway 헤더 파싱 구현 ([gateway-headers.md](../../contracts/gateway-headers.md))
  - `X-User-Id` (필수), `X-User-Effective-Roles` (필수)
  - `X-User-Memberships`, `X-User-Nickname` (선택)
- [ ] 공개 API와 인증 필요 API 구분
- [ ] 역할 기반 접근 제어 (RBAC) 적용

**스택별 구현 참조**:
| 스택 | 참조 |
|------|------|
| Java/Spring | `SecurityContextFilter` + `@PreAuthorize` |
| NestJS | `AuthGuard` + `@Roles()` decorator |
| Python | `get_current_user()` dependency + role check |

---

## 4. 보안

- [ ] XSS 필터링 적용 (입력 sanitize + 출력 escape)
- [ ] SQL Injection 방어 (ORM 사용, raw query 금지 원칙)
- [ ] Audit 로깅 (민감 작업: 생성/수정/삭제)
- [ ] CORS는 Gateway에서 처리 (서비스 레벨은 개발 환경용만)

**참고**: [ADR-029 Cross-cutting 보안 처리](../../adr/ADR-029-cross-cutting-security-layer.md)

---

## 5. 관찰성 (Observability)

- [ ] **Logging**: JSON 구조화 로깅 (traceId, spanId 포함)
- [ ] **Metrics**: Prometheus 메트릭 엔드포인트 (`/actuator/prometheus` 또는 `/metrics`)
- [ ] **Tracing**: Zipkin 분산 추적 (W3C Trace Context 전파)
- [ ] **Health**: Health check 엔드포인트 (`/actuator/health` 또는 `/health`)

**참고**: [ADR-033 Polyglot 관찰성 통일 전략](../../adr/ADR-033-polyglot-observability-strategy.md)

---

## 6. 이벤트 (Kafka 사용 시)

- [ ] Topic 명명: `portal.{domain}.{event}` 형식
- [ ] Topic constants 정의 (enum/class/module)
- [ ] Event contract JSON Schema 작성 (`docs/contracts/events/`)
- [ ] Dead Letter Queue 처리 전략 수립
- [ ] Consumer group ID: `{service-name}-group`

**참고**: [ADR-032 Kafka Configuration](../../adr/ADR-032-kafka-configuration-standardization.md), [ADR-038 Event Contract](../../adr/ADR-038-polyglot-event-contract-management.md)

---

## 7. 인프라

- [ ] `Dockerfile` 작성 (멀티스테이지 빌드)
- [ ] `docker-compose.yml` 및 `docker-compose-local.yml`에 서비스 추가
- [ ] API Gateway에 라우트 추가 (`application-{profile}.yml`)
- [ ] CI 워크플로우 추가 (`.github/workflows/`)
- [ ] K8s Deployment/Service manifest 작성 (해당시)

**참고**: [ADR-034 비Java 서비스 CI/CD 통합](../../adr/ADR-034-non-java-cicd-integration.md)

---

## 8. Frontend (MF Remote 추가 시)

- [ ] Module Federation `remoteEntry.js` 설정
- [ ] `shared` 의존성 설정 (React: `react`, `react-dom`, `react-dom/client` 필수)
- [ ] Design System 컴포넌트 사용 (raw HTML 금지)
- [ ] Portal Shell Host에 Remote 등록 (`vite.config.ts`, 라우트, 사이드바)
- [ ] Standalone 모드 지원 (Host 없이 독립 실행 가능)
- [ ] 다크모드 동기화

**참고**: [Module Federation Guide](module-federation-guide.md), [Onboarding - E: 신규 Frontend Remote 추가](onboarding-path.md)

---

## 9. 문서

- [ ] API 명세 작성 (`docs/api/{service-name}/`)
- [ ] 아키텍처 문서 작성 (`docs/architecture/{service-name}/`)
- [ ] `docs/contracts/error-codes.md`에 prefix 추가
- [ ] 주요 설계 결정이 있으면 ADR 작성 (`docs/adr/`)
- [ ] `docs/README.md` 서비스 테이블에 추가

**참고**: [Documentation Rules](../../../.claude/rules/documentation.md)

---

## 10. 최종 검증

- [ ] 전체 빌드 성공
- [ ] API Gateway 경유 API 호출 정상
- [ ] Health check 응답 확인
- [ ] Prometheus 메트릭 수집 확인 (`/metrics` 또는 `/actuator/prometheus`)
- [ ] Zipkin에 트레이스 표시 확인
- [ ] JSON 구조화 로그 출력 확인
- [ ] 에러 응답이 `ApiResponse` 스키마를 준수하는지 확인

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-14 | 초기 작성 - Polyglot 표준화 Phase 7 |

---

작성자: Laze
