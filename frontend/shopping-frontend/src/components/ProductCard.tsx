/**
 * Product Card Component
 *
 * 상품 목록에서 사용되는 상품 카드
 */
import React from 'react'
import { Link } from 'react-router-dom'
import type { Product, Inventory } from '@/types'
import { useCartStore } from '@/stores/cartStore'

interface ProductCardProps {
  product: Product
  inventory?: Inventory
}

const ProductCard: React.FC<ProductCardProps> = ({ product, inventory }) => {
  const { addItem, loading: cartLoading } = useCartStore()
  const [adding, setAdding] = React.useState(false)

  const isInStock = inventory ? inventory.availableQuantity > 0 : true
  const stockLevel = inventory?.availableQuantity ?? 0

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const handleAddToCart = async (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()

    if (!isInStock || adding) return

    setAdding(true)
    try {
      await addItem(product.id, product.name, product.price, 1)
      // Show success feedback (could use toast)
      console.log(`Added ${product.name} to cart`)
    } catch (error) {
      console.error('Failed to add to cart:', error)
    } finally {
      setAdding(false)
    }
  }

  return (
    <Link
      to={`/products/${product.id}`}
      className="group bg-bg-card border border-border-default rounded-lg overflow-hidden hover:shadow-lg hover:border-brand-primary/30 transition-all"
    >
      {/* Product Image */}
      <div className="aspect-square bg-bg-subtle relative overflow-hidden">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-text-placeholder">
            <svg
              className="w-16 h-16"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
          </div>
        )}

        {/* Stock Badge */}
        {inventory && (
          <div
            className={`absolute top-2 right-2 px-2 py-1 rounded text-xs font-medium ${
              isInStock
                ? stockLevel <= 5
                  ? 'bg-status-warning-bg text-status-warning'
                  : 'bg-status-success-bg text-status-success'
                : 'bg-status-error-bg text-status-error'
            }`}
          >
            {isInStock
              ? stockLevel <= 5
                ? `Only ${stockLevel} left`
                : 'In Stock'
              : 'Out of Stock'}
          </div>
        )}
      </div>

      {/* Product Info */}
      <div className="p-4 space-y-3">
        {/* Category */}
        {product.category && (
          <span className="text-xs text-text-meta uppercase tracking-wider">
            {product.category}
          </span>
        )}

        {/* Name */}
        <h3 className="font-medium text-text-heading line-clamp-2 group-hover:text-brand-primary transition-colors">
          {product.name}
        </h3>

        {/* Description */}
        <p className="text-sm text-text-meta line-clamp-2">
          {product.description}
        </p>

        {/* Price & Add to Cart */}
        <div className="flex items-center justify-between pt-2">
          <span className="text-lg font-bold text-brand-primary">
            {formatPrice(product.price)}
          </span>

          <button
            onClick={handleAddToCart}
            disabled={!isInStock || adding}
            className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
              isInStock && !adding
                ? 'bg-brand-primary text-white hover:bg-brand-primary/90'
                : 'bg-bg-disabled text-text-disabled cursor-not-allowed'
            }`}
          >
            {adding ? (
              <span className="flex items-center gap-2">
                <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24">
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                    fill="none"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                Adding...
              </span>
            ) : (
              'Add to Cart'
            )}
          </button>
        </div>
      </div>
    </Link>
  )
}

export default ProductCard
