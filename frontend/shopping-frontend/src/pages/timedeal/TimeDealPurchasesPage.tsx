/**
 * Time Deal Purchases Page
 *
 * 타임딜 구매 내역 페이지
 */
import React from 'react'
import { Link } from 'react-router-dom'
import { useTimeDealPurchases } from '@/hooks/useTimeDeals'
import { Button, Spinner, Alert } from '@portal/design-react'

const TimeDealPurchasesPage: React.FC = () => {
  const { data: purchases, isLoading, error, refetch } = useTimeDealPurchases()

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

  // Loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-4">
          <Spinner size="lg" />
          <p className="text-text-meta">Loading purchase history...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-heading">My Time Deal Purchases</h1>
        <Button asChild variant="secondary">
          <Link to="/time-deals">Back to Time Deals</Link>
        </Button>
      </div>

      {/* Error */}
      {error && (
        <Alert variant="error" className="text-center">
          <p className="mb-4">{error.message}</p>
          <Button onClick={refetch} variant="primary">
            Retry
          </Button>
        </Alert>
      )}

      {/* Empty State */}
      {!error && purchases.length === 0 && (
        <div className="bg-bg-card border border-border-default rounded-lg p-12 text-center">
          <div className="w-16 h-16 mx-auto mb-4 text-text-placeholder">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <h2 className="text-lg font-medium text-text-heading mb-2">No purchases yet</h2>
          <p className="text-text-meta mb-6">Start shopping time deals and your purchases will appear here.</p>
          <Button asChild variant="primary">
            <Link to="/time-deals">Browse Time Deals</Link>
          </Button>
        </div>
      )}

      {/* Purchase List */}
      {!error && purchases.length > 0 && (
        <div className="space-y-4">
          {purchases.map((purchase) => (
            <div
              key={purchase.id}
              className="block bg-bg-card border border-border-default rounded-lg p-6"
            >
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                {/* Purchase Info */}
                <div className="space-y-2 flex-1">
                  <h3 className="font-semibold text-text-heading text-lg">
                    {purchase.productName}
                  </h3>
                  <div className="flex flex-wrap items-center gap-3 text-sm text-text-meta">
                    <span className="flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                      </svg>
                      {formatDate(purchase.purchasedAt)}
                    </span>
                    <span className="flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
                        />
                      </svg>
                      Quantity: {purchase.quantity}
                    </span>
                  </div>
                </div>

                {/* Price Info */}
                <div className="text-right space-y-1">
                  <div className="text-sm text-text-meta">
                    Unit Price: {formatPrice(purchase.purchasePrice)}
                  </div>
                  <div className="text-lg font-bold text-text-heading">
                    Total: {formatPrice(purchase.totalPrice)}
                  </div>
                </div>
              </div>

              {/* Additional Info */}
              <div className="mt-4 pt-4 border-t border-border-default">
                <div className="flex items-center gap-2 text-xs text-text-meta">
                  <span className="px-2 py-1 bg-status-success-bg text-status-success rounded">
                    Purchase Complete
                  </span>
                  <span className="font-mono">
                    ID: {purchase.timeDealProductId}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Summary */}
      {!error && purchases.length > 0 && (
        <div className="bg-bg-subtle border border-border-default rounded-lg p-6">
          <div className="flex items-center justify-between">
            <span className="text-text-body font-medium">Total Purchases</span>
            <span className="text-text-heading font-bold text-lg">
              {purchases.length} {purchases.length === 1 ? 'item' : 'items'}
            </span>
          </div>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-text-body font-medium">Total Spent</span>
            <span className="text-brand-primary font-bold text-xl">
              {formatPrice(purchases.reduce((sum, p) => sum + p.totalPrice, 0))}
            </span>
          </div>
        </div>
      )}
    </div>
  )
}

export default TimeDealPurchasesPage
