# Prism Refactoring Plan

## 1. Overview

| Item | Description |
|------|-------------|
| Feature | prism-refactoring |
| Created | 2026-02-04 |
| Status | Planning |
| Level | Dynamic |

## 2. Current State Analysis

### 2.1 Identified Problems

#### Problem 1: Provider API Key Required for All Types
- **현재**: API Key가 모든 Provider 타입에서 필수 (`if (!formData.apiKey.trim()) return`)
- **문제**: Ollama/Local 타입은 API Key가 불필요
- **위치**: `frontend/prism-frontend/src/pages/ProvidersPage.tsx:37`

#### Problem 2: Model Selection Not Dynamic
- **현재**: Model을 수동 입력 (기본값 `gpt-4o`)
- **문제**: Provider 선택 후 해당 Provider에서 지원하는 Model 목록을 제공하지 않음
- **위치**: `frontend/prism-frontend/src/pages/AgentsPage.tsx:196-200`

#### Problem 3: Due Date Field Missing in Backend
- **현재**: Frontend UI에는 Due Date 필드가 있으나 Backend Entity에 없음
- **문제**: 데이터 불일치, 실제 저장 안됨
- **위치**:
  - Frontend: `frontend/prism-frontend/src/components/kanban/TaskModal.tsx:145-152`
  - Backend: `services/prism-service/src/modules/task/task.entity.ts` (dueDate 컬럼 없음)

#### Problem 4: Task Edit Available During Execution
- **현재**: 모든 상태에서 Edit 버튼 활성화
- **문제**: IN_PROGRESS 상태에서는 Edit이 아닌 View만 가능해야 함
- **위치**: `frontend/prism-frontend/src/components/kanban/TaskCard.tsx:110-114`

#### Problem 5: No Result View in IN_REVIEW State
- **현재**: IN_REVIEW 상태에서 Agent 응답 결과 확인 UI 없음
- **문제**: 사용자가 결과를 확인하고 approve/retry 결정 불가
- **필요 기능**:
  - Agent 응답 결과 표시
  - Approve (완료 처리) 버튼
  - Retry (재작업) + 추가 지시 입력

#### Problem 6: No Agent Result Reference
- **현재**: 단독 실행만 지원, 이전 실행 결과 참조 불가
- **문제**:
  - 같은 Task 내 이전 실행 결과를 context로 전달 불가
  - 다른 Task의 실행 결과를 참조 불가

### 2.2 State Machine (정상 동작 확인)
```
TODO ──[execute]──> IN_PROGRESS ──[complete]──> IN_REVIEW ──[approve]──> DONE
  │                     │                          │
  └──[cancel]──>        └──[cancel]──>             ├──[retry]──> IN_PROGRESS
               CANCELLED              CANCELLED    └──[cancel]──> CANCELLED
```

### 2.3 Execution Flow (정상 동작 확인)
1. Task Run (execute) → IN_PROGRESS
2. AI Service 호출
3. 응답 수신 → completeTask() → IN_REVIEW
4. SSE로 상태 변경 전파

## 3. Goals

### 3.1 Primary Goals
1. **Provider 개선**: Ollama/Local 타입은 API Key 선택적
2. **Model 동적 선택**: Provider API로 모델 목록 조회
3. **Task 상태별 UI 제어**: IN_PROGRESS=View Only, IN_REVIEW=결과 확인+액션
4. **Agent 결과 참조**: 같은 Task 히스토리 + 다른 Task 결과 참조

### 3.2 Out of Scope
- Due Date 필드: 의미 없음 확인 → optional로 유지 (사용자 메모용)
- Multi-Agent Orchestration: 단순 참조만, 복잡한 워크플로우 X

## 4. Detailed Scenarios

### Scenario 1: Provider 등록 (Ollama/Local)
```
1. 사용자가 Providers 페이지 접속
2. "Add Provider" 클릭
3. Provider Type = "OLLAMA" 선택
4. Name 입력 (필수)
5. API Key 입력란 비활성화 또는 optional 표시
6. Base URL 입력 (e.g., http://localhost:11434)
7. "Add Provider" 클릭
8. Provider 목록에 추가됨
```

### Scenario 2: Agent 생성 (Model 동적 선택)
```
1. 사용자가 Agents 페이지 접속
2. "New Agent" 클릭
3. Provider 선택 (e.g., "My Ollama")
4. Model 드롭다운에 해당 Provider의 모델 목록 표시
   - Ollama: llama3.2, codellama, mistral...
   - OpenAI: gpt-4o, gpt-4-turbo, gpt-3.5-turbo...
   - Anthropic: claude-3-5-sonnet-20241022...
5. Model 선택 또는 직접 입력 (custom model 지원)
6. System Prompt, Temperature, Max Tokens 설정
7. "Create" 클릭
```

### Scenario 3: Task 실행 및 결과 확인
```
1. Board 페이지에서 TODO에 Task 생성
   - Title: "블로그 글 작성"
   - Description: "AI 트렌드에 대한 500자 글"
   - Agent: "Content Writer" 선택

2. "Run" 버튼 클릭
   - Task → IN_PROGRESS 이동
   - 로딩 스피너 표시
   - Edit 버튼 → View 버튼으로 변경

3. AI 응답 완료
   - Task → IN_REVIEW 이동
   - SSE로 실시간 업데이트

4. IN_REVIEW에서 Task 클릭
   - 결과 확인 모달 표시
   - Agent 응답 내용 표시
   - [Approve] [Retry] 버튼

5a. Approve 클릭
   - Task → DONE 이동
   - 완료 처리

5b. Retry 클릭
   - 추가 지시 입력 모달
   - "더 짧게 요약해줘" 입력
   - Task → IN_PROGRESS 이동
   - 이전 결과 + 추가 지시를 context로 재실행
```

### Scenario 4: Agent 간 결과 참조
```
[같은 Task 내 히스토리 참조]
1. Task "리서치" 실행 → Agent A 응답
2. Retry with feedback → Agent A 재실행
   - Input: 이전 응답 + 피드백

[다른 Task 결과 참조]
1. Board에 Task A "리서치" 완료 (DONE)
2. Task B "글쓰기" 생성
3. Task B Description에서 "@Task:리서치" 또는 드롭다운으로 참조 선택
4. Task B 실행 시 Task A의 마지막 실행 결과를 context에 포함
```

## 5. Technical Implementation

### 5.1 Backend Changes

#### 5.1.1 Provider Model List API
```typescript
// GET /api/v1/providers/:id/models
interface ModelListResponse {
  models: Array<{
    id: string;
    name: string;
    contextLength?: number;
  }>;
}

// Provider별 구현
- Ollama: GET http://{baseUrl}/api/tags
- OpenAI: GET https://api.openai.com/v1/models
- Anthropic: 하드코딩 (API 미제공)
```

#### 5.1.2 Execution History API
```typescript
// GET /api/v1/tasks/:taskId/executions
// 이미 구현됨 - 활용 필요

// GET /api/v1/boards/:boardId/tasks/:taskId/context
// Task 실행 시 참조할 context 조회
interface TaskContextResponse {
  previousExecutions: Execution[];  // 같은 Task 히스토리
  referencedTasks: Array<{          // 다른 Task 참조
    taskId: number;
    taskTitle: string;
    lastExecution: Execution;
  }>;
}
```

#### 5.1.3 Task Entity 수정
```typescript
// dueDate 컬럼 추가 (optional)
@Column({ type: 'date', nullable: true })
dueDate: Date | null;

// referencedTaskIds 컬럼 추가
@Column({ type: 'simple-array', nullable: true })
referencedTaskIds: number[] | null;
```

### 5.2 Frontend Changes

#### 5.2.1 ProvidersPage
- Provider Type에 따라 API Key 필드 required/optional 분기
- OLLAMA, LOCAL 타입은 API Key 선택적

#### 5.2.2 AgentsPage
- Provider 선택 시 Model 목록 API 호출
- Model Select 컴포넌트로 변경 (with custom input option)

#### 5.2.3 TaskCard
- status === 'IN_PROGRESS' → Edit 버튼 숨김, View 버튼 표시
- status === 'IN_REVIEW' → View Result 버튼 표시

#### 5.2.4 TaskResultModal (신규)
- Agent 실행 결과 표시
- Approve 버튼
- Retry 버튼 + 추가 지시 입력

#### 5.2.5 TaskModal 수정
- 다른 Task 참조 선택 UI
- Due Date는 optional로 유지

### 5.3 API Changes Summary

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | /api/v1/providers/:id/models | 모델 목록 조회 | New |
| GET | /api/v1/tasks/:id/context | 실행 context 조회 | New |
| PATCH | /api/v1/tasks/:id/approve | Task 승인 | Existing |
| PATCH | /api/v1/tasks/:id/retry | Task 재작업 | Existing (수정) |

## 6. Testing Plan

### 6.1 Playwright E2E Tests

```typescript
// 1. Provider 등록 테스트
test('should create Ollama provider without API key', async () => {
  // Providers 페이지 접속
  // OLLAMA 선택 시 API Key 필드 optional 확인
  // Provider 생성 성공 확인
});

// 2. Agent 생성 (Model 선택) 테스트
test('should show model list based on provider', async () => {
  // Provider 선택 후 Model 목록 로딩 확인
  // Model 선택 후 Agent 생성 성공 확인
});

// 3. Task 실행 플로우 테스트
test('should execute task and show result in review', async () => {
  // Task 생성 (TODO)
  // Run 클릭 → IN_PROGRESS 확인
  // 완료 후 IN_REVIEW 확인
  // 결과 확인 → Approve → DONE 확인
});

// 4. Task Retry 테스트
test('should retry task with additional instruction', async () => {
  // IN_REVIEW Task에서 Retry 클릭
  // 추가 지시 입력
  // IN_PROGRESS로 이동 확인
  // 완료 후 IN_REVIEW 재확인
});

// 5. Task 참조 테스트
test('should reference another task result', async () => {
  // Task A 실행 완료
  // Task B 생성 시 Task A 참조 설정
  // Task B 실행 시 context에 Task A 결과 포함 확인
});
```

### 6.2 Local Environment

```bash
# 1. 인프라 실행
docker compose -f docker-compose-local.yml up -d

# 2. prism-service 실행
cd services/prism-service
npm run start:dev

# 3. prism-frontend 빌드 및 실행
cd frontend
pnpm run build:prism
pnpm run dev:prism  # standalone 모드

# 4. E2E 테스트
cd e2e
npx playwright test prism --headed
```

## 7. Implementation Order

1. **Phase 1: Backend API** (Provider Models, Task Context)
2. **Phase 2: Backend Entity** (dueDate, referencedTaskIds)
3. **Phase 3: Frontend Provider** (API Key optional)
4. **Phase 4: Frontend Agent** (Model dynamic select)
5. **Phase 5: Frontend Task** (View Only, Result Modal)
6. **Phase 6: Frontend Task Reference** (다른 Task 참조 UI)
7. **Phase 7: E2E Tests** (Playwright)

## 8. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Provider API 호출 실패 | 모델 목록 표시 불가 | 캐싱 + 폴백 (하드코딩 목록) |
| Ollama 미설치 환경 | 로컬 테스트 불가 | Mock 서버 또는 OpenAI로 대체 테스트 |
| SSE 연결 불안정 | 실시간 업데이트 누락 | 폴링 폴백 구현 |

## 9. Acceptance Criteria

- [ ] Ollama Provider 등록 시 API Key 없이 생성 가능
- [ ] Agent 생성 시 Provider별 Model 목록 동적 표시
- [ ] Task IN_PROGRESS 상태에서 Edit 불가, View만 가능
- [ ] Task IN_REVIEW 상태에서 결과 확인 + Approve/Retry 가능
- [ ] Retry 시 추가 지시 입력 가능
- [ ] 다른 Task 결과 참조하여 실행 가능
- [ ] E2E 테스트 전체 통과
