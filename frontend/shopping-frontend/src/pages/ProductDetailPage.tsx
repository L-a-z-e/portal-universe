import React, { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { productApi, inventoryApi } from '@/api'
import { useCartStore } from '@/stores/cartStore'
import { useInventoryStream } from '@/hooks/useInventoryStream'
import type { Product, Inventory } from '@/types'
import ProductReviews from '@/components/product/ProductReviews'
import ImageGallery from '@/components/product/ImageGallery'
import StarRating from '@/components/common/StarRating'
import PriceDisplay from '@/components/common/PriceDisplay'
import QuantityStepper from '@/components/common/QuantityStepper'
import SecurityBadges from '@/components/common/SecurityBadges'
import { Button, Spinner, Alert, Badge, Tabs } from '@portal/design-react'

const ProductDetailPage: React.FC = () => {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const { addItem } = useCartStore()

  const [product, setProduct] = useState<Product | null>(null)
  const [inventory, setInventory] = useState<Inventory | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const [addSuccess, setAddSuccess] = useState(false)
  const [activeTab, setActiveTab] = useState('description')

  const parsedId = productId ? parseInt(productId) : 0
  const { getUpdate } = useInventoryStream({
    productIds: parsedId > 0 ? [parsedId] : [],
    enabled: parsedId > 0
  })
  const sseUpdate = parsedId > 0 ? getUpdate(parsedId) : null

  const liveAvailable = sseUpdate ? sseUpdate.available : inventory?.availableQuantity
  const isInStock = liveAvailable !== undefined ? liveAvailable > 0 : (inventory ? inventory.availableQuantity > 0 : true)
  const maxQuantity = liveAvailable ?? inventory?.availableQuantity ?? 10

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
        if (inventoryRes) setInventory(inventoryRes.data)
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch product')
      } finally {
        setLoading(false)
      }
    }
    fetchProduct()
  }, [productId])

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

  const handleAddToCart = async () => {
    if (!product || !isInStock || adding) return
    setAdding(true)
    setAddSuccess(false)
    try {
      await addItem(product.id, product.name, product.discountPrice ?? product.price, quantity)
      setAddSuccess(true)
      setTimeout(() => setAddSuccess(false), 4000)
    } catch {
      // error handled by store
    } finally {
      setAdding(false)
    }
  }

  const handleBuyNow = async () => {
    if (!product || !isInStock) return
    setAdding(true)
    try {
      await addItem(product.id, product.name, product.discountPrice ?? product.price, quantity)
      navigate('/checkout')
    } catch {
      setAdding(false)
    }
  }

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

  if (error) {
    return (
      <div className="space-y-6">
        <Button onClick={() => navigate(-1)} variant="ghost" className="gap-2">
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

  const galleryImages = product.images?.length
    ? product.images
    : product.imageUrl
      ? [product.imageUrl]
      : []

  const galleryBadges = [
    ...(product.featured ? [{ label: 'Bestseller', color: 'bg-brand-primary text-white' }] : []),
    ...(product.category ? [{ label: product.category, color: 'bg-bg-card/80 text-text-heading backdrop-blur-sm' }] : []),
  ]

  const effectivePrice = product.discountPrice ?? product.price

  const tabItems = [
    { label: 'Description', value: 'description' },
    { label: `Reviews${product.reviewCount ? ` (${product.reviewCount})` : ''}`, value: 'reviews' },
  ]

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-text-meta">
        <Link to="/" className="hover:text-brand-primary transition-colors">Store</Link>
        <span>/</span>
        {product.category && (
          <>
            <Link to={`/?category=${product.category}`} className="hover:text-brand-primary transition-colors">
              {product.category}
            </Link>
            <span>/</span>
          </>
        )}
        <span className="text-text-body truncate">{product.name}</span>
      </nav>

      {/* Product Detail - 7:5 Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12">
        {/* Left: Gallery */}
        <div className="lg:col-span-7">
          <ImageGallery images={galleryImages} alt={product.name} badges={galleryBadges} />
        </div>

        {/* Right: Info */}
        <div className="lg:col-span-5 space-y-6">
          {/* Name */}
          <h1 className="text-[28px] font-bold text-text-heading leading-tight">
            {product.name}
          </h1>

          {/* Rating + Stock */}
          <div className="flex items-center gap-3 flex-wrap">
            {product.averageRating != null && product.averageRating > 0 && (
              <StarRating rating={product.averageRating} reviewCount={product.reviewCount} size="md" />
            )}
            {(inventory || sseUpdate) && (
              <Badge variant={isInStock ? 'success' : 'error'}>
                {isInStock ? 'In Stock' : 'Out of Stock'}
              </Badge>
            )}
          </div>

          {/* Price Block */}
          <div className="bg-bg-elevated rounded-2xl p-5 space-y-3">
            <PriceDisplay price={product.price} discountPrice={product.discountPrice} size="lg" />
            <p className="text-xs text-text-meta">Free delivery &middot; VAT included</p>
          </div>

          {/* Quantity */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-text-heading">Quantity</label>
            <div className="flex items-center gap-3">
              <QuantityStepper
                value={quantity}
                min={1}
                max={maxQuantity}
                onChange={setQuantity}
                variant="pill"
              />
              <span className="text-sm text-text-meta">Max: {maxQuantity}</span>
            </div>
          </div>

          {/* Dual CTA */}
          <div className="flex gap-3">
            <Button
              onClick={handleAddToCart}
              disabled={!isInStock || adding}
              variant="outline"
              size="lg"
              className="flex-1 rounded-full border-2"
            >
              {adding ? 'Adding...' : 'Add to Cart'}
            </Button>
            <Button
              onClick={handleBuyNow}
              disabled={!isInStock || adding}
              variant="primary"
              size="lg"
              className="flex-1 rounded-full shadow-lg shadow-brand-primary/20"
            >
              Buy Now &middot; {formatPrice(effectivePrice * quantity)}
            </Button>
          </div>

          {/* Add success */}
          {addSuccess && (
            <Alert variant="success" className="flex items-center justify-between">
              <span className="text-sm">Added to cart!</span>
              <Button asChild variant="ghost" size="sm">
                <Link to="/cart">View Cart</Link>
              </Button>
            </Alert>
          )}

          {/* Security Badges */}
          <SecurityBadges />
        </div>
      </div>

      {/* Tabs Section */}
      <div className="mt-20">
        <Tabs
          value={activeTab}
          items={tabItems}
          variant="underline"
          onChange={setActiveTab}
        />

        <div className="mt-6">
          {activeTab === 'description' && (
            <div className="prose prose-sm max-w-none">
              <p className="text-text-body leading-relaxed whitespace-pre-line">
                {product.description}
              </p>
            </div>
          )}

          {activeTab === 'reviews' && (
            <ProductReviews productId={parsedId} />
          )}
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
