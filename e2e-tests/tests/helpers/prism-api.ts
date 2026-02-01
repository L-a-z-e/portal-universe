/**
 * Prism API Helper for E2E Tests
 *
 * Provides typed API calls for seeding data and verifying state.
 */

interface ApiResponse<T> {
  success: boolean
  data: T
  error: unknown
}

export interface PrismProvider {
  id: number
  providerType: string
  name: string
  apiKeyMasked: string
  baseUrl: string | null
  isActive: boolean
  models: string[] | null
}

export interface PrismAgent {
  id: number
  providerId: number
  name: string
  role: string
  model: string
  systemPrompt: string
  temperature: number
  maxTokens: number
}

export interface PrismBoard {
  id: number
  name: string
  description: string | null
  isArchived: boolean
}

export interface PrismTask {
  id: number
  boardId: number
  agentId: number | null
  title: string
  description: string | null
  status: string
  priority: string
  position: number
  availableActions: string[]
}

export interface PrismExecution {
  id: number
  taskId: number
  agentId: number
  executionNumber: number
  status: string
  inputPrompt: string
  outputResult: string | null
  inputTokens: number | null
  outputTokens: number | null
  durationMs: number | null
  errorMessage: string | null
}

export class PrismApi {
  constructor(
    private baseUrl: string,
    private token: string,
  ) {}

  private async request<T>(method: string, path: string, body?: unknown): Promise<T | null> {
    try {
      const resp = await fetch(`${this.baseUrl}/api/v1/prism${path}`, {
        method,
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: body ? JSON.stringify(body) : undefined,
      })
      if (!resp.ok) {
        const text = await resp.text()
        console.log(`  ${method} ${path} failed: ${resp.status} ${text.substring(0, 200)}`)
        return null
      }
      const json: ApiResponse<T> = await resp.json()
      return json.success ? json.data : null
    } catch (e) {
      console.log(`  ${method} ${path} error: ${e}`)
      return null
    }
  }

  // Provider
  async createProvider(data: {
    providerType: string
    name: string
    apiKey: string
    baseUrl?: string
  }): Promise<PrismProvider | null> {
    return this.request<PrismProvider>('POST', '/providers', data)
  }

  async getProviders(): Promise<PrismProvider[]> {
    return (await this.request<PrismProvider[]>('GET', '/providers')) ?? []
  }

  async deleteProvider(id: number): Promise<void> {
    await this.request('DELETE', `/providers/${id}`)
  }

  async verifyProvider(id: number): Promise<{ success: boolean; models?: string[] } | null> {
    return this.request('POST', `/providers/${id}/verify`)
  }

  // Agent
  async createAgent(data: {
    providerId: number
    name: string
    role: string
    systemPrompt: string
    model: string
    temperature?: number
    maxTokens?: number
  }): Promise<PrismAgent | null> {
    return this.request<PrismAgent>('POST', '/agents', data)
  }

  async getAgents(): Promise<PrismAgent[]> {
    return (await this.request<PrismAgent[]>('GET', '/agents')) ?? []
  }

  async deleteAgent(id: number): Promise<void> {
    await this.request('DELETE', `/agents/${id}`)
  }

  // Board
  async createBoard(data: {
    name: string
    description?: string
  }): Promise<PrismBoard | null> {
    return this.request<PrismBoard>('POST', '/boards', data)
  }

  async getBoards(): Promise<PrismBoard[]> {
    return (await this.request<PrismBoard[]>('GET', '/boards')) ?? []
  }

  async deleteBoard(id: number): Promise<void> {
    await this.request('DELETE', `/boards/${id}`)
  }

  // Task
  async createTask(boardId: number, data: {
    title: string
    description?: string
    priority?: string
    agentId?: number
  }): Promise<PrismTask | null> {
    return this.request<PrismTask>('POST', `/boards/${boardId}/tasks`, data)
  }

  async getTasks(boardId: number): Promise<PrismTask[]> {
    return (await this.request<PrismTask[]>('GET', `/boards/${boardId}/tasks`)) ?? []
  }

  async deleteTask(id: number): Promise<void> {
    await this.request('DELETE', `/tasks/${id}`)
  }

  async executeTask(taskId: number): Promise<PrismExecution | null> {
    return this.request<PrismExecution>('POST', `/tasks/${taskId}/execute`)
  }

  // Execution
  async getExecutions(taskId: number): Promise<PrismExecution[]> {
    return (await this.request<PrismExecution[]>('GET', `/tasks/${taskId}/executions`)) ?? []
  }
}

/**
 * Check if Ollama is running locally.
 */
export async function checkOllamaAvailable(ollamaUrl = 'http://localhost:11434'): Promise<boolean> {
  try {
    const resp = await fetch(`${ollamaUrl}/api/tags`, { signal: AbortSignal.timeout(3000) })
    return resp.ok
  } catch {
    return false
  }
}

/**
 * Get first available Ollama model name.
 */
export async function getOllamaModel(ollamaUrl = 'http://localhost:11434'): Promise<string | null> {
  try {
    const resp = await fetch(`${ollamaUrl}/api/tags`, { signal: AbortSignal.timeout(3000) })
    if (!resp.ok) return null
    const data = await resp.json() as { models: { name: string }[] }
    return data.models?.[0]?.name ?? null
  } catch {
    return null
  }
}
