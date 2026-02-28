import { getApiClient } from './client'
import type { ApiResponse } from '@/types'
import type { Payment, PaymentIntent, ConfirmPaymentRequest } from '@/dto/payment'

const PAYMENT_API_PREFIX = '/api/v1/payment'

export const paymentApi = {
  getIntent: async (intentId: string) => {
    const response = await getApiClient().get<ApiResponse<PaymentIntent>>(
      `${PAYMENT_API_PREFIX}/intents/${intentId}`
    )
    return response.data
  },

  confirmPayment: async (intentId: string, data: ConfirmPaymentRequest) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${PAYMENT_API_PREFIX}/intents/${intentId}/confirm`,
      data
    )
    return response.data
  },

  getPayment: async (paymentNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Payment>>(
      `${PAYMENT_API_PREFIX}/payments/${paymentNumber}`
    )
    return response.data
  },

  cancelPayment: async (paymentNumber: string) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${PAYMENT_API_PREFIX}/payments/${paymentNumber}/cancel`
    )
    return response.data
  },
}

export const adminPaymentApi = {
  refundPayment: async (paymentNumber: string) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${PAYMENT_API_PREFIX}/payments/${paymentNumber}/refund`
    )
    return response.data
  }
}
