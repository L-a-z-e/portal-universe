/**
 * Product Reviews Component
 * Blog 서비스 연동 상품 리뷰 섹션
 */
import React from 'react'
import { useProductReviews } from '@/hooks/useProductReviews'
import { Spinner } from '@portal/design-system-react'

interface ProductReviewsProps {
  productId: number
}

const ProductReviews: React.FC<ProductReviewsProps> = ({ productId }) => {
  const { data, isLoading, error } = useProductReviews(productId)

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Spinner size="md" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-8 text-text-meta">
        <p>Failed to load reviews</p>
      </div>
    )
  }

  const reviews = data?.reviews ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-text-heading">
          Reviews ({reviews.length})
        </h2>
      </div>

      {reviews.length === 0 ? (
        <div className="bg-bg-subtle rounded-lg p-8 text-center">
          <svg className="w-12 h-12 mx-auto text-text-placeholder mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p className="text-text-meta">No reviews yet for this product</p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div
              key={review.id}
              className="bg-bg-card border border-border-default rounded-lg p-5 space-y-2"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-text-heading">{review.title}</h3>
                <span className="text-xs text-text-meta">by {review.authorId}</span>
              </div>
              <p className="text-sm text-text-body leading-relaxed line-clamp-4">
                {review.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default ProductReviews
