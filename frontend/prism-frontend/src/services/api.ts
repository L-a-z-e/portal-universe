import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
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

// Window 타입 확장 (Portal Shell 연동)
declare global {
  interface Window {
    __POWERED_BY_PORTAL_SHELL__?: boolean;
    __PORTAL_ACCESS_TOKEN__?: string;
    __PORTAL_API_CLIENT__?: AxiosInstance;
    __PORTAL_ON_AUTH_ERROR__?: () => void;
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
        // 1. Portal Shell에서 주입된 토큰 우선 확인
        const portalToken = window.__PORTAL_ACCESS_TOKEN__;
        // 2. localStorage에서 토큰 확인 (standalone 모드)
        const localToken = localStorage.getItem('accessToken');

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
      throw new Error(response.data.error?.message || 'Request failed');
    }

    return response.data.data as T;
  }

  // Provider APIs
  async getProviders(): Promise<Provider[]> {
    return this.request<Provider[]>('get', '/prism/providers');
  }

  async getProvider(id: number): Promise<Provider> {
    return this.request<Provider>('get', `/prism/providers/${id}`);
  }

  async createProvider(data: CreateProviderRequest): Promise<Provider> {
    return this.request<Provider>('post', '/prism/providers', data);
  }

  async deleteProvider(id: number): Promise<void> {
    return this.request<void>('delete', `/prism/providers/${id}`);
  }

  // Agent APIs
  async getAgents(): Promise<Agent[]> {
    return this.request<Agent[]>('get', '/prism/agents');
  }

  async getAgent(id: number): Promise<Agent> {
    return this.request<Agent>('get', `/prism/agents/${id}`);
  }

  async createAgent(data: CreateAgentRequest): Promise<Agent> {
    return this.request<Agent>('post', '/prism/agents', data);
  }

  async updateAgent(id: number, data: Partial<CreateAgentRequest>): Promise<Agent> {
    return this.request<Agent>('put', `/prism/agents/${id}`, data);
  }

  async deleteAgent(id: number): Promise<void> {
    return this.request<void>('delete', `/prism/agents/${id}`);
  }

  // Board APIs
  async getBoards(): Promise<Board[]> {
    return this.request<Board[]>('get', '/prism/boards');
  }

  async getBoard(id: number): Promise<Board> {
    return this.request<Board>('get', `/prism/boards/${id}`);
  }

  async createBoard(data: CreateBoardRequest): Promise<Board> {
    return this.request<Board>('post', '/prism/boards', data);
  }

  async updateBoard(id: number, data: Partial<CreateBoardRequest>): Promise<Board> {
    return this.request<Board>('put', `/prism/boards/${id}`, data);
  }

  async deleteBoard(id: number): Promise<void> {
    return this.request<void>('delete', `/prism/boards/${id}`);
  }

  // Task APIs
  async getTasks(boardId: number): Promise<Task[]> {
    return this.request<Task[]>('get', `/prism/boards/${boardId}/tasks`);
  }

  async getTask(id: number): Promise<Task> {
    return this.request<Task>('get', `/prism/tasks/${id}`);
  }

  async createTask(data: CreateTaskRequest): Promise<Task> {
    const { boardId, ...taskData } = data;
    return this.request<Task>('post', `/prism/boards/${boardId}/tasks`, taskData);
  }

  async updateTask(id: number, data: UpdateTaskRequest): Promise<Task> {
    return this.request<Task>('put', `/prism/tasks/${id}`, data);
  }

  async moveTask(id: number, data: MoveTaskRequest): Promise<Task> {
    return this.request<Task>('patch', `/prism/tasks/${id}/position`, data);
  }

  async assignAgent(taskId: number, agentId: number): Promise<Task> {
    // Backend uses UpdateTaskDto for agent assignment via PUT /tasks/:id
    return this.request<Task>('put', `/prism/tasks/${taskId}`, { agentId });
  }

  async deleteTask(id: number): Promise<void> {
    return this.request<void>('delete', `/prism/tasks/${id}`);
  }

  // Execution APIs
  async executeTask(taskId: number): Promise<Execution> {
    return this.request<Execution>('post', `/prism/tasks/${taskId}/execute`);
  }

  async getExecutions(taskId: number): Promise<Execution[]> {
    return this.request<Execution[]>('get', `/prism/tasks/${taskId}/executions`);
  }

  async getExecution(id: number): Promise<Execution> {
    return this.request<Execution>('get', `/prism/executions/${id}`);
  }
}

export const api = new ApiService();
