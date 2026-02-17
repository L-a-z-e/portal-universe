import React, { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCartStore } from '@/stores/cartStore'
import CartItemComponent from '@/components/cart/CartItem'
import SecurityBadges from '@/components/common/SecurityBadges'
import { Button, Spinner, Alert } from '@portal/design-react'

const formatPrice = (price: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(price)

const CartPage: React.FC = () => {
  const navigate = useNavigate()
  const { cart, loading, error, fetchCart, clearCart } = useCartStore()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  const handleCheckout = () => navigate('/checkout')

  const handleClearCart = async () => {
    if (window.confirm('장바구니를 비우시겠습니까?')) {
      try {
        await clearCart()
      } catch {
        // error handled by store
      }
    }
  }

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

  if (error && !cart) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-text-heading">Shopping Cart</h1>
        <Alert variant="error" className="text-center">
          <p className="text-lg mb-4">{error}</p>
          <Button onClick={() => fetchCart()} variant="primary">Retry</Button>
        </Alert>
      </div>
    )
  }

  const items = cart?.items || []
  const isEmpty = items.length === 0

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-text-meta">
        <Link to="/" className="hover:text-brand-primary transition-colors">Store</Link>
        <span>/</span>
        <span className="text-text-body">Shopping Cart</span>
      </nav>

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-black text-text-heading">
          Shopping Cart
          {!isEmpty && (
            <span className="ml-2 text-lg font-normal text-text-meta">
              ({items.length} {items.length === 1 ? 'item' : 'items'})
            </span>
          )}
        </h1>
        {!isEmpty && (
          <Button onClick={handleClearCart} variant="ghost" size="sm" className="text-status-error hover:text-status-error">
            Clear Cart
          </Button>
        )}
      </div>

      {isEmpty ? (
        /* Empty Cart */
        <div className="border-2 border-dashed border-border-default rounded-2xl p-16 text-center">
          <div className="w-20 h-20 mx-auto mb-6 text-text-placeholder">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-text-heading mb-2">Your cart is empty</h2>
          <p className="text-text-meta mb-8">Looks like you haven't added any products yet.</p>
          <Button asChild variant="primary" size="lg" className="rounded-full">
            <Link to="/">Start Shopping</Link>
          </Button>
        </div>
      ) : (
        /* Cart Content */
        <div className="flex flex-col lg:flex-row gap-8">
          {/* Cart Items */}
          <div className="flex-1 space-y-4">
            {items.map((item) => (
              <CartItemComponent key={item.id} item={item} />
            ))}
          </div>

          {/* Order Summary */}
          <div className="w-full lg:w-[380px] flex-shrink-0">
            <div className="bg-bg-card border border-border-default rounded-2xl p-6 shadow-2xl sticky top-6 space-y-5">
              <h2 className="text-lg font-bold text-text-heading">
                Order Summary
              </h2>

              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-text-meta">Subtotal ({cart?.totalQuantity} items)</span>
                  <span className="text-text-body font-medium">{formatPrice(cart?.totalAmount || 0)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-meta">Delivery</span>
                  <span className="text-accent-teal font-medium">Free</span>
                </div>
              </div>

              <div className="border-t border-border-default pt-4">
                <div className="flex justify-between items-center">
                  <span className="text-text-heading font-bold">Total</span>
                  <span className="text-2xl font-black text-text-heading">
                    {formatPrice(cart?.totalAmount || 0)}
                  </span>
                </div>
              </div>

              <Button
                onClick={handleCheckout}
                variant="primary"
                size="lg"
                className="w-full rounded-full shadow-lg shadow-brand-primary/20"
              >
                주문하기
              </Button>

              <Button asChild variant="ghost" className="w-full">
                <Link to="/">Continue Shopping</Link>
              </Button>

              <SecurityBadges />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default CartPage
