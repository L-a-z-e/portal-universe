/**
 * AdminTimeDealListPage
 * 관리자 타임딜 목록 페이지
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminTimeDeals, useCancelTimeDeal } from '@/hooks/useAdminTimeDeals'
import { TIMEDEAL_STATUS_LABELS } from '@/types'
import { Button, Card, Badge, Spinner, useApiError, useToast } from '@portal/design-react'

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price)
}

export function AdminTimeDealListPage() {
  const { handleError } = useApiError()
  const { success } = useToast()
  const [page, setPage] = useState(1)
  const { data, isLoading, error, refetch } = useAdminTimeDeals({ page, size: 10 })
  const { mutateAsync: cancelTimeDeal, isPending: isCancelling } = useCancelTimeDeal()

  const handleCancel = async (id: number, name: string) => {
    if (!confirm(`"${name}" 타임딜을 취소하시겠습니까?`)) return

    try {
      await cancelTimeDeal(id)
      success('타임딜이 취소되었습니다.')
      refetch()
    } catch (err) {
      handleError(err, '취소에 실패했습니다.')
    }
  }

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text-heading">타임딜 관리</h1>
          <p className="text-text-meta mt-1">타임딜을 생성하고 관리합니다</p>
        </div>
        <Link
          to="/admin/time-deals/new"
          className="inline-flex items-center justify-center h-9 px-4 text-sm font-medium rounded-md bg-status-error text-white hover:opacity-90 active:opacity-80 border border-transparent shadow-sm transition-all"
        >
          새 타임딜 생성
        </Link>
      </div>

      {/* 로딩 */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <Spinner size="lg" />
        </div>
      )}

      {/* 에러 */}
      {error && (
        <Card variant="elevated" padding="md" className="mb-6 border-status-error bg-status-error/10">
          <p className="text-status-error">{error.message}</p>
        </Card>
      )}

      {/* 테이블 */}
      {!isLoading && data && (
        <>
          <Card variant="elevated" padding="none" className="overflow-hidden">
            <table className="w-full">
              <thead className="bg-bg-muted">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">타임딜</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">상품</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-text-meta">정가</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-text-meta">딜가</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">할인율</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">판매/재고</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">상태</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">기간</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-default">
                {data.items.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="px-4 py-8 text-center text-text-muted">
                      등록된 타임딜이 없습니다
                    </td>
                  </tr>
                ) : (
                  data.items.map((timeDeal) =>
                    (timeDeal.products ?? []).map((product, idx) => (
                      <tr key={`${timeDeal.id}-${product.id}`} className="hover:bg-bg-hover transition-colors">
                        {idx === 0 && (
                          <td className="px-4 py-3 text-sm text-text-body" rowSpan={timeDeal.products?.length || 1}>
                            <div className="font-medium">{timeDeal.name}</div>
                            {timeDeal.description && (
                              <div className="text-xs text-text-muted mt-1">{timeDeal.description}</div>
                            )}
                          </td>
                        )}
                        <td className="px-4 py-3">
                          <span className="text-sm text-text-body">{product.productName}</span>
                        </td>
                        <td className="px-4 py-3 text-sm text-right text-text-meta">
                          {formatPrice(product.originalPrice)}원
                        </td>
                        <td className="px-4 py-3 text-sm text-right font-medium text-status-error">
                          {formatPrice(product.dealPrice)}원
                        </td>
                        <td className="px-4 py-3 text-center">
                          <Badge variant="error">
                            {product.discountRate}% OFF
                          </Badge>
                        </td>
                        <td className="px-4 py-3 text-sm text-center text-text-meta">
                          {product.soldQuantity} / {product.dealQuantity}
                        </td>
                        {idx === 0 && (
                          <>
                            <td className="px-4 py-3 text-center" rowSpan={timeDeal.products?.length || 1}>
                              <Badge
                                variant={
                                  timeDeal.status === 'ACTIVE' ? 'success' :
                                  timeDeal.status === 'SCHEDULED' ? 'info' :
                                  timeDeal.status === 'ENDED' ? 'neutral' :
                                  timeDeal.status === 'SOLD_OUT' ? 'warning' :
                                  'error'
                                }
                              >
                                {TIMEDEAL_STATUS_LABELS[timeDeal.status]}
                              </Badge>
                            </td>
                            <td className="px-4 py-3 text-sm text-text-meta" rowSpan={timeDeal.products?.length || 1}>
                              <div className="text-xs">
                                <div>{formatDate(timeDeal.startsAt)}</div>
                                <div className="text-text-muted">~ {formatDate(timeDeal.endsAt)}</div>
                              </div>
                            </td>
                            <td className="px-4 py-3 text-center" rowSpan={timeDeal.products?.length || 1}>
                              {(timeDeal.status === 'ACTIVE' || timeDeal.status === 'SCHEDULED') && (
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleCancel(timeDeal.id, timeDeal.name)}
                                  disabled={isCancelling}
                                  className="text-status-error hover:opacity-80"
                                >
                                  취소
                                </Button>
                              )}
                            </td>
                          </>
                        )}
                      </tr>
                    ))
                  )
                )}
              </tbody>
            </table>
          </Card>

          {/* 페이지네이션 */}
          {data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
              >
                이전
              </Button>
              <span className="text-sm text-text-meta">
                {page} / {data.totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => Math.min(data.totalPages, p + 1))}
                disabled={page >= data.totalPages}
              >
                다음
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default AdminTimeDealListPage
