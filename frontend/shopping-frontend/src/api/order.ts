import { getApiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type { Order, CreateOrderRequest, CancelOrderRequest } from '@/dto/order'

const API_PREFIX = '/api/v1/shopping'

export const orderApi = {
  getOrders: async (page = 1, size = 10) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PageResponse<Order>>>(
      `${API_PREFIX}/orders?${params}`
    )
    return response.data
  },

  getOrder: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Order>>(
      `${API_PREFIX}/orders/${orderNumber}`
    )
    return response.data
  },

  createOrder: async (data: CreateOrderRequest) => {
    const response = await getApiClient().post<ApiResponse<Order>>(
      `${API_PREFIX}/orders`,
      data
    )
    return response.data
  },

  cancelOrder: async (orderNumber: string, data: CancelOrderRequest) => {
    const response = await getApiClient().post<ApiResponse<Order>>(
      `${API_PREFIX}/orders/${orderNumber}/cancel`,
      data
    )
    return response.data
  }
}

export const adminOrderApi = {
  getOrders: async (params: {
    page?: number
    size?: number
    status?: string
    keyword?: string
  } = {}) => {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        searchParams.append(key, String(value))
      }
    })
    const response = await getApiClient().get<ApiResponse<PageResponse<Order>>>(
      `${API_PREFIX}/admin/orders?${searchParams}`
    )
    return response.data
  },

  getOrder: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Order>>(
      `${API_PREFIX}/admin/orders/${orderNumber}`
    )
    return response.data
  },

  updateOrderStatus: async (orderNumber: string, status: string) => {
    const response = await getApiClient().put<ApiResponse<Order>>(
      `${API_PREFIX}/admin/orders/${orderNumber}/status`,
      { status }
    )
    return response.data
  }
}
