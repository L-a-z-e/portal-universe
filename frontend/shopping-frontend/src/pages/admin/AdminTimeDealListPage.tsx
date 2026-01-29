/**
 * AdminTimeDealListPage
 * 관리자 타임딜 목록 페이지
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminTimeDeals, useCancelTimeDeal } from '@/hooks/useAdminTimeDeals'
import { TIMEDEAL_STATUS_LABELS } from '@/types'
import { Button, Card, Badge, Spinner, useApiError, useToast } from '@portal/design-system-react'

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
  const [page, setPage] = useState(0)
  const { data, isLoading, error, refetch } = useAdminTimeDeals({ page, size: 10 })
  const { mutateAsync: cancelTimeDeal, isPending: isCancelling } = useCancelTimeDeal()

  const handleCancel = async (id: number, productName: string) => {
    if (!confirm(`"${productName}" 타임딜을 취소하시겠습니까?`)) return

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
          className="inline-flex items-center justify-center h-9 px-4 text-sm font-medium rounded-md bg-[#E03131] text-white hover:bg-[#C92A2A] active:bg-[#A51D1D] border border-transparent shadow-sm transition-all"
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
        <Card variant="elevated" padding="md" className="mb-6 border-status-error bg-[#E03131]/10 light:bg-red-50">
          <p className="text-status-error">{error.message}</p>
        </Card>
      )}

      {/* 테이블 */}
      {!isLoading && data && (
        <>
          <Card variant="elevated" padding="none" className="overflow-hidden">
            <table className="w-full">
              <thead className="bg-bg-hover light:bg-gray-50">
                <tr>
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
                {data.content.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-text-muted">
                      등록된 타임딜이 없습니다
                    </td>
                  </tr>
                ) : (
                  data.content.map((timeDeal) => (
                    <tr key={timeDeal.id} className="hover:bg-bg-hover transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          {timeDeal.product.imageUrl ? (
                            <img
                              src={timeDeal.product.imageUrl}
                              alt={timeDeal.product.name}
                              className="w-10 h-10 object-cover rounded"
                            />
                          ) : (
                            <div className="w-10 h-10 bg-bg-muted light:bg-gray-200 rounded flex items-center justify-center">
                              <svg className="w-5 h-5 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                              </svg>
                            </div>
                          )}
                          <span className="text-sm text-text-body">{timeDeal.product.name}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-text-meta">
                        {formatPrice(timeDeal.product.price)}원
                      </td>
                      <td className="px-4 py-3 text-sm text-right font-medium text-status-error">
                        {formatPrice(timeDeal.dealPrice)}원
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Badge variant="error">
                          {timeDeal.discountRate}% OFF
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-sm text-center text-text-meta">
                        {timeDeal.soldCount} / {timeDeal.totalStock}
                      </td>
                      <td className="px-4 py-3 text-center">
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
                      <td className="px-4 py-3 text-sm text-text-meta">
                        <div className="text-xs">
                          <div>{formatDate(timeDeal.startsAt)}</div>
                          <div className="text-text-muted">~ {formatDate(timeDeal.endsAt)}</div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {(timeDeal.status === 'ACTIVE' || timeDeal.status === 'SCHEDULED') && (
                          <button
                            onClick={() => handleCancel(timeDeal.id, timeDeal.product.name)}
                            disabled={isCancelling}
                            className="text-sm text-status-error hover:text-[#C92A2A] disabled:opacity-50 transition-colors"
                          >
                            취소
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
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
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={data.first}
              >
                이전
              </Button>
              <span className="text-sm text-text-meta">
                {page + 1} / {data.totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={data.last}
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
