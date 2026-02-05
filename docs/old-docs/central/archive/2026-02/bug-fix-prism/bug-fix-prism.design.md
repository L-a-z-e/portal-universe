# Design: Bug Fix Prism Service

## 개요
| 항목 | 내용 |
|------|------|
| Feature | bug-fix-prism |
| 작성일 | 2026-02-04 |
| Plan 참조 | `docs/pdca/01-plan/features/bug-fix-prism.plan.md` |
| 상태 | Draft |

## 1. 테스트 환경 설계

### 1.1 서비스 실행 순서

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 0: Infrastructure                                      │
├─────────────────────────────────────────────────────────────┤
│ docker-compose -f docker-compose-local.yml up -d            │
│   ├── PostgreSQL (5432) ─── Prism DB                        │
│   ├── MySQL (3307) ─────── Auth/Blog/Shopping DB            │
│   ├── Redis (6379)                                          │
│   ├── Kafka (9092)                                          │
│   └── MongoDB (27017) ──── Blog DB                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Phase 0: Backend Services (순차 실행)                        │
├─────────────────────────────────────────────────────────────┤
│ 1. api-gateway (8080)     - ./gradlew bootRun (local)       │
│ 2. auth-service (8081)    - ./gradlew bootRun (local)       │
│ 3. prism-service (8085)   - npm run start:dev               │
│ 4. chatbot-service (8086) - uvicorn                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Phase 0: Frontend                                            │
├─────────────────────────────────────────────────────────────┤
│ cd frontend                                                  │
│ 1. npm run build           - 디자인 시스템 빌드              │
│ 2. npm run dev:portal      - Portal Shell (30000)           │
│ 3. npm run dev:prism       - Prism Frontend (30003)         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Phase 0: Local LLM (Ollama)                                  │
├─────────────────────────────────────────────────────────────┤
│ ollama serve               - Ollama 서버 실행 (11434)        │
│ ollama pull llama3         - 모델 다운로드                   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 헬스체크 명령어

```bash
# Infrastructure
docker ps --format "table {{.Names}}\t{{.Status}}"

# Backend Services
curl -s http://localhost:8080/actuator/health | jq .status  # api-gateway
curl -s http://localhost:8081/actuator/health | jq .status  # auth-service
curl -s http://localhost:8085/api/v1/health | jq .status    # prism-service

# Frontend
curl -s http://localhost:30000 > /dev/null && echo "OK"     # portal-shell
curl -s http://localhost:30003 > /dev/null && echo "OK"     # prism-frontend

# Ollama
curl -s http://localhost:11434/api/tags | jq .models        # ollama
```

## 2. 테스트 시나리오 상세 설계

### 2.1 Phase 1: 로그인 테스트

```
┌──────────────────────────────────────────────────────────┐
│ Step 1: Navigate to Login                                 │
│   URL: http://localhost:30000                            │
│   Expected: 로그인 페이지 표시                            │
├──────────────────────────────────────────────────────────┤
│ Step 2: Enter Credentials                                 │
│   Email: test@example.com                                │
│   Password: password123                                  │
├──────────────────────────────────────────────────────────┤
│ Step 3: Submit Login                                      │
│   Action: Click "로그인" 버튼                             │
│   Expected: Dashboard 리다이렉트                          │
├──────────────────────────────────────────────────────────┤
│ Step 4: Verify Auth State                                 │
│   Check: localStorage에 accessToken 존재                  │
│   Check: 사용자 이름 표시 (Header)                        │
└──────────────────────────────────────────────────────────┘
```

### 2.2 Phase 2: Prism 페이지 접근

```
┌──────────────────────────────────────────────────────────┐
│ Step 1: Navigate to Prism                                 │
│   Action: 사이드바 "Prism" 메뉴 클릭                      │
│   Expected: /prism 경로 이동                              │
├──────────────────────────────────────────────────────────┤
│ Step 2: Verify Boards Page                                │
│   URL: /prism/boards                                     │
│   Expected: 보드 목록 또는 빈 상태 표시                   │
├──────────────────────────────────────────────────────────┤
│ Step 3: Navigate to Agents                                │
│   URL: /prism/agents                                     │
│   Expected: 에이전트 목록 또는 빈 상태 표시               │
├──────────────────────────────────────────────────────────┤
│ Step 4: Navigate to Providers                             │
│   URL: /prism/providers                                  │
│   Expected: 프로바이더 목록 또는 빈 상태 표시             │
└──────────────────────────────────────────────────────────┘
```

### 2.3 Phase 3: Provider 등록

```
┌──────────────────────────────────────────────────────────┐
│ Step 1: Open Create Provider Modal                        │
│   Action: "Add Provider" 버튼 클릭                        │
│   Expected: 생성 모달/폼 표시                             │
├──────────────────────────────────────────────────────────┤
│ Step 2: Fill Provider Form                                │
│   Name: "Local Ollama"                                   │
│   Type: "ollama" (드롭다운 선택)                         │
│   Base URL: "http://localhost:11434"                     │
│   API Key: (빈값 또는 "ollama")                          │
├──────────────────────────────────────────────────────────┤
│ Step 3: Submit Provider                                   │
│   Action: "Create" 버튼 클릭                              │
│   Expected: 성공 메시지, 목록에 추가                      │
├──────────────────────────────────────────────────────────┤
│ Step 4: Test Connection                                   │
│   Action: "Test Connection" 버튼 클릭                     │
│   Expected: "Connection successful" 표시                  │
├──────────────────────────────────────────────────────────┤
│ Step 5: Fetch Models                                      │
│   Action: (자동 또는 수동) 모델 목록 조회                 │
│   Expected: llama3 등 설치된 모델 표시                    │
└──────────────────────────────────────────────────────────┘
```

### 2.4 Phase 4: Agent 생성

```
┌──────────────────────────────────────────────────────────┐
│ Step 1: Navigate to Agents                                │
│   URL: /prism/agents                                     │
├──────────────────────────────────────────────────────────┤
│ Step 2: Open Create Agent Modal                           │
│   Action: "Add Agent" 버튼 클릭                           │
├──────────────────────────────────────────────────────────┤
│ Step 3: Fill Agent Form                                   │
│   Name: "Task Assistant"                                 │
│   Role: "ASSISTANT" (드롭다운)                           │
│   Provider: "Local Ollama" (드롭다운)                    │
│   Model: "deepseek-r1:14b" (드롭다운, Provider 선택 후 활성화) │
│   System Prompt: "You are a helpful task assistant..."   │
│   Temperature: 0.7                                       │
│   Max Tokens: 2048                                       │
├──────────────────────────────────────────────────────────┤
│ Step 4: Submit Agent                                      │
│   Action: "Create" 버튼 클릭                              │
│   Expected: 성공 메시지, 목록에 추가                      │
└──────────────────────────────────────────────────────────┘
```

### 2.5 Phase 5: Board 및 Task 생성

```
┌──────────────────────────────────────────────────────────┐
│ Step 1: Create Board                                      │
│   Action: "New Board" 버튼 클릭                           │
│   Name: "Test Board"                                     │
│   Description: "테스트용 보드"                            │
│   Expected: 보드 생성, 목록에 표시                        │
├──────────────────────────────────────────────────────────┤
│ Step 2: Enter Board                                       │
│   Action: 보드 클릭                                       │
│   Expected: Kanban 보드 화면 (TODO/IN_PROGRESS/...)       │
├──────────────────────────────────────────────────────────┤
│ Step 3: Create Task                                       │
│   Action: "Add Task" 버튼 클릭                            │
│   Title: "Summarize this document"                       │
│   Description: "Please provide a brief summary..."       │
│   Agent: "Task Assistant" (드롭다운)                     │
│   Priority: "MEDIUM"                                     │
├──────────────────────────────────────────────────────────┤
│ Step 4: Verify Task                                       │
│   Expected: TODO 컬럼에 Task 카드 표시                    │
│   Expected: Agent 이름 표시                               │
└──────────────────────────────────────────────────────────┘
```

### 2.6 Phase 6: AI 실행 및 상태 전환

```
┌──────────────────────────────────────────────────────────┐
│ Step 1: Execute Task                                      │
│   Action: Task 카드의 "Execute" 버튼 클릭                 │
│   Expected: 상태 TODO → IN_PROGRESS                       │
│   Expected: 로딩 인디케이터 표시                          │
├──────────────────────────────────────────────────────────┤
│ Step 2: Wait for Completion                               │
│   Timeout: 60초 (Ollama 응답 대기)                        │
│   Expected: AI 응답 수신                                  │
│   Expected: 상태 IN_PROGRESS → IN_REVIEW                  │
├──────────────────────────────────────────────────────────┤
│ Step 3: View Execution Result                             │
│   Action: Task 카드 클릭 (상세 보기)                      │
│   Expected: AI 응답 내용 표시                             │
│   Expected: 토큰 사용량 표시                              │
├──────────────────────────────────────────────────────────┤
│ Step 4: Approve Task                                      │
│   Action: "Approve" 버튼 클릭                             │
│   Expected: 상태 IN_REVIEW → DONE                         │
├──────────────────────────────────────────────────────────┤
│ Step 5: (Optional) Reopen/Retry                           │
│   Action: "Reopen" 버튼 클릭                              │
│   Expected: 상태 DONE → TODO                              │
└──────────────────────────────────────────────────────────┘
```

### 2.7 Phase 7: SSE 실시간 업데이트

```
┌──────────────────────────────────────────────────────────┐
│ 검증 포인트                                               │
├──────────────────────────────────────────────────────────┤
│ 1. SSE 연결 확인                                          │
│    - DevTools Network 탭에서 EventStream 확인             │
│    - /api/v1/prism/sse/boards/{boardId} 연결              │
├──────────────────────────────────────────────────────────┤
│ 2. 이벤트 수신 확인                                       │
│    - execution:started 이벤트                             │
│    - execution:completed 이벤트                           │
│    - task:updated 이벤트                                  │
├──────────────────────────────────────────────────────────┤
│ 3. UI 업데이트 확인                                       │
│    - Task 상태 변경 시 Kanban 즉시 업데이트               │
│    - 새로고침 없이 실시간 반영                            │
└──────────────────────────────────────────────────────────┘
```

## 3. 예상 버그 패턴 및 수정 방안

### 3.1 카테고리별 예상 버그

#### A. 환경 설정 문제

| ID | 증상 | 원인 | 수정 방안 |
|----|------|------|----------|
| A1 | prism-service 시작 실패 | PostgreSQL 연결 실패 | `.env.local` DB 설정 확인 |
| A2 | Ollama 연결 실패 | Ollama 미실행 | `ollama serve` 실행 확인 |
| A3 | CORS 에러 | prism-service CORS 미설정 | `CORS_ORIGINS` 환경변수 확인 |
| A4 | JWT 인증 실패 | 토큰 검증 실패 | api-gateway JWT 설정 확인 |

#### B. API 문제

| ID | 증상 | 원인 | 수정 방안 |
|----|------|------|----------|
| B1 | Provider 생성 실패 | DTO 필드 누락 | CreateProviderDto 검증 |
| B2 | Agent 모델 목록 미표시 | Provider.listModels() 오류 | OllamaProvider.listModels() 수정 |
| B3 | Task 상태 전환 실패 | TaskStateMachine 로직 오류 | transition 메서드 확인 |
| B4 | Execution 결과 미저장 | DB 트랜잭션 오류 | ExecutionService 수정 |

#### C. Frontend 문제

| ID | 증상 | 원인 | 수정 방안 |
|----|------|------|----------|
| C1 | 페이지 접근 불가 | 라우터 설정 오류 | router/index.tsx 확인 |
| C2 | API 호출 실패 | apiClient 설정 오류 | Authorization 헤더 확인 |
| C3 | SSE 연결 끊김 | 인증 토큰 미전달 | SSE 연결 로직 확인 |
| C4 | Kanban 업데이트 안됨 | Store 상태 관리 오류 | Zustand store 확인 |

#### D. AI 실행 문제

| ID | 증상 | 원인 | 수정 방안 |
|----|------|------|----------|
| D1 | AI 응답 없음 | Provider 연결 실패 | OllamaProvider 디버깅 |
| D2 | 토큰 사용량 0 | 응답 파싱 오류 | LLMResponse 확인 |
| D3 | 실행 타임아웃 | Ollama 응답 지연 | 타임아웃 설정 증가 |
| D4 | 실행 후 상태 미변경 | completeTask() 미호출 | ExecutionService 수정 |

### 3.2 버그 추적 체크리스트

```markdown
## 발견된 버그 목록

### Phase 1: 로그인
- [ ] BUG-001: (설명)

### Phase 2: 페이지 접근
- [ ] BUG-002: (설명)

### Phase 3: Provider
- [ ] BUG-003: (설명)

### Phase 4: Agent
- [ ] BUG-004: (설명)

### Phase 5: Board/Task
- [ ] BUG-005: (설명)

### Phase 6: AI 실행
- [ ] BUG-006: (설명)

### Phase 7: SSE
- [ ] BUG-007: (설명)
```

## 4. 수정 후 검증 방법

### 4.1 단위 검증

각 버그 수정 후:
1. 해당 기능 단독 테스트
2. 관련 API 호출 확인
3. 콘솔 에러 없음 확인

### 4.2 통합 검증

모든 버그 수정 후:
1. Phase 1-7 전체 시나리오 재실행
2. 새로운 버그 발생 여부 확인
3. 성능 저하 여부 확인

### 4.3 회귀 테스트

```bash
# E2E 테스트 (있는 경우)
cd e2e-tests
npm run test -- --grep "prism"
```

## 5. 파일 변경 예상

### Backend (prism-service)

| 파일 | 변경 가능성 | 이유 |
|------|:-----------:|------|
| `modules/provider/provider.service.ts` | 높음 | Provider CRUD 로직 |
| `modules/ai/providers/ollama.provider.ts` | 높음 | Ollama 연결 |
| `modules/execution/execution.service.ts` | 중간 | 실행 로직 |
| `modules/task/task.service.ts` | 중간 | 상태 전환 |
| `modules/sse/sse.service.ts` | 중간 | 실시간 이벤트 |
| `common/guards/jwt-auth.guard.ts` | 낮음 | 인증 |

### Frontend (prism-frontend)

| 파일 | 변경 가능성 | 이유 |
|------|:-----------:|------|
| `pages/ProvidersPage.tsx` | 높음 | Provider UI |
| `pages/AgentsPage.tsx` | 높음 | Agent UI |
| `pages/BoardPage.tsx` | 중간 | Kanban UI |
| `stores/*.ts` | 중간 | 상태 관리 |
| `services/api.ts` | 낮음 | API 클라이언트 |

## 6. 실행 명령어 요약

```bash
# 1. Infrastructure
docker-compose -f docker-compose-local.yml up -d

# 2. Backend
cd services/api-gateway && ./gradlew bootRun --args='--spring.profiles.active=local' &
cd services/auth-service && ./gradlew bootRun --args='--spring.profiles.active=local' &
cd services/prism-service && npm run start:dev &

# 3. Frontend
cd frontend && npm run build && npm run dev:portal & npm run dev:prism &

# 4. Ollama (이미 실행 중이면 생략)
ollama serve &
# 사용 가능 모델: deepseek-r1:14b, nomic-embed-text:latest

# 5. Health Check
curl http://localhost:8080/actuator/health
curl http://localhost:8085/api/v1/health
curl http://localhost:30000
curl http://localhost:11434/api/tags
```

---
*Generated by bkit PDCA Skill*
