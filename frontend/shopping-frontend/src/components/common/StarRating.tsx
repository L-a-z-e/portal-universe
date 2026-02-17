import React from 'react'

interface StarRatingProps {
  rating: number
  reviewCount?: number
  size?: 'sm' | 'md'
}

const StarRating: React.FC<StarRatingProps> = ({ rating, reviewCount, size = 'sm' }) => {
  if (!rating && rating !== 0) return null

  const starSize = size === 'sm' ? 'w-3.5 h-3.5' : 'w-4.5 h-4.5'
  const textSize = size === 'sm' ? 'text-xs' : 'text-sm'

  const stars = Array.from({ length: 5 }, (_, i) => {
    const fill = Math.min(1, Math.max(0, rating - i))
    return fill
  })

  return (
    <div className="flex items-center gap-1.5">
      <div className="flex items-center gap-0.5">
        {stars.map((fill, i) => (
          <svg
            key={i}
            className={`${starSize} flex-shrink-0`}
            viewBox="0 0 20 20"
            fill="none"
          >
            {fill >= 1 ? (
              <path
                d="M10 1l2.39 4.84 5.34.78-3.87 3.77.91 5.33L10 13.27l-4.77 2.45.91-5.33L2.27 6.62l5.34-.78L10 1z"
                fill="#ff9f1c"
              />
            ) : fill > 0 ? (
              <>
                <defs>
                  <linearGradient id={`half-${i}`}>
                    <stop offset={`${fill * 100}%`} stopColor="#ff9f1c" />
                    <stop offset={`${fill * 100}%`} stopColor="#d1d5db" />
                  </linearGradient>
                </defs>
                <path
                  d="M10 1l2.39 4.84 5.34.78-3.87 3.77.91 5.33L10 13.27l-4.77 2.45.91-5.33L2.27 6.62l5.34-.78L10 1z"
                  fill={`url(#half-${i})`}
                />
              </>
            ) : (
              <path
                d="M10 1l2.39 4.84 5.34.78-3.87 3.77.91 5.33L10 13.27l-4.77 2.45.91-5.33L2.27 6.62l5.34-.78L10 1z"
                fill="#d1d5db"
              />
            )}
          </svg>
        ))}
      </div>
      <span className={`${textSize} font-semibold text-text-heading`}>
        {rating.toFixed(1)}
      </span>
      {reviewCount !== undefined && (
        <span className={`${textSize} text-text-meta`}>
          ({reviewCount})
        </span>
      )}
    </div>
  )
}

export default StarRating
