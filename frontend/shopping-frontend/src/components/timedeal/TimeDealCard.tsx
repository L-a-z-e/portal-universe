/**
 * TimeDealCard Component
 * 타임딜 카드 UI
 */
import { Link } from 'react-router-dom'
import { CountdownTimer } from './CountdownTimer'
import { calculateStockPercentage } from '@/hooks/useTimeDeals'
import type { TimeDeal } from '@/types'
import { TIMEDEAL_STATUS_LABELS } from '@/types'

interface TimeDealCardProps {
  timeDeal: TimeDeal
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price)
}

export function TimeDealCard({ timeDeal }: TimeDealCardProps) {
  const { product, dealPrice, discountRate, totalStock, soldCount, status, endsAt } = timeDeal
  const remainingStock = totalStock - soldCount
  const stockPercentage = calculateStockPercentage(soldCount, totalStock)
  const isSoldOut = status === 'SOLD_OUT' || remainingStock <= 0
  const isEnded = status === 'ENDED'
  const isActive = status === 'ACTIVE'

  return (
    <Link
      to={isActive ? `/time-deals/${timeDeal.id}` : '#'}
      className={`
        block bg-white border border-gray-200 rounded-lg overflow-hidden
        transition-all hover:shadow-lg
        ${(!isActive || isSoldOut) ? 'opacity-75 cursor-not-allowed' : 'cursor-pointer'}
      `}
    >
      {/* 상품 이미지 */}
      <div className="relative aspect-square bg-gray-100">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-400">
            <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        )}

        {/* 할인율 배지 */}
        <div className="absolute top-2 left-2 bg-red-600 text-white px-2 py-1 rounded-lg text-sm font-bold">
          {discountRate}% OFF
        </div>

        {/* 상태 배지 */}
        {(isSoldOut || isEnded) && (
          <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
            <span className="text-white text-xl font-bold">
              {isSoldOut ? '품절' : '종료'}
            </span>
          </div>
        )}
      </div>

      {/* 상품 정보 */}
      <div className="p-4">
        {/* 타이머 */}
        {isActive && !isSoldOut && (
          <div className="mb-3">
            <CountdownTimer endsAt={endsAt} size="sm" showLabels={false} />
          </div>
        )}

        {/* 상품명 */}
        <h3 className="font-medium text-gray-900 mb-2 line-clamp-2">
          {product.name}
        </h3>

        {/* 가격 */}
        <div className="flex items-baseline gap-2 mb-3">
          <span className="text-xl font-bold text-red-600">
            {formatPrice(dealPrice)}원
          </span>
          <span className="text-sm text-gray-400 line-through">
            {formatPrice(product.price)}원
          </span>
        </div>

        {/* 재고 진행률 바 */}
        <div className="mb-2">
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>{soldCount}개 판매</span>
            <span>{remainingStock}개 남음</span>
          </div>
          <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={`h-full transition-all duration-300 ${
                stockPercentage >= 80 ? 'bg-red-500' :
                stockPercentage >= 50 ? 'bg-orange-500' : 'bg-green-500'
              }`}
              style={{ width: `${stockPercentage}%` }}
            />
          </div>
        </div>

        {/* 상태 표시 */}
        {!isActive && (
          <span className={`
            inline-block px-2 py-1 rounded text-xs font-medium
            ${status === 'SCHEDULED' ? 'bg-blue-100 text-blue-700' : ''}
            ${status === 'SOLD_OUT' ? 'bg-red-100 text-red-700' : ''}
            ${status === 'ENDED' ? 'bg-gray-100 text-gray-700' : ''}
            ${status === 'CANCELLED' ? 'bg-gray-100 text-gray-500' : ''}
          `}>
            {TIMEDEAL_STATUS_LABELS[status]}
          </span>
        )}
      </div>
    </Link>
  )
}

export default TimeDealCard
