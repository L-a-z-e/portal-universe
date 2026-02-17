/**
 * Admin Stock Movement Page
 * 관리자 재고 이동 이력 조회 페이지
 */
import React, { useState } from 'react'
import { useAdminStockMovements } from '@/hooks/useAdminStockMovements'
import type { MovementType } from '@/types'
import type { StockMovement } from '@/dto/inventory'
import { Button, Spinner, Input, Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'

const MOVEMENT_TYPE_LABELS: Record<MovementType, string> = {
  INITIAL: 'Initial Stock',
  RESERVE: 'Reserved',
  DEDUCT: 'Deducted',
  RELEASE: 'Released',
  INBOUND: 'Inbound',
  RETURN: 'Returned',
  ADJUSTMENT: 'Adjustment'
}

const getMovementColor = (type: MovementType): string => {
  switch (type) {
    case 'INITIAL':
    case 'INBOUND':
    case 'RETURN':
    case 'RELEASE':
      return 'text-status-success bg-status-success/10'
    case 'RESERVE':
      return 'text-status-warning bg-status-warning/10'
    case 'DEDUCT':
      return 'text-status-error bg-status-error/10'
    case 'ADJUSTMENT':
      return 'text-brand-primary bg-brand-primary/10'
    default:
      return 'text-text-meta bg-bg-subtle'
  }
}

const AdminStockMovementPage: React.FC = () => {
  const [productIdInput, setProductIdInput] = useState('')
  const [productId, setProductId] = useState<number | null>(null)
  const [page, setPage] = useState(1)

  const { data, isLoading, error } = useAdminStockMovements({
    productId,
    page,
    size: 20
  })

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const id = parseInt(productIdInput)
    if (!isNaN(id) && id > 0) {
      setProductId(id)
      setPage(1)
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-text-heading">Stock Movements</h1>

      {/* Search */}
      <form onSubmit={handleSearch} className="flex items-center gap-2">
        <Input
          type="number"
          value={productIdInput}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setProductIdInput(e.target.value)}
          placeholder="Enter Product ID..."
          min="1"
        />
        <Button type="submit" variant="primary">Search</Button>
      </form>

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
            <Table<StockMovement>
              columns={[
                {
                  key: 'movementType',
                  label: 'Type',
                  render: (_, row) => (
                    <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full ${getMovementColor(row.movementType)}`}>
                      {MOVEMENT_TYPE_LABELS[row.movementType]}
                    </span>
                  ),
                },
                {
                  key: 'quantity',
                  label: 'Qty',
                  align: 'right',
                  render: (_, row) => (
                    <span className={`font-mono ${row.quantity > 0 ? 'text-status-success' : 'text-status-error'}`}>
                      {row.quantity > 0 ? '+' : ''}{row.quantity}
                    </span>
                  ),
                },
                {
                  key: 'previousAvailable',
                  label: 'Available',
                  align: 'right',
                  render: (_, row) => `${row.previousAvailable} → ${row.afterAvailable}`,
                } as TableColumn<StockMovement>,
                {
                  key: 'previousReserved',
                  label: 'Reserved',
                  align: 'right',
                  render: (_, row) => `${row.previousReserved} → ${row.afterReserved}`,
                } as TableColumn<StockMovement>,
                {
                  key: 'referenceType',
                  label: 'Reference',
                  render: (_, row) => row.referenceType ? (
                    <span className="font-mono text-xs">{row.referenceType}:{row.referenceId}</span>
                  ) : null,
                } as TableColumn<StockMovement>,
                {
                  key: 'reason',
                  label: 'Reason',
                  render: (_, row) => <span className="max-w-[200px] truncate block">{row.reason ?? '-'}</span>,
                } as TableColumn<StockMovement>,
                {
                  key: 'createdAt',
                  label: 'Date',
                  render: (_, row) => (
                    <span className="whitespace-nowrap">{new Date(row.createdAt).toLocaleString('ko-KR')}</span>
                  ),
                },
              ]}
              data={data.items}
              hoverable
              emptyText="No stock movements found"
            />
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
              <span className="text-sm text-text-meta">{page} / {data.totalPages}</span>
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

      {/* Empty state when no product selected */}
      {!productId && !isLoading && (
        <div className="bg-bg-subtle rounded-lg p-12 text-center">
          <svg className="w-16 h-16 mx-auto text-text-placeholder mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <p className="text-text-meta">Enter a Product ID to view stock movement history</p>
        </div>
      )}
    </div>
  )
}

export default AdminStockMovementPage
