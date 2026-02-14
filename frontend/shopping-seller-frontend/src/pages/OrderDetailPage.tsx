import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sellerOrderApi } from '@/api'
import { Button } from '@portal/design-system-react'

export const OrderDetailPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!orderNumber) return
    const load = async () => {
      try {
        const res = await sellerOrderApi.getOrder(orderNumber)
        setOrder(res.data?.data || null)
      } catch (err: any) {
        const status = err?.response?.status
        if (status === 404 || status === 502 || status === 503) {
          setError('Order data is not available. Cross-service integration with shopping-service is required.')
        } else {
          console.error(err)
          setError('Failed to load order details.')
        }
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [orderNumber])

  if (isLoading) {
    return <div className="p-12 text-center text-text-meta">Loading...</div>
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Order {orderNumber}</h1>
        <Button variant="ghost" onClick={() => navigate('/orders')}>
          Back
        </Button>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-status-warning-bg border border-status-warning rounded-lg">
          <p className="text-sm text-status-warning">{error}</p>
        </div>
      )}

      {order && (
        <div className="bg-bg-card border border-border-default rounded-lg p-6">
          <p className="text-sm text-text-meta">Status: {order.status}</p>
          <p className="text-sm text-text-meta">Amount: {order.finalAmount || order.totalAmount}</p>
        </div>
      )}

      {!order && !error && (
        <p className="text-status-error">Order not found</p>
      )}
    </div>
  )
}

export default OrderDetailPage
