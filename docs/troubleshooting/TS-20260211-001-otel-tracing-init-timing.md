# TS-20260211-001: OTel Tracing 초기화 타이밍 이슈

**심각도**: 🟡 Medium
**상태**: Resolved
**영향 서비스**: chatbot-service, prism-service

| 항목 | 내용 |
|------|------|
| **발생일시** | 2026-02-11 |
| **해결일시** | 2026-02-11 |
| **담당자** | Laze |

## 증상 (Symptoms)

두 서비스에서 OpenTelemetry tracing이 예상대로 작동하지 않는 문제가 발생했다.

**Issue 1: chatbot-service (Python/FastAPI)**
- chatbot-service에서 HTTP 요청 처리 후 Zipkin UI에 span이 나타나지 않음
- 로그에는 "Zipkin tracing enabled" 메시지가 출력됨
- 에러 메시지: 없음 (silent failure)
- 영향 범위: 로컬 및 Docker/K8s 환경에서 chatbot-service의 모든 HTTP 트레이스 유실

**Issue 2: prism-service (NestJS)**
- local 환경에서 `NODE_ENV=local`일 때 tracing이 항상 비활성화됨
- Docker/K8s 환경에서도 Zipkin endpoint가 미설정되어 span 유실
- 영향 범위: 모든 환경에서 prism-service의 분산 추적 불가

## 원인 (Root Cause)

**공통 원인**: OTel instrumentation 초기화 타이밍과 환경 설정 불일치

**Issue 1 근본 원인**:
1. `FastAPIInstrumentor.instrument_app(app)`를 lifespan 내부에서 호출 → ASGI 미들웨어 래핑 타이밍 이슈로 span 미생성
2. `TracerProvider`가 app 생성 이후에 설정됨 (lifespan은 app 시작 시 실행)
3. shutdown 시 `force_flush()` 없음 → BatchSpanProcessor 버퍼의 span 유실

**Issue 2 근본 원인**:
1. `instrumentation.ts`에서 `NODE_ENV !== 'local'`로 tracing 제어 → NODE_ENV가 인프라 설정(DB host 등)과 tracing을 결합
2. `instrumentation.ts`가 ConfigModule 로드 전에 실행 → `.env.*` 파일의 OTEL 설정을 읽지 못함
3. `.env.docker`/`.env.k8s`에 Zipkin endpoint 미정의 → 기본값 localhost:9411이 Docker/K8s에서 작동 안 함

**분석 과정**:
1. Zipkin UI에서 span 검색 시 chatbot/prism service 트레이스가 누락된 것을 확인
2. chatbot-service: lifespan 내부 instrumentation 코드 검토 → app 생성 이후 TracerProvider 설정 발견
3. prism-service: `instrumentation.ts` 코드 검토 → `NODE_ENV !== 'local'` 조건과 dotenv 미호출 발견
4. 환경변수 파일(`.env.local`, `.env.docker`, `.env.k8s`) 검토 → Zipkin endpoint 누락 확인

## 해결 방법 (Solution)

### Issue 1: chatbot-service 수정

**즉시 조치**
```bash
# 트레이싱 활성화하여 서비스 재시작
cd services/chatbot-service
TRACING_ENABLED=true uvicorn app.main:app --reload --port 8086
```

**영구 조치**
1. `app/core/telemetry.py` 수정:
   - `setup_telemetry()` + `FastAPIInstrumentor.instrument()` 호출을 모듈 최상단으로 이동
   - `instrument_app(app)` 대신 `instrument()` (전역 모드) 사용
   - `shutdown_telemetry()`에서 `force_flush()` + `shutdown()` 호출

2. `app/main.py` 수정:
   - lifespan 함수에서 telemetry 초기화 제거
   - shutdown hook에서 `shutdown_telemetry()` 호출

**수정 파일**:
- `services/chatbot-service/app/core/telemetry.py`
- `services/chatbot-service/app/main.py`

### Issue 2: prism-service 수정

**즉시 조치**
```bash
# 로컬에서 tracing 활성화
cd services/prism-service
OTEL_TRACES_EXPORTER=zipkin npm run start:dev
```

**영구 조치**
1. `src/instrumentation.ts` 수정:
   - 최상단에서 `dotenv.config()` 호출 (ConfigModule과 동일한 `.env.{NODE_ENV}` 로드)
   - tracing 제어를 `OTEL_TRACES_EXPORTER` 환경변수로 변경 (NODE_ENV에서 분리)

2. 환경변수 파일 업데이트:
   - `.env.local`: `OTEL_TRACES_EXPORTER=none` 추가 (기본 OFF)
   - `.env.docker`: `OTEL_TRACES_EXPORTER=zipkin`, `OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans` 추가
   - `.env.k8s`: `OTEL_TRACES_EXPORTER=zipkin`, `OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://zipkin-service:9411/api/v2/spans` 추가
   - `.env.*.example`: 위 설정 반영

**수정 파일**:
- `services/prism-service/src/instrumentation.ts`
- `services/prism-service/.env.local`
- `services/prism-service/.env.docker`
- `services/prism-service/.env.k8s`
- `services/prism-service/.env.*.example`

## 재발 방지 (Prevention)

- [ ] Observability E2E 테스트에 chatbot-service span 검증 추가
- [ ] prism-service E2E 테스트에 tracing 활성화 검증 추가
- [ ] 새 서비스 추가 시 OTel 초기화 타이밍 체크리스트 추가 (문서화)
- [ ] 환경변수 템플릿(`.env.*.example`)에 OTEL 필수 설정 항목 포함

## 학습 포인트

1. **OTel instrumentation 초기화는 app 생성 전에 완료해야 한다**
   - ASGI/Express 미들웨어 래핑은 app 인스턴스 생성 시점에 발생하므로, tracer가 먼저 설정되어야 span이 생성된다.

2. **환경 구분(NODE_ENV)과 기능 제어(tracing on/off)는 별도 변수로 분리해야 한다**
   - NODE_ENV는 인프라 설정 선택에만 사용하고, 기능 토글은 전용 환경변수를 사용한다.

3. **Node.js의 `--require instrumentation.ts`는 NestJS ConfigModule보다 먼저 실행된다**
   - dotenv로 직접 env 파일을 로드해야 한다.

4. **BatchSpanProcessor는 flush 없이 종료하면 버퍼 데이터가 유실된다**
   - shutdown hook에서 `force_flush()` 호출 필수.

## 환경별 동작 매트릭스 (수정 후)

| 시나리오 | 명령 | tracing | endpoint |
|---------|------|---------|----------|
| prism local 기본 | `npm run start:dev` | OFF | - |
| prism local + tracing | `OTEL_TRACES_EXPORTER=zipkin npm run start:dev` | ON | localhost:9411 |
| prism docker | `NODE_ENV=docker npm run start:prod` | ON | zipkin:9411 |
| prism k8s | `NODE_ENV=k8s npm run start:prod` | ON | zipkin-service:9411 |
| chatbot local 기본 | `uvicorn app.main:app --reload --port 8086` | OFF | - |
| chatbot local + tracing | `TRACING_ENABLED=true uvicorn ...` | ON | localhost:9411 |
| chatbot docker | `docker compose up chatbot-service` | ON | zipkin:9411 |

---

## 관련 문서
- [ADR-033: Polyglot 통합 Observability 아키텍처](../adr/ADR-033-polyglot-observability.md)
- [Zipkin-ES8 Snappy 압축 문제](./TS-20260211-001-zipkin-es8-snappy.md)
- [Observability 운영 가이드](../guides/observability-guide.md)

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
