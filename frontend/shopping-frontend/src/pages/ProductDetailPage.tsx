/**
 * Product Detail Page
 *
 * 상품 상세 정보 페이지
 */
import React, { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { productApi, inventoryApi } from '@/api/endpoints'
import { useCartStore } from '@/stores/cartStore'
import type { Product, Inventory } from '@/types'

const ProductDetailPage: React.FC = () => {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const { addItem } = useCartStore()

  // State
  const [product, setProduct] = useState<Product | null>(null)
  const [inventory, setInventory] = useState<Inventory | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const [addSuccess, setAddSuccess] = useState(false)

  // Fetch product data
  useEffect(() => {
    const fetchProduct = async () => {
      if (!productId) return

      setLoading(true)
      setError(null)

      try {
        const [productRes, inventoryRes] = await Promise.all([
          productApi.getProduct(parseInt(productId)),
          inventoryApi.getInventory(parseInt(productId)).catch(() => null)
        ])

        if (productRes.success) {
          setProduct(productRes.data)
        } else {
          setError(productRes.message || 'Failed to fetch product')
        }

        if (inventoryRes?.success) {
          setInventory(inventoryRes.data)
        }
      } catch (err: any) {
        if (err.response?.status === 404) {
          setError('Product not found')
        } else {
          setError(err.response?.data?.message || err.message || 'Failed to fetch product')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchProduct()
  }, [productId])

  const isInStock = inventory ? inventory.availableQuantity > 0 : true
  const maxQuantity = inventory?.availableQuantity || 10

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const handleQuantityChange = (delta: number) => {
    const newQuantity = quantity + delta
    if (newQuantity >= 1 && newQuantity <= maxQuantity) {
      setQuantity(newQuantity)
    }
  }

  const handleAddToCart = async () => {
    if (!product || !isInStock || adding) return

    setAdding(true)
    setAddSuccess(false)

    try {
      await addItem(product.id, product.name, product.price, quantity)
      setAddSuccess(true)
      setTimeout(() => setAddSuccess(false), 3000)
    } catch (error) {
      console.error('Failed to add to cart:', error)
    } finally {
      setAdding(false)
    }
  }

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-text-meta">Loading product...</p>
        </div>
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="space-y-6">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-text-meta hover:text-text-body transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back
        </button>

        <div className="bg-status-error-bg border border-status-error/20 rounded-lg p-8 text-center">
          <p className="text-status-error text-lg mb-4">{error}</p>
          <Link
            to="/"
            className="inline-block px-6 py-3 bg-brand-primary text-white rounded-lg hover:bg-brand-primary/90 transition-colors"
          >
            Browse Products
          </Link>
        </div>
      </div>
    )
  }

  if (!product) return null

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-text-meta">
        <Link to="/" className="hover:text-brand-primary transition-colors">
          Products
        </Link>
        <span>/</span>
        {product.category && (
          <>
            <Link
              to={`/?category=${product.category}`}
              className="hover:text-brand-primary transition-colors"
            >
              {product.category}
            </Link>
            <span>/</span>
          </>
        )}
        <span className="text-text-body">{product.name}</span>
      </nav>

      {/* Product Detail */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Image */}
        <div className="aspect-square bg-bg-subtle rounded-lg overflow-hidden">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-text-placeholder">
              <svg
                className="w-24 h-24"
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
        </div>

        {/* Info */}
        <div className="space-y-6">
          {/* Category */}
          {product.category && (
            <span className="inline-block px-3 py-1 bg-brand-primary/10 text-brand-primary rounded-full text-sm">
              {product.category}
            </span>
          )}

          {/* Name */}
          <h1 className="text-3xl font-bold text-text-heading">
            {product.name}
          </h1>

          {/* Price */}
          <div className="text-3xl font-bold text-brand-primary">
            {formatPrice(product.price)}
          </div>

          {/* Stock Status */}
          {inventory && (
            <div className="flex items-center gap-2">
              <div
                className={`w-3 h-3 rounded-full ${
                  isInStock
                    ? inventory.availableQuantity <= 5
                      ? 'bg-status-warning'
                      : 'bg-status-success'
                    : 'bg-status-error'
                }`}
              />
              <span
                className={`text-sm font-medium ${
                  isInStock
                    ? inventory.availableQuantity <= 5
                      ? 'text-status-warning'
                      : 'text-status-success'
                    : 'text-status-error'
                }`}
              >
                {isInStock
                  ? inventory.availableQuantity <= 5
                    ? `Only ${inventory.availableQuantity} left in stock`
                    : `${inventory.availableQuantity} in stock`
                  : 'Out of Stock'}
              </span>
            </div>
          )}

          {/* Description */}
          <div className="prose prose-sm max-w-none">
            <p className="text-text-body leading-relaxed">
              {product.description}
            </p>
          </div>

          {/* Quantity Selector */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-text-heading">
              Quantity
            </label>
            <div className="flex items-center gap-4">
              <div className="flex items-center border border-border-default rounded-lg">
                <button
                  onClick={() => handleQuantityChange(-1)}
                  disabled={quantity <= 1}
                  className="w-10 h-10 flex items-center justify-center text-text-body hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
                  </svg>
                </button>
                <span className="w-12 text-center font-medium text-text-heading">
                  {quantity}
                </span>
                <button
                  onClick={() => handleQuantityChange(1)}
                  disabled={quantity >= maxQuantity}
                  className="w-10 h-10 flex items-center justify-center text-text-body hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                  </svg>
                </button>
              </div>
              <span className="text-sm text-text-meta">
                Max: {maxQuantity}
              </span>
            </div>
          </div>

          {/* Add to Cart */}
          <div className="space-y-4">
            <button
              onClick={handleAddToCart}
              disabled={!isInStock || adding}
              className={`w-full py-4 rounded-lg font-medium text-lg transition-all ${
                isInStock && !adding
                  ? 'bg-brand-primary text-white hover:bg-brand-primary/90'
                  : 'bg-bg-disabled text-text-disabled cursor-not-allowed'
              }`}
            >
              {adding ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="w-5 h-5 animate-spin" viewBox="0 0 24 24">
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
                  Adding to Cart...
                </span>
              ) : isInStock ? (
                `Add to Cart - ${formatPrice(product.price * quantity)}`
              ) : (
                'Out of Stock'
              )}
            </button>

            {/* Success message */}
            {addSuccess && (
              <div className="bg-status-success-bg border border-status-success/20 rounded-lg p-4 flex items-center justify-between">
                <span className="text-status-success text-sm">
                  Added to cart successfully!
                </span>
                <Link
                  to="/cart"
                  className="text-brand-primary text-sm font-medium hover:underline"
                >
                  View Cart
                </Link>
              </div>
            )}
          </div>

          {/* Product Meta */}
          <div className="pt-6 border-t border-border-default space-y-2 text-sm text-text-meta">
            <p>
              <span className="font-medium text-text-body">Product ID:</span> {product.id}
            </p>
            <p>
              <span className="font-medium text-text-body">Added:</span>{' '}
              {new Date(product.createdAt).toLocaleDateString('ko-KR')}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
