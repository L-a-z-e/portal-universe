// Provider Types
export interface Provider {
  id: number;
  name: string;
  type: ProviderType;
  baseUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type ProviderType = 'OPENAI' | 'ANTHROPIC' | 'GOOGLE' | 'OLLAMA' | 'LOCAL';

export interface CreateProviderRequest {
  name: string;
  type: ProviderType;
  apiKey: string;
  baseUrl?: string;
}

// Agent Types
export type AgentRole = 'PM' | 'BACKEND' | 'FRONTEND' | 'DEVOPS' | 'TESTER' | 'CUSTOM';

export interface Agent {
  id: number;
  name: string;
  role: AgentRole;
  description?: string;
  providerId: number;
  providerName: string;
  model: string;
  systemPrompt: string;
  temperature: number;
  maxTokens: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAgentRequest {
  name: string;
  role: AgentRole;
  description?: string;
  providerId: number;
  model: string;
  systemPrompt: string;
  temperature?: number;
  maxTokens?: number;
}

// Board Types
export interface Board {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBoardRequest {
  name: string;
  description?: string;
}

// Task Types
export interface Task {
  id: number;
  boardId: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  agentId?: number;
  agentName?: string;
  position: number;
  dueDate?: string;
  referencedTaskIds?: number[];
  availableActions?: string[];
  createdAt: string;
  updatedAt: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface CreateTaskRequest {
  boardId: number;
  title: string;
  description?: string;
  priority?: TaskPriority;
  agentId?: number;
  dueDate?: string;
  referencedTaskIds?: number[];
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  agentId?: number;
  dueDate?: string;
  referencedTaskIds?: number[];
}

export interface MoveTaskRequest {
  status: TaskStatus;
  position: number;
}

// Execution Types
export interface Execution {
  id: number;
  taskId: number;
  agentId: number;
  agentName?: string;
  executionNumber: number;
  status: ExecutionStatus;
  inputPrompt: string;
  outputResult?: string;
  errorMessage?: string;
  inputTokens?: number;
  outputTokens?: number;
  durationMs?: number;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export type ExecutionStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

// API Response (axios interceptor에서 성공/실패 모두 파싱하기 위해 union 형태 유지)
// Canonical API types: @portal/design-types의 ApiResponse, ApiErrorResponse 참조
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: {
    code: string;
    message: string;
  } | null;
}

// Kanban DnD Types
export interface KanbanColumn {
  id: TaskStatus;
  title: string;
  tasks: Task[];
}
