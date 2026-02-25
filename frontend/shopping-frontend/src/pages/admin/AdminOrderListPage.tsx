/**
 * Admin Order List Page
 * 관리자 주문 목록 페이지
 */
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminOrders } from '@/hooks/useAdminOrders'
import { ORDER_STATUS_LABELS } from '@/types'
import type { OrderStatus, Order } from '@/types'
import { Button, Spinner, Input, Select, Table } from '@portal/design-react'
import type { SelectValue } from '@portal/design-react'
import type { SelectOption, TableColumn } from '@portal/design-core'

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

const STATUS_SELECT_OPTIONS: SelectOption[] = STATUS_OPTIONS.map(opt => ({
  value: opt.value,
  label: opt.label,
}))

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

  const handleStatusChange = (value: SelectValue) => {
    setStatusFilter(String(value ?? ''))
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
        <Select
          value={statusFilter}
          options={STATUS_SELECT_OPTIONS}
          onChange={handleStatusChange}
          className="w-40"
        />

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
      {!error && (
        <>
          <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden">
            <Table<Order>
              columns={[
                {
                  key: 'orderNumber',
                  label: 'Order Number',
                  render: (_, row) => (
                    <span className="font-mono text-brand-primary">{row.orderNumber}</span>
                  ),
                },
                { key: 'userId', label: 'User' },
                {
                  key: 'status',
                  label: 'Status',
                  render: (_, row) => (
                    <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full ${getStatusColor(row.status)}`}>
                      {ORDER_STATUS_LABELS[row.status]}
                    </span>
                  ),
                },
                {
                  key: 'totalAmount',
                  label: 'Amount',
                  align: 'right',
                  render: (_, row) => (
                    <span className="font-medium">{formatPrice(row.finalAmount ?? row.totalAmount)}</span>
                  ),
                },
                {
                  key: 'items',
                  label: 'Items',
                  render: (_, row) => `${row.items?.length ?? 0} items`,
                } as TableColumn<Order>,
                {
                  key: 'createdAt',
                  label: 'Date',
                  render: (_, row) => formatDate(row.createdAt),
                },
              ]}
              data={data?.items ?? []}
              loading={isLoading}
              hoverable
              onRowClick={(row) => navigate(`/admin/orders/${row.orderNumber}`)}
              emptyText="No orders found"
            />
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
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
