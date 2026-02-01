/**
 * Product Detail Page
 *
 * 상품 상세 정보 페이지
 */
import React, { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { productApi, inventoryApi } from '@/api/endpoints'
import { useCartStore } from '@/stores/cartStore'
import { useInventoryStream } from '@/hooks/useInventoryStream'
import type { Product, Inventory } from '@/types'
import ProductReviews from '@/components/product/ProductReviews'
import { Button, Spinner, Alert, Badge } from '@portal/design-system-react'

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
  const [imgError, setImgError] = useState(false)

  // SSE for real-time inventory updates
  const parsedId = productId ? parseInt(productId) : 0
  const { getUpdate } = useInventoryStream({
    productIds: parsedId > 0 ? [parsedId] : [],
    enabled: parsedId > 0
  })
  const sseUpdate = parsedId > 0 ? getUpdate(parsedId) : null

  // Override inventory with SSE data if available
  const liveAvailable = sseUpdate ? sseUpdate.available : inventory?.availableQuantity
  const liveReserved = sseUpdate ? sseUpdate.reserved : inventory?.reservedQuantity

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

        setProduct(productRes.data)

        if (inventoryRes) {
          setInventory(inventoryRes.data)
        }
      } catch (err) {
        if (err instanceof Error && err.message) {
          setError(err.message)
        } else {
          setError('Failed to fetch product')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchProduct()
  }, [productId])

  const isInStock = liveAvailable !== undefined ? liveAvailable > 0 : (inventory ? inventory.availableQuantity > 0 : true)
  const maxQuantity = liveAvailable ?? inventory?.availableQuantity ?? 10

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
          <Spinner size="lg" />
          <p className="text-text-meta">Loading product...</p>
        </div>
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="space-y-6">
        <Button
          onClick={() => navigate(-1)}
          variant="ghost"
          className="gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back
        </Button>

        <Alert variant="error" className="text-center">
          <p className="text-lg mb-4">{error}</p>
          <Button asChild variant="primary">
            <Link to="/">Browse Products</Link>
          </Button>
        </Alert>
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
          {product.imageUrl && !imgError ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover"
              onError={() => setImgError(true)}
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
            <Badge variant="info">{product.category}</Badge>
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
          {(inventory || sseUpdate) && (
            <div className="flex items-center gap-2">
              <div
                className={`w-3 h-3 rounded-full ${
                  isInStock
                    ? maxQuantity <= 5
                      ? 'bg-status-warning'
                      : 'bg-status-success'
                    : 'bg-status-error'
                }${sseUpdate ? ' animate-pulse' : ''}`}
              />
              <span
                className={`text-sm font-medium ${
                  isInStock
                    ? maxQuantity <= 5
                      ? 'text-status-warning'
                      : 'text-status-success'
                    : 'text-status-error'
                }`}
              >
                {isInStock
                  ? maxQuantity <= 5
                    ? `Only ${maxQuantity} left in stock`
                    : `${maxQuantity} in stock`
                  : 'Out of Stock'}
              </span>
              {sseUpdate && (
                <span className="text-xs text-text-meta">(live)</span>
              )}
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
                <Button
                  onClick={() => handleQuantityChange(-1)}
                  disabled={quantity <= 1}
                  variant="ghost"
                  size="sm"
                  className="w-10 h-10 rounded-none rounded-l-lg"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
                  </svg>
                </Button>
                <span className="w-12 text-center font-medium text-text-heading">
                  {quantity}
                </span>
                <Button
                  onClick={() => handleQuantityChange(1)}
                  disabled={quantity >= maxQuantity}
                  variant="ghost"
                  size="sm"
                  className="w-10 h-10 rounded-none rounded-r-lg"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                  </svg>
                </Button>
              </div>
              <span className="text-sm text-text-meta">
                Max: {maxQuantity}
              </span>
            </div>
          </div>

          {/* Add to Cart */}
          <div className="space-y-4">
            <Button
              onClick={handleAddToCart}
              disabled={!isInStock || adding}
              variant={isInStock && !adding ? 'primary' : 'secondary'}
              size="lg"
              className="w-full"
            >
              {adding ? (
                <span className="flex items-center justify-center gap-2">
                  <Spinner size="sm" />
                  Adding to Cart...
                </span>
              ) : isInStock ? (
                `Add to Cart - ${formatPrice(product.price * quantity)}`
              ) : (
                'Out of Stock'
              )}
            </Button>

            {/* Success message */}
            {addSuccess && (
              <Alert variant="success" className="flex items-center justify-between">
                <span className="text-sm">Added to cart successfully!</span>
                <Button asChild variant="ghost" size="sm">
                  <Link to="/cart">View Cart</Link>
                </Button>
              </Alert>
            )}
          </div>

          {/* Product Meta */}
          <div className="pt-6 border-t border-border-default space-y-2 text-sm text-text-meta">
            <p>
              <span className="font-medium text-text-body">Product ID:</span> {product.id}
            </p>
            <p>
              <span className="font-medium text-text-body">Added:</span>{' '}
              {product.createdAt ? new Date(product.createdAt).toLocaleDateString('ko-KR') : '-'}
            </p>
          </div>
        </div>
      </div>
      {/* Reviews Section */}
      <div className="pt-6 border-t border-border-default">
        <ProductReviews productId={parsedId} />
      </div>
    </div>
  )
}

export default ProductDetailPage
