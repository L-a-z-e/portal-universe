# ADR-017: Prism Basic - AI Agent 기반 칸반 시스템 아키텍처

**Status**: Accepted
**Date**: 2026-02-01
**Source**: PDCA archive (prism-basic)

## Context

AI 에이전트가 태스크를 자동으로 실행하는 프로젝트 관리 도구를 구현해야 했다. 사용자가 여러 AI Provider(OpenAI, Anthropic, Ollama)를 등록하고, Provider별로 Agent를 정의한 후, Board의 Task에 Agent를 할당하여 실행할 수 있어야 한다. 또한 실행 결과를 실시간으로 전달하고, NestJS 서비스를 Portal Universe의 Spring Gateway와 통합해야 했다.

## Decision

**NestJS + PostgreSQL + Factory Pattern**으로 다중 AI Provider를 지원하고, **SSE**로 실시간 스트리밍을 제공한다.

## Rationale

- **NestJS 선택**: Portal Universe의 유일한 Non-Java 서비스, TypeScript로 AI SDK 통합 용이
- **Factory Pattern**: `AIProviderFactory.create(type)`로 OpenAI/Anthropic/Ollama 제공자를 추상화, 각 Provider는 `LLMProvider` 인터페이스 구현
- **Ollama는 OpenAI SDK 재사용**: OpenAI 호환 API를 제공하므로 별도 SDK 없이 `baseURL` 변경으로 지원
- **PostgreSQL**: 5개 테이블(ai_providers, agents, boards, tasks, executions) + 5개 ENUM 타입으로 상태 관리
- **SSE (Server-Sent Events)**: `/api/v1/prism/sse/boards/:boardId`로 task_created, execution_started 등 8가지 이벤트 스트리밍

## Trade-offs

✅ **장점**:
- 다중 AI Provider 지원으로 vendor lock-in 회피
- Ollama 지원으로 로컬 AI 실행 가능 (API 키 불필요)
- SSE로 WebSocket 없이 단방향 실시간 통신 구현
- Factory Pattern으로 신규 Provider 추가 용이

⚠️ **단점 및 완화**:
- NestJS ↔ Spring Gateway 이질성 → API 경로 컨벤션 통일 (`/api/v1/prism/**`)
- AI 응답 대기 시간 → API Gateway에 60초 Circuit Breaker 설정
- SSE 연결 유지 → Heartbeat 30초 간격, 타임아웃 5분 설정

## Implementation

- **Provider Factory**: `services/prism-service/src/modules/ai/ai-provider.factory.ts`
- **Ollama Provider**: `modules/ai/providers/ollama.provider.ts` (OpenAI SDK 재사용)
- **SSE Controller**: `modules/sse/sse.controller.ts` - 8가지 이벤트 타입
- **Database Schema**: `infrastructure/init-scripts/init-prism.sql` - 5 테이블
- **E2E 테스트**: `e2e-tests/tests/prism/` - 6개 spec 파일 (~32 cases)

## References

- PDCA: `pdca/archive/2026-02/prism-basic/`
- DB 스키마: PostgreSQL 18, ENUM 기반 상태 머신
- API: 33개 endpoints (Provider CRUD, Agent, Board, Task, Execution, SSE)
