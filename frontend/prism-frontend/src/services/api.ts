import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { getPortalApiClient, isBridgeReady, getAdapter } from '@portal/react-bridge';
import type {
  ApiResponse,
  ApiErrorResponse,
  ErrorDetails,
  Provider,
  CreateProviderRequest,
  Agent,
  CreateAgentRequest,
  Board,
  CreateBoardRequest,
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  MoveTaskRequest,
  Execution,
} from '@/types';

/**
 * API 에러 클래스 - 에러 코드 정보를 보존
 */
export class ApiError extends Error {
  code: string | null;
  errorDetails: ErrorDetails | null;

  constructor(message: string, code?: string, errorDetails?: ErrorDetails) {
    super(message);
    this.name = 'ApiError';
    this.code = code ?? null;
    this.errorDetails = errorDetails ?? null;
  }
}

// Backend Task API response type (includes agent object instead of agentName)
interface TaskApiResponse extends Omit<Task, 'agentName'> {
  agent?: {
    id: number;
    name: string;
    role: string;
  };
}

// API Base URL 설정 (환경별)
const getBaseUrl = (): string => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  if (import.meta.env.DEV) {
    return 'http://localhost:8080';
  }
  return '';
};

class ApiService {
  private _client: AxiosInstance | null = null;

  /**
   * portal/api가 있으면 완전판 사용 (토큰 갱신, 401/429 재시도),
   * 없으면 local fallback (Standalone 모드)
   */
  private get client(): AxiosInstance {
    // portal/api의 apiClient가 있으면 우선 사용
    const portalClient = getPortalApiClient();
    if (portalClient) return portalClient;

    // local fallback (lazy 생성)
    if (this._client) return this._client;

    this._client = axios.create({
      baseURL: getBaseUrl(),
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    this._client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        let token: string | null | undefined = null;
        if (isBridgeReady()) {
          token = getAdapter('auth').getAccessToken?.();
        }
        if (!token) {
          token = window.__PORTAL_GET_ACCESS_TOKEN__?.() ?? window.__PORTAL_ACCESS_TOKEN__;
        }
        if (!token) {
          token = localStorage.getItem('access_token');
        }

        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
      }
    );

    this._client.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiErrorResponse>) => {
        const status = error.response?.status;

        if (status === 401) {
          console.warn('[Prism API] Unauthorized - token may be expired');
          if (window.__PORTAL_ON_AUTH_ERROR__) {
            window.__PORTAL_ON_AUTH_ERROR__();
          }
        }

        const errorData = error.response?.data?.error;
        if (errorData) {
          const apiError = new ApiError(
            errorData.message || error.message,
            errorData.code,
            errorData,
          );
          return Promise.reject(apiError);
        }

        return Promise.reject(error);
      }
    );

    return this._client;
  }

  private async request<T>(
    method: 'get' | 'post' | 'put' | 'patch' | 'delete',
    url: string,
    data?: unknown
  ): Promise<T> {
    const response = await this.client.request<ApiResponse<T>>({
      method,
      url,
      data,
    });

    return response.data.data;
  }

  // Helper to map backend task response to frontend Task type
  private mapTaskResponse(task: TaskApiResponse): Task {
    return {
      ...task,
      agentName: task.agent?.name,
    };
  }

  // Provider APIs
  async getProviders(): Promise<Provider[]> {
    interface ProviderApiResponse {
      id: number;
      providerType: string;
      name: string;
      baseUrl?: string;
      isActive: boolean;
      createdAt: string;
      updatedAt: string;
    }
    const result = await this.request<{ items: ProviderApiResponse[] }>('get', '/api/v1/prism/providers');
    const items = result.items ?? [];
    // Map API response to frontend type
    return items.map((p) => ({
      id: p.id,
      name: p.name,
      type: p.providerType as Provider['type'],
      baseUrl: p.baseUrl,
      isActive: p.isActive,
      createdAt: p.createdAt,
      updatedAt: p.updatedAt,
    }));
  }

  async getProvider(id: number): Promise<Provider> {
    return this.request<Provider>('get', `/api/v1/prism/providers/${id}`);
  }

  async createProvider(data: CreateProviderRequest): Promise<Provider> {
    // Map frontend 'type' to backend 'providerType'
    const backendData = {
      name: data.name,
      providerType: data.type,
      apiKey: data.apiKey,
      baseUrl: data.baseUrl,
    };
    return this.request<Provider>('post', '/api/v1/prism/providers', backendData);
  }

  async deleteProvider(id: number): Promise<void> {
    return this.request<void>('delete', `/api/v1/prism/providers/${id}`);
  }

  async getProviderModels(id: number): Promise<string[]> {
    return this.request<string[]>('get', `/api/v1/prism/providers/${id}/models`);
  }

  // Agent APIs
  async getAgents(): Promise<Agent[]> {
    interface AgentApiResponse {
      id: number;
      providerId: number;
      provider: {
        id: number;
        name: string;
        providerType: string;
      };
      name: string;
      role: string;
      description?: string;
      systemPrompt: string;
      model: string;
      temperature: number;
      maxTokens: number;
      createdAt: string;
      updatedAt: string;
    }
    const result = await this.request<{ items: AgentApiResponse[] }>('get', '/api/v1/prism/agents');
    const items = result.items ?? [];
    // Map API response to frontend type
    return items.map((a) => ({
      id: a.id,
      name: a.name,
      role: (a.role || 'CUSTOM') as Agent['role'],
      description: a.description,
      providerId: a.providerId,
      providerName: a.provider?.name ?? 'Unknown',
      model: a.model,
      systemPrompt: a.systemPrompt,
      temperature: a.temperature,
      maxTokens: a.maxTokens,
      isActive: true, // Backend doesn't have isActive for agents, default to true
      createdAt: a.createdAt,
      updatedAt: a.updatedAt,
    }));
  }

  async getAgent(id: number): Promise<Agent> {
    return this.request<Agent>('get', `/api/v1/prism/agents/${id}`);
  }

  async createAgent(data: CreateAgentRequest): Promise<Agent> {
    return this.request<Agent>('post', '/api/v1/prism/agents', data);
  }

  async updateAgent(id: number, data: Partial<CreateAgentRequest>): Promise<Agent> {
    return this.request<Agent>('put', `/api/v1/prism/agents/${id}`, data);
  }

  async deleteAgent(id: number): Promise<void> {
    return this.request<void>('delete', `/api/v1/prism/agents/${id}`);
  }

  // Board APIs
  async getBoards(): Promise<Board[]> {
    const result = await this.request<{ items: Board[] }>('get', '/api/v1/prism/boards');
    return result.items ?? result as unknown as Board[];
  }

  async getBoard(id: number): Promise<Board> {
    return this.request<Board>('get', `/api/v1/prism/boards/${id}`);
  }

  async createBoard(data: CreateBoardRequest): Promise<Board> {
    return this.request<Board>('post', '/api/v1/prism/boards', data);
  }

  async updateBoard(id: number, data: Partial<CreateBoardRequest>): Promise<Board> {
    return this.request<Board>('put', `/api/v1/prism/boards/${id}`, data);
  }

  async deleteBoard(id: number): Promise<void> {
    return this.request<void>('delete', `/api/v1/prism/boards/${id}`);
  }

  // Task APIs
  async getTasks(boardId: number): Promise<Task[]> {
    const tasks = await this.request<TaskApiResponse[]>('get', `/api/v1/prism/boards/${boardId}/tasks`);
    return tasks.map((t) => this.mapTaskResponse(t));
  }

  async getTask(id: number): Promise<Task> {
    const task = await this.request<TaskApiResponse>('get', `/api/v1/prism/tasks/${id}`);
    return this.mapTaskResponse(task);
  }

  async createTask(data: CreateTaskRequest): Promise<Task> {
    const { boardId, ...taskData } = data;
    const task = await this.request<TaskApiResponse>('post', `/api/v1/prism/boards/${boardId}/tasks`, taskData);
    return this.mapTaskResponse(task);
  }

  async updateTask(id: number, data: UpdateTaskRequest): Promise<Task> {
    const task = await this.request<TaskApiResponse>('put', `/api/v1/prism/tasks/${id}`, data);
    return this.mapTaskResponse(task);
  }

  async moveTask(id: number, data: MoveTaskRequest): Promise<Task> {
    const task = await this.request<TaskApiResponse>('patch', `/api/v1/prism/tasks/${id}/position`, data);
    return this.mapTaskResponse(task);
  }

  async assignAgent(taskId: number, agentId: number): Promise<Task> {
    // Backend uses UpdateTaskDto for agent assignment via PUT /tasks/:id
    const task = await this.request<TaskApiResponse>('put', `/api/v1/prism/tasks/${taskId}`, { agentId });
    return this.mapTaskResponse(task);
  }

  async deleteTask(id: number): Promise<void> {
    return this.request<void>('delete', `/api/v1/prism/tasks/${id}`);
  }

  // Task Actions
  async approveTask(id: number): Promise<Task> {
    const task = await this.request<TaskApiResponse>('post', `/api/v1/prism/tasks/${id}/approve`);
    return this.mapTaskResponse(task);
  }

  async rejectTask(id: number, feedback?: string): Promise<Task> {
    const task = await this.request<TaskApiResponse>('post', `/api/v1/prism/tasks/${id}/reject`, { feedback });
    return this.mapTaskResponse(task);
  }

  async cancelTask(id: number): Promise<Task> {
    const task = await this.request<TaskApiResponse>('post', `/api/v1/prism/tasks/${id}/cancel`);
    return this.mapTaskResponse(task);
  }

  async reopenTask(id: number): Promise<Task> {
    const task = await this.request<TaskApiResponse>('post', `/api/v1/prism/tasks/${id}/reopen`);
    return this.mapTaskResponse(task);
  }

  async getTaskContext(id: number): Promise<{ previousExecutions: Execution[]; referencedTasks: Array<{ taskId: number; taskTitle: string; lastExecution: Execution | null }> }> {
    return this.request('get', `/api/v1/prism/tasks/${id}/context`);
  }

  // Execution APIs
  async executeTask(taskId: number): Promise<Execution> {
    return this.request<Execution>('post', `/api/v1/prism/tasks/${taskId}/execute`);
  }

  async getExecutions(taskId: number): Promise<Execution[]> {
    return this.request<Execution[]>('get', `/api/v1/prism/tasks/${taskId}/executions`);
  }

  async getExecution(id: number): Promise<Execution> {
    return this.request<Execution>('get', `/api/v1/prism/executions/${id}`);
  }
}

export const api = new ApiService();
