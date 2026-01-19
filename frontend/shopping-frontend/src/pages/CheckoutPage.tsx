/**
 * Checkout Page
 *
 * 주문 생성 및 결제 처리 페이지
 */
import React, { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCartStore } from '@/stores/cartStore'
import { orderApi, paymentApi } from '@/api/endpoints'
import type { AddressRequest, PaymentMethod, Order, UserCoupon } from '@/types'
import { PAYMENT_METHOD_LABELS } from '@/types'
import { CouponSelector } from '@/components/coupon/CouponSelector'
import { calculateDiscount } from '@/hooks/useCoupons'

type CheckoutStep = 'address' | 'payment' | 'confirm' | 'complete'

const CheckoutPage: React.FC = () => {
  const navigate = useNavigate()
  const { cart, fetchCart, clearCart } = useCartStore()

  // State
  const [step, setStep] = useState<CheckoutStep>('address')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [order, setOrder] = useState<Order | null>(null)

  // Form state
  const [address, setAddress] = useState<AddressRequest>({
    receiverName: '',
    receiverPhone: '',
    zipCode: '',
    address1: '',
    address2: ''
  })

  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CREDIT_CARD')
  const [selectedCoupon, setSelectedCoupon] = useState<UserCoupon | null>(null)

  // Fetch cart on mount
  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  // Redirect if cart is empty
  useEffect(() => {
    if (cart && cart.items.length === 0 && step === 'address') {
      navigate('/cart')
    }
  }, [cart, step, navigate])

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  // Calculate discount and final amount
  const orderAmount = cart?.totalAmount || 0
  const discountAmount = selectedCoupon
    ? calculateDiscount(selectedCoupon.coupon, orderAmount)
    : 0
  const finalAmount = orderAmount - discountAmount

  // Handle address form change
  const handleAddressChange = (field: keyof AddressRequest, value: string) => {
    setAddress(prev => ({ ...prev, [field]: value }))
  }

  // Validate address
  const isAddressValid = () => {
    return (
      address.receiverName.trim() !== '' &&
      address.receiverPhone.trim() !== '' &&
      address.zipCode.trim() !== '' &&
      address.address1.trim() !== ''
    )
  }

  // Handle order creation
  const handleCreateOrder = async () => {
    if (!isAddressValid()) {
      setError('Please fill in all required address fields')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const response = await orderApi.createOrder({
        shippingAddress: address,
        userCouponId: selectedCoupon?.id
      })

      if (response.success) {
        setOrder(response.data)
        setStep('payment')
      } else {
        setError(response.message || 'Failed to create order')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to create order')
    } finally {
      setLoading(false)
    }
  }

  // Handle payment processing
  const handleProcessPayment = async () => {
    if (!order) return

    setLoading(true)
    setError(null)

    try {
      const response = await paymentApi.processPayment({
        orderNumber: order.orderNumber,
        method: paymentMethod
      })

      if (response.success) {
        // Clear cart after successful payment
        await clearCart()
        setStep('complete')
      } else {
        setError(response.message || 'Payment failed')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Payment failed')
    } finally {
      setLoading(false)
    }
  }

  // Render step indicator
  const renderStepIndicator = () => {
    const steps = [
      { key: 'address', label: 'Shipping' },
      { key: 'payment', label: 'Payment' },
      { key: 'complete', label: 'Complete' }
    ]

    const currentIndex = steps.findIndex(s => s.key === step || (step === 'confirm' && s.key === 'payment'))

    return (
      <div className="flex items-center justify-center mb-8">
        {steps.map((s, index) => (
          <React.Fragment key={s.key}>
            <div className="flex items-center">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  index <= currentIndex
                    ? 'bg-brand-primary text-white'
                    : 'bg-bg-subtle text-text-meta'
                }`}
              >
                {index < currentIndex ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                ) : (
                  index + 1
                )}
              </div>
              <span
                className={`ml-2 text-sm font-medium ${
                  index <= currentIndex ? 'text-text-heading' : 'text-text-meta'
                }`}
              >
                {s.label}
              </span>
            </div>
            {index < steps.length - 1 && (
              <div
                className={`w-16 h-0.5 mx-4 ${
                  index < currentIndex ? 'bg-brand-primary' : 'bg-border-default'
                }`}
              />
            )}
          </React.Fragment>
        ))}
      </div>
    )
  }

  // Render address form
  const renderAddressForm = () => (
    <div className="space-y-6">
      <h2 className="text-lg font-bold text-text-heading">Shipping Address</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-text-heading mb-1">
            Receiver Name *
          </label>
          <input
            type="text"
            value={address.receiverName}
            onChange={(e) => handleAddressChange('receiverName', e.target.value)}
            placeholder="Enter receiver name"
            className="w-full px-4 py-3 border border-border-default rounded-lg bg-bg-input text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-text-heading mb-1">
            Phone Number *
          </label>
          <input
            type="tel"
            value={address.receiverPhone}
            onChange={(e) => handleAddressChange('receiverPhone', e.target.value)}
            placeholder="010-0000-0000"
            className="w-full px-4 py-3 border border-border-default rounded-lg bg-bg-input text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-text-heading mb-1">
            Zip Code *
          </label>
          <input
            type="text"
            value={address.zipCode}
            onChange={(e) => handleAddressChange('zipCode', e.target.value)}
            placeholder="12345"
            className="w-full px-4 py-3 border border-border-default rounded-lg bg-bg-input text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
          />
        </div>

        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-text-heading mb-1">
            Address *
          </label>
          <input
            type="text"
            value={address.address1}
            onChange={(e) => handleAddressChange('address1', e.target.value)}
            placeholder="Street address"
            className="w-full px-4 py-3 border border-border-default rounded-lg bg-bg-input text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
          />
        </div>

        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-text-heading mb-1">
            Detail Address
          </label>
          <input
            type="text"
            value={address.address2}
            onChange={(e) => handleAddressChange('address2', e.target.value)}
            placeholder="Apartment, suite, etc. (optional)"
            className="w-full px-4 py-3 border border-border-default rounded-lg bg-bg-input text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
          />
        </div>
      </div>

      {/* Coupon Selection */}
      <div className="mt-6">
        <CouponSelector
          orderAmount={orderAmount}
          selectedCoupon={selectedCoupon}
          onSelectCoupon={setSelectedCoupon}
        />
      </div>

      {/* Order Summary */}
      {cart && (
        <div className="mt-6 bg-bg-subtle rounded-lg p-4">
          <h3 className="font-medium text-text-heading mb-3">Order Summary</h3>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-text-meta">Subtotal ({cart.itemCount} items)</span>
              <span className="text-text-body">{formatPrice(orderAmount)}</span>
            </div>
            {discountAmount > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Coupon Discount</span>
                <span>-{formatPrice(discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between pt-2 border-t border-border-default">
              <span className="font-medium text-text-heading">Total</span>
              <span className="font-bold text-brand-primary">{formatPrice(finalAmount)}</span>
            </div>
          </div>
        </div>
      )}

      <div className="flex justify-between pt-4">
        <Link
          to="/cart"
          className="px-6 py-3 text-text-body hover:text-text-heading transition-colors"
        >
          Back to Cart
        </Link>
        <button
          onClick={handleCreateOrder}
          disabled={!isAddressValid() || loading}
          className="px-8 py-3 bg-brand-primary text-white rounded-lg font-medium hover:bg-brand-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? 'Creating Order...' : 'Continue to Payment'}
        </button>
      </div>
    </div>
  )

  // Render payment form
  const renderPaymentForm = () => (
    <div className="space-y-6">
      <h2 className="text-lg font-bold text-text-heading">Payment Method</h2>

      <div className="space-y-3">
        {(Object.entries(PAYMENT_METHOD_LABELS) as [PaymentMethod, string][]).map(([method, label]) => (
          <label
            key={method}
            className={`flex items-center p-4 border rounded-lg cursor-pointer transition-colors ${
              paymentMethod === method
                ? 'border-brand-primary bg-brand-primary/5'
                : 'border-border-default hover:border-brand-primary/50'
            }`}
          >
            <input
              type="radio"
              name="paymentMethod"
              value={method}
              checked={paymentMethod === method}
              onChange={() => setPaymentMethod(method)}
              className="w-4 h-4 text-brand-primary"
            />
            <span className="ml-3 font-medium text-text-heading">{label}</span>
          </label>
        ))}
      </div>

      {/* Order Summary */}
      {order && (
        <div className="bg-bg-subtle rounded-lg p-4 mt-6">
          <h3 className="font-medium text-text-heading mb-3">Order Summary</h3>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-text-meta">Order Number</span>
              <span className="text-text-body font-mono">{order.orderNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-text-meta">Items</span>
              <span className="text-text-body">{order.items.length} items</span>
            </div>
            <div className="flex justify-between">
              <span className="text-text-meta">Subtotal</span>
              <span className="text-text-body">{formatPrice(order.totalAmount)}</span>
            </div>
            {order.discountAmount && order.discountAmount > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Coupon Discount</span>
                <span>-{formatPrice(order.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between pt-2 border-t border-border-default">
              <span className="font-medium text-text-heading">Total</span>
              <span className="font-bold text-brand-primary">
                {formatPrice(order.finalAmount || order.totalAmount)}
              </span>
            </div>
          </div>
        </div>
      )}

      <div className="flex justify-between pt-4">
        <button
          onClick={() => setStep('address')}
          className="px-6 py-3 text-text-body hover:text-text-heading transition-colors"
        >
          Back
        </button>
        <button
          onClick={handleProcessPayment}
          disabled={loading}
          className="px-8 py-3 bg-brand-primary text-white rounded-lg font-medium hover:bg-brand-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? 'Processing...' : `Pay ${formatPrice(order?.finalAmount || order?.totalAmount || 0)}`}
        </button>
      </div>
    </div>
  )

  // Render completion
  const renderComplete = () => (
    <div className="text-center py-8">
      <div className="w-16 h-16 mx-auto mb-6 bg-status-success-bg rounded-full flex items-center justify-center">
        <svg className="w-8 h-8 text-status-success" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
      </div>

      <h2 className="text-2xl font-bold text-text-heading mb-2">
        Order Placed Successfully!
      </h2>
      <p className="text-text-meta mb-2">
        Thank you for your purchase.
      </p>
      {order && (
        <p className="text-text-meta mb-6">
          Order Number: <span className="font-mono font-medium text-text-body">{order.orderNumber}</span>
        </p>
      )}

      <div className="flex items-center justify-center gap-4">
        <Link
          to={order ? `/orders/${order.orderNumber}` : '/orders'}
          className="px-6 py-3 bg-brand-primary text-white rounded-lg font-medium hover:bg-brand-primary/90 transition-colors"
        >
          View Order
        </Link>
        <Link
          to="/"
          className="px-6 py-3 bg-bg-subtle text-text-body rounded-lg font-medium hover:bg-bg-hover transition-colors"
        >
          Continue Shopping
        </Link>
      </div>
    </div>
  )

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-text-heading mb-8">Checkout</h1>

      {renderStepIndicator()}

      {/* Error Message */}
      {error && (
        <div className="mb-6 bg-status-error-bg border border-status-error/20 rounded-lg p-4">
          <p className="text-status-error text-sm">{error}</p>
        </div>
      )}

      {/* Content */}
      <div className="bg-bg-card border border-border-default rounded-lg p-6">
        {step === 'address' && renderAddressForm()}
        {step === 'payment' && renderPaymentForm()}
        {step === 'complete' && renderComplete()}
      </div>
    </div>
  )
}

export default CheckoutPage
