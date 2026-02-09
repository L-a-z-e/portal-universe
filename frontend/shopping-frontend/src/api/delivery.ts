import { getApiClient } from './client'
import type { ApiResponse } from '@/types'
import type { Delivery, UpdateDeliveryStatusRequest } from '@/dto/delivery'

const API_PREFIX = '/api/v1/shopping'

export const deliveryApi = {
  getDeliveryByOrder: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Delivery>>(
      `${API_PREFIX}/deliveries/order/${orderNumber}`
    )
    return response.data
  },

  trackDelivery: async (trackingNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Delivery>>(
      `${API_PREFIX}/deliveries/${trackingNumber}`
    )
    return response.data
  },

  updateDeliveryStatus: async (trackingNumber: string, data: UpdateDeliveryStatusRequest) => {
    const response = await getApiClient().put<ApiResponse<Delivery>>(
      `${API_PREFIX}/deliveries/${trackingNumber}/status`,
      data
    )
    return response.data
  }
}
