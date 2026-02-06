# Prism Refactoring Design Document

## 1. Overview

| Item | Description |
|------|-------------|
| Feature | prism-refactoring |
| Created | 2026-02-04 |
| Status | Design |
| Plan Reference | [prism-refactoring.plan.md](../../01-plan/features/prism-refactoring.plan.md) |

## 2. Existing Implementation Analysis

### 2.1 Already Implemented (Backend)

| Feature | Location | Status |
|---------|----------|--------|
| Models API | `GET /providers/:id/models` | Implemented |
| Model Storage | `AIProvider.models` (jsonb) | Implemented |
| Ollama Provider | `OllamaProvider` class | Works without API Key |
| Execution History | `GET /tasks/:id/executions` | Implemented |
| User Feedback | `Execution.userFeedback` column | Implemented |
| Task Reject | `TaskService.reject(feedback)` | Implemented |

### 2.2 Gaps to Address

| Gap | Current State | Required |
|-----|---------------|----------|
| Provider API Key | Always required | Optional for OLLAMA/LOCAL |
| Frontend Model Select | Manual input | Dynamic dropdown from API |
| Task Card (IN_PROGRESS) | Edit button visible | View only |
| Task Result Modal | Not exists | New component needed |
| Task Reference | Not supported | Select other tasks to reference |

## 3. Backend Design

### 3.1 Provider Entity Changes

**File**: `services/prism-service/src/modules/provider/provider.entity.ts`

```typescript
// 기존 ProviderType에 LOCAL 추가
export enum ProviderType {
  OPENAI = 'OPENAI',
  ANTHROPIC = 'ANTHROPIC',
  OLLAMA = 'OLLAMA',
  AZURE_OPENAI = 'AZURE_OPENAI',
  LOCAL = 'LOCAL',  // NEW
}
```

### 3.2 Provider DTO Changes

**File**: `services/prism-service/src/modules/provider/dto/create-provider.dto.ts`

```typescript
export class CreateProviderDto {
  @IsString()
  @Length(1, 100)
  name: string;

  @IsEnum(ProviderType)
  providerType: ProviderType;

  @IsOptional()  // CHANGE: Required → Optional
  @IsString()
  apiKey?: string;

  @IsOptional()
  @IsString()
  @IsUrl()
  baseUrl?: string;
}
```

### 3.3 Provider Service Changes

**File**: `services/prism-service/src/modules/provider/provider.service.ts`

```typescript
// create 메서드에서 API Key 검증 로직 변경
async create(userId: string, dto: CreateProviderDto): Promise<ProviderResponseDto> {
  // API Key 필수 여부 체크
  const requiresApiKey = this.requiresApiKey(dto.providerType);
  if (requiresApiKey && !dto.apiKey) {
    throw BusinessException.validationFailed('API Key is required for this provider type');
  }

  const provider = this.providerRepository.create({
    userId,
    providerType: dto.providerType,
    name: dto.name,
    apiKeyEncrypted: this.encryptionUtil.encrypt(dto.apiKey || ''),
    baseUrl: dto.baseUrl || this.getDefaultBaseUrl(dto.providerType),
    isActive: true,
  });
  // ...
}

private requiresApiKey(type: ProviderType): boolean {
  return ![ProviderType.OLLAMA, ProviderType.LOCAL].includes(type);
}
```

### 3.4 Task Entity Changes

**File**: `services/prism-service/src/modules/task/task.entity.ts`

```typescript
@Entity('tasks')
export class Task {
  // 기존 필드들...

  @Column({ name: 'due_date', type: 'date', nullable: true })
  dueDate: Date | null;  // NEW

  @Column({ name: 'referenced_task_ids', type: 'simple-array', nullable: true })
  referencedTaskIds: number[] | null;  // NEW
}
```

### 3.5 Task Context API

**File**: `services/prism-service/src/modules/task/task.controller.ts`

```typescript
@Get(':id/context')
@ApiOperation({ summary: 'Get execution context for a task' })
async getContext(
  @CurrentUserId() userId: string,
  @Param('id', ParseIntPipe) id: number,
): Promise<TaskContextResponseDto> {
  return this.taskService.getContext(userId, id);
}
```

**File**: `services/prism-service/src/modules/task/dto/task-context.dto.ts` (NEW)

```typescript
export class TaskContextResponseDto {
  // 같은 Task의 이전 실행들
  previousExecutions: ExecutionResponseDto[];

  // 참조된 다른 Task들의 마지막 실행
  referencedTasks: Array<{
    taskId: number;
    taskTitle: string;
    lastExecution: ExecutionResponseDto | null;
  }>;
}
```

### 3.6 Execution Service Changes

**File**: `services/prism-service/src/modules/execution/execution.service.ts`

```typescript
async executeTask(userId: string, taskId: number): Promise<ExecutionResponseDto> {
  const task = await this.taskService.getTaskEntity(userId, taskId);

  // Get context (previous executions + referenced tasks)
  const context = await this.taskService.getContext(userId, taskId);

  // Build enhanced prompt with context
  const enhancedPrompt = this.buildPromptWithContext(
    task.title,
    task.description,
    context,
  );
  // ...
}

private buildPromptWithContext(
  title: string,
  description: string | null,
  context: TaskContextResponseDto,
): string {
  let prompt = `Task: ${title}`;

  if (description) {
    prompt += `\n\nDescription:\n${description}`;
  }

  // Add previous execution context
  if (context.previousExecutions.length > 0) {
    const lastExec = context.previousExecutions[0];
    prompt += `\n\n---\nPrevious Result:\n${lastExec.outputResult}`;

    if (lastExec.userFeedback) {
      prompt += `\n\nUser Feedback:\n${lastExec.userFeedback}`;
    }
  }

  // Add referenced task results
  if (context.referencedTasks.length > 0) {
    prompt += `\n\n---\nReferenced Tasks:`;
    for (const ref of context.referencedTasks) {
      if (ref.lastExecution) {
        prompt += `\n\n[${ref.taskTitle}]:\n${ref.lastExecution.outputResult}`;
      }
    }
  }

  return prompt;
}
```

### 3.7 Database Migration

```sql
-- Add dueDate and referencedTaskIds to tasks table
ALTER TABLE tasks
ADD COLUMN due_date DATE NULL,
ADD COLUMN referenced_task_ids TEXT NULL;

-- Add LOCAL to provider_type enum
ALTER TYPE provider_type ADD VALUE 'LOCAL';
```

## 4. Frontend Design

### 4.1 Type Changes

**File**: `frontend/prism-frontend/src/types/index.ts`

```typescript
// Provider Types
export type ProviderType = 'OPENAI' | 'ANTHROPIC' | 'GOOGLE' | 'OLLAMA' | 'LOCAL';

// Task Types
export interface Task {
  // 기존 필드들...
  dueDate?: string;
  referencedTaskIds?: number[];  // NEW
}

export interface CreateTaskRequest {
  // 기존 필드들...
  referencedTaskIds?: number[];  // NEW
}

// Execution Types (확장)
export interface TaskContext {
  previousExecutions: Execution[];
  referencedTasks: Array<{
    taskId: number;
    taskTitle: string;
    lastExecution: Execution | null;
  }>;
}
```

### 4.2 Provider Store Changes

**File**: `frontend/prism-frontend/src/stores/providerStore.ts`

```typescript
interface ProviderStore {
  // 기존...
  models: Record<number, string[]>;  // providerId -> models
  modelsLoading: Record<number, boolean>;

  fetchModels: (providerId: number) => Promise<string[]>;
}

// Implementation
fetchModels: async (providerId: number) => {
  set((state) => ({
    modelsLoading: { ...state.modelsLoading, [providerId]: true }
  }));

  try {
    const response = await api.get<string[]>(`/providers/${providerId}/models`);
    set((state) => ({
      models: { ...state.models, [providerId]: response },
      modelsLoading: { ...state.modelsLoading, [providerId]: false }
    }));
    return response;
  } catch (error) {
    set((state) => ({
      modelsLoading: { ...state.modelsLoading, [providerId]: false }
    }));
    throw error;
  }
}
```

### 4.3 ProvidersPage Changes

**File**: `frontend/prism-frontend/src/pages/ProvidersPage.tsx`

```tsx
// API Key 필수 여부 판단
const requiresApiKey = (type: ProviderType): boolean => {
  return !['OLLAMA', 'LOCAL'].includes(type);
};

// Form 제출 시 검증 로직 변경
const handleSubmit = useCallback(async (e: React.FormEvent) => {
  e.preventDefault();
  if (!formData.name.trim()) return;

  // API Key가 필수인 타입에서만 검증
  if (requiresApiKey(formData.type) && !formData.apiKey?.trim()) {
    return;
  }
  // ...
}, [formData, createProvider]);

// API Key 입력 필드 조건부 렌더링
{requiresApiKey(formData.type) ? (
  <Input
    label="API Key"
    type="password"
    value={formData.apiKey}
    onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
    placeholder="sk-..."
    required
  />
) : (
  <Input
    label="API Key (Optional)"
    type="password"
    value={formData.apiKey || ''}
    onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
    placeholder="Optional for this provider"
  />
)}
```

### 4.4 AgentsPage Changes

**File**: `frontend/prism-frontend/src/pages/AgentsPage.tsx`

```tsx
// Model 목록 조회 추가
const { models, modelsLoading, fetchModels } = useProviderStore();
const [customModel, setCustomModel] = useState('');

// Provider 선택 시 Model 목록 조회
const handleProviderChange = useCallback((providerId: number) => {
  setFormData({ ...formData, providerId, model: '' });
  fetchModels(providerId);
}, [formData, fetchModels]);

// Model 선택 UI
const selectedProviderModels = models[formData.providerId] || [];
const modelOptions = [
  ...selectedProviderModels.map(m => ({ value: m, label: m })),
  { value: '__custom__', label: 'Custom model...' }
];

// 렌더링
<Select
  label="Model"
  value={formData.model === customModel ? '__custom__' : formData.model}
  onChange={(value) => {
    if (value === '__custom__') {
      // Show custom input
    } else {
      setFormData({ ...formData, model: value as string });
    }
  }}
  options={modelOptions}
  loading={modelsLoading[formData.providerId]}
/>
{formData.model === '__custom__' && (
  <Input
    label="Custom Model Name"
    value={customModel}
    onChange={(e) => {
      setCustomModel(e.target.value);
      setFormData({ ...formData, model: e.target.value });
    }}
    placeholder="Enter custom model name"
  />
)}
```

### 4.5 TaskCard Changes

**File**: `frontend/prism-frontend/src/components/kanban/TaskCard.tsx`

```tsx
// 상태별 버튼 렌더링
const renderActionButtons = () => {
  if (isExecuting) {
    return (
      <span className="flex items-center gap-1 text-xs text-brand-primary">
        <svg className="animate-spin h-3 w-3" /* ... */ />
        Running...
      </span>
    );
  }

  switch (task.status) {
    case 'TODO':
      return (
        <>
          {task.agentId && (
            <button onClick={handleExecute} className="...">Run</button>
          )}
          <button onClick={handleEdit} className="...">Edit</button>
        </>
      );

    case 'IN_PROGRESS':
      // View only - no edit
      return (
        <button onClick={handleView} className="...">View</button>
      );

    case 'IN_REVIEW':
      return (
        <button onClick={handleViewResult} className="...">
          View Result
        </button>
      );

    case 'DONE':
    case 'CANCELLED':
      return (
        <button onClick={handleView} className="...">View</button>
      );

    default:
      return null;
  }
};
```

### 4.6 TaskResultModal (NEW)

**File**: `frontend/prism-frontend/src/components/kanban/TaskResultModal.tsx`

```tsx
interface TaskResultModalProps {
  isOpen: boolean;
  onClose: () => void;
  task: Task;
  onApprove: () => Promise<void>;
  onRetry: (feedback: string) => Promise<void>;
}

export function TaskResultModal({
  isOpen,
  onClose,
  task,
  onApprove,
  onRetry,
}: TaskResultModalProps) {
  const [executions, setExecutions] = useState<Execution[]>([]);
  const [loading, setLoading] = useState(false);
  const [retryMode, setRetryMode] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Fetch executions on open
  useEffect(() => {
    if (isOpen && task) {
      fetchExecutions();
    }
  }, [isOpen, task]);

  const fetchExecutions = async () => {
    setLoading(true);
    try {
      const data = await api.get<Execution[]>(`/tasks/${task.id}/executions`);
      setExecutions(data);
    } finally {
      setLoading(false);
    }
  };

  const latestExecution = executions[0];

  const handleApprove = async () => {
    setActionLoading(true);
    try {
      await onApprove();
      onClose();
    } finally {
      setActionLoading(false);
    }
  };

  const handleRetry = async () => {
    if (!feedback.trim() && !confirm('No feedback provided. Continue?')) {
      return;
    }
    setActionLoading(true);
    try {
      await onRetry(feedback);
      onClose();
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <Modal open={isOpen} onClose={onClose} title="Task Result" size="lg">
      <div className="space-y-4">
        {/* Task Info */}
        <div className="bg-bg-subtle p-4 rounded-lg">
          <h3 className="font-semibold text-text-heading">{task.title}</h3>
          {task.description && (
            <p className="text-sm text-text-body mt-1">{task.description}</p>
          )}
        </div>

        {/* Execution Result */}
        {loading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : latestExecution ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between text-sm text-text-meta">
              <span>Execution #{latestExecution.executionNumber}</span>
              <span>
                {latestExecution.inputTokens}+{latestExecution.outputTokens} tokens
              </span>
            </div>

            <div className="bg-bg-card border border-border-default rounded-lg p-4 max-h-96 overflow-y-auto">
              <pre className="whitespace-pre-wrap text-sm text-text-body">
                {latestExecution.outputResult}
              </pre>
            </div>

            {latestExecution.errorMessage && (
              <div className="bg-status-error/10 text-status-error p-3 rounded-lg text-sm">
                Error: {latestExecution.errorMessage}
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-8 text-text-meta">
            No execution results yet
          </div>
        )}

        {/* Retry Feedback Input */}
        {retryMode && (
          <div className="space-y-2">
            <Textarea
              label="Additional Instructions"
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              placeholder="Provide feedback or additional instructions for retry..."
              rows={3}
            />
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end gap-2 pt-4 border-t border-border-default">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>

          {task.status === 'IN_REVIEW' && (
            <>
              {retryMode ? (
                <>
                  <Button
                    variant="secondary"
                    onClick={() => setRetryMode(false)}
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleRetry}
                    loading={actionLoading}
                  >
                    Submit & Retry
                  </Button>
                </>
              ) : (
                <>
                  <Button
                    variant="secondary"
                    onClick={() => setRetryMode(true)}
                  >
                    Retry with Feedback
                  </Button>
                  <Button
                    onClick={handleApprove}
                    loading={actionLoading}
                  >
                    Approve
                  </Button>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </Modal>
  );
}
```

### 4.7 TaskModal Changes (Task Reference)

**File**: `frontend/prism-frontend/src/components/kanban/TaskModal.tsx`

```tsx
// 다른 Task 선택 UI 추가
const { tasks } = useTaskStore();

// 참조 가능한 Task (DONE 상태만)
const referenceableTasks = tasks
  .filter(t => t.status === 'DONE' && t.id !== task?.id)
  .map(t => ({ value: t.id.toString(), label: t.title }));

// Form에 referencedTaskIds 추가
const [formData, setFormData] = useState({
  // 기존...
  referencedTaskIds: [] as number[],
});

// 렌더링
{referenceableTasks.length > 0 && (
  <div>
    <label className="block text-sm font-medium text-text-body mb-1">
      Reference Other Tasks (Optional)
    </label>
    <Select
      value={formData.referencedTaskIds.map(String)}
      onChange={(values) => setFormData({
        ...formData,
        referencedTaskIds: (values as string[]).map(Number)
      })}
      options={referenceableTasks}
      multiple
      placeholder="Select tasks to reference..."
    />
    <p className="mt-1 text-xs text-text-meta">
      Results from selected tasks will be included as context
    </p>
  </div>
)}
```

## 5. API Endpoints Summary

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/providers/:id/models` | Get available models | Existing |
| POST | `/providers` | Create provider (apiKey optional) | Modify |
| GET | `/tasks/:id/context` | Get execution context | New |
| GET | `/tasks/:id/executions` | Get execution history | Existing |
| PATCH | `/tasks/:id/approve` | Approve task | Existing |
| PATCH | `/tasks/:id/retry` | Retry task with feedback | Existing (use body) |

## 6. File Changes Summary

### 6.1 Backend Files

| File | Action | Description |
|------|--------|-------------|
| `provider.entity.ts` | Modify | Add LOCAL to ProviderType |
| `create-provider.dto.ts` | Modify | Make apiKey optional |
| `provider.service.ts` | Modify | Add requiresApiKey check |
| `task.entity.ts` | Modify | Add dueDate, referencedTaskIds |
| `task.controller.ts` | Modify | Add getContext endpoint |
| `task.service.ts` | Modify | Add getContext method |
| `task-context.dto.ts` | New | Context response DTO |
| `execution.service.ts` | Modify | Use context in prompt |

### 6.2 Frontend Files

| File | Action | Description |
|------|--------|-------------|
| `types/index.ts` | Modify | Add referencedTaskIds |
| `stores/providerStore.ts` | Modify | Add models state, fetchModels |
| `pages/ProvidersPage.tsx` | Modify | Optional API Key |
| `pages/AgentsPage.tsx` | Modify | Dynamic model select |
| `components/kanban/TaskCard.tsx` | Modify | Status-based buttons |
| `components/kanban/TaskModal.tsx` | Modify | Task reference select |
| `components/kanban/TaskResultModal.tsx` | New | Result view + actions |
| `components/kanban/index.ts` | Modify | Export TaskResultModal |

## 7. Implementation Order

```
Phase 1: Backend Entity & DTO (Day 1)
├── 1.1 Add LOCAL to ProviderType
├── 1.2 Make apiKey optional in CreateProviderDto
├── 1.3 Add requiresApiKey check in ProviderService
├── 1.4 Add dueDate, referencedTaskIds to Task entity
└── 1.5 Create migration

Phase 2: Backend API (Day 1-2)
├── 2.1 Create TaskContextResponseDto
├── 2.2 Add getContext to TaskService
├── 2.3 Add getContext endpoint to TaskController
└── 2.4 Modify ExecutionService to use context

Phase 3: Frontend Provider (Day 2)
├── 3.1 Add models state to providerStore
├── 3.2 Implement fetchModels
└── 3.3 Update ProvidersPage (optional API Key)

Phase 4: Frontend Agent (Day 2-3)
├── 4.1 Update AgentsPage with dynamic model select
└── 4.2 Add custom model input option

Phase 5: Frontend Task (Day 3)
├── 5.1 Create TaskResultModal component
├── 5.2 Update TaskCard with status-based buttons
├── 5.3 Add View/ViewResult handlers to BoardPage
└── 5.4 Connect approve/retry actions

Phase 6: Frontend Task Reference (Day 3-4)
├── 6.1 Add referencedTaskIds to TaskModal
├── 6.2 Update Task types
└── 6.3 Test reference flow

Phase 7: E2E Tests (Day 4)
├── 7.1 Provider tests (OLLAMA without API Key)
├── 7.2 Agent tests (dynamic model select)
├── 7.3 Task flow tests (execute → review → approve)
├── 7.4 Retry tests (with feedback)
└── 7.5 Reference tests (task context)
```

## 8. Test Scenarios

### 8.1 Provider Tests

```typescript
// e2e/tests/prism/provider.spec.ts
test.describe('Provider Management', () => {
  test('should create Ollama provider without API key', async ({ page }) => {
    await page.goto('/prism/providers');
    await page.getByRole('button', { name: 'Add Provider' }).click();

    await page.getByLabel('Provider Name').fill('Local Ollama');
    await page.getByLabel('Provider Type').selectOption('OLLAMA');

    // API Key should be optional
    const apiKeyInput = page.getByLabel(/API Key/);
    await expect(apiKeyInput).not.toHaveAttribute('required');

    await page.getByLabel('Base URL').fill('http://localhost:11434');
    await page.getByRole('button', { name: 'Add Provider' }).click();

    await expect(page.getByText('Local Ollama')).toBeVisible();
  });
});
```

### 8.2 Agent Tests

```typescript
// e2e/tests/prism/agent.spec.ts
test.describe('Agent Management', () => {
  test('should show model list from provider', async ({ page }) => {
    await page.goto('/prism/agents');
    await page.getByRole('button', { name: 'New Agent' }).click();

    await page.getByLabel('Provider').selectOption({ label: /Ollama/ });

    // Wait for models to load
    await page.waitForResponse(res => res.url().includes('/models'));

    const modelSelect = page.getByLabel('Model');
    await expect(modelSelect).toBeEnabled();

    // Should have model options
    const options = await modelSelect.locator('option').count();
    expect(options).toBeGreaterThan(1);
  });
});
```

### 8.3 Task Flow Tests

```typescript
// e2e/tests/prism/task-flow.spec.ts
test.describe('Task Execution Flow', () => {
  test('should execute task and show result in review', async ({ page }) => {
    // Create task and run
    await page.goto('/prism/boards/1');

    // Find task with agent assigned
    const taskCard = page.locator('[data-status="TODO"]').first();
    await taskCard.getByRole('button', { name: 'Run' }).click();

    // Wait for IN_PROGRESS
    await expect(taskCard).toHaveAttribute('data-status', 'IN_PROGRESS');

    // Edit button should be hidden
    await expect(taskCard.getByRole('button', { name: 'Edit' })).not.toBeVisible();
    await expect(taskCard.getByRole('button', { name: 'View' })).toBeVisible();

    // Wait for IN_REVIEW (with timeout for AI response)
    await expect(taskCard).toHaveAttribute('data-status', 'IN_REVIEW', {
      timeout: 60000
    });

    // Click View Result
    await taskCard.getByRole('button', { name: 'View Result' }).click();

    // Result modal should be visible
    const modal = page.getByRole('dialog', { name: 'Task Result' });
    await expect(modal).toBeVisible();
    await expect(modal.locator('pre')).toContainText(/.+/); // Has content

    // Approve
    await modal.getByRole('button', { name: 'Approve' }).click();

    // Task should be DONE
    await expect(taskCard).toHaveAttribute('data-status', 'DONE');
  });

  test('should retry task with feedback', async ({ page }) => {
    // Navigate to IN_REVIEW task
    const taskCard = page.locator('[data-status="IN_REVIEW"]').first();
    await taskCard.getByRole('button', { name: 'View Result' }).click();

    const modal = page.getByRole('dialog');
    await modal.getByRole('button', { name: 'Retry with Feedback' }).click();

    // Enter feedback
    await modal.getByLabel('Additional Instructions').fill('Please make it shorter');
    await modal.getByRole('button', { name: 'Submit & Retry' }).click();

    // Task should go back to IN_PROGRESS
    await expect(taskCard).toHaveAttribute('data-status', 'IN_PROGRESS');
  });
});
```

## 9. Acceptance Criteria

- [ ] Ollama/LOCAL Provider 생성 시 API Key 없이 생성 가능
- [ ] Agent 생성 시 Provider 선택하면 Model 목록 자동 로드
- [ ] Model 목록에서 선택하거나 Custom 입력 가능
- [ ] IN_PROGRESS 상태에서는 Edit 불가, View만 표시
- [ ] IN_REVIEW 상태에서 View Result 버튼으로 결과 확인 가능
- [ ] 결과 모달에서 Approve → DONE 상태 전환
- [ ] 결과 모달에서 Retry with Feedback → IN_PROGRESS로 재실행
- [ ] Task 생성 시 다른 완료된 Task 참조 선택 가능
- [ ] 참조된 Task 결과가 실행 context에 포함
- [ ] 모든 E2E 테스트 통과
