import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@portal/design-react'
import { useCartStore } from '@/stores/cartStore'
import QuantityStepper from '@/components/common/QuantityStepper'
import type { CartItem } from '@/types'

interface CartItemComponentProps {
  item: CartItem
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

const CartItemComponent: React.FC<CartItemComponentProps> = ({ item }) => {
  const { updateItemQuantity, removeItem } = useCartStore()
  const [updating, setUpdating] = useState(false)
  const [removing, setRemoving] = useState(false)

  const handleQuantityChange = async (newQuantity: number) => {
    if (newQuantity < 1 || updating) return
    setUpdating(true)
    try {
      await updateItemQuantity(item.id, newQuantity)
    } catch {
      // error handled by store
    } finally {
      setUpdating(false)
    }
  }

  const handleRemove = async () => {
    if (removing) return
    setRemoving(true)
    try {
      await removeItem(item.id)
    } catch {
      setRemoving(false)
    }
  }

  const subtotal = item.price * item.quantity

  return (
    <div className={`bg-bg-card border border-border-default rounded-2xl p-5 hover:border-brand-primary/30 transition-all duration-300 ${
      removing ? 'opacity-50 scale-[0.98]' : ''
    }`}>
      <div className="flex gap-4">
        {/* Product Image */}
        <Link
          to={`/products/${item.productId}`}
          className="flex-shrink-0 w-24 h-24 bg-bg-subtle rounded-xl overflow-hidden"
        >
          <div className="w-full h-full flex items-center justify-center text-text-placeholder">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        </Link>

        {/* Product Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <Link
                to={`/products/${item.productId}`}
                className="font-medium text-text-heading hover:text-brand-primary transition-colors line-clamp-2"
              >
                {item.productName}
              </Link>
              <p className="text-sm text-text-meta mt-1">
                {formatPrice(item.price)} each
              </p>
            </div>

            {/* Remove Button */}
            <Button
              onClick={handleRemove}
              disabled={removing}
              variant="ghost"
              size="sm"
              className="flex-shrink-0 text-text-meta hover:text-status-error"
              aria-label="Remove item"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </Button>
          </div>

          {/* Quantity & Subtotal */}
          <div className="flex items-center justify-between mt-4">
            <QuantityStepper
              value={item.quantity}
              min={1}
              max={99}
              onChange={handleQuantityChange}
              loading={updating}
              variant="default"
            />
            <span className="font-bold text-text-heading text-lg">
              {formatPrice(subtotal)}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default CartItemComponent
