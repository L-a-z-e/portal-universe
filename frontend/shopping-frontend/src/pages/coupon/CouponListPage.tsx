/**
 * CouponListPage
 * 쿠폰 목록 페이지 - 발급 가능한 쿠폰 및 내 쿠폰 관리
 */
import { useState, useEffect, useRef } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { Button, Spinner, Badge, useApiError, useToast } from '@portal/design-react'
import { useAvailableCoupons, useUserCoupons, useIssueCoupon } from '@/hooks/useCoupons'
import { CouponCard } from '@/components/coupon/CouponCard'
import { queueApi } from '@/api'

type TabType = 'available' | 'my'

export function CouponListPage() {
  const { handleError } = useApiError()
  const { success } = useToast()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [activeTab, setActiveTab] = useState<TabType>('available')
  const [issuingCouponId, setIssuingCouponId] = useState<number | null>(null)
  const autoIssueAttempted = useRef(false)

  const { data: availableCoupons, isLoading: loadingAvailable, refetch: refetchAvailable } = useAvailableCoupons()
  const { data: userCoupons, isLoading: loadingMy, refetch: refetchMy } = useUserCoupons()
  const { mutateAsync: issueCoupon } = useIssueCoupon()

  // Queue 통과 후 자동 발급 처리
  useEffect(() => {
    const issueCouponId = searchParams.get('issueCouponId')
    if (!issueCouponId || autoIssueAttempted.current) return

    autoIssueAttempted.current = true
    // URL에서 param 제거 (중복 발급 방지)
    searchParams.delete('issueCouponId')
    setSearchParams(searchParams, { replace: true })

    executeIssueCoupon(parseInt(issueCouponId))
  }, [searchParams])

  const executeIssueCoupon = async (couponId: number) => {
    try {
      setIssuingCouponId(couponId)
      await issueCoupon(couponId)
      await Promise.all([refetchAvailable(), refetchMy()])
      success('쿠폰이 발급되었습니다!')
    } catch (error) {
      handleError(error, '쿠폰 발급에 실패했습니다.')
    } finally {
      setIssuingCouponId(null)
    }
  }

  const handleIssueCoupon = async (couponId: number) => {
    try {
      // Queue 활성 여부 확인
      const queueResponse = await queueApi.checkQueueActive('COUPON', couponId)
      if (queueResponse.success && queueResponse.data) {
        // Queue 활성 → 대기열 페이지로 이동
        const returnUrl = encodeURIComponent(`/coupons?issueCouponId=${couponId}`)
        navigate(`/queue/COUPON/${couponId}?returnUrl=${returnUrl}`)
        return
      }
    } catch {
      // Queue 확인 실패 시 직접 발급 진행
    }

    // Queue 비활성 → 직접 발급
    await executeIssueCoupon(couponId)
  }

  const isLoading = activeTab === 'available' ? loadingAvailable : loadingMy

  // 이미 발급받은 쿠폰 ID 목록
  const issuedCouponIds = userCoupons.map((uc) => uc.couponId)

  // 발급 가능한 쿠폰 중 아직 발급받지 않은 것만 필터링
  const filteredAvailableCoupons = availableCoupons.filter(
    (coupon) => !issuedCouponIds.includes(coupon.id)
  )

  return (
    <div className="container mx-auto px-4 py-8">
      {/* 페이지 헤더 */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-text-heading">쿠폰</h1>
        <p className="text-text-body mt-1">쿠폰을 발급받고 주문 시 할인을 받으세요</p>
      </div>

      {/* 탭 네비게이션 */}
      <div className="border-b border-border-default mb-6">
        <nav className="-mb-px flex space-x-8">
          <Button
            variant="ghost"
            onClick={() => setActiveTab('available')}
            className={`
              py-4 px-1 rounded-none border-b-2 font-medium text-sm
              ${activeTab === 'available'
                ? 'border-brand-primary text-brand-primary'
                : 'border-transparent text-text-meta hover:text-text-body hover:border-border-default'
              }
            `}
          >
            발급 가능한 쿠폰
            {filteredAvailableCoupons.length > 0 && (
              <Badge variant="brand" className="ml-2">
                {filteredAvailableCoupons.length}
              </Badge>
            )}
          </Button>
          <Button
            variant="ghost"
            onClick={() => setActiveTab('my')}
            className={`
              py-4 px-1 rounded-none border-b-2 font-medium text-sm
              ${activeTab === 'my'
                ? 'border-brand-primary text-brand-primary'
                : 'border-transparent text-text-meta hover:text-text-body hover:border-border-default'
              }
            `}
          >
            내 쿠폰함
            {userCoupons.filter((uc) => uc.status === 'AVAILABLE').length > 0 && (
              <Badge variant="success" className="ml-2">
                {userCoupons.filter((uc) => uc.status === 'AVAILABLE').length}
              </Badge>
            )}
          </Button>
        </nav>
      </div>

      {/* 로딩 상태 */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <Spinner size="lg" />
          <span className="ml-3 text-text-body">로딩 중...</span>
        </div>
      )}

      {/* 발급 가능한 쿠폰 탭 */}
      {!isLoading && activeTab === 'available' && (
        <div>
          {filteredAvailableCoupons.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-text-meta text-5xl mb-4">🎫</div>
              <p className="text-text-body">현재 발급 가능한 쿠폰이 없습니다</p>
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
              <div className="text-text-meta text-5xl mb-4">🎫</div>
              <p className="text-text-body mb-4">보유한 쿠폰이 없습니다</p>
              <Button
                onClick={() => setActiveTab('available')}
                variant="ghost"
                className="text-brand-primary hover:text-brand-primaryHover font-medium"
              >
                쿠폰 받으러 가기 →
              </Button>
            </div>
          ) : (
            <div>
              {/* 사용 가능한 쿠폰 */}
              <div className="mb-8">
                <h2 className="text-lg font-semibold text-text-heading mb-4">
                  사용 가능 ({userCoupons.filter((uc) => uc.status === 'AVAILABLE').length})
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {userCoupons
                    .filter((uc) => uc.status === 'AVAILABLE')
                    .map((userCoupon) => (
                      <CouponCard
                        key={userCoupon.id}
                        userCoupon={userCoupon}
                      />
                    ))}
                </div>
                {userCoupons.filter((uc) => uc.status === 'AVAILABLE').length === 0 && (
                  <p className="text-text-meta text-sm">사용 가능한 쿠폰이 없습니다</p>
                )}
              </div>

              {/* 사용 완료/만료된 쿠폰 */}
              {userCoupons.filter((uc) => uc.status !== 'AVAILABLE').length > 0 && (
                <div>
                  <h2 className="text-lg font-semibold text-text-meta mb-4">
                    사용 완료/만료 ({userCoupons.filter((uc) => uc.status !== 'AVAILABLE').length})
                  </h2>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {userCoupons
                      .filter((uc) => uc.status !== 'AVAILABLE')
                      .map((userCoupon) => (
                        <CouponCard
                          key={userCoupon.id}
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
