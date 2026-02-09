import { getApiClient } from './client'
import type { ApiResponse } from '@/types'
import type { Payment, ProcessPaymentRequest } from '@/dto/payment'

const API_PREFIX = '/api/v1/shopping'

export const paymentApi = {
  getPayment: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Payment>>(
      `${API_PREFIX}/payments/${orderNumber}`
    )
    return response.data
  },

  processPayment: async (data: ProcessPaymentRequest) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${API_PREFIX}/payments`,
      data
    )
    return response.data
  },

  cancelPayment: async (orderNumber: string) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${API_PREFIX}/payments/${orderNumber}/cancel`
    )
    return response.data
  }
}

export const adminPaymentApi = {
  refundPayment: async (paymentNumber: string) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${API_PREFIX}/payments/${paymentNumber}/refund`
    )
    return response.data
  }
}
