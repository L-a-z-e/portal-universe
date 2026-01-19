/**
 * TimeDealDetailPage
 * 타임딜 상세 및 구매 페이지
 */
import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useTimeDeal, usePurchaseTimeDeal, calculateStockPercentage } from '@/hooks/useTimeDeals'
import { CountdownTimer } from '@/components/timedeal/CountdownTimer'
import { TIMEDEAL_STATUS_LABELS } from '@/types'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price)
}

export function TimeDealDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const timeDealId = id ? parseInt(id) : null

  const { data: timeDeal, isLoading, error } = useTimeDeal(timeDealId)
  const { mutateAsync: purchaseTimeDeal, isPending: isPurchasing } = usePurchaseTimeDeal()

  const [quantity, setQuantity] = useState(1)

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
          <span className="ml-3 text-gray-600">로딩 중...</span>
        </div>
      </div>
    )
  }

  if (error || !timeDeal) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="text-red-500 text-5xl mb-4">!</div>
          <p className="text-gray-600 mb-4">타임딜을 찾을 수 없습니다</p>
          <Link
            to="/time-deals"
            className="text-red-600 hover:text-red-700 font-medium"
          >
            타임딜 목록으로 돌아가기
          </Link>
        </div>
      </div>
    )
  }

  const { product, dealPrice, discountRate, totalStock, soldCount, purchaseLimit, status, endsAt } = timeDeal
  const remainingStock = totalStock - soldCount
  const stockPercentage = calculateStockPercentage(soldCount, totalStock)
  const isSoldOut = status === 'SOLD_OUT' || remainingStock <= 0
  const isActive = status === 'ACTIVE'
  const maxQuantity = Math.min(remainingStock, purchaseLimit)
  const totalPrice = dealPrice * quantity

  const handleQuantityChange = (delta: number) => {
    const newQuantity = quantity + delta
    if (newQuantity >= 1 && newQuantity <= maxQuantity) {
      setQuantity(newQuantity)
    }
  }

  const handlePurchase = async () => {
    if (!isActive || isSoldOut || isPurchasing) return

    try {
      const order = await purchaseTimeDeal(timeDeal.id, quantity)
      alert('구매가 완료되었습니다!')
      navigate(`/orders/${order.orderNumber}`)
    } catch (err) {
      const message = err instanceof Error ? err.message : '구매에 실패했습니다'
      alert(message)
    }
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* 뒤로가기 */}
      <Link
        to="/time-deals"
        className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        타임딜 목록
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* 상품 이미지 */}
        <div className="relative aspect-square bg-gray-100 rounded-lg overflow-hidden">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400">
              <svg className="w-24 h-24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          )}

          {/* 할인율 배지 */}
          <div className="absolute top-4 left-4 bg-red-600 text-white px-4 py-2 rounded-lg text-lg font-bold">
            {discountRate}% OFF
          </div>

          {/* 품절/종료 오버레이 */}
          {(isSoldOut || !isActive) && (
            <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
              <span className="text-white text-3xl font-bold">
                {isSoldOut ? '품절' : TIMEDEAL_STATUS_LABELS[status]}
              </span>
            </div>
          )}
        </div>

        {/* 상품 정보 */}
        <div>
          {/* 타이머 */}
          {isActive && !isSoldOut && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
              <div className="flex items-center justify-between">
                <span className="text-red-700 font-medium">남은 시간</span>
                <CountdownTimer endsAt={endsAt} size="lg" />
              </div>
            </div>
          )}

          {/* 상품명 */}
          <h1 className="text-2xl font-bold text-gray-900 mb-4">{product.name}</h1>

          {/* 가격 */}
          <div className="mb-6">
            <div className="flex items-baseline gap-3 mb-2">
              <span className="text-3xl font-bold text-red-600">
                {formatPrice(dealPrice)}원
              </span>
              <span className="text-lg text-gray-400 line-through">
                {formatPrice(product.price)}원
              </span>
            </div>
            <p className="text-sm text-gray-500">
              {formatPrice(product.price - dealPrice)}원 할인
            </p>
          </div>

          {/* 재고 현황 */}
          <div className="mb-6">
            <div className="flex justify-between text-sm text-gray-600 mb-2">
              <span>{soldCount}개 판매</span>
              <span>{remainingStock}개 남음</span>
            </div>
            <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
              <div
                className={`h-full transition-all duration-300 ${
                  stockPercentage >= 80 ? 'bg-red-500' :
                  stockPercentage >= 50 ? 'bg-orange-500' : 'bg-green-500'
                }`}
                style={{ width: `${stockPercentage}%` }}
              />
            </div>
          </div>

          {/* 상품 설명 */}
          {product.description && (
            <div className="mb-6 p-4 bg-gray-50 rounded-lg">
              <p className="text-gray-700">{product.description}</p>
            </div>
          )}

          {/* 구매 제한 안내 */}
          <div className="mb-6 text-sm text-gray-500">
            <p>1인당 최대 {purchaseLimit}개 구매 가능</p>
          </div>

          {/* 수량 선택 */}
          {isActive && !isSoldOut && (
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">수량</label>
              <div className="flex items-center gap-4">
                <div className="flex items-center border border-gray-300 rounded-lg">
                  <button
                    onClick={() => handleQuantityChange(-1)}
                    disabled={quantity <= 1}
                    className="px-4 py-2 text-gray-600 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    -
                  </button>
                  <span className="px-4 py-2 font-medium">{quantity}</span>
                  <button
                    onClick={() => handleQuantityChange(1)}
                    disabled={quantity >= maxQuantity}
                    className="px-4 py-2 text-gray-600 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    +
                  </button>
                </div>
                <span className="text-gray-500">
                  총 <span className="font-bold text-gray-900">{formatPrice(totalPrice)}원</span>
                </span>
              </div>
            </div>
          )}

          {/* 구매 버튼 */}
          <button
            onClick={handlePurchase}
            disabled={!isActive || isSoldOut || isPurchasing}
            className={`
              w-full py-4 rounded-lg font-bold text-lg transition-colors
              ${isActive && !isSoldOut && !isPurchasing
                ? 'bg-red-600 text-white hover:bg-red-700'
                : 'bg-gray-200 text-gray-500 cursor-not-allowed'
              }
            `}
          >
            {isPurchasing ? '구매 처리 중...' :
             isSoldOut ? '품절' :
             !isActive ? '구매 불가' :
             `${formatPrice(totalPrice)}원 구매하기`}
          </button>
        </div>
      </div>
    </div>
  )
}

export default TimeDealDetailPage
