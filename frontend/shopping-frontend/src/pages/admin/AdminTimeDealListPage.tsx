/**
 * AdminTimeDealListPage
 * 관리자 타임딜 목록 페이지
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminTimeDeals, useCancelTimeDeal } from '@/hooks/useAdminTimeDeals'
import { TIMEDEAL_STATUS_LABELS } from '@/types'

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
  const [page, setPage] = useState(0)
  const { data, isLoading, error, refetch } = useAdminTimeDeals({ page, size: 10 })
  const { mutateAsync: cancelTimeDeal, isPending: isCancelling } = useCancelTimeDeal()

  const handleCancel = async (id: number, productName: string) => {
    if (!confirm(`"${productName}" 타임딜을 취소하시겠습니까?`)) return

    try {
      await cancelTimeDeal(id)
      alert('타임딜이 취소되었습니다.')
      refetch()
    } catch (err) {
      const message = err instanceof Error ? err.message : '취소에 실패했습니다'
      alert(message)
    }
  }

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">타임딜 관리</h1>
          <p className="text-gray-600 mt-1">타임딜을 생성하고 관리합니다</p>
        </div>
        <Link
          to="/admin/time-deals/new"
          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
        >
          새 타임딜 생성
        </Link>
      </div>

      {/* 로딩 */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
        </div>
      )}

      {/* 에러 */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-600">{error.message}</p>
        </div>
      )}

      {/* 테이블 */}
      {!isLoading && data && (
        <>
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">상품</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-500">정가</th>
                  <th className="px-4 py-3 text-right text-sm font-medium text-gray-500">딜가</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">할인율</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">판매/재고</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">상태</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">기간</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {data.content.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-gray-500">
                      등록된 타임딜이 없습니다
                    </td>
                  </tr>
                ) : (
                  data.content.map((timeDeal) => (
                    <tr key={timeDeal.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          {timeDeal.product.imageUrl ? (
                            <img
                              src={timeDeal.product.imageUrl}
                              alt={timeDeal.product.name}
                              className="w-10 h-10 object-cover rounded"
                            />
                          ) : (
                            <div className="w-10 h-10 bg-gray-200 rounded flex items-center justify-center">
                              <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                              </svg>
                            </div>
                          )}
                          <span className="text-sm text-gray-900">{timeDeal.product.name}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-gray-600">
                        {formatPrice(timeDeal.product.price)}원
                      </td>
                      <td className="px-4 py-3 text-sm text-right font-medium text-red-600">
                        {formatPrice(timeDeal.dealPrice)}원
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className="inline-block px-2 py-1 bg-red-100 text-red-700 rounded text-xs font-medium">
                          {timeDeal.discountRate}% OFF
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-center text-gray-600">
                        {timeDeal.soldCount} / {timeDeal.totalStock}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={`
                          inline-block px-2 py-1 rounded text-xs font-medium
                          ${timeDeal.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : ''}
                          ${timeDeal.status === 'SCHEDULED' ? 'bg-blue-100 text-blue-700' : ''}
                          ${timeDeal.status === 'ENDED' ? 'bg-gray-100 text-gray-700' : ''}
                          ${timeDeal.status === 'SOLD_OUT' ? 'bg-orange-100 text-orange-700' : ''}
                          ${timeDeal.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : ''}
                        `}>
                          {TIMEDEAL_STATUS_LABELS[timeDeal.status]}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">
                        <div className="text-xs">
                          <div>{formatDate(timeDeal.startsAt)}</div>
                          <div className="text-gray-400">~ {formatDate(timeDeal.endsAt)}</div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {(timeDeal.status === 'ACTIVE' || timeDeal.status === 'SCHEDULED') && (
                          <button
                            onClick={() => handleCancel(timeDeal.id, timeDeal.product.name)}
                            disabled={isCancelling}
                            className="text-sm text-red-600 hover:text-red-700 disabled:opacity-50"
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
          </div>

          {/* 페이지네이션 */}
          {data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={data.first}
                className="px-3 py-1 border border-gray-300 rounded disabled:opacity-50"
              >
                이전
              </button>
              <span className="text-sm text-gray-600">
                {page + 1} / {data.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={data.last}
                className="px-3 py-1 border border-gray-300 rounded disabled:opacity-50"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default AdminTimeDealListPage
