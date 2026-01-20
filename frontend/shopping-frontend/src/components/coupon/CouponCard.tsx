/**
 * CouponCard Component
 * 쿠폰 카드 UI 컴포넌트
 */
import { Button, Badge } from '@portal/design-system-react'
import type { Coupon, UserCoupon } from '@/types'
import { DISCOUNT_TYPE_LABELS, USER_COUPON_STATUS_LABELS } from '@/types'

interface CouponCardProps {
  coupon: Coupon
  userCoupon?: UserCoupon
  onIssue?: (couponId: number) => void
  onSelect?: (userCoupon: UserCoupon) => void
  isIssuing?: boolean
  isSelected?: boolean
  selectable?: boolean
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function formatDiscountValue(coupon: Coupon): string {
  if (coupon.discountType === 'FIXED') {
    return `${coupon.discountValue.toLocaleString()}원`
  }
  return `${coupon.discountValue}%`
}

export function CouponCard({
  coupon,
  userCoupon,
  onIssue,
  onSelect,
  isIssuing = false,
  isSelected = false,
  selectable = false
}: CouponCardProps) {
  const isOwned = !!userCoupon
  const isUsable = userCoupon?.status === 'AVAILABLE'

  const handleClick = () => {
    if (selectable && userCoupon && isUsable && onSelect) {
      onSelect(userCoupon)
    }
  }

  return (
    <div
      className={`
        border rounded-lg p-4 transition-all
        ${selectable && isUsable ? 'cursor-pointer hover:border-brand-primary' : ''}
        ${isSelected ? 'border-brand-primary light:bg-white bg-bg-card' : 'border-border-default'}
        ${!isUsable && isOwned ? 'opacity-60' : ''}
      `}
      onClick={handleClick}
    >
      {/* 쿠폰 헤더 */}
      <div className="flex justify-between items-start mb-3">
        <div>
          <Badge variant="brand" className="mb-2">
            {DISCOUNT_TYPE_LABELS[coupon.discountType]}
          </Badge>
          <h3 className="text-lg font-semibold text-text-heading">{coupon.name}</h3>
        </div>
        <div className="text-right">
          <span className="text-2xl font-bold text-brand-primary">
            {formatDiscountValue(coupon)}
          </span>
          <span className="block text-sm text-text-meta">할인</span>
        </div>
      </div>

      {/* 쿠폰 설명 */}
      {coupon.description && (
        <p className="text-sm text-text-body mb-3">{coupon.description}</p>
      )}

      {/* 쿠폰 조건 */}
      <div className="text-xs text-text-meta space-y-1 mb-3">
        {coupon.minimumOrderAmount && (
          <p>{coupon.minimumOrderAmount.toLocaleString()}원 이상 구매 시 사용 가능</p>
        )}
        {coupon.maximumDiscountAmount && (
          <p>최대 {coupon.maximumDiscountAmount.toLocaleString()}원 할인</p>
        )}
        <p>유효기간: {formatDate(coupon.expiresAt)}까지</p>
      </div>

      {/* 상태 표시 및 버튼 */}
      <div className="flex justify-between items-center pt-3 border-t border-border-default">
        {isOwned ? (
          <>
            <span
              className={`text-sm font-medium ${
                isUsable ? 'text-status-success' : 'text-text-meta'
              }`}
            >
              {USER_COUPON_STATUS_LABELS[userCoupon.status]}
            </span>
            {selectable && isUsable && (
              <span className="text-sm text-brand-primary">
                {isSelected ? '선택됨' : '선택하기'}
              </span>
            )}
          </>
        ) : (
          <>
            <span className="text-sm text-text-meta">
              {coupon.remainingQuantity > 0
                ? `${coupon.remainingQuantity}장 남음`
                : '소진됨'}
            </span>
            {onIssue && coupon.remainingQuantity > 0 && (
              <Button
                onClick={(e) => {
                  e.stopPropagation()
                  onIssue(coupon.id)
                }}
                disabled={isIssuing}
                variant="primary"
                size="sm"
              >
                {isIssuing ? '발급 중...' : '쿠폰 받기'}
              </Button>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default CouponCard
