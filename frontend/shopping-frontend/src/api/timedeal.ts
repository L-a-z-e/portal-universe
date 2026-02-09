import { getApiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type { TimeDeal, TimeDealCreateRequest, TimeDealPurchase } from '@/dto/timedeal'

const API_PREFIX = '/api/v1/shopping'

export const timeDealApi = {
  getActiveTimeDeals: async () => {
    const response = await getApiClient().get<ApiResponse<TimeDeal[]>>(
      `${API_PREFIX}/time-deals`
    )
    return response.data
  },

  getTimeDeal: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<TimeDeal>>(
      `${API_PREFIX}/time-deals/${id}`
    )
    return response.data
  },

  purchaseTimeDeal: async (timeDealProductId: number, quantity: number) => {
    const response = await getApiClient().post<ApiResponse<unknown>>(
      `${API_PREFIX}/time-deals/purchase`,
      { timeDealProductId, quantity }
    )
    return response.data
  },

  getMyPurchases: async () => {
    const response = await getApiClient().get<ApiResponse<TimeDealPurchase[]>>(
      `${API_PREFIX}/time-deals/my/purchases`
    )
    return response.data
  }
}

export const adminTimeDealApi = {
  getTimeDeals: async (page = 1, size = 10) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PageResponse<TimeDeal>>>(
      `${API_PREFIX}/admin/time-deals?${params}`
    )
    return response.data
  },

  getTimeDeal: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<TimeDeal>>(
      `${API_PREFIX}/admin/time-deals/${id}`
    )
    return response.data
  },

  createTimeDeal: async (data: TimeDealCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<TimeDeal>>(
      `${API_PREFIX}/admin/time-deals`,
      data
    )
    return response.data
  },

  cancelTimeDeal: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/time-deals/${id}`
    )
    return response.data
  }
}
