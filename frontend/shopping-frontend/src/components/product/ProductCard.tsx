import React from 'react'
import { Link } from 'react-router-dom'
import type { Product, Inventory } from '@/types'
import StarRating from '@/components/common/StarRating'
import PriceDisplay from '@/components/common/PriceDisplay'

interface ProductCardProps {
  product: Product
  inventory?: Inventory
}

const ProductCard: React.FC<ProductCardProps> = ({ product, inventory }) => {
  const [imgError, setImgError] = React.useState(false)

  const isInStock = inventory ? inventory.availableQuantity > 0 : true
  const isNew = product.createdAt
    ? Date.now() - new Date(product.createdAt).getTime() < 7 * 24 * 60 * 60 * 1000
    : false

  return (
    <Link
      to={`/products/${product.id}`}
      className={`group flex flex-col ${!isInStock ? 'opacity-70' : ''}`}
    >
      {/* Image */}
      <div className="relative aspect-square rounded-3xl overflow-hidden bg-bg-subtle">
        {product.imageUrl && !imgError ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-text-placeholder">
            <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        )}

        {/* Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5">
          {product.featured && (
            <span className="px-2.5 py-1 rounded-full bg-brand-primary text-white text-[10px] font-bold uppercase tracking-wider">
              Bestseller
            </span>
          )}
          {isNew && !product.featured && (
            <span className="px-2.5 py-1 rounded-full bg-accent-teal text-white text-[10px] font-bold uppercase tracking-wider">
              New
            </span>
          )}
        </div>

        {!isInStock && (
          <div className="absolute inset-0 bg-bg-page/50 flex items-center justify-center">
            <span className="px-3 py-1.5 rounded-full bg-bg-card text-text-meta text-sm font-medium">
              Out of Stock
            </span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="mt-4 space-y-1.5">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-medium text-text-heading line-clamp-1 group-hover:text-brand-primary transition-colors">
            {product.name}
          </h3>
          {product.averageRating != null && product.averageRating > 0 && (
            <StarRating rating={product.averageRating} size="sm" />
          )}
        </div>

        {product.category && (
          <p className="text-xs text-text-meta">{product.category}</p>
        )}

        <PriceDisplay
          price={product.price}
          discountPrice={product.discountPrice}
          size="sm"
        />
      </div>
    </Link>
  )
}

export default ProductCard
