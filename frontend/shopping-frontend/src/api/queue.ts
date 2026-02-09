import { getApiClient } from './client'
import type { ApiResponse } from '@/types'
import type { QueueStatusResponse, QueueActivateRequest } from '@/dto/queue'

const API_PREFIX = '/api/v1/shopping'

export const queueApi = {
  enterQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().post<ApiResponse<QueueStatusResponse>>(
      `${API_PREFIX}/queue/${eventType}/${eventId}/enter`
    )
    return response.data
  },

  getQueueStatus: async (eventType: string, eventId: number) => {
    const response = await getApiClient().get<ApiResponse<QueueStatusResponse>>(
      `${API_PREFIX}/queue/${eventType}/${eventId}/status`
    )
    return response.data
  },

  getQueueStatusByToken: async (entryToken: string) => {
    const response = await getApiClient().get<ApiResponse<QueueStatusResponse>>(
      `${API_PREFIX}/queue/token/${entryToken}`
    )
    return response.data
  },

  leaveQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/queue/${eventType}/${eventId}/leave`
    )
    return response.data
  },

  leaveQueueByToken: async (entryToken: string) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/queue/token/${entryToken}`
    )
    return response.data
  },

  getSubscribeUrl: (eventType: string, eventId: number, entryToken: string) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    return `${baseUrl}${API_PREFIX}/queue/${eventType}/${eventId}/subscribe/${entryToken}`
  }
}

export const adminQueueApi = {
  activateQueue: async (eventType: string, eventId: number, request: QueueActivateRequest) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/admin/queue/${eventType}/${eventId}/activate`,
      request
    )
    return response.data
  },

  deactivateQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/admin/queue/${eventType}/${eventId}/deactivate`
    )
    return response.data
  },

  processQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/admin/queue/${eventType}/${eventId}/process`
    )
    return response.data
  }
}
