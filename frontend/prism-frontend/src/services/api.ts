import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ErrorDetails } from '@portal/design-types';
import type {
  ApiResponse,
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
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: getBaseUrl(),
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    // Request Interceptor: 토큰 자동 첨부
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        // 1. Portal Shell에서 주입된 토큰 우선 확인 (getter 함수 우선)
        const portalToken = window.__PORTAL_GET_ACCESS_TOKEN__?.() ?? window.__PORTAL_ACCESS_TOKEN__;
        // 2. localStorage에서 토큰 확인 (standalone 모드)
        const localToken = localStorage.getItem('access_token');

        const token = portalToken || localToken;

        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        if (import.meta.env.DEV) {
          console.log(`[Prism API] ${config.method?.toUpperCase()} ${config.url}`);
        }

        return config;
      }
    );

    // Response Interceptor: 에러 핸들링
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiResponse<null>>) => {
        const status = error.response?.status;

        if (status === 401) {
          console.warn('[Prism API] Unauthorized - token may be expired');
          // Portal Shell에게 인증 만료 알림
          if (window.__PORTAL_ON_AUTH_ERROR__) {
            window.__PORTAL_ON_AUTH_ERROR__();
          }
        } else if (status === 403) {
          console.warn('[Prism API] Forbidden - insufficient permissions');
        }

        // 에러 응답에서 errorDetails 추출하여 보존
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

    if (!response.data.success) {
      const errorData = response.data.error;
      throw new ApiError(
        errorData?.message || 'Request failed',
        errorData?.code,
        errorData ?? undefined,
      );
    }

    return response.data.data as T;
  }

  // Provider APIs
  async getProviders(): Promise<Provider[]> {
    return this.request<Provider[]>('get', '/api/v1/prism/providers');
  }

  async getProvider(id: number): Promise<Provider> {
    return this.request<Provider>('get', `/api/v1/prism/providers/${id}`);
  }

  async createProvider(data: CreateProviderRequest): Promise<Provider> {
    return this.request<Provider>('post', '/api/v1/prism/providers', data);
  }

  async deleteProvider(id: number): Promise<void> {
    return this.request<void>('delete', `/api/v1/prism/providers/${id}`);
  }

  // Agent APIs
  async getAgents(): Promise<Agent[]> {
    return this.request<Agent[]>('get', '/api/v1/prism/agents');
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
    return this.request<Board[]>('get', '/api/v1/prism/boards');
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
    return this.request<Task[]>('get', `/api/v1/prism/boards/${boardId}/tasks`);
  }

  async getTask(id: number): Promise<Task> {
    return this.request<Task>('get', `/api/v1/prism/tasks/${id}`);
  }

  async createTask(data: CreateTaskRequest): Promise<Task> {
    const { boardId, ...taskData } = data;
    return this.request<Task>('post', `/api/v1/prism/boards/${boardId}/tasks`, taskData);
  }

  async updateTask(id: number, data: UpdateTaskRequest): Promise<Task> {
    return this.request<Task>('put', `/api/v1/prism/tasks/${id}`, data);
  }

  async moveTask(id: number, data: MoveTaskRequest): Promise<Task> {
    return this.request<Task>('patch', `/api/v1/prism/tasks/${id}/position`, data);
  }

  async assignAgent(taskId: number, agentId: number): Promise<Task> {
    // Backend uses UpdateTaskDto for agent assignment via PUT /tasks/:id
    return this.request<Task>('put', `/api/v1/prism/tasks/${taskId}`, { agentId });
  }

  async deleteTask(id: number): Promise<void> {
    return this.request<void>('delete', `/api/v1/prism/tasks/${id}`);
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
