import { ref } from 'vue'
import apiClient from '../api/apiClient'
import type { ChatMessage, ChatResponse, ConversationSummary, StreamEvent } from '../types/chat'
import { authService } from '../services/authService'

export function useChat() {
  const messages = ref<ChatMessage[]>([])
  const conversations = ref<ConversationSummary[]>([])
  const currentConversationId = ref<string | null>(null)
  const loading = ref(false)
  const streaming = ref(false)

  async function sendMessage(message: string): Promise<void> {
    loading.value = true

    // 사용자 메시지를 즉시 UI에 추가
    const userMsg: ChatMessage = {
      message_id: crypto.randomUUID(),
      role: 'user',
      content: message,
      created_at: new Date().toISOString(),
    }
    messages.value.push(userMsg)

    try {
      const response = await apiClient.post<{ success: boolean; data: ChatResponse }>(
        '/api/v1/chat/message',
        {
          message,
          conversation_id: currentConversationId.value,
        }
      )

      const data = response.data.data
      currentConversationId.value = data.conversation_id

      const assistantMsg: ChatMessage = {
        message_id: data.message_id,
        role: 'assistant',
        content: data.answer,
        sources: data.sources,
        created_at: new Date().toISOString(),
      }
      messages.value.push(assistantMsg)
    } finally {
      loading.value = false
    }
  }

  async function sendMessageStream(message: string): Promise<void> {
    streaming.value = true

    const userMsg: ChatMessage = {
      message_id: crypto.randomUUID(),
      role: 'user',
      content: message,
      created_at: new Date().toISOString(),
    }
    messages.value.push(userMsg)

    // assistant placeholder
    const assistantMsg: ChatMessage = {
      message_id: '',
      role: 'assistant',
      content: '',
      sources: null,
      created_at: new Date().toISOString(),
    }
    messages.value.push(assistantMsg)
    const assistantIdx = messages.value.length - 1

    try {
      const token = authService.getAccessToken()
      const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
      const response = await fetch(`${baseUrl}/api/v1/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          message,
          conversation_id: currentConversationId.value,
        }),
      })

      if (!response.ok || !response.body) {
        throw new Error(`Stream failed: ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (!line.startsWith('data: ')) continue
          const jsonStr = line.slice(6).trim()
          if (!jsonStr) continue

          try {
            const event: StreamEvent = JSON.parse(jsonStr)

            const assistantMsg = messages.value[assistantIdx]
            if (!assistantMsg) continue

            if (event.type === 'token' && event.content) {
              assistantMsg.content += event.content
            } else if (event.type === 'sources' && event.sources) {
              assistantMsg.sources = event.sources
            } else if (event.type === 'done') {
              if (event.conversation_id) {
                currentConversationId.value = event.conversation_id
              }
              if (event.message_id) {
                assistantMsg.message_id = event.message_id
              }
            }
          } catch {
            // skip malformed JSON
          }
        }
      }
    } finally {
      streaming.value = false
    }
  }

  async function loadConversations(): Promise<void> {
    const response = await apiClient.get<{ success: boolean; data: ConversationSummary[] }>(
      '/api/v1/chat/conversations'
    )
    conversations.value = response.data.data
  }

  async function loadConversation(conversationId: string): Promise<void> {
    currentConversationId.value = conversationId
    const response = await apiClient.get<{ success: boolean; data: ChatMessage[] }>(
      `/api/v1/chat/conversations/${conversationId}`
    )
    messages.value = response.data.data
  }

  function startNewConversation(): void {
    currentConversationId.value = null
    messages.value = []
  }

  async function deleteConversation(conversationId: string): Promise<void> {
    await apiClient.delete(`/api/v1/chat/conversations/${conversationId}`)
    conversations.value = conversations.value.filter(
      (c) => c.conversation_id !== conversationId
    )
    if (currentConversationId.value === conversationId) {
      startNewConversation()
    }
  }

  return {
    messages,
    conversations,
    currentConversationId,
    loading,
    streaming,
    sendMessage,
    sendMessageStream,
    loadConversations,
    loadConversation,
    startNewConversation,
    deleteConversation,
  }
}
