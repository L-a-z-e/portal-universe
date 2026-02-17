/**
 * CouponSelector Component
 * 체크아웃 시 쿠폰 선택 UI
 */
import { useState } from 'react'
import { Spinner, Button } from '@portal/design-react'
import { useAvailableUserCoupons, calculateDiscountFromUserCoupon, canApplyUserCoupon } from '@/hooks/useCoupons'
import type { UserCoupon } from '@/types'

interface CouponSelectorProps {
  orderAmount: number
  selectedCoupon: UserCoupon | null
  onSelectCoupon: (coupon: UserCoupon | null) => void
}

export function CouponSelector({
  orderAmount,
  selectedCoupon,
  onSelectCoupon
}: CouponSelectorProps) {
  const [isOpen, setIsOpen] = useState(false)
  const { data: availableCoupons, isLoading } = useAvailableUserCoupons()

  // 적용 가능한 쿠폰만 필터링
  const applicableCoupons = availableCoupons.filter((uc) =>
    canApplyUserCoupon(uc, orderAmount)
  )

  const handleSelectCoupon = (coupon: UserCoupon) => {
    onSelectCoupon(coupon)
    setIsOpen(false)
  }

  const handleRemoveCoupon = () => {
    onSelectCoupon(null)
  }

  const discountAmount = selectedCoupon
    ? calculateDiscountFromUserCoupon(selectedCoupon, orderAmount)
    : 0

  return (
    <div className="border border-border-default rounded-lg p-4">
      <div className="flex justify-between items-center mb-3">
        <h3 className="font-medium text-text-heading">쿠폰 할인</h3>
        {!selectedCoupon && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsOpen(!isOpen)}
            className="text-brand-primary hover:text-brand-primaryHover font-medium p-0 h-auto"
          >
            {isOpen ? '닫기' : '쿠폰 선택'}
          </Button>
        )}
      </div>

      {/* 선택된 쿠폰 표시 */}
      {selectedCoupon && (
        <div className="bg-bg-muted border border-brand-primary rounded-lg p-3 mb-3">
          <div className="flex justify-between items-start">
            <div>
              <span className="text-sm font-medium text-brand-primary">
                {selectedCoupon.couponName}
              </span>
              <p className="text-xs text-text-body mt-1">
                {selectedCoupon.discountType === 'FIXED'
                  ? `${selectedCoupon.discountValue.toLocaleString()}원 할인`
                  : `${selectedCoupon.discountValue}% 할인`}
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleRemoveCoupon}
              className="text-brand-primary hover:text-brand-primaryHover p-0 h-auto"
            >
              취소
            </Button>
          </div>
          <div className="mt-2 pt-2 border-t border-border-default">
            <span className="text-sm font-semibold text-brand-primary">
              -{discountAmount.toLocaleString()}원
            </span>
          </div>
        </div>
      )}

      {/* 쿠폰 선택 모달 */}
      {isOpen && (
        <div className="mt-3 pt-3 border-t border-border-default">
          {isLoading ? (
            <div className="flex justify-center items-center py-4">
              <Spinner size="md" />
            </div>
          ) : applicableCoupons.length === 0 ? (
            <div className="text-center py-4">
              <p className="text-text-meta text-sm">
                {availableCoupons.length === 0
                  ? '사용 가능한 쿠폰이 없습니다'
                  : '현재 주문 금액에 적용 가능한 쿠폰이 없습니다'}
              </p>
              {availableCoupons.length > 0 && (
                <p className="text-xs text-text-meta mt-1">
                  최소 주문 금액을 확인해 주세요
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-3 max-h-80 overflow-y-auto">
              {applicableCoupons.map((userCoupon) => {
                const discount = calculateDiscountFromUserCoupon(userCoupon, orderAmount)
                return (
                  <div
                    key={userCoupon.id}
                    onClick={() => handleSelectCoupon(userCoupon)}
                    className={`
                      border rounded-lg p-3 cursor-pointer transition-all
                      hover:border-brand-primary hover:bg-bg-muted
                      ${selectedCoupon?.id === userCoupon.id
                        ? 'border-brand-primary bg-bg-muted'
                        : 'border-border-default'
                      }
                    `}
                  >
                    <div className="flex justify-between items-center">
                      <div>
                        <span className="font-medium text-text-heading">
                          {userCoupon.couponName}
                        </span>
                        <p className="text-xs text-text-meta mt-1">
                          {userCoupon.discountType === 'FIXED'
                            ? `${userCoupon.discountValue.toLocaleString()}원`
                            : `${userCoupon.discountValue}%`}
                          {userCoupon.maximumDiscountAmount &&
                            ` (최대 ${userCoupon.maximumDiscountAmount.toLocaleString()}원)`}
                        </p>
                      </div>
                      <span className="text-brand-primary font-semibold">
                        -{discount.toLocaleString()}원
                      </span>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* 선택 안 함 상태 */}
      {!selectedCoupon && !isOpen && (
        <p className="text-sm text-text-meta">
          {applicableCoupons.length > 0
            ? `적용 가능한 쿠폰 ${applicableCoupons.length}장`
            : '적용 가능한 쿠폰이 없습니다'}
        </p>
      )}
    </div>
  )
}

export default CouponSelector
