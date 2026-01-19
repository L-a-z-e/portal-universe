/**
 * CouponSelector Component
 * 체크아웃 시 쿠폰 선택 UI
 */
import { useState } from 'react'
import { useAvailableUserCoupons, calculateDiscount, canApplyCoupon } from '@/hooks/useCoupons'
import { CouponCard } from './CouponCard'
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
    canApplyCoupon(uc.coupon, orderAmount)
  )

  const handleSelectCoupon = (coupon: UserCoupon) => {
    onSelectCoupon(coupon)
    setIsOpen(false)
  }

  const handleRemoveCoupon = () => {
    onSelectCoupon(null)
  }

  const discountAmount = selectedCoupon
    ? calculateDiscount(selectedCoupon.coupon, orderAmount)
    : 0

  return (
    <div className="border border-gray-200 rounded-lg p-4">
      <div className="flex justify-between items-center mb-3">
        <h3 className="font-medium text-gray-900">쿠폰 할인</h3>
        {!selectedCoupon && (
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
          >
            {isOpen ? '닫기' : '쿠폰 선택'}
          </button>
        )}
      </div>

      {/* 선택된 쿠폰 표시 */}
      {selectedCoupon && (
        <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-3 mb-3">
          <div className="flex justify-between items-start">
            <div>
              <span className="text-sm font-medium text-indigo-700">
                {selectedCoupon.coupon.name}
              </span>
              <p className="text-xs text-indigo-600 mt-1">
                {selectedCoupon.coupon.discountType === 'FIXED'
                  ? `${selectedCoupon.coupon.discountValue.toLocaleString()}원 할인`
                  : `${selectedCoupon.coupon.discountValue}% 할인`}
              </p>
            </div>
            <button
              onClick={handleRemoveCoupon}
              className="text-indigo-600 hover:text-indigo-700 text-sm"
            >
              취소
            </button>
          </div>
          <div className="mt-2 pt-2 border-t border-indigo-200">
            <span className="text-sm font-semibold text-indigo-700">
              -{discountAmount.toLocaleString()}원
            </span>
          </div>
        </div>
      )}

      {/* 쿠폰 선택 모달 */}
      {isOpen && (
        <div className="mt-3 pt-3 border-t border-gray-200">
          {isLoading ? (
            <div className="flex justify-center items-center py-4">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600"></div>
            </div>
          ) : applicableCoupons.length === 0 ? (
            <div className="text-center py-4">
              <p className="text-gray-500 text-sm">
                {availableCoupons.length === 0
                  ? '사용 가능한 쿠폰이 없습니다'
                  : '현재 주문 금액에 적용 가능한 쿠폰이 없습니다'}
              </p>
              {availableCoupons.length > 0 && (
                <p className="text-xs text-gray-400 mt-1">
                  최소 주문 금액을 확인해 주세요
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-3 max-h-80 overflow-y-auto">
              {applicableCoupons.map((userCoupon) => {
                const discount = calculateDiscount(userCoupon.coupon, orderAmount)
                return (
                  <div
                    key={userCoupon.id}
                    onClick={() => handleSelectCoupon(userCoupon)}
                    className={`
                      border rounded-lg p-3 cursor-pointer transition-all
                      hover:border-indigo-500 hover:bg-indigo-50
                      ${selectedCoupon?.id === userCoupon.id
                        ? 'border-indigo-500 bg-indigo-50'
                        : 'border-gray-200'
                      }
                    `}
                  >
                    <div className="flex justify-between items-center">
                      <div>
                        <span className="font-medium text-gray-900">
                          {userCoupon.coupon.name}
                        </span>
                        <p className="text-xs text-gray-500 mt-1">
                          {userCoupon.coupon.discountType === 'FIXED'
                            ? `${userCoupon.coupon.discountValue.toLocaleString()}원`
                            : `${userCoupon.coupon.discountValue}%`}
                          {userCoupon.coupon.maximumDiscountAmount &&
                            ` (최대 ${userCoupon.coupon.maximumDiscountAmount.toLocaleString()}원)`}
                        </p>
                      </div>
                      <span className="text-indigo-600 font-semibold">
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
        <p className="text-sm text-gray-500">
          {applicableCoupons.length > 0
            ? `적용 가능한 쿠폰 ${applicableCoupons.length}장`
            : '적용 가능한 쿠폰이 없습니다'}
        </p>
      )}
    </div>
  )
}

export default CouponSelector
