/**
 * AdminCouponListPage
 * 관리자 쿠폰 목록 페이지
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminCoupons, useDeactivateCoupon } from '@/hooks/useAdminCoupons'
import { COUPON_STATUS_LABELS, DISCOUNT_TYPE_LABELS } from '@/types'
import type { Coupon } from '@/types'
import { Button, Card, Badge, Spinner, Table, useApiError, useToast } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'

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
          className="inline-flex items-center justify-center h-9 px-4 text-sm font-medium rounded-md bg-white/90 text-text-inverse hover:bg-white active:bg-white/80 light:bg-brand-primary light:text-white light:hover:bg-brand-primaryHover border border-transparent shadow-sm transition-all"
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
        <Card variant="elevated" padding="md" className="mb-6 border-status-error bg-status-error/10">
          <p className="text-status-error">{error.message}</p>
        </Card>
      )}

      {/* 테이블 */}
      {!isLoading && data && (
        <>
          <Card variant="elevated" padding="none" className="overflow-hidden">
            <Table<Coupon>
              columns={[
                {
                  key: 'code',
                  label: '코드',
                  render: (_, row) => <span className="font-mono text-text-heading">{row.code}</span>,
                },
                { key: 'name', label: '이름' },
                {
                  key: 'discountValue',
                  label: '할인',
                  render: (_, row) => (
                    <>
                      <span className="text-brand-primary font-medium">
                        {row.discountType === 'FIXED' ? `${formatPrice(row.discountValue)}원` : `${row.discountValue}%`}
                      </span>
                      <span className="text-text-muted ml-1">({DISCOUNT_TYPE_LABELS[row.discountType]})</span>
                    </>
                  ),
                },
                {
                  key: 'issuedQuantity',
                  label: '발급/총수량',
                  align: 'center',
                  render: (_, row) => `${row.issuedQuantity} / ${row.totalQuantity}`,
                } as TableColumn<Coupon>,
                {
                  key: 'status',
                  label: '상태',
                  align: 'center',
                  render: (_, row) => (
                    <Badge
                      variant={
                        row.status === 'ACTIVE' ? 'success' :
                        row.status === 'INACTIVE' ? 'neutral' :
                        row.status === 'EXPIRED' ? 'error' :
                        'warning'
                      }
                    >
                      {COUPON_STATUS_LABELS[row.status]}
                    </Badge>
                  ),
                },
                {
                  key: 'startsAt',
                  label: '유효기간',
                  render: (_, row) => `${formatDate(row.startsAt)} ~ ${formatDate(row.expiresAt)}`,
                },
                {
                  key: 'id',
                  label: '관리',
                  align: 'center',
                  render: (_, row) => row.status === 'ACTIVE' ? (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDeactivate(row.id, row.name)}
                      disabled={isDeactivating}
                      className="text-status-error hover:opacity-80"
                    >
                      비활성화
                    </Button>
                  ) : null,
                } as TableColumn<Coupon>,
              ]}
              data={data.items}
              hoverable
              emptyText="등록된 쿠폰이 없습니다"
            />
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
