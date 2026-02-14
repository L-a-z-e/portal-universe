import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sellerOrderApi } from '@/api'
import { Button } from '@portal/design-system-react'

export const OrderDetailPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!orderNumber) return
    const load = async () => {
      try {
        const res = await sellerOrderApi.getOrder(orderNumber)
        setOrder(res.data || null)
      } catch (err) {
        console.error(err)
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [orderNumber])

  if (isLoading) {
    return <div className="p-12 text-center text-text-meta">Loading...</div>
  }

  if (!order) {
    return (
      <div>
        <Button variant="ghost" onClick={() => navigate('/orders')}>
          ← Back
        </Button>
        <p className="text-status-error mt-4">Order not found</p>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Order {order.orderNumber}</h1>
        <Button variant="ghost" onClick={() => navigate('/orders')}>
          Back
        </Button>
      </div>
      <div className="bg-bg-card border border-border-default rounded-lg p-6">
        <p className="text-sm text-text-meta">Status: {order.status}</p>
        <p className="text-sm text-text-meta">Amount: {order.finalAmount || order.totalAmount}</p>
      </div>
    </div>
  )
}

export default OrderDetailPage
