/**
 * Admin Order Hooks
 * 관리자 주문 관리 관련 React Hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { adminOrderApi } from '@/api/endpoints'
import type { Order, PagedResponse } from '@/types'

interface UseAdminOrdersOptions {
  page?: number
  size?: number
  status?: string
  keyword?: string
}

export function useAdminOrders(options: UseAdminOrdersOptions = {}) {
  const { page = 0, size = 20, status, keyword } = options
  const [data, setData] = useState<PagedResponse<Order> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchOrders = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminOrderApi.getOrders({ page, size, status, keyword })
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch orders'))
    } finally {
      setIsLoading(false)
    }
  }, [page, size, status, keyword])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  return { data, isLoading, error, refetch: fetchOrders }
}

export function useAdminOrder(orderNumber: string | null) {
  const [data, setData] = useState<Order | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchOrder = useCallback(async () => {
    if (!orderNumber) {
      setIsLoading(false)
      return
    }
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminOrderApi.getOrder(orderNumber)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch order'))
    } finally {
      setIsLoading(false)
    }
  }, [orderNumber])

  useEffect(() => {
    fetchOrder()
  }, [fetchOrder])

  return { data, isLoading, error, refetch: fetchOrder }
}

export function useUpdateOrderStatus() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const updateStatus = useCallback(async (orderNumber: string, status: string) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminOrderApi.updateOrderStatus(orderNumber, status)
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to update order status')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: updateStatus, isPending: isLoading, error }
}
