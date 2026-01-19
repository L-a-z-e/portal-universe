/**
 * AdminCouponListPage
 * 관리자 쿠폰 목록 페이지
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminCoupons, useDeactivateCoupon } from '@/hooks/useAdminCoupons'
import { COUPON_STATUS_LABELS, DISCOUNT_TYPE_LABELS } from '@/types'

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price)
}

export function AdminCouponListPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, error, refetch } = useAdminCoupons({ page, size: 10 })
  const { mutateAsync: deactivateCoupon, isPending: isDeactivating } = useDeactivateCoupon()

  const handleDeactivate = async (id: number, name: string) => {
    if (!confirm(`"${name}" 쿠폰을 비활성화하시겠습니까?`)) return

    try {
      await deactivateCoupon(id)
      alert('쿠폰이 비활성화되었습니다.')
      refetch()
    } catch (err) {
      const message = err instanceof Error ? err.message : '비활성화에 실패했습니다'
      alert(message)
    }
  }

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">쿠폰 관리</h1>
          <p className="text-gray-600 mt-1">쿠폰을 생성하고 관리합니다</p>
        </div>
        <Link
          to="/admin/coupons/new"
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          새 쿠폰 생성
        </Link>
      </div>

      {/* 로딩 */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
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
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">코드</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">이름</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">할인</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">발급/총수량</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">상태</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">유효기간</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {data.content.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-gray-500">
                      등록된 쿠폰이 없습니다
                    </td>
                  </tr>
                ) : (
                  data.content.map((coupon) => (
                    <tr key={coupon.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm font-mono text-gray-900">{coupon.code}</td>
                      <td className="px-4 py-3 text-sm text-gray-900">{coupon.name}</td>
                      <td className="px-4 py-3 text-sm">
                        <span className="text-indigo-600 font-medium">
                          {coupon.discountType === 'FIXED'
                            ? `${formatPrice(coupon.discountValue)}원`
                            : `${coupon.discountValue}%`}
                        </span>
                        <span className="text-gray-400 ml-1">
                          ({DISCOUNT_TYPE_LABELS[coupon.discountType]})
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-center text-gray-600">
                        {coupon.issuedQuantity} / {coupon.totalQuantity}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={`
                          inline-block px-2 py-1 rounded text-xs font-medium
                          ${coupon.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : ''}
                          ${coupon.status === 'INACTIVE' ? 'bg-gray-100 text-gray-700' : ''}
                          ${coupon.status === 'EXPIRED' ? 'bg-red-100 text-red-700' : ''}
                          ${coupon.status === 'EXHAUSTED' ? 'bg-orange-100 text-orange-700' : ''}
                        `}>
                          {COUPON_STATUS_LABELS[coupon.status]}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">
                        {formatDate(coupon.startsAt)} ~ {formatDate(coupon.expiresAt)}
                      </td>
                      <td className="px-4 py-3 text-center">
                        {coupon.status === 'ACTIVE' && (
                          <button
                            onClick={() => handleDeactivate(coupon.id, coupon.name)}
                            disabled={isDeactivating}
                            className="text-sm text-red-600 hover:text-red-700 disabled:opacity-50"
                          >
                            비활성화
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

export default AdminCouponListPage
