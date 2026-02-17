/**
 * Cart Item Component
 *
 * 장바구니 항목 표시 및 수량 조절
 */
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@portal/design-react'
import { useCartStore } from '@/stores/cartStore'
import type { CartItem } from '@/types'

interface CartItemComponentProps {
  item: CartItem
}

const CartItemComponent: React.FC<CartItemComponentProps> = ({ item }) => {
  const { updateItemQuantity, removeItem } = useCartStore()
  const [updating, setUpdating] = useState(false)
  const [removing, setRemoving] = useState(false)

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const handleQuantityChange = async (newQuantity: number) => {
    if (newQuantity < 1 || updating) return

    setUpdating(true)
    try {
      await updateItemQuantity(item.id, newQuantity)
    } catch (error) {
      console.error('Failed to update quantity:', error)
    } finally {
      setUpdating(false)
    }
  }

  const handleRemove = async () => {
    if (removing) return

    setRemoving(true)
    try {
      await removeItem(item.id)
    } catch (error) {
      console.error('Failed to remove item:', error)
      setRemoving(false)
    }
  }

  const subtotal = item.price * item.quantity

  return (
    <div className={`bg-bg-card border border-border-default rounded-lg p-4 transition-opacity ${
      removing ? 'opacity-50' : ''
    }`}>
      <div className="flex gap-4">
        {/* Product Image */}
        <Link
          to={`/products/${item.productId}`}
          className="flex-shrink-0 w-24 h-24 bg-bg-subtle rounded-lg overflow-hidden"
        >
          <div className="w-full h-full flex items-center justify-center text-text-placeholder">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
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
              {removing ? (
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
              ) : (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  />
                </svg>
              )}
            </Button>
          </div>

          {/* Quantity & Subtotal */}
          <div className="flex items-center justify-between mt-4">
            {/* Quantity Selector */}
            <div className="flex items-center border border-border-default rounded-lg">
              <button
                onClick={() => handleQuantityChange(item.quantity - 1)}
                disabled={item.quantity <= 1 || updating}
                className="w-8 h-8 flex items-center justify-center text-text-body hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
                </svg>
              </button>

              <span className={`w-10 text-center font-medium ${
                updating ? 'text-text-placeholder' : 'text-text-heading'
              }`}>
                {updating ? (
                  <svg className="w-4 h-4 mx-auto animate-spin" viewBox="0 0 24 24">
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
                ) : (
                  item.quantity
                )}
              </span>

              <button
                onClick={() => handleQuantityChange(item.quantity + 1)}
                disabled={updating}
                className="w-8 h-8 flex items-center justify-center text-text-body hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
              </button>
            </div>

            {/* Subtotal */}
            <span className="font-bold text-text-heading">
              {formatPrice(subtotal)}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default CartItemComponent
