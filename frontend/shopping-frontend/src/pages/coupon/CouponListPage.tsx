/**
 * CouponListPage
 * 쿠폰 목록 페이지 - 발급 가능한 쿠폰 및 내 쿠폰 관리
 */
import { useState } from 'react'
import { useAvailableCoupons, useUserCoupons, useIssueCoupon } from '@/hooks/useCoupons'
import { CouponCard } from '@/components/coupon/CouponCard'

type TabType = 'available' | 'my'

export function CouponListPage() {
  const [activeTab, setActiveTab] = useState<TabType>('available')
  const [issuingCouponId, setIssuingCouponId] = useState<number | null>(null)

  const { data: availableCoupons, isLoading: loadingAvailable, refetch: refetchAvailable } = useAvailableCoupons()
  const { data: userCoupons, isLoading: loadingMy, refetch: refetchMy } = useUserCoupons()
  const { mutateAsync: issueCoupon } = useIssueCoupon()

  const handleIssueCoupon = async (couponId: number) => {
    try {
      setIssuingCouponId(couponId)
      await issueCoupon(couponId)
      // 발급 성공 후 목록 갱신
      await Promise.all([refetchAvailable(), refetchMy()])
      alert('쿠폰이 발급되었습니다!')
    } catch (error) {
      const message = error instanceof Error ? error.message : '쿠폰 발급에 실패했습니다.'
      alert(message)
    } finally {
      setIssuingCouponId(null)
    }
  }

  const isLoading = activeTab === 'available' ? loadingAvailable : loadingMy

  // 이미 발급받은 쿠폰 ID 목록
  const issuedCouponIds = userCoupons.map((uc) => uc.coupon.id)

  // 발급 가능한 쿠폰 중 아직 발급받지 않은 것만 필터링
  const filteredAvailableCoupons = availableCoupons.filter(
    (coupon) => !issuedCouponIds.includes(coupon.id)
  )

  return (
    <div className="container mx-auto px-4 py-8">
      {/* 페이지 헤더 */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">쿠폰</h1>
        <p className="text-gray-600 mt-1">쿠폰을 발급받고 주문 시 할인을 받으세요</p>
      </div>

      {/* 탭 네비게이션 */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('available')}
            className={`
              py-4 px-1 border-b-2 font-medium text-sm transition-colors
              ${activeTab === 'available'
                ? 'border-indigo-500 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }
            `}
          >
            발급 가능한 쿠폰
            {filteredAvailableCoupons.length > 0 && (
              <span className="ml-2 bg-indigo-100 text-indigo-600 px-2 py-0.5 rounded-full text-xs">
                {filteredAvailableCoupons.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('my')}
            className={`
              py-4 px-1 border-b-2 font-medium text-sm transition-colors
              ${activeTab === 'my'
                ? 'border-indigo-500 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }
            `}
          >
            내 쿠폰함
            {userCoupons.filter((uc) => uc.status === 'AVAILABLE').length > 0 && (
              <span className="ml-2 bg-green-100 text-green-600 px-2 py-0.5 rounded-full text-xs">
                {userCoupons.filter((uc) => uc.status === 'AVAILABLE').length}
              </span>
            )}
          </button>
        </nav>
      </div>

      {/* 로딩 상태 */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          <span className="ml-3 text-gray-600">로딩 중...</span>
        </div>
      )}

      {/* 발급 가능한 쿠폰 탭 */}
      {!isLoading && activeTab === 'available' && (
        <div>
          {filteredAvailableCoupons.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-gray-400 text-5xl mb-4">🎫</div>
              <p className="text-gray-600">현재 발급 가능한 쿠폰이 없습니다</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredAvailableCoupons.map((coupon) => (
                <CouponCard
                  key={coupon.id}
                  coupon={coupon}
                  onIssue={handleIssueCoupon}
                  isIssuing={issuingCouponId === coupon.id}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* 내 쿠폰함 탭 */}
      {!isLoading && activeTab === 'my' && (
        <div>
          {userCoupons.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-gray-400 text-5xl mb-4">🎫</div>
              <p className="text-gray-600 mb-4">보유한 쿠폰이 없습니다</p>
              <button
                onClick={() => setActiveTab('available')}
                className="text-indigo-600 hover:text-indigo-700 font-medium"
              >
                쿠폰 받으러 가기 →
              </button>
            </div>
          ) : (
            <div>
              {/* 사용 가능한 쿠폰 */}
              <div className="mb-8">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  사용 가능 ({userCoupons.filter((uc) => uc.status === 'AVAILABLE').length})
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {userCoupons
                    .filter((uc) => uc.status === 'AVAILABLE')
                    .map((userCoupon) => (
                      <CouponCard
                        key={userCoupon.id}
                        coupon={userCoupon.coupon}
                        userCoupon={userCoupon}
                      />
                    ))}
                </div>
                {userCoupons.filter((uc) => uc.status === 'AVAILABLE').length === 0 && (
                  <p className="text-gray-500 text-sm">사용 가능한 쿠폰이 없습니다</p>
                )}
              </div>

              {/* 사용 완료/만료된 쿠폰 */}
              {userCoupons.filter((uc) => uc.status !== 'AVAILABLE').length > 0 && (
                <div>
                  <h2 className="text-lg font-semibold text-gray-500 mb-4">
                    사용 완료/만료 ({userCoupons.filter((uc) => uc.status !== 'AVAILABLE').length})
                  </h2>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {userCoupons
                      .filter((uc) => uc.status !== 'AVAILABLE')
                      .map((userCoupon) => (
                        <CouponCard
                          key={userCoupon.id}
                          coupon={userCoupon.coupon}
                          userCoupon={userCoupon}
                        />
                      ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default CouponListPage
