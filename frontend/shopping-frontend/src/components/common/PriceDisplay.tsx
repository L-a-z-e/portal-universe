import React from 'react'

interface PriceDisplayProps {
  price: number
  discountPrice?: number
  size?: 'sm' | 'md' | 'lg'
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

const PriceDisplay: React.FC<PriceDisplayProps> = ({ price, discountPrice, size = 'md' }) => {
  const hasDiscount = discountPrice != null && discountPrice < price
  const discountPercent = hasDiscount
    ? Math.round(((price - discountPrice!) / price) * 100)
    : 0

  const sizeClasses = {
    sm: { current: 'text-sm font-bold', original: 'text-xs', badge: 'text-[10px] px-1 py-0.5' },
    md: { current: 'text-lg font-bold', original: 'text-sm', badge: 'text-xs px-1.5 py-0.5' },
    lg: { current: 'text-3xl font-black', original: 'text-base', badge: 'text-sm px-2 py-0.5' },
  }[size]

  if (!hasDiscount) {
    return (
      <span className={`${sizeClasses.current} text-text-heading`}>
        {formatPrice(price)}
      </span>
    )
  }

  return (
    <div className="flex items-baseline gap-2 flex-wrap">
      <span className={`${sizeClasses.current} text-text-heading`}>
        {formatPrice(discountPrice!)}
      </span>
      <span className={`${sizeClasses.original} text-text-meta line-through`}>
        {formatPrice(price)}
      </span>
      <span className={`${sizeClasses.badge} rounded-md bg-status-error/15 text-status-error font-semibold`}>
        -{discountPercent}%
      </span>
    </div>
  )
}

export default PriceDisplay
