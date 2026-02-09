import { getApiClient } from './client'
import type { ApiResponse } from '@/types'
import type { Cart, AddCartItemRequest, UpdateCartItemRequest } from '@/dto/cart'

const API_PREFIX = '/api/v1/shopping'

export const cartApi = {
  getCart: async () => {
    const response = await getApiClient().get<ApiResponse<Cart>>(
      `${API_PREFIX}/cart`
    )
    return response.data
  },

  addItem: async (data: AddCartItemRequest) => {
    const response = await getApiClient().post<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/items`,
      data
    )
    return response.data
  },

  updateItem: async (itemId: number, data: UpdateCartItemRequest) => {
    const response = await getApiClient().put<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/items/${itemId}`,
      data
    )
    return response.data
  },

  removeItem: async (itemId: number) => {
    const response = await getApiClient().delete<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/items/${itemId}`
    )
    return response.data
  },

  clearCart: async () => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/cart`
    )
    return response.data
  },

  checkout: async () => {
    const response = await getApiClient().post<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/checkout`
    )
    return response.data
  }
}
