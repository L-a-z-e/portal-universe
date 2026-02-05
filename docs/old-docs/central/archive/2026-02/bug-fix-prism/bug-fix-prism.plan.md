# Plan: Bug Fix Prism Service

## 개요
| 항목 | 내용 |
|------|------|
| Feature | bug-fix-prism |
| 작성일 | 2026-02-04 |
| 작성자 | Claude |
| 상태 | Draft |

## 목적

Prism 서비스의 실제 시나리오 테스트를 통해 발견되는 버그를 수정한다.
E2E 테스트로 전체 플로우가 정상 동작하는지 검증한다.

## 테스트 시나리오

### Phase 0: 환경 구성 및 서비스 실행

#### 0.1 Infrastructure 실행
```bash
docker-compose -f docker-compose-local.yml up -d
```

필수 서비스:
- PostgreSQL (5432) - Prism DB
- MySQL (3307) - Auth/Blog/Shopping DB
- Redis (6379)
- Kafka (9092)
- MongoDB (27017) - Blog DB

#### 0.2 Backend 서비스 실행

| 서비스 | 포트 | 실행 방법 |
|--------|------|----------|
| api-gateway | 8080 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| auth-service | 8081 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| prism-service | 8085 | `npm run start:dev` (NestJS) |
| chatbot-service | 8086 | `python -m uvicorn app.main:app --reload --port 8086` |

#### 0.3 Frontend 실행
```bash
cd frontend
npm run build        # 디자인 시스템 빌드
npm run dev:portal   # Portal Shell (30000)
npm run dev:prism    # Prism Frontend (30003)
```

#### 0.4 Local LLM (Ollama) 확인
```bash
# Ollama 실행 여부 확인
curl http://localhost:11434/api/tags

# 모델 확인
ollama list
# 사용 가능: deepseek-r1:14b, nomic-embed-text:latest
```

### Phase 1: 로그인 테스트

**URL**: http://localhost:30000
**계정**: test@example.com / password123

검증 포인트:
- [ ] 로그인 페이지 접근
- [ ] 로그인 성공
- [ ] JWT 토큰 발급
- [ ] 사용자 정보 표시

### Phase 2: Prism 페이지 접근 테스트

#### 2.1 Navigation
- [ ] 사이드바에서 Prism 메뉴 접근
- [ ] `/prism` 경로 이동

#### 2.2 페이지 접근
| 페이지 | 경로 | 검증 |
|--------|------|------|
| Boards | `/prism/boards` | 보드 목록 표시 |
| Agents | `/prism/agents` | 에이전트 목록 표시 |
| Providers | `/prism/providers` | 프로바이더 목록 표시 |

### Phase 3: Provider 등록 테스트

#### 3.1 Ollama Provider 등록
| 필드 | 값 |
|------|-----|
| Name | Local Ollama |
| Type | ollama |
| Base URL | http://localhost:11434 |
| API Key | (불필요) |

검증 포인트:
- [ ] Provider 생성 API 호출 성공
- [ ] 목록에 표시
- [ ] 연결 테스트 (testConnection)
- [ ] 모델 목록 조회 (listModels)

#### 3.2 (선택) 외부 Provider 등록
- OpenAI: API Key 필요
- Anthropic: API Key 필요

### Phase 4: Agent 등록 테스트

#### 4.1 Agent 생성
| 필드 | 값 |
|------|-----|
| Name | Task Assistant |
| Role | ASSISTANT |
| Provider | Local Ollama (Phase 3에서 생성) |
| Model | deepseek-r1:14b |
| System Prompt | "You are a helpful assistant..." |
| Temperature | 0.7 |
| Max Tokens | 2048 |

검증 포인트:
- [ ] Agent 생성 API 호출 성공
- [ ] Provider 연결 확인
- [ ] 모델 선택 가능

### Phase 5: Board 및 Task 관리 테스트

#### 5.1 Board 생성
- [ ] 새 Board 생성
- [ ] Board 목록에 표시
- [ ] Board 상세 페이지 진입

#### 5.2 Task 생성
| 필드 | 값 |
|------|-----|
| Title | "Summarize this document" |
| Description | "Please provide a brief summary..." |
| Agent | Task Assistant (Phase 4에서 생성) |
| Priority | MEDIUM |

검증 포인트:
- [ ] Task 생성 성공
- [ ] 초기 상태: TODO
- [ ] Kanban 보드에 표시

### Phase 6: AI 실행 및 상태 전환 테스트

#### 6.1 Task 실행
- [ ] Task에서 "Execute" 버튼 클릭
- [ ] 상태 전환: TODO → IN_PROGRESS

#### 6.2 실행 완료
- [ ] AI 응답 수신
- [ ] 상태 전환: IN_PROGRESS → IN_REVIEW
- [ ] 응답 내용 표시

#### 6.3 Task 승인
- [ ] "Approve" 버튼 클릭
- [ ] 상태 전환: IN_REVIEW → DONE

#### 6.4 상태 머신 전체 플로우
```
TODO ──[execute]──> IN_PROGRESS ──[complete]──> IN_REVIEW ──[approve]──> DONE
  │                     │                          │
  └──[cancel]──>        └──[cancel]──>             ├──[retry]──> IN_PROGRESS
             CANCELLED              CANCELLED    └──[cancel]──> CANCELLED

DONE ──[reopen]──> TODO
```

### Phase 7: SSE 실시간 업데이트 테스트

검증 포인트:
- [ ] Execution 시작 이벤트 수신
- [ ] Execution 완료 이벤트 수신
- [ ] UI 실시간 업데이트

## 관련 코드 위치

### Backend (NestJS)
| 모듈 | 경로 | 역할 |
|------|------|------|
| Provider | `services/prism-service/src/modules/provider/` | AI Provider CRUD |
| Agent | `services/prism-service/src/modules/agent/` | Agent CRUD |
| Board | `services/prism-service/src/modules/board/` | Board CRUD |
| Task | `services/prism-service/src/modules/task/` | Task CRUD, State Machine |
| Execution | `services/prism-service/src/modules/execution/` | AI 실행 |
| AI | `services/prism-service/src/modules/ai/` | LLM Provider Factory |
| SSE | `services/prism-service/src/modules/sse/` | 실시간 이벤트 |

### Frontend (React)
| 페이지 | 경로 | 역할 |
|--------|------|------|
| ProvidersPage | `frontend/prism-frontend/src/pages/ProvidersPage.tsx` | Provider 관리 |
| AgentsPage | `frontend/prism-frontend/src/pages/AgentsPage.tsx` | Agent 관리 |
| BoardListPage | `frontend/prism-frontend/src/pages/BoardListPage.tsx` | Board 목록 |
| BoardPage | `frontend/prism-frontend/src/pages/BoardPage.tsx` | Kanban 보드 |

### AI Providers
| Provider | 경로 | Base URL |
|----------|------|----------|
| Ollama | `modules/ai/providers/ollama.provider.ts` | localhost:11434 |
| OpenAI | `modules/ai/providers/openai.provider.ts` | api.openai.com |
| Anthropic | `modules/ai/providers/anthropic.provider.ts` | api.anthropic.com |

## 예상 문제점

### 1. Ollama 연결 문제
- Ollama 미설치 또는 미실행
- 모델 미다운로드
- Docker 네트워크 내 접근 불가 (localhost vs host.docker.internal)

### 2. CORS 문제
- prism-service CORS 설정 누락
- API Gateway 라우팅 문제

### 3. 인증 문제
- JWT 토큰 전달 누락
- CurrentUser 데코레이터 오류

### 4. 상태 전환 문제
- Task State Machine 로직 오류
- Execution 완료 후 Task 상태 미갱신

### 5. SSE 연결 문제
- SSE 연결 끊김
- 이벤트 미전달

## 테스트 방법

### Playwright 브라우저 테스트
```bash
# MCP Playwright 사용
# 브라우저 열기 → 로그인 → 시나리오 진행
```

### API 직접 테스트
```bash
# Provider 목록
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/prism/providers

# Agent 목록
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/prism/agents

# Board 목록
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/prism/boards
```

## 성공 기준

| 항목 | 기준 |
|------|------|
| 로그인 | 성공 |
| 페이지 접근 | boards, agents, providers 모두 접근 가능 |
| Provider 등록 | Ollama 연결 성공 |
| Agent 생성 | Provider 연결 상태로 생성 |
| Task 실행 | AI 응답 수신 |
| 상태 전환 | TODO → IN_PROGRESS → IN_REVIEW → DONE 정상 |
| SSE | 실시간 업데이트 동작 |

## 다음 단계

1. Phase 0-7 테스트 진행
2. 발견된 버그 기록
3. 버그 수정 구현
4. Gap Analysis (`/pdca analyze bug-fix-prism`)

---
*Generated by bkit PDCA Skill*
