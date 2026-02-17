/**
 * Admin Order Detail Page
 * 관리자 주문 상세 + 상태 변경 + 환불
 */
import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAdminOrder, useUpdateOrderStatus } from '@/hooks/useAdminOrders'
import { useRefundPayment } from '@/hooks/useAdminPayments'
import { paymentApi, deliveryApi } from '@/api'
import {
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  DELIVERY_STATUS_LABELS,
  PAYMENT_METHOD_LABELS
} from '@/types'
import type { OrderStatus, Payment, Delivery } from '@/types'
import { Button, Spinner, Alert } from '@portal/design-react'

const STATUS_TRANSITIONS: Record<string, string[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PAID', 'CANCELLED'],
  PAID: ['SHIPPING', 'REFUNDED'],
  SHIPPING: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
  REFUNDED: []
}

const getStatusColor = (status: OrderStatus): string => {
  switch (status) {
    case 'PAID':
    case 'DELIVERED':
      return 'text-status-success bg-status-success/10'
    case 'PENDING':
    case 'CONFIRMED':
      return 'text-status-warning bg-status-warning/10'
    case 'SHIPPING':
      return 'text-brand-primary bg-brand-primary/10'
    case 'CANCELLED':
    case 'REFUNDED':
      return 'text-status-error bg-status-error/10'
    default:
      return 'text-text-meta bg-bg-subtle'
  }
}

const AdminOrderDetailPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const navigate = useNavigate()
  const { data: order, isLoading, error, refetch } = useAdminOrder(orderNumber ?? null)
  const updateStatusMutation = useUpdateOrderStatus()
  const refundMutation = useRefundPayment()

  const [payment, setPayment] = useState<Payment | null>(null)
  const [delivery, setDelivery] = useState<Delivery | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  // Fetch related data
  useEffect(() => {
    if (!orderNumber) return
    paymentApi.getPayment(orderNumber).then(res => setPayment(res.data)).catch(() => {})
    deliveryApi.getDeliveryByOrder(orderNumber).then(res => setDelivery(res.data)).catch(() => {})
  }, [orderNumber])

  const handleStatusChange = async (newStatus: string) => {
    if (!orderNumber) return
    setActionError(null)
    try {
      await updateStatusMutation.mutateAsync(orderNumber, newStatus)
      refetch()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Failed to update status')
    }
  }

  const handleRefund = async () => {
    if (!payment?.transactionId) return
    if (!confirm('Are you sure you want to refund this payment?')) return
    setActionError(null)
    try {
      await refundMutation.mutateAsync(payment.transactionId)
      refetch()
      // Refresh payment
      if (orderNumber) {
        paymentApi.getPayment(orderNumber).then(res => setPayment(res.data)).catch(() => {})
      }
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Failed to refund payment')
    }
  }

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

  const formatDateTime = (date: string) =>
    new Date(date).toLocaleString('ko-KR')

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" onClick={() => navigate('/admin/orders')}>
          ← Back to Orders
        </Button>
        <Alert variant="error">Order not found</Alert>
      </div>
    )
  }

  const availableTransitions = STATUS_TRANSITIONS[order.status] ?? []
  const canRefund = order.status === 'PAID' || order.status === 'DELIVERED'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/admin/orders')}
            className="p-2 text-text-meta hover:text-text-body"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-text-heading">Order {order.orderNumber}</h1>
            <p className="text-sm text-text-meta">User: {order.userId} | Created: {formatDateTime(order.createdAt)}</p>
          </div>
        </div>
        <span className={`inline-flex px-3 py-1.5 text-sm font-medium rounded-full ${getStatusColor(order.status)}`}>
          {ORDER_STATUS_LABELS[order.status]}
        </span>
      </div>

      {actionError && (
        <Alert variant="error">{actionError}</Alert>
      )}

      {/* Status Actions */}
      {(availableTransitions.length > 0 || canRefund) && (
        <div className="bg-bg-card border border-border-default rounded-lg p-5">
          <h2 className="text-sm font-medium text-text-heading mb-3">Actions</h2>
          <div className="flex flex-wrap gap-2">
            {availableTransitions.map(status => (
              <Button
                key={status}
                variant={status === 'CANCELLED' || status === 'REFUNDED' ? 'secondary' : 'primary'}
                size="sm"
                onClick={() => handleStatusChange(status)}
                disabled={updateStatusMutation.isPending}
              >
                → {ORDER_STATUS_LABELS[status as OrderStatus]}
              </Button>
            ))}
            {canRefund && payment && (
              <Button
                variant="secondary"
                size="sm"
                onClick={handleRefund}
                disabled={refundMutation.isPending}
                className="text-status-error border-status-error/30"
              >
                {refundMutation.isPending ? 'Processing...' : 'Refund Payment'}
              </Button>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Order Items */}
        <div className="bg-bg-card border border-border-default rounded-lg p-5">
          <h2 className="text-sm font-medium text-text-heading mb-4">Order Items</h2>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex items-center justify-between py-2 border-b border-border-default last:border-0">
                <div>
                  <p className="text-sm font-medium text-text-body">{item.productName}</p>
                  <p className="text-xs text-text-meta">Qty: {item.quantity} × {formatPrice(item.price)}</p>
                </div>
                <span className="text-sm font-medium text-text-heading">{formatPrice(item.subtotal)}</span>
              </div>
            ))}
          </div>
          <div className="mt-4 pt-3 border-t border-border-default space-y-1">
            <div className="flex justify-between text-sm">
              <span className="text-text-meta">Subtotal</span>
              <span className="text-text-body">{formatPrice(order.totalAmount)}</span>
            </div>
            {order.discountAmount && order.discountAmount > 0 && (
              <div className="flex justify-between text-sm">
                <span className="text-text-meta">Discount</span>
                <span className="text-status-error">-{formatPrice(order.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between text-sm font-bold">
              <span className="text-text-heading">Total</span>
              <span className="text-text-heading">{formatPrice(order.finalAmount ?? order.totalAmount)}</span>
            </div>
          </div>
        </div>

        {/* Shipping Address */}
        <div className="space-y-6">
          <div className="bg-bg-card border border-border-default rounded-lg p-5">
            <h2 className="text-sm font-medium text-text-heading mb-3">Shipping Address</h2>
            {order.shippingAddress ? (
              <div className="text-sm text-text-body space-y-1">
                <p className="font-medium">{order.shippingAddress.receiverName}</p>
                <p>{order.shippingAddress.receiverPhone}</p>
                <p>{order.shippingAddress.address1}</p>
                {order.shippingAddress.address2 && <p>{order.shippingAddress.address2}</p>}
                <p>{order.shippingAddress.zipCode}</p>
              </div>
            ) : (
              <p className="text-sm text-text-meta">No address</p>
            )}
          </div>

          {/* Payment Info */}
          {payment && (
            <div className="bg-bg-card border border-border-default rounded-lg p-5">
              <h2 className="text-sm font-medium text-text-heading mb-3">Payment</h2>
              <div className="text-sm space-y-2">
                <div className="flex justify-between">
                  <span className="text-text-meta">Status</span>
                  <span className="text-text-body">{PAYMENT_STATUS_LABELS[payment.status]}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Method</span>
                  <span className="text-text-body">{PAYMENT_METHOD_LABELS[payment.method]}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Amount</span>
                  <span className="font-medium text-text-heading">{formatPrice(payment.amount)}</span>
                </div>
                {payment.paidAt && (
                  <div className="flex justify-between">
                    <span className="text-text-meta">Paid at</span>
                    <span className="text-text-body">{formatDateTime(payment.paidAt)}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Delivery Info */}
          {delivery && (
            <div className="bg-bg-card border border-border-default rounded-lg p-5">
              <h2 className="text-sm font-medium text-text-heading mb-3">Delivery</h2>
              <div className="text-sm space-y-2">
                <div className="flex justify-between">
                  <span className="text-text-meta">Status</span>
                  <span className="text-text-body">{DELIVERY_STATUS_LABELS[delivery.status]}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Carrier</span>
                  <span className="text-text-body">{delivery.carrier}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Tracking</span>
                  <span className="font-mono text-text-body">{delivery.trackingNumber}</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Cancel Info */}
      {order.cancelReason && (
        <div className="bg-status-error/5 border border-status-error/20 rounded-lg p-5">
          <h2 className="text-sm font-medium text-status-error mb-2">Cancellation</h2>
          <p className="text-sm text-text-body">{order.cancelReason}</p>
          {order.cancelledAt && (
            <p className="text-xs text-text-meta mt-1">Cancelled: {formatDateTime(order.cancelledAt)}</p>
          )}
        </div>
      )}
    </div>
  )
}

export default AdminOrderDetailPage
