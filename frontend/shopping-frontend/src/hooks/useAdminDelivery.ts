/**
 * Admin Delivery Hooks
 * 관리자 배송 관리 관련 React Hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { deliveryApi } from '@/api/endpoints'
import type { Delivery, UpdateDeliveryStatusRequest } from '@/types'

export function useDeliveryByOrder(orderNumber: string | null) {
  const [data, setData] = useState<Delivery | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    if (!orderNumber) return
    try {
      setIsLoading(true)
      setError(null)
      const response = await deliveryApi.getDeliveryByOrder(orderNumber)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch delivery'))
    } finally {
      setIsLoading(false)
    }
  }, [orderNumber])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { data, isLoading, error, refetch: fetch }
}

export function useTrackDelivery() {
  const [data, setData] = useState<Delivery | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const track = useCallback(async (trackingNumber: string) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await deliveryApi.trackDelivery(trackingNumber)
      if (response.success) {
        setData(response.data)
      }
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to track delivery')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { data, isLoading, error, track }
}

export function useUpdateDeliveryStatus() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const updateStatus = useCallback(async (trackingNumber: string, data: UpdateDeliveryStatusRequest) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await deliveryApi.updateDeliveryStatus(trackingNumber, data)
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to update delivery status')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: updateStatus, isPending: isLoading, error }
}
