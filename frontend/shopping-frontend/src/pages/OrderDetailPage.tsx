/**
 * Order Detail Page
 *
 * 주문 상세 및 배송 추적 페이지
 */
import React, { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { orderApi, deliveryApi } from '@/api/endpoints'
import type { Order, Delivery, OrderStatus, DeliveryStatus } from '@/types'
import { ORDER_STATUS_LABELS, DELIVERY_STATUS_LABELS } from '@/types'
import { Button, Spinner, Alert, Badge, useApiError } from '@portal/design-system-react'

const OrderDetailPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const navigate = useNavigate()
  const { handleError } = useApiError()

  // State
  const [order, setOrder] = useState<Order | null>(null)
  const [delivery, setDelivery] = useState<Delivery | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState(false)

  // Fetch order and delivery
  useEffect(() => {
    const fetchData = async () => {
      if (!orderNumber) return

      setLoading(true)
      setError(null)

      try {
        const orderRes = await orderApi.getOrder(orderNumber)
        setOrder(orderRes.data)

        // Fetch delivery info if order is paid/shipping
        if (['PAID', 'SHIPPING', 'DELIVERED'].includes(orderRes.data.status)) {
          try {
            const deliveryRes = await deliveryApi.getDeliveryByOrder(orderNumber)
            setDelivery(deliveryRes.data)
          } catch (deliveryErr) {
            console.warn('Failed to fetch delivery:', deliveryErr)
          }
        }
      } catch (err: unknown) {
        const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } }; message?: string };
        if (axiosErr.response?.status === 404) {
          setError('주문을 찾을 수 없습니다.')
        } else {
          setError(axiosErr.response?.data?.error?.message || axiosErr.message || '주문 정보를 불러오는 데 실패했습니다.')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [orderNumber])

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const getStatusColor = (status: OrderStatus | DeliveryStatus) => {
    switch (status) {
      case 'PENDING':
      case 'CONFIRMED':
      case 'PREPARING':
        return 'bg-status-warning text-white'
      case 'PAID':
      case 'SHIPPED':
      case 'IN_TRANSIT':
      case 'OUT_FOR_DELIVERY':
        return 'bg-status-info text-white'
      case 'DELIVERED':
      case 'SHIPPING':
        return 'bg-status-success text-white'
      case 'CANCELLED':
      case 'REFUNDED':
      case 'FAILED':
        return 'bg-status-error text-white'
      default:
        return 'bg-bg-subtle text-text-meta'
    }
  }

  const handleCancelOrder = async () => {
    if (!order || !window.confirm('Are you sure you want to cancel this order?')) return

    setCancelling(true)
    try {
      const response = await orderApi.cancelOrder(order.orderNumber, {
        reason: 'Cancelled by customer'
      })
      setOrder(response.data)
    } catch (err: unknown) {
      handleError(err, '주문 취소에 실패했습니다.')
    } finally {
      setCancelling(false)
    }
  }

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-4">
          <Spinner size="lg" />
          <p className="text-text-meta">Loading order...</p>
        </div>
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="space-y-6">
        <Button
          onClick={() => navigate(-1)}
          variant="ghost"
          className="gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back
        </Button>

        <Alert variant="error" className="text-center">
          <p className="text-lg mb-4">{error}</p>
          <Button asChild variant="primary">
            <Link to="/orders">View All Orders</Link>
          </Button>
        </Alert>
      </div>
    )
  }

  if (!order) return null

  const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED' || order.status === 'PAID'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link
            to="/orders"
            className="flex items-center gap-2 text-text-meta hover:text-text-body transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Orders
          </Link>
          <span className="text-text-meta">/</span>
          <span className="font-mono text-text-body">{order.orderNumber}</span>
        </div>

        {canCancel && (
          <Button
            onClick={handleCancelOrder}
            disabled={cancelling}
            variant="outline"
            className="text-status-error border-status-error hover:bg-status-error hover:text-white"
          >
            {cancelling ? 'Cancelling...' : 'Cancel Order'}
          </Button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Order Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Status Card */}
          <div className="bg-bg-card border border-border-default rounded-lg p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-text-heading">Order Status</h2>
              <span className={`px-3 py-1 rounded text-sm font-medium ${getStatusColor(order.status)}`}>
                {ORDER_STATUS_LABELS[order.status]}
              </span>
            </div>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-text-meta">Order Date</span>
                <span className="text-text-body">{formatDate(order.createdAt)}</span>
              </div>
              {order.cancelledAt && (
                <div className="flex justify-between">
                  <span className="text-text-meta">Cancelled Date</span>
                  <span className="text-status-error">{formatDate(order.cancelledAt)}</span>
                </div>
              )}
              {order.cancelReason && (
                <div className="flex justify-between">
                  <span className="text-text-meta">Cancel Reason</span>
                  <span className="text-status-error">{order.cancelReason}</span>
                </div>
              )}
            </div>
          </div>

          {/* Items */}
          <div className="bg-bg-card border border-border-default rounded-lg p-6">
            <h2 className="text-lg font-bold text-text-heading mb-4">Order Items</h2>

            <div className="divide-y divide-border-default">
              {order.items.map((item) => (
                <div key={item.id} className="py-4 first:pt-0 last:pb-0">
                  <div className="flex items-center gap-4">
                    <div className="w-16 h-16 bg-bg-subtle rounded-lg flex items-center justify-center text-text-placeholder">
                      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={1.5}
                          d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                        />
                      </svg>
                    </div>

                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium text-text-heading truncate">
                        {item.productName}
                      </h3>
                      <p className="text-sm text-text-meta">
                        {formatPrice(item.price)} x {item.quantity}
                      </p>
                    </div>

                    <div className="text-right">
                      <span className="font-medium text-text-heading">
                        {formatPrice(item.subtotal)}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Delivery Tracking */}
          {delivery && (
            <div className="bg-bg-card border border-border-default rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-text-heading">Delivery Tracking</h2>
                <span className={`px-3 py-1 rounded text-sm font-medium ${getStatusColor(delivery.status)}`}>
                  {DELIVERY_STATUS_LABELS[delivery.status]}
                </span>
              </div>

              <div className="space-y-2 text-sm mb-6">
                <div className="flex justify-between">
                  <span className="text-text-meta">Tracking Number</span>
                  <span className="font-mono text-text-body">{delivery.trackingNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Carrier</span>
                  <span className="text-text-body">{delivery.carrier}</span>
                </div>
                {delivery.estimatedDeliveryDate && (
                  <div className="flex justify-between">
                    <span className="text-text-meta">Estimated Delivery</span>
                    <span className="text-text-body">{formatDate(delivery.estimatedDeliveryDate)}</span>
                  </div>
                )}
              </div>

              {/* Timeline */}
              {delivery.history.length > 0 && (
                <div className="border-t border-border-default pt-4">
                  <h3 className="text-sm font-medium text-text-heading mb-4">Tracking History</h3>
                  <div className="space-y-4">
                    {delivery.history.map((event, index) => (
                      <div key={event.id} className="flex gap-4">
                        <div className="relative">
                          <div className={`w-3 h-3 rounded-full ${
                            index === 0 ? 'bg-brand-primary' : 'bg-border-default'
                          }`} />
                          {index < delivery.history.length - 1 && (
                            <div className="absolute top-3 left-1/2 -translate-x-1/2 w-0.5 h-full bg-border-default" />
                          )}
                        </div>
                        <div className="flex-1 pb-4">
                          <p className={`text-sm font-medium ${
                            index === 0 ? 'text-text-heading' : 'text-text-meta'
                          }`}>
                            {DELIVERY_STATUS_LABELS[event.status]}
                          </p>
                          {event.location && (
                            <p className="text-xs text-text-meta">{event.location}</p>
                          )}
                          {event.description && (
                            <p className="text-xs text-text-meta">{event.description}</p>
                          )}
                          <p className="text-xs text-text-placeholder mt-1">
                            {formatDate(event.createdAt)}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Summary Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-bg-card border border-border-default rounded-lg p-6 sticky top-6">
            {/* Shipping Address */}
            <div className="mb-6">
              <h3 className="font-medium text-text-heading mb-2">Shipping Address</h3>
              <div className="text-sm text-text-body space-y-1">
                <p className="font-medium">{order.shippingAddress.receiverName}</p>
                <p>{order.shippingAddress.receiverPhone}</p>
                <p>
                  [{order.shippingAddress.zipCode}] {order.shippingAddress.address1}
                  {order.shippingAddress.address2 && ` ${order.shippingAddress.address2}`}
                </p>
              </div>
            </div>

            {/* Order Summary */}
            <div className="border-t border-border-default pt-4">
              <h3 className="font-medium text-text-heading mb-4">Order Summary</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-text-meta">Subtotal</span>
                  <span className="text-text-body">{formatPrice(order.totalAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Shipping</span>
                  <span className="text-text-body">Free</span>
                </div>
                <div className="border-t border-border-default pt-2 mt-2">
                  <div className="flex justify-between">
                    <span className="font-bold text-text-heading">Total</span>
                    <span className="font-bold text-brand-primary text-lg">
                      {formatPrice(order.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default OrderDetailPage
