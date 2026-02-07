/**
 * Admin Order List Page
 * 관리자 주문 목록 페이지
 */
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminOrders } from '@/hooks/useAdminOrders'
import { ORDER_STATUS_LABELS } from '@/types'
import type { OrderStatus } from '@/types'
import { Button, Spinner, Input } from '@portal/design-system-react'

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'All Status' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'PAID', label: 'Paid' },
  { value: 'SHIPPING', label: 'Shipping' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'REFUNDED', label: 'Refunded' }
]

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

const AdminOrderListPage: React.FC = () => {
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')

  const { data, isLoading, error } = useAdminOrders({
    page,
    size: 20,
    status: statusFilter || undefined,
    keyword: keyword || undefined
  })

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setKeyword(searchInput)
    setPage(1)
  }

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setStatusFilter(e.target.value)
    setPage(1)
  }

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-heading">Orders</h1>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
        <select
          value={statusFilter}
          onChange={handleStatusChange}
          className="px-4 py-2 border border-border-default rounded-lg bg-bg-card text-text-body focus:outline-none focus:ring-2 focus:ring-brand-primary"
        >
          {STATUS_OPTIONS.map(opt => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>

        <form onSubmit={handleSearch} className="flex items-center gap-2">
          <Input
            type="text"
            value={searchInput}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchInput(e.target.value)}
            placeholder="Order number or user ID..."
          />
          <Button type="submit" variant="primary" size="sm">Search</Button>
          {keyword && (
            <Button type="button" variant="secondary" size="sm" onClick={() => { setKeyword(''); setSearchInput(''); setPage(1) }}>
              Clear
            </Button>
          )}
        </form>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      )}

      {/* Error */}
      {error && !isLoading && (
        <div className="bg-status-error/10 border border-status-error/20 rounded-lg p-4 text-status-error text-center">
          {error.message}
        </div>
      )}

      {/* Table */}
      {!isLoading && !error && data && (
        <>
          <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden">
            <table className="w-full">
              <thead className="bg-bg-subtle border-b border-border-default">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Order Number</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">User</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Status</th>
                  <th className="px-6 py-4 text-right text-xs font-medium text-text-meta">Amount</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Items</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-default">
                {data.items.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-12 text-center text-text-meta">
                      No orders found
                    </td>
                  </tr>
                ) : (
                  data.items.map((order) => (
                    <tr
                      key={order.id}
                      onClick={() => navigate(`/admin/orders/${order.orderNumber}`)}
                      className="hover:bg-bg-hover transition-colors cursor-pointer"
                    >
                      <td className="px-6 py-4 text-sm font-mono text-brand-primary">
                        {order.orderNumber}
                      </td>
                      <td className="px-6 py-4 text-sm text-text-body">
                        {order.userId}
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full ${getStatusColor(order.status)}`}>
                          {ORDER_STATUS_LABELS[order.status]}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-text-body text-right font-medium">
                        {formatPrice(order.finalAmount ?? order.totalAmount)}
                      </td>
                      <td className="px-6 py-4 text-sm text-text-meta">
                        {order.items?.length ?? 0} items
                      </td>
                      <td className="px-6 py-4 text-sm text-text-meta">
                        {formatDate(order.createdAt)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
              >
                Previous
              </Button>
              <span className="text-sm text-text-meta">
                {page} / {data.totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage(p => Math.min(data.totalPages, p + 1))}
                disabled={page >= data.totalPages}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default AdminOrderListPage
