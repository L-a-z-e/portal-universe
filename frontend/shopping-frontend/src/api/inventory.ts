import { getApiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type { Inventory, InventoryUpdateRequest, StockMovement } from '@/dto/inventory'

const API_PREFIX = '/api/v1/shopping'

export const inventoryApi = {
  getInventory: async (productId: number) => {
    const response = await getApiClient().get<ApiResponse<Inventory>>(
      `${API_PREFIX}/inventory/${productId}`
    )
    return response.data
  },

  getInventories: async (productIds: number[]) => {
    const response = await getApiClient().post<ApiResponse<Inventory[]>>(
      `${API_PREFIX}/inventory/batch`,
      { productIds }
    )
    return response.data
  },

  initializeInventory: async (productId: number, data: InventoryUpdateRequest) => {
    const response = await getApiClient().post<ApiResponse<Inventory>>(
      `${API_PREFIX}/inventory/${productId}`,
      data
    )
    return response.data
  },

  addStock: async (productId: number, data: InventoryUpdateRequest) => {
    const response = await getApiClient().put<ApiResponse<Inventory>>(
      `${API_PREFIX}/inventory/${productId}/add`,
      data
    )
    return response.data
  }
}

export const stockMovementApi = {
  getMovements: async (productId: number, page = 1, size = 20) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PageResponse<StockMovement>>>(
      `${API_PREFIX}/inventory/${productId}/movements?${params}`
    )
    return response.data
  }
}

export const inventoryStreamApi = {
  getStreamUrl: (productIds: number[]) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const params = productIds.map(id => `productIds=${id}`).join('&')
    return `${baseUrl}${API_PREFIX}/inventory/stream?${params}`
  }
}
