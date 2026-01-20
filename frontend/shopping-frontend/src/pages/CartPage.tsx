/**
 * Cart Page
 *
 * 장바구니 조회 및 관리 페이지
 */
import React, { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCartStore } from '@/stores/cartStore'
import CartItemComponent from '@/components/CartItem'
import { Button, Spinner, Alert } from '@portal/design-system-react'

const CartPage: React.FC = () => {
  const navigate = useNavigate()
  const {
    cart,
    loading,
    error,
    fetchCart,
    clearCart
  } = useCartStore()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const handleCheckout = () => {
    navigate('/checkout')
  }

  const handleClearCart = async () => {
    if (window.confirm('Are you sure you want to clear your cart?')) {
      try {
        await clearCart()
      } catch (error) {
        console.error('Failed to clear cart:', error)
      }
    }
  }

  // Loading state
  if (loading && !cart) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-4">
          <Spinner size="lg" />
          <p className="text-text-meta">Loading cart...</p>
        </div>
      </div>
    )
  }

  // Error state
  if (error && !cart) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-text-heading">Shopping Cart</h1>
        <Alert variant="error" className="text-center">
          <p className="text-lg mb-4">{error}</p>
          <Button onClick={() => fetchCart()} variant="primary">
            Retry
          </Button>
        </Alert>
      </div>
    )
  }

  const items = cart?.items || []
  const isEmpty = items.length === 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-heading">
          Shopping Cart
          {!isEmpty && (
            <span className="ml-2 text-lg font-normal text-text-meta">
              ({items.length} {items.length === 1 ? 'item' : 'items'})
            </span>
          )}
        </h1>

        {!isEmpty && (
          <button
            onClick={handleClearCart}
            className="text-sm text-status-error hover:underline"
          >
            Clear Cart
          </button>
        )}
      </div>

      {isEmpty ? (
        /* Empty Cart */
        <div className="bg-bg-card border border-border-default rounded-lg p-12 text-center">
          <div className="w-16 h-16 mx-auto mb-4 text-text-placeholder">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
              />
            </svg>
          </div>
          <h2 className="text-lg font-medium text-text-heading mb-2">
            Your cart is empty
          </h2>
          <p className="text-text-meta mb-6">
            Looks like you haven't added any products yet.
          </p>
          <Button asChild variant="primary">
            <Link to="/">Start Shopping</Link>
          </Button>
        </div>
      ) : (
        /* Cart Content */
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {items.map((item) => (
              <CartItemComponent key={item.id} item={item} />
            ))}
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <div className="bg-bg-card border border-border-default rounded-lg p-6 sticky top-6">
              <h2 className="text-lg font-bold text-text-heading mb-4">
                Order Summary
              </h2>

              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-text-meta">
                    Subtotal ({cart?.totalQuantity} items)
                  </span>
                  <span className="text-text-body">
                    {formatPrice(cart?.totalAmount || 0)}
                  </span>
                </div>

                <div className="flex justify-between">
                  <span className="text-text-meta">Shipping</span>
                  <span className="text-text-body">Free</span>
                </div>

                <div className="border-t border-border-default pt-3 mt-3">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-bold text-text-heading">
                      Total
                    </span>
                    <span className="text-xl font-bold text-brand-primary">
                      {formatPrice(cart?.totalAmount || 0)}
                    </span>
                  </div>
                </div>
              </div>

              <Button
                onClick={handleCheckout}
                variant="primary"
                size="lg"
                className="w-full mt-6"
              >
                Proceed to Checkout
              </Button>

              <Link
                to="/"
                className="block text-center mt-4 text-sm text-brand-primary hover:underline"
              >
                Continue Shopping
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default CartPage
