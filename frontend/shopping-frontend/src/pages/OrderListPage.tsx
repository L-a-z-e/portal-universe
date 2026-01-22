/**
 * Order List Page
 *
 * 주문 내역 목록 페이지
 */
import React, { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { orderApi } from '@/api/endpoints'
import type { Order, OrderStatus } from '@/types'
import { ORDER_STATUS_LABELS } from '@/types'
import { Button, Spinner, Alert, Badge } from '@portal/design-system-react'

const OrderListPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '0')

  // State
  const [orders, setOrders] = useState<Order[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Fetch orders
  const fetchOrders = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      const response = await orderApi.getOrders(currentPage, 10)
      setOrders(response.data.content)
      setTotalPages(response.data.totalPages)
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Failed to fetch orders')
    } finally {
      setLoading(false)
    }
  }, [currentPage])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

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

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case 'PENDING':
      case 'CONFIRMED':
        return 'bg-status-warning-bg text-status-warning'
      case 'PAID':
      case 'SHIPPING':
        return 'bg-status-info-bg text-status-info'
      case 'DELIVERED':
        return 'bg-status-success-bg text-status-success'
      case 'CANCELLED':
      case 'REFUNDED':
        return 'bg-status-error-bg text-status-error'
      default:
        return 'bg-bg-subtle text-text-meta'
    }
  }

  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(page))
    setSearchParams(params)
  }

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-4">
          <Spinner size="lg" />
          <p className="text-text-meta">Loading orders...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <h1 className="text-2xl font-bold text-text-heading">My Orders</h1>

      {/* Error */}
      {error && (
        <Alert variant="error" className="text-center">
          <p className="mb-4">{error}</p>
          <Button onClick={fetchOrders} variant="primary">
            Retry
          </Button>
        </Alert>
      )}

      {/* Empty State */}
      {!error && orders.length === 0 && (
        <div className="bg-bg-card border border-border-default rounded-lg p-12 text-center">
          <div className="w-16 h-16 mx-auto mb-4 text-text-placeholder">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
              />
            </svg>
          </div>
          <h2 className="text-lg font-medium text-text-heading mb-2">No orders yet</h2>
          <p className="text-text-meta mb-6">Start shopping and your orders will appear here.</p>
          <Button asChild variant="primary">
            <Link to="/">Browse Products</Link>
          </Button>
        </div>
      )}

      {/* Order List */}
      {!error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.orderNumber}`}
              className="block bg-bg-card border border-border-default rounded-lg p-6 hover:border-brand-primary/30 transition-colors"
            >
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                {/* Order Info */}
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-sm text-text-meta">
                      {order.orderNumber}
                    </span>
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${getStatusColor(order.status)}`}>
                      {ORDER_STATUS_LABELS[order.status]}
                    </span>
                  </div>
                  <p className="text-sm text-text-meta">
                    {formatDate(order.createdAt)}
                  </p>
                  <p className="text-sm text-text-body">
                    {order.items.length} {order.items.length === 1 ? 'item' : 'items'}
                  </p>
                </div>

                {/* Total */}
                <div className="text-right">
                  <span className="text-lg font-bold text-text-heading">
                    {formatPrice(order.totalAmount)}
                  </span>
                </div>
              </div>

              {/* Items Preview */}
              <div className="mt-4 pt-4 border-t border-border-default">
                <div className="flex flex-wrap gap-2">
                  {order.items.slice(0, 3).map((item) => (
                    <span
                      key={item.id}
                      className="px-3 py-1 bg-bg-subtle rounded text-sm text-text-body"
                    >
                      {item.productName} x{item.quantity}
                    </span>
                  ))}
                  {order.items.length > 3 && (
                    <span className="px-3 py-1 bg-bg-subtle rounded text-sm text-text-meta">
                      +{order.items.length - 3} more
                    </span>
                  )}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <Button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 0}
            variant="secondary"
          >
            Previous
          </Button>

          <span className="px-4 py-2 text-text-meta">
            Page {currentPage + 1} of {totalPages}
          </span>

          <Button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages - 1}
            variant="secondary"
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}

export default OrderListPage
