/**
 * Admin Stock Movement Page
 * 관리자 재고 이동 이력 조회 페이지
 */
import React, { useState } from 'react'
import { useAdminStockMovements } from '@/hooks/useAdminStockMovements'
import type { MovementType } from '@/types'
import { Button, Spinner, Input } from '@portal/design-system-react'

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
            <table className="w-full">
              <thead className="bg-bg-subtle border-b border-border-default">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Type</th>
                  <th className="px-6 py-4 text-right text-xs font-medium text-text-meta">Qty</th>
                  <th className="px-6 py-4 text-right text-xs font-medium text-text-meta">Available</th>
                  <th className="px-6 py-4 text-right text-xs font-medium text-text-meta">Reserved</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Reference</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Reason</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-default">
                {data.items.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-12 text-center text-text-meta">
                      No stock movements found
                    </td>
                  </tr>
                ) : (
                  data.items.map((movement) => (
                    <tr key={movement.id} className="hover:bg-bg-hover transition-colors">
                      <td className="px-6 py-4">
                        <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full ${getMovementColor(movement.movementType)}`}>
                          {MOVEMENT_TYPE_LABELS[movement.movementType]}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-right font-mono">
                        <span className={movement.quantity > 0 ? 'text-status-success' : 'text-status-error'}>
                          {movement.quantity > 0 ? '+' : ''}{movement.quantity}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-right text-text-body">
                        {movement.previousAvailable} → {movement.afterAvailable}
                      </td>
                      <td className="px-6 py-4 text-sm text-right text-text-body">
                        {movement.previousReserved} → {movement.afterReserved}
                      </td>
                      <td className="px-6 py-4 text-sm text-text-meta">
                        {movement.referenceType && (
                          <span className="font-mono text-xs">
                            {movement.referenceType}:{movement.referenceId}
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-sm text-text-meta max-w-[200px] truncate">
                        {movement.reason ?? '-'}
                      </td>
                      <td className="px-6 py-4 text-sm text-text-meta whitespace-nowrap">
                        {new Date(movement.createdAt).toLocaleString('ko-KR')}
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
