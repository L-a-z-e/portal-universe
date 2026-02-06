---
id: prism-arch-data-flow
title: Prism Frontend Data Flow
type: architecture
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [architecture, data-flow, react, zustand, sse, dnd-kit, kanban]
related:
  - prism-arch-system-overview
  - prism-arch-module-federation
---

# Prism Frontend Data Flow

## 개요

Prism Frontend는 React 18 기반의 AI Agent Orchestration Kanban Board로, API Gateway를 통해 Prism Service(NestJS)와 통신합니다. Zustand를 사용하여 클라이언트 상태를 관리하며, SSE(Server-Sent Events)로 실시간 업데이트를 수신합니다.

**핵심 특징**:
- Portal Shell에서 주입된 `apiClient` (axios 인스턴스) 또는 local fallback 사용
- API Gateway를 통한 중앙집중식 라우팅 (`/api/v1/prism/**`)
- Zustand 4개 Store (board, task, agent, provider)
- SSE 기반 Board별 실시간 이벤트 스트림
- @dnd-kit Kanban DnD + Optimistic Update + Rollback
- Embedded/Standalone 듀얼 모드 지원

---

## 전체 데이터 흐름 아키텍처

```mermaid
graph TB
    subgraph "Portal Shell"
        PS[Portal Shell App]
        AC[apiClient - Axios]
        TST[themeStore - Pinia]
        AST[authStore - Pinia]
    end

    subgraph "Prism Frontend"
        APP[App.tsx]
        RT[PrismRouter]
        PG[Pages - BoardList, Board, Agents, Providers]
        API[ApiService]
        SSE[useSse Hook]

        subgraph "Zustand Stores"
            BDS[boardStore]
            TKS[taskStore + columns]
            AGS[agentStore]
            PVS[providerStore]
        end
    end

    subgraph "Backend"
        GW[API Gateway :8080]
        PRS[Prism Service :8085]
        DB[(MongoDB)]
    end

    PS -->|Module Federation| APP
    AC -->|getPortalApiClient| API
    TST -.->|usePortalTheme| APP
    AST -.->|RequireAuth| RT

    APP --> RT
    RT --> PG
    PG -->|read/write| Zustand Stores
    Zustand Stores -->|call| API
    SSE -->|이벤트 핸들러| TKS

    API -->|HTTP| GW
    GW -->|route /api/v1/prism/**| PRS
    PRS -->|query| DB
    PRS -.->|SSE Stream| SSE
```

---

## 주요 데이터 흐름

### 1. 애플리케이션 마운트 흐름

```mermaid
sequenceDiagram
    participant PS as Portal Shell
    participant BS as bootstrap.tsx
    participant RB as @portal/react-bootstrap
    participant APP as App.tsx
    participant RT as PrismRouter
    participant BR as @portal/react-bridge

    PS->>BS: import('prism/bootstrap')
    BS->>RB: createAppBootstrap({ name: 'prism', App, ... })
    RB-->>BS: { mount, unmount }

    PS->>BS: mount(container, options)
    Note over BS,RB: options = { initialPath, onNavigate, theme }
    RB->>RB: createRoot(container)
    RB->>RB: data-service="prism" 설정
    RB->>APP: render(<App theme locale userRole initialPath onNavigate />)

    APP->>APP: mode detection (Embedded/Standalone)

    alt Embedded Mode
        APP->>BR: usePortalTheme()
        BR-->>APP: { isDark, isConnected }
        APP->>APP: data-theme 동기화
    else Standalone Mode
        APP->>APP: props.theme 사용
    end

    APP->>RT: <PrismRouter isEmbedded initialPath onNavigate />
    RT->>RT: createRouter (Memory or Browser)
    RT-->>APP: RouterProvider 렌더링
```

### 2. Board CRUD

```mermaid
sequenceDiagram
    participant User
    participant BLP as BoardListPage
    participant BDS as boardStore
    participant API as ApiService
    participant GW as API Gateway
    participant PRS as Prism Service

    User->>BLP: 페이지 접근
    BLP->>BDS: fetchBoards()
    BDS->>API: api.getBoards()
    API->>GW: GET /api/v1/prism/boards
    GW->>PRS: JWT 검증 후 라우팅
    PRS-->>GW: { items: Board[] }
    GW-->>API: ApiResponse<{ items: Board[] }>
    API-->>BDS: boards[]
    BDS-->>BLP: state 업데이트
    BLP-->>User: Board 카드 목록 렌더링

    User->>BLP: "New Board" 클릭
    BLP->>BLP: Modal 열기 (이름, 설명 입력)
    User->>BLP: 폼 제출
    BLP->>BDS: createBoard({ name, description })
    BDS->>API: api.createBoard(data)
    API->>PRS: POST /api/v1/prism/boards
    PRS-->>API: Board (created)
    API-->>BDS: board
    BDS-->>BLP: boards 목록 업데이트
    BLP->>BLP: navigate(`/boards/${board.id}`)
```

### 3. Task 생명주기 (TODO -> DONE)

```mermaid
sequenceDiagram
    participant User
    participant BP as BoardPage
    participant TM as TaskModal
    participant TKS as taskStore
    participant API as ApiService
    participant PRS as Prism Service
    participant SSE as SSE Stream

    Note over User,SSE: Phase 1: Task 생성
    User->>BP: "Add Task" 클릭
    BP->>TM: Modal 열기
    User->>TM: 제목, 설명, 우선순위, Agent 선택
    TM->>TKS: createTask({ boardId, title, ... })
    TKS->>API: api.createTask(data)
    API->>PRS: POST /api/v1/prism/boards/:boardId/tasks
    PRS-->>API: Task (status: TODO)
    API-->>TKS: task
    TKS->>TKS: tasks 배열에 추가 + columns 재빌드

    Note over User,SSE: Phase 2: Task 실행 (Run)
    User->>BP: TaskCard "Run" 클릭
    BP->>TKS: executeTask(taskId)
    TKS->>API: api.executeTask(taskId)
    API->>PRS: POST /api/v1/prism/tasks/:id/execute

    Note over PRS,SSE: Prism Service가 Agent를 호출하고 SSE 이벤트 발행
    PRS-->>SSE: event: execution.started
    SSE->>TKS: handleExecutionStarted(taskId)
    TKS->>TKS: executingTaskIds.add(taskId)
    Note over BP: TaskCard에 "Running..." 스피너 표시

    PRS-->>SSE: event: execution.completed
    SSE->>TKS: handleExecutionCompleted(taskId)
    TKS->>TKS: executingTaskIds.delete(taskId)
    TKS->>API: fetchTasks(boardId) (최신 상태 동기화)

    Note over User,SSE: Phase 3: Task 리뷰 (Approve/Reject)
    PRS-->>SSE: event: task.updated (status: IN_REVIEW)
    SSE->>TKS: handleTaskUpdated(task)
    TKS->>TKS: columns 재빌드 (IN_REVIEW 컬럼으로 이동)

    User->>BP: TaskCard "Review" 클릭
    BP->>BP: TaskResultModal 열기 (실행 결과 표시)

    alt Approve
        User->>BP: "Approve" 클릭
        BP->>TKS: approveTask(taskId)
        TKS->>API: api.approveTask(taskId)
        API->>PRS: POST /api/v1/prism/tasks/:id/approve
        PRS-->>API: Task (status: DONE)
    else Reject
        User->>BP: "Reject & Retry" 클릭 (피드백 입력)
        BP->>TKS: rejectTask(taskId, feedback)
        TKS->>API: api.rejectTask(taskId, feedback)
        API->>PRS: POST /api/v1/prism/tasks/:id/reject
        PRS-->>API: Task (status: TODO, 재시도 가능)
    end
```

### 4. Kanban DnD + Optimistic Update

```mermaid
sequenceDiagram
    participant User
    participant KB as KanbanBoard
    participant DnD as @dnd-kit
    participant TKS as taskStore
    participant API as ApiService
    participant PRS as Prism Service

    User->>KB: Task 카드 드래그 시작
    KB->>DnD: onDragStart
    DnD->>KB: activeTask 설정 (DragOverlay 표시)

    User->>KB: 다른 컬럼으로 드롭
    KB->>DnD: onDragEnd
    DnD->>KB: { active: taskId, over: targetColumn/task }

    KB->>KB: targetStatus, targetPosition 계산

    KB->>TKS: moveTask(taskId, targetStatus, targetPosition)

    Note over TKS: Optimistic Update
    TKS->>TKS: 1. 현재 상태 스냅샷 저장 (prev)
    TKS->>TKS: 2. 즉시 columns 재빌드 (UI 반영)

    TKS->>API: api.moveTask(id, { status, position })
    API->>PRS: PATCH /api/v1/prism/tasks/:id/position

    alt 성공
        PRS-->>API: Task (moved)
        Note over TKS: UI 이미 반영됨, 추가 작업 없음
    else 실패
        API-->>TKS: Error
        TKS->>TKS: 3. 스냅샷으로 롤백 (prev.tasks, prev.columns)
        Note over KB: UI가 원래 위치로 복원
    end
```

### 5. SSE 실시간 업데이트

```mermaid
sequenceDiagram
    participant BP as BoardPage
    participant HOOK as useSse Hook
    participant PRS as Prism Service
    participant TKS as taskStore

    BP->>HOOK: useSse({ boardId, onEvent, enabled })

    Note over HOOK,PRS: SSE 연결
    HOOK->>HOOK: getAccessToken() (bridge -> window -> localStorage)
    HOOK->>PRS: fetch(GET /api/v1/prism/sse/boards/:boardId)
    Note over HOOK,PRS: headers: { Authorization, Accept: text/event-stream }
    PRS-->>HOOK: 200 OK (ReadableStream)
    HOOK->>HOOK: reader.read() 루프 시작

    loop 이벤트 수신
        PRS-->>HOOK: event: task.created\ndata: { task }
        HOOK->>BP: onEvent({ type: 'task.created', payload: { task } })
        BP->>TKS: handleTaskCreated(task)
        TKS->>TKS: tasks에 추가 + columns 재빌드
    end

    Note over HOOK,PRS: 연결 끊김 시
    HOOK->>HOOK: Exponential Backoff (1s -> 2s -> 4s -> ... max 30s)
    HOOK->>HOOK: 최대 5회 재연결 시도
    HOOK->>PRS: 재연결

    Note over BP: 언마운트 시
    BP->>HOOK: cleanup (useEffect return)
    HOOK->>HOOK: abortController.abort()
    HOOK->>HOOK: clearTimeout(reconnect)
```

**SSE 이벤트 유형**:

| 이벤트 | 페이로드 | taskStore 핸들러 | 동작 |
|--------|---------|-----------------|------|
| `task.created` | `{ task: Task }` | `handleTaskCreated` | tasks 배열에 추가 (중복 방지) |
| `task.updated` | `{ task: Task }` | `handleTaskUpdated` | 해당 Task 교체 |
| `task.moved` | `{ taskId, toStatus, position }` | `handleTaskMoved` | status/position 업데이트 |
| `task.deleted` | `{ taskId }` | `handleTaskDeleted` | tasks에서 제거 |
| `execution.started` | `{ taskId }` | `handleExecutionStarted` | executingTaskIds에 추가 |
| `execution.completed` | `{ taskId }` | `handleExecutionCompleted` | executingTaskIds에서 제거 + fetchTasks |
| `execution.failed` | `{ taskId }` | `handleExecutionFailed` | executingTaskIds에서 제거 |

### 6. Agent/Provider 관리

```mermaid
sequenceDiagram
    participant User
    participant AP as AgentsPage
    participant AGS as agentStore
    participant PVS as providerStore
    participant API as ApiService
    participant PRS as Prism Service

    User->>AP: Agents 페이지 접근
    AP->>AGS: fetchAgents()
    AP->>PVS: fetchProviders()

    par 병렬 요청
        AGS->>API: api.getAgents()
        API->>PRS: GET /api/v1/prism/agents
        PRS-->>API: { items: Agent[] }
        API-->>AGS: agents[]
    and
        PVS->>API: api.getProviders()
        API->>PRS: GET /api/v1/prism/providers
        PRS-->>API: { items: Provider[] }
        API-->>PVS: providers[]
    end

    AGS-->>AP: agents 렌더링
    PVS-->>AP: providers (Agent 폼의 Provider 선택지)

    User->>AP: "New Agent" 클릭
    AP->>AP: Modal 열기
    User->>AP: Provider 선택

    AP->>API: api.getProviderModels(providerId)
    API->>PRS: GET /api/v1/prism/providers/:id/models
    PRS-->>API: string[] (사용 가능 모델 목록)
    API-->>AP: 모델 선택 드롭다운 업데이트

    User->>AP: 폼 제출 (이름, 역할, Provider, 모델, 시스템 프롬프트, 온도, 토큰)
    AP->>AGS: createAgent(data)
    AGS->>API: api.createAgent(data)
    API->>PRS: POST /api/v1/prism/agents
    PRS-->>API: Agent (created)
    API-->>AGS: agent
    AGS-->>AP: agents 목록 업데이트
```

### 7. API 호출 패턴 (Portal apiClient vs Local Fallback)

```mermaid
sequenceDiagram
    participant Store as Zustand Store
    participant SVC as ApiService
    participant BR as @portal/react-bridge
    participant PAC as Portal apiClient
    participant LC as Local Client

    Store->>SVC: api.someMethod()
    SVC->>SVC: this.client (getter)
    SVC->>BR: getPortalApiClient()

    alt Portal apiClient 사용 가능 (Embedded)
        BR-->>SVC: AxiosInstance (Portal Shell)
        SVC->>PAC: request (토큰 자동 갱신, 401/429 재시도)
        PAC-->>SVC: ApiResponse<T>
    else Portal apiClient 없음 (Standalone)
        BR-->>SVC: null
        SVC->>SVC: lazy 생성 local client
        SVC->>LC: request
        Note over LC: interceptors: token 주입, 401 처리
        LC-->>SVC: ApiResponse<T>
    end

    SVC->>SVC: response.data.data 추출
    SVC-->>Store: T (unwrapped data)
```

**토큰 획득 우선순위** (Local Fallback):
1. `@portal/react-bridge` adapter의 `getAccessToken()`
2. `window.__PORTAL_GET_ACCESS_TOKEN__()`
3. `window.__PORTAL_ACCESS_TOKEN__`
4. `localStorage.getItem('access_token')`

### 8. 에러 처리 패턴

```mermaid
sequenceDiagram
    participant User
    participant Page
    participant Store as Zustand Store
    participant API as ApiService
    participant GW as API Gateway

    Page->>Store: action() 호출
    Store->>Store: set({ loading: true, error: null })
    Store->>API: api.method()
    API->>GW: HTTP Request

    alt 성공 (2xx)
        GW-->>API: ApiResponse<T>
        API-->>Store: T (data)
        Store->>Store: set({ data, loading: false })
        Store-->>Page: state 업데이트
    else 비즈니스 에러 (4xx with ApiErrorResponse)
        GW-->>API: { success: false, error: { code, message } }
        API->>API: new ApiError(message, code, errorDetails)
        API-->>Store: throw ApiError
        Store->>Store: set({ error: message, loading: false })
        Store-->>Page: error 표시
        Page-->>User: 에러 배너 렌더링
    else 인증 에러 (401)
        GW-->>API: 401 Unauthorized
        API->>API: window.__PORTAL_ON_AUTH_ERROR__()
        Note over API: Portal Shell 로그인 페이지로 이동
    else 네트워크 에러
        API-->>Store: throw Error
        Store->>Store: set({ error: 'Failed to ...', loading: false })
        Store-->>Page: error 표시
    end
```

---

## Task 컨텍스트 (Referenced Tasks)

Task 생성 시 다른 완료된 Task를 참조(Reference)할 수 있습니다. Agent 실행 시 참조된 Task의 실행 결과가 컨텍스트로 전달됩니다.

```mermaid
sequenceDiagram
    participant User
    participant TRM as TaskResultModal
    participant API as ApiService
    participant PRS as Prism Service

    User->>TRM: Task 결과 보기
    TRM->>API: api.getTaskContext(taskId)
    API->>PRS: GET /api/v1/prism/tasks/:id/context
    PRS-->>API: { previousExecutions, referencedTasks }
    API-->>TRM: context

    Note over TRM: previousExecutions: 해당 Task의 실행 이력
    Note over TRM: referencedTasks: 참조된 Task + 마지막 실행 결과

    TRM->>TRM: Agent별 실행 결과 그룹핑
    TRM-->>User: 실행 이력 + 참조 Task 결과 표시
```

**TaskContext 구조**:

```typescript
interface TaskContext {
  previousExecutions: Execution[];  // 해당 Task의 이전 실행 이력
  referencedTasks: Array<{
    taskId: number;
    taskTitle: string;
    lastExecution: Execution | null; // 참조 Task의 마지막 실행 결과
  }>;
}
```

---

## 관련 문서

- [System Overview](./system-overview.md) - 아키텍처 개요
- [Module Federation](./module-federation.md) - MF 설정 상세

---

**작성자**: Laze
**최종 업데이트**: 2026-02-06
