export interface SourceInfo {
  document: string
  chunk: string
  relevance_score: number
}

export interface ChatMessage {
  message_id: string
  role: 'user' | 'assistant'
  content: string
  sources?: SourceInfo[] | null
  created_at: string
}

export interface ChatRequest {
  message: string
  conversation_id?: string
}

export interface ChatResponse {
  answer: string
  sources: SourceInfo[]
  conversation_id: string
  message_id: string
}

export interface ConversationSummary {
  conversation_id: string
  title: string
  message_count: number
  created_at: string
  updated_at: string
}

export interface StreamEvent {
  type: 'token' | 'sources' | 'done'
  content?: string
  sources?: SourceInfo[]
  message_id?: string
  conversation_id?: string
}
