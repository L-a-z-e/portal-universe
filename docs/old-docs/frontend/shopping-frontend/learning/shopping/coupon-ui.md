# 쿠폰 UI 구현

## 학습 목표
- 쿠폰 목록 및 발급 UI 구현
- 결제 시 쿠폰 선택 UI 구현
- 할인 금액 계산 로직 이해

---

## 1. 쿠폰 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            COUPON                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Coupon Card    ─────►  쿠폰 정보 (이름, 할인, 유효기간)                    │
│   Issue Button   ─────►  쿠폰 발급 버튼                                      │
│   Selector       ─────►  결제 시 쿠폰 선택 UI                                │
│   Discount       ─────►  할인 금액 계산 및 표시                              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. CouponCard 컴포넌트

### 2.1 Portal Universe 코드 분석

**components/coupon/CouponCard.tsx**
```tsx
import React from 'react'
import type { Coupon } from '@/types'

interface Props {
  coupon: Coupon
  onIssue?: (couponId: number) => void
  issuing?: boolean
}

export const CouponCard: React.FC<Props> = ({ coupon, onIssue, issuing }) => {
  const formatDiscount = () => {
    if (coupon.discountType === 'FIXED') {
      return `${coupon.discountValue.toLocaleString()}원`
    } else {
      return `${coupon.discountValue}%`
    }
  }

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('ko-KR')
  }

  const isExpiringSoon = () => {
    const validTo = new Date(coupon.validTo)
    const now = new Date()
    const daysLeft = Math.ceil((validTo.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    return daysLeft <= 7 && daysLeft > 0
  }

  return (
    <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden hover:shadow-lg transition-shadow">
      {/* Header */}
      <div className="bg-gradient-to-r from-brand-primary to-brand-secondary text-white p-4">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm opacity-90 mb-1">{coupon.discountType === 'FIXED' ? '정액 할인' : '비율 할인'}</p>
            <h3 className="text-3xl font-bold">{formatDiscount()}</h3>
          </div>

          {isExpiringSoon() && (
            <div className="bg-white/20 px-3 py-1 rounded-full text-xs font-medium">
              ⏰ 곧 만료
            </div>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="p-4 space-y-3">
        <h4 className="text-lg font-semibold text-text-heading">
          {coupon.name}
        </h4>

        <p className="text-sm text-text-meta line-clamp-2">
          {coupon.description}
        </p>

        {/* Conditions */}
        <div className="space-y-1 text-sm">
          {coupon.minimumOrderAmount && (
            <div className="flex items-center gap-2 text-text-meta">
              <span>•</span>
              <span>{coupon.minimumOrderAmount.toLocaleString()}원 이상 구매 시</span>
            </div>
          )}

          {coupon.maximumDiscountAmount && coupon.discountType === 'PERCENTAGE' && (
            <div className="flex items-center gap-2 text-text-meta">
              <span>•</span>
              <span>최대 {coupon.maximumDiscountAmount.toLocaleString()}원 할인</span>
            </div>
          )}

          <div className="flex items-center gap-2 text-text-meta">
            <span>•</span>
            <span>
              유효기간: {formatDate(coupon.validFrom)} ~ {formatDate(coupon.validTo)}
            </span>
          </div>
        </div>

        {/* Issue Button */}
        {onIssue && (
          <button
            onClick={() => onIssue(coupon.id)}
            disabled={issuing || coupon.issuedCount >= coupon.maxIssueCount}
            className={`
              w-full py-3 rounded-lg font-medium transition-colors
              ${issuing || coupon.issuedCount >= coupon.maxIssueCount
                ? 'bg-bg-subtle text-text-meta cursor-not-allowed'
                : 'bg-brand-primary text-white hover:bg-brand-primary/90'
              }
            `}
          >
            {issuing
              ? '발급 중...'
              : coupon.issuedCount >= coupon.maxIssueCount
                ? '발급 마감'
                : '쿠폰 받기'
            }
          </button>
        )}

        {/* Issue Progress */}
        {coupon.maxIssueCount > 0 && (
          <div className="text-xs text-text-meta">
            {coupon.issuedCount.toLocaleString()} / {coupon.maxIssueCount.toLocaleString()} 발급됨
          </div>
        )}
      </div>
    </div>
  )
}
```

---

## 3. CouponListPage

### 3.1 발급 가능한 쿠폰 목록

```tsx
// pages/coupon/CouponListPage.tsx
import React, { useState } from 'react'
import { useAvailableCoupons, useIssueCoupon } from '@/hooks/useCoupons'
import { CouponCard } from '@/components/coupon/CouponCard'

const CouponListPage: React.FC = () => {
  const { data: coupons, isLoading, error, refetch } = useAvailableCoupons()
  const { mutateAsync, isPending } = useIssueCoupon()

  const [issuingCouponId, setIssuingCouponId] = useState<number | null>(null)

  const handleIssueCoupon = async (couponId: number) => {
    setIssuingCouponId(couponId)

    try {
      await mutateAsync(couponId)
      alert('쿠폰이 발급되었습니다!')
      refetch() // 목록 새로고침
    } catch (error: any) {
      alert(error.response?.data?.error?.message || '쿠폰 발급에 실패했습니다.')
    } finally {
      setIssuingCouponId(null)
    }
  }

  if (isLoading) return <div>Loading...</div>
  if (error) return <Alert variant="error">{error.message}</Alert>

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-text-heading mb-2">
          🎁 쿠폰 다운로드
        </h1>
        <p className="text-text-meta">
          다양한 할인 쿠폰을 받아보세요!
        </p>
      </div>

      {coupons && coupons.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-text-meta">발급 가능한 쿠폰이 없습니다.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {coupons?.map((coupon) => (
            <CouponCard
              key={coupon.id}
              coupon={coupon}
              onIssue={handleIssueCoupon}
              issuing={issuingCouponId === coupon.id && isPending}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export default CouponListPage
```

---

## 4. CouponSelector 컴포넌트

### 4.1 결제 시 쿠폰 선택 UI

**Portal Universe 코드 (components/coupon/CouponSelector.tsx)**
```tsx
import React, { useState } from 'react'
import { useAvailableUserCoupons } from '@/hooks/useCoupons'
import { calculateDiscount, canApplyCoupon } from '@/hooks/useCoupons'
import type { UserCoupon } from '@/types'

interface Props {
  orderAmount: number
  selectedCoupon: UserCoupon | null
  onSelectCoupon: (coupon: UserCoupon | null) => void
}

export const CouponSelector: React.FC<Props> = ({
  orderAmount,
  selectedCoupon,
  onSelectCoupon
}) => {
  const { data: userCoupons, isLoading } = useAvailableUserCoupons()
  const [showModal, setShowModal] = useState(false)

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const formatDiscount = (coupon: UserCoupon) => {
    if (coupon.coupon.discountType === 'FIXED') {
      return `${coupon.coupon.discountValue.toLocaleString()}원`
    } else {
      return `${coupon.coupon.discountValue}%`
    }
  }

  const handleSelectCoupon = (coupon: UserCoupon | null) => {
    onSelectCoupon(coupon)
    setShowModal(false)
  }

  return (
    <div>
      <button
        onClick={() => setShowModal(true)}
        className="w-full p-4 border border-border-default rounded-lg text-left hover:border-brand-primary transition-colors"
      >
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-text-meta mb-1">쿠폰 사용</p>
            {selectedCoupon ? (
              <div>
                <p className="font-medium text-text-heading">
                  {selectedCoupon.coupon.name}
                </p>
                <p className="text-sm text-status-success">
                  -{formatPrice(calculateDiscount(selectedCoupon.coupon, orderAmount))} 할인
                </p>
              </div>
            ) : (
              <p className="text-text-body">
                {userCoupons && userCoupons.length > 0
                  ? `사용 가능한 쿠폰 ${userCoupons.length}개`
                  : '사용 가능한 쿠폰이 없습니다'
                }
              </p>
            )}
          </div>
          <svg className="w-5 h-5 text-text-meta" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </button>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-bg-card rounded-lg max-w-md w-full max-h-[80vh] overflow-hidden flex flex-col">
            {/* Header */}
            <div className="p-4 border-b border-border-default flex items-center justify-between">
              <h3 className="text-lg font-bold text-text-heading">쿠폰 선택</h3>
              <button onClick={() => setShowModal(false)} className="text-text-meta hover:text-text-heading">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
              {/* No Coupon Option */}
              <button
                onClick={() => handleSelectCoupon(null)}
                className={`
                  w-full p-4 border rounded-lg text-left transition-colors
                  ${!selectedCoupon
                    ? 'border-brand-primary bg-brand-primary/5'
                    : 'border-border-default hover:border-brand-primary/50'
                  }
                `}
              >
                <p className="font-medium text-text-heading">쿠폰 사용 안 함</p>
              </button>

              {/* Coupon List */}
              {isLoading && <p className="text-text-meta text-center">Loading...</p>}

              {userCoupons?.map((userCoupon) => {
                const canUse = canApplyCoupon(userCoupon.coupon, orderAmount)
                const discountAmount = canUse ? calculateDiscount(userCoupon.coupon, orderAmount) : 0

                return (
                  <button
                    key={userCoupon.id}
                    onClick={() => canUse && handleSelectCoupon(userCoupon)}
                    disabled={!canUse}
                    className={`
                      w-full p-4 border rounded-lg text-left transition-colors
                      ${selectedCoupon?.id === userCoupon.id
                        ? 'border-brand-primary bg-brand-primary/5'
                        : canUse
                          ? 'border-border-default hover:border-brand-primary/50'
                          : 'border-border-default opacity-50 cursor-not-allowed'
                      }
                    `}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <p className="font-medium text-text-heading">
                        {userCoupon.coupon.name}
                      </p>
                      <span className="text-sm font-bold text-brand-primary">
                        {formatDiscount(userCoupon)}
                      </span>
                    </div>

                    {canUse ? (
                      <p className="text-sm text-status-success">
                        -{formatPrice(discountAmount)} 할인
                      </p>
                    ) : (
                      <p className="text-sm text-status-error">
                        {userCoupon.coupon.minimumOrderAmount
                          ? `${formatPrice(userCoupon.coupon.minimumOrderAmount)} 이상 구매 시 사용 가능`
                          : '사용 불가'
                        }
                      </p>
                    )}
                  </button>
                )
              })}

              {userCoupons && userCoupons.length === 0 && (
                <p className="text-text-meta text-center py-8">
                  사용 가능한 쿠폰이 없습니다.
                </p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
```

---

## 5. 할인 금액 계산

### 5.1 calculateDiscount 함수

**Portal Universe 코드 (hooks/useCoupons.ts)**
```tsx
export function calculateDiscount(coupon: Coupon, orderAmount: number): number {
  // 1. 최소 주문 금액 검증
  if (coupon.minimumOrderAmount && orderAmount < coupon.minimumOrderAmount) {
    return 0
  }

  // 2. 할인 금액 계산
  let discount: number
  if (coupon.discountType === 'FIXED') {
    // 정액 할인
    discount = coupon.discountValue
  } else {
    // 비율 할인 (PERCENTAGE)
    discount = Math.round(orderAmount * coupon.discountValue / 100)
  }

  // 3. 최대 할인 금액 제한
  if (coupon.maximumDiscountAmount && discount > coupon.maximumDiscountAmount) {
    discount = coupon.maximumDiscountAmount
  }

  // 4. 할인 금액이 주문 금액을 초과하지 않도록
  if (discount > orderAmount) {
    discount = orderAmount
  }

  return discount
}

export function canApplyCoupon(coupon: Coupon, orderAmount: number): boolean {
  if (coupon.minimumOrderAmount && orderAmount < coupon.minimumOrderAmount) {
    return false
  }
  return true
}
```

---

## 6. 사용자 보유 쿠폰 목록

### 6.1 My Coupons Page

```tsx
// pages/coupon/MyCouponsPage.tsx
import React from 'react'
import { useUserCoupons } from '@/hooks/useCoupons'

const MyCouponsPage: React.FC = () => {
  const { data: userCoupons, isLoading, error } = useUserCoupons()

  if (isLoading) return <div>Loading...</div>
  if (error) return <Alert variant="error">{error.message}</Alert>

  const availableCoupons = userCoupons?.filter(uc => uc.status === 'AVAILABLE')
  const usedCoupons = userCoupons?.filter(uc => uc.status === 'USED')
  const expiredCoupons = userCoupons?.filter(uc => uc.status === 'EXPIRED')

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold text-text-heading">내 쿠폰함</h1>

      {/* Available Coupons */}
      <section>
        <h2 className="text-lg font-semibold text-text-heading mb-4">
          사용 가능 ({availableCoupons?.length || 0})
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {availableCoupons?.map((uc) => (
            <CouponCard key={uc.id} coupon={uc.coupon} />
          ))}
        </div>
      </section>

      {/* Used Coupons */}
      {usedCoupons && usedCoupons.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-text-meta mb-4">
            사용 완료 ({usedCoupons.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 opacity-60">
            {usedCoupons.map((uc) => (
              <CouponCard key={uc.id} coupon={uc.coupon} />
            ))}
          </div>
        </section>
      )}

      {/* Expired Coupons */}
      {expiredCoupons && expiredCoupons.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-text-meta mb-4">
            만료됨 ({expiredCoupons.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 opacity-40">
            {expiredCoupons.map((uc) => (
              <CouponCard key={uc.id} coupon={uc.coupon} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

export default MyCouponsPage
```

---

## 7. 핵심 정리

| 컴포넌트 | 역할 |
|----------|------|
| `CouponCard` | 쿠폰 카드 (정보 표시 + 발급 버튼) |
| `CouponListPage` | 발급 가능한 쿠폰 목록 |
| `CouponSelector` | 결제 시 쿠폰 선택 모달 |
| `calculateDiscount` | 할인 금액 계산 |
| `canApplyCoupon` | 쿠폰 사용 가능 여부 검증 |

---

## 다음 학습

- [Checkout Flow](./checkout-flow.md)
- [TimeDeal UI](./timedeal-ui.md)
- [API Integration](../react/api-integration.md)
