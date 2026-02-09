import { getApiClient } from './client'
import type { ApiResponse } from '@/types'

const API_PREFIX = '/api/v1/shopping'

export const searchApi = {
  suggest: async (keyword: string, size = 5) => {
    const params = new URLSearchParams({ keyword, size: String(size) })
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/search/suggest?${params}`
    )
    return response.data
  },

  getPopularKeywords: async (size = 10) => {
    const params = new URLSearchParams({ size: String(size) })
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/search/popular?${params}`
    )
    return response.data
  },

  getRecentKeywords: async (size = 10) => {
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/search/recent?size=${size}`
    )
    return response.data
  },

  addRecentKeyword: async (keyword: string) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/search/recent?keyword=${encodeURIComponent(keyword)}`
    )
    return response.data
  },

  deleteRecentKeyword: async (keyword: string) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/search/recent/${encodeURIComponent(keyword)}`
    )
    return response.data
  },

  clearRecentKeywords: async () => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/search/recent`
    )
    return response.data
  }
}
