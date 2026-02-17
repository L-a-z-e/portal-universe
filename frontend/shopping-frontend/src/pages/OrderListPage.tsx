import React, { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { orderApi } from '@/api'
import type { Order, OrderStatus } from '@/types'
import { ORDER_STATUS_LABELS } from '@/types'
import { Button, Spinner, Alert, Pagination } from '@portal/design-react'

const formatPrice = (price: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

const formatDate = (dateString: string) =>
  new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric'
  })

const getStatusStyle = (status: OrderStatus) => {
  switch (status) {
    case 'PENDING':
    case 'CONFIRMED':
      return { dot: 'bg-amber-400', badge: 'bg-amber-400/10 text-amber-500 ring-1 ring-inset ring-amber-400/20' }
    case 'PAID':
    case 'SHIPPING':
      return { dot: 'bg-blue-400', badge: 'bg-blue-400/10 text-blue-500 ring-1 ring-inset ring-blue-400/20' }
    case 'DELIVERED':
      return { dot: 'bg-emerald-400', badge: 'bg-emerald-400/10 text-emerald-500 ring-1 ring-inset ring-emerald-400/20' }
    case 'CANCELLED':
    case 'REFUNDED':
      return { dot: 'bg-red-400', badge: 'bg-red-400/10 text-red-500 ring-1 ring-inset ring-red-400/20' }
    default:
      return { dot: 'bg-gray-400', badge: 'bg-gray-400/10 text-gray-500 ring-1 ring-inset ring-gray-400/20' }
  }
}

const OrderListPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '1')

  const [orders, setOrders] = useState<Order[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchOrders = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await orderApi.getOrders(currentPage, 10)
      setOrders(response.data?.items ?? [])
      setTotalPages(response.data?.totalPages ?? 0)
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Failed to fetch orders')
    } finally {
      setLoading(false)
    }
  }, [currentPage])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(page))
    setSearchParams(params)
  }

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
    <div className="space-y-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-text-meta">
        <Link to="/" className="hover:text-brand-primary transition-colors">Store</Link>
        <span>/</span>
        <span className="text-text-body">Order History</span>
      </nav>

      {/* Header */}
      <h1 className="text-3xl font-black text-text-heading">Order History</h1>

      {/* Error */}
      {error && (
        <Alert variant="error" className="text-center">
          <p className="mb-4">{error}</p>
          <Button onClick={fetchOrders} variant="primary">Retry</Button>
        </Alert>
      )}

      {/* Empty */}
      {!error && orders.length === 0 && (
        <div className="border-2 border-dashed border-border-default rounded-2xl p-16 text-center">
          <div className="w-20 h-20 mx-auto mb-6 text-text-placeholder">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-text-heading mb-2">No orders yet</h2>
          <p className="text-text-meta mb-8">Start shopping and your orders will appear here.</p>
          <Button asChild variant="primary" size="lg" className="rounded-full">
            <Link to="/">Browse Products</Link>
          </Button>
        </div>
      )}

      {/* Order List */}
      {!error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => {
            const statusStyle = getStatusStyle(order.status)
            return (
              <div
                key={order.id}
                className="bg-bg-card border border-border-default rounded-2xl overflow-hidden hover:border-brand-primary/30 transition-colors"
              >
                {/* Header Bar */}
                <div className="bg-bg-muted/50 px-5 py-4 border-b border-border-default flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-4 text-sm">
                    <div>
                      <span className="text-text-meta">Order Placed</span>
                      <p className="text-text-heading font-medium">{formatDate(order.createdAt)}</p>
                    </div>
                    <div className="hidden sm:block">
                      <span className="text-text-meta">Order Number</span>
                      <p className="font-mono text-text-heading">{order.orderNumber}</p>
                    </div>
                    <div>
                      <span className="text-text-meta">Total</span>
                      <p className="text-text-heading font-bold">{formatPrice(order.totalAmount)}</p>
                    </div>
                  </div>
                  <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium ${statusStyle.badge}`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${statusStyle.dot}`} />
                    {ORDER_STATUS_LABELS[order.status]}
                  </span>
                </div>

                {/* Items */}
                <div className="p-5">
                  <div className="space-y-3">
                    {order.items.slice(0, 3).map((item) => (
                      <div key={item.id} className="flex items-center gap-4">
                        <div className="flex-shrink-0 size-16 bg-bg-subtle rounded-xl overflow-hidden flex items-center justify-center text-text-placeholder">
                          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                              d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                          </svg>
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-text-heading truncate">{item.productName}</p>
                          <p className="text-xs text-text-meta">
                            {formatPrice(item.price)} x {item.quantity}
                          </p>
                        </div>
                        <span className="text-sm font-medium text-text-heading flex-shrink-0">
                          {formatPrice(item.subtotal)}
                        </span>
                      </div>
                    ))}
                    {order.items.length > 3 && (
                      <p className="text-sm text-text-meta pl-20">
                        +{order.items.length - 3} more items
                      </p>
                    )}
                  </div>

                  {/* Action */}
                  <div className="mt-4 pt-4 border-t border-border-default flex justify-end">
                    <Link
                      to={`/orders/${order.orderNumber}`}
                      className="inline-flex items-center gap-1 text-sm font-medium text-brand-primary hover:text-brand-primaryHover transition-colors"
                    >
                      주문 상세
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </Link>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center pt-4">
          <Pagination page={currentPage} totalPages={totalPages} onChange={handlePageChange} />
        </div>
      )}
    </div>
  )
}

export default OrderListPage
