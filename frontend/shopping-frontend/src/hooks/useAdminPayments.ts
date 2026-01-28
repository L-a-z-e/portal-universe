/**
 * Admin Payment Hooks
 * 관리자 결제/환불 관련 React Hooks
 */
import { useState, useCallback } from 'react'
import { adminPaymentApi } from '@/api/endpoints'

export function useRefundPayment() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const refund = useCallback(async (paymentNumber: string) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminPaymentApi.refundPayment(paymentNumber)
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to refund payment')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: refund, isPending: isLoading, error }
}
