/**
 * AdminCouponListPage
 * 관리자 쿠폰 목록 페이지
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminCoupons, useDeactivateCoupon } from '@/hooks/useAdminCoupons'
import { COUPON_STATUS_LABELS, DISCOUNT_TYPE_LABELS } from '@/types'
import { Button, Card, Badge, Spinner, useApiError, useToast } from '@portal/design-react'

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
  const { handleError } = useApiError()
  const { success } = useToast()
  const [page, setPage] = useState(1)
  const { data, isLoading, error, refetch } = useAdminCoupons({ page, size: 10 })
  const { mutateAsync: deactivateCoupon, isPending: isDeactivating } = useDeactivateCoupon()

  const handleDeactivate = async (id: number, name: string) => {
    if (!confirm(`"${name}" 쿠폰을 비활성화하시겠습니까?`)) return

    try {
      await deactivateCoupon(id)
      success('쿠폰이 비활성화되었습니다.')
      refetch()
    } catch (err) {
      handleError(err, '비활성화에 실패했습니다.')
    }
  }

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text-heading">쿠폰 관리</h1>
          <p className="text-text-meta mt-1">쿠폰을 생성하고 관리합니다</p>
        </div>
        <Link
          to="/admin/coupons/new"
          className="inline-flex items-center justify-center h-9 px-4 text-sm font-medium rounded-md bg-white/90 text-[#08090a] hover:bg-white active:bg-white/80 light:bg-brand-primary light:text-white light:hover:bg-brand-primaryHover border border-transparent shadow-sm transition-all"
        >
          새 쿠폰 생성
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
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">코드</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">이름</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">할인</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">발급/총수량</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">상태</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-text-meta">유효기간</th>
                  <th className="px-4 py-3 text-center text-sm font-medium text-text-meta">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-default">
                {data.items.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-text-muted">
                      등록된 쿠폰이 없습니다
                    </td>
                  </tr>
                ) : (
                  data.items.map((coupon) => (
                    <tr key={coupon.id} className="hover:bg-bg-hover transition-colors">
                      <td className="px-4 py-3 text-sm font-mono text-text-heading">{coupon.code}</td>
                      <td className="px-4 py-3 text-sm text-text-body">{coupon.name}</td>
                      <td className="px-4 py-3 text-sm">
                        <span className="text-brand-primary font-medium">
                          {coupon.discountType === 'FIXED'
                            ? `${formatPrice(coupon.discountValue)}원`
                            : `${coupon.discountValue}%`}
                        </span>
                        <span className="text-text-muted ml-1">
                          ({DISCOUNT_TYPE_LABELS[coupon.discountType]})
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-center text-text-meta">
                        {coupon.issuedQuantity} / {coupon.totalQuantity}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Badge
                          variant={
                            coupon.status === 'ACTIVE' ? 'success' :
                            coupon.status === 'INACTIVE' ? 'neutral' :
                            coupon.status === 'EXPIRED' ? 'error' :
                            'warning'
                          }
                        >
                          {COUPON_STATUS_LABELS[coupon.status]}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-sm text-text-meta">
                        {formatDate(coupon.startsAt)} ~ {formatDate(coupon.expiresAt)}
                      </td>
                      <td className="px-4 py-3 text-center">
                        {coupon.status === 'ACTIVE' && (
                          <button
                            onClick={() => handleDeactivate(coupon.id, coupon.name)}
                            disabled={isDeactivating}
                            className="text-sm text-status-error hover:text-[#C92A2A] disabled:opacity-50 transition-colors"
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

export default AdminCouponListPage
