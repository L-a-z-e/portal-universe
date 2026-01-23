# 결제 플로우 구현

## 학습 목표
- 다단계 결제 프로세스 구현 이해
- 배송지 입력, 결제 수단 선택, 주문 완료 플로우 학습
- 쿠폰 적용 및 할인 계산 로직 분석

---

## 1. Checkout 플로우

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CHECKOUT FLOW                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Step 1: Address   ─────►  배송지 입력 + 쿠폰 선택                          │
│   Step 2: Payment   ─────►  결제 수단 선택                                   │
│   Step 3: Complete  ─────►  주문 완료                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe 코드 분석

### 2.1 CheckoutPage.tsx

```tsx
// pages/CheckoutPage.tsx
import React, { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCartStore } from '@/stores/cartStore'
import { orderApi, paymentApi } from '@/api/endpoints'
import type { AddressRequest, PaymentMethod, Order, UserCoupon } from '@/types'
import { CouponSelector } from '@/components/coupon/CouponSelector'
import { calculateDiscount } from '@/hooks/useCoupons'

type CheckoutStep = 'address' | 'payment' | 'complete'

const CheckoutPage: React.FC = () => {
  const navigate = useNavigate()
  const { cart, fetchCart, clearCart } = useCartStore()

  // ============================================
  // State
  // ============================================
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

  // ============================================
  // Fetch cart on mount
  // ============================================
  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  // ============================================
  // Redirect if cart is empty
  // ============================================
  useEffect(() => {
    if (cart && cart.items.length === 0 && step === 'address') {
      navigate('/cart')
    }
  }, [cart, step, navigate])

  // ============================================
  // Price calculation
  // ============================================
  const orderAmount = cart?.totalAmount || 0
  const discountAmount = selectedCoupon
    ? calculateDiscount(selectedCoupon.coupon, orderAmount)
    : 0
  const finalAmount = orderAmount - discountAmount

  // ============================================
  // Handle address form change
  // ============================================
  const handleAddressChange = (field: keyof AddressRequest, value: string) => {
    setAddress(prev => ({ ...prev, [field]: value }))
  }

  // ============================================
  // Validate address
  // ============================================
  const isAddressValid = () => {
    return (
      address.receiverName.trim() !== '' &&
      address.receiverPhone.trim() !== '' &&
      address.zipCode.trim() !== '' &&
      address.address1.trim() !== ''
    )
  }

  // ============================================
  // Handle order creation
  // ============================================
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
      setOrder(response.data)
      setStep('payment')
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Failed to create order')
    } finally {
      setLoading(false)
    }
  }

  // ============================================
  // Handle payment processing
  // ============================================
  const handleProcessPayment = async () => {
    if (!order) return

    setLoading(true)
    setError(null)

    try {
      await paymentApi.processPayment({
        orderNumber: order.orderNumber,
        method: paymentMethod
      })

      // Clear cart after successful payment
      await clearCart()
      setStep('complete')
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Payment failed')
    } finally {
      setLoading(false)
    }
  }

  // ============================================
  // Render
  // ============================================
  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-text-heading mb-8">Checkout</h1>

      {/* Step Indicator */}
      <StepIndicator currentStep={step} />

      {/* Error Message */}
      {error && (
        <Alert variant="error" className="mb-6">
          {error}
        </Alert>
      )}

      {/* Content */}
      <div className="bg-bg-card border border-border-default rounded-lg p-6">
        {step === 'address' && (
          <AddressForm
            address={address}
            onChange={handleAddressChange}
            selectedCoupon={selectedCoupon}
            onSelectCoupon={setSelectedCoupon}
            orderAmount={orderAmount}
            discountAmount={discountAmount}
            finalAmount={finalAmount}
            onSubmit={handleCreateOrder}
            loading={loading}
            isValid={isAddressValid()}
          />
        )}

        {step === 'payment' && order && (
          <PaymentForm
            order={order}
            paymentMethod={paymentMethod}
            onPaymentMethodChange={setPaymentMethod}
            onSubmit={handleProcessPayment}
            onBack={() => setStep('address')}
            loading={loading}
          />
        )}

        {step === 'complete' && order && (
          <OrderComplete order={order} />
        )}
      </div>
    </div>
  )
}

export default CheckoutPage
```

---

## 3. Step Indicator 컴포넌트

### 3.1 단계 표시

```tsx
interface Props {
  currentStep: 'address' | 'payment' | 'complete'
}

function StepIndicator({ currentStep }: Props) {
  const steps = [
    { key: 'address', label: 'Shipping' },
    { key: 'payment', label: 'Payment' },
    { key: 'complete', label: 'Complete' }
  ]

  const currentIndex = steps.findIndex(s => s.key === currentStep)

  return (
    <div className="flex items-center justify-center mb-8">
      {steps.map((step, index) => (
        <React.Fragment key={step.key}>
          {/* Step Circle */}
          <div className="flex items-center">
            <div
              className={`
                w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium
                ${index <= currentIndex
                  ? 'bg-brand-primary text-white'
                  : 'bg-bg-subtle text-text-meta'
                }
              `}
            >
              {index < currentIndex ? '✓' : index + 1}
            </div>
            <span
              className={`
                ml-2 text-sm font-medium
                ${index <= currentIndex ? 'text-text-heading' : 'text-text-meta'}
              `}
            >
              {step.label}
            </span>
          </div>

          {/* Connector Line */}
          {index < steps.length - 1 && (
            <div
              className={`
                w-16 h-0.5 mx-4
                ${index < currentIndex ? 'bg-brand-primary' : 'bg-border-default'}
              `}
            />
          )}
        </React.Fragment>
      ))}
    </div>
  )
}
```

---

## 4. Address Form 컴포넌트

### 4.1 배송지 입력 폼

```tsx
interface AddressFormProps {
  address: AddressRequest
  onChange: (field: keyof AddressRequest, value: string) => void
  selectedCoupon: UserCoupon | null
  onSelectCoupon: (coupon: UserCoupon | null) => void
  orderAmount: number
  discountAmount: number
  finalAmount: number
  onSubmit: () => void
  loading: boolean
  isValid: boolean
}

function AddressForm({
  address,
  onChange,
  selectedCoupon,
  onSelectCoupon,
  orderAmount,
  discountAmount,
  finalAmount,
  onSubmit,
  loading,
  isValid
}: AddressFormProps) {
  return (
    <div className="space-y-6">
      <h2 className="text-lg font-bold text-text-heading">Shipping Address</h2>

      {/* Address Fields */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Input
          label="Receiver Name *"
          value={address.receiverName}
          onChange={(e) => onChange('receiverName', e.target.value)}
          placeholder="Enter receiver name"
        />

        <Input
          label="Phone Number *"
          type="tel"
          value={address.receiverPhone}
          onChange={(e) => onChange('receiverPhone', e.target.value)}
          placeholder="010-0000-0000"
        />

        <Input
          label="Zip Code *"
          value={address.zipCode}
          onChange={(e) => onChange('zipCode', e.target.value)}
          placeholder="12345"
        />

        <div className="md:col-span-2">
          <Input
            label="Address *"
            value={address.address1}
            onChange={(e) => onChange('address1', e.target.value)}
            placeholder="Street address"
          />
        </div>

        <div className="md:col-span-2">
          <Input
            label="Detail Address"
            value={address.address2}
            onChange={(e) => onChange('address2', e.target.value)}
            placeholder="Apartment, suite, etc. (optional)"
          />
        </div>
      </div>

      {/* Coupon Selection */}
      <CouponSelector
        orderAmount={orderAmount}
        selectedCoupon={selectedCoupon}
        onSelectCoupon={onSelectCoupon}
      />

      {/* Order Summary */}
      <OrderSummary
        orderAmount={orderAmount}
        discountAmount={discountAmount}
        finalAmount={finalAmount}
      />

      {/* Actions */}
      <div className="flex justify-between pt-4">
        <Button asChild variant="ghost">
          <Link to="/cart">Back to Cart</Link>
        </Button>
        <Button
          onClick={onSubmit}
          disabled={!isValid || loading}
          variant="primary"
          size="lg"
        >
          {loading ? 'Creating Order...' : 'Continue to Payment'}
        </Button>
      </div>
    </div>
  )
}
```

---

## 5. Payment Form 컴포넌트

### 5.1 결제 수단 선택

```tsx
interface PaymentFormProps {
  order: Order
  paymentMethod: PaymentMethod
  onPaymentMethodChange: (method: PaymentMethod) => void
  onSubmit: () => void
  onBack: () => void
  loading: boolean
}

function PaymentForm({
  order,
  paymentMethod,
  onPaymentMethodChange,
  onSubmit,
  onBack,
  loading
}: PaymentFormProps) {
  const paymentMethods = [
    { value: 'CREDIT_CARD', label: '신용카드' },
    { value: 'BANK_TRANSFER', label: '계좌이체' },
    { value: 'VIRTUAL_ACCOUNT', label: '가상계좌' },
    { value: 'MOBILE', label: '휴대폰 결제' }
  ]

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-bold text-text-heading">Payment Method</h2>

      <div className="space-y-3">
        {paymentMethods.map(({ value, label }) => (
          <label
            key={value}
            className={`
              flex items-center p-4 border rounded-lg cursor-pointer transition-colors
              ${paymentMethod === value
                ? 'border-brand-primary bg-brand-primary/5'
                : 'border-border-default hover:border-brand-primary/50'
              }
            `}
          >
            <input
              type="radio"
              name="paymentMethod"
              value={value}
              checked={paymentMethod === value}
              onChange={() => onPaymentMethodChange(value as PaymentMethod)}
              className="w-4 h-4 text-brand-primary"
            />
            <span className="ml-3 font-medium text-text-heading">{label}</span>
          </label>
        ))}
      </div>

      {/* Order Summary */}
      <div className="bg-bg-subtle rounded-lg p-4">
        <h3 className="font-medium text-text-heading mb-3">Order Summary</h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-text-meta">Order Number</span>
            <span className="text-text-body font-mono">{order.orderNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-text-meta">Total</span>
            <span className="font-bold text-brand-primary">
              {formatPrice(order.finalAmount || order.totalAmount)}
            </span>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex justify-between pt-4">
        <Button onClick={onBack} variant="ghost">
          Back
        </Button>
        <Button
          onClick={onSubmit}
          disabled={loading}
          variant="primary"
          size="lg"
        >
          {loading ? 'Processing...' : `Pay ${formatPrice(order.finalAmount || order.totalAmount)}`}
        </Button>
      </div>
    </div>
  )
}
```

---

## 6. Order Complete 컴포넌트

### 6.1 주문 완료 화면

```tsx
interface OrderCompleteProps {
  order: Order
}

function OrderComplete({ order }: OrderCompleteProps) {
  return (
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
      <p className="text-text-meta mb-6">
        Order Number: <span className="font-mono font-medium text-text-body">{order.orderNumber}</span>
      </p>

      <div className="flex items-center justify-center gap-4">
        <Button asChild variant="primary">
          <Link to={`/orders/${order.orderNumber}`}>
            View Order
          </Link>
        </Button>
        <Button asChild variant="secondary">
          <Link to="/">Continue Shopping</Link>
        </Button>
      </div>
    </div>
  )
}
```

---

## 7. 쿠폰 할인 계산

### 7.1 calculateDiscount 함수

**Portal Universe 코드 (hooks/useCoupons.ts)**
```tsx
export function calculateDiscount(coupon: Coupon, orderAmount: number): number {
  // 최소 주문 금액 검증
  if (coupon.minimumOrderAmount && orderAmount < coupon.minimumOrderAmount) {
    return 0
  }

  let discount: number
  if (coupon.discountType === 'FIXED') {
    // 정액 할인
    discount = coupon.discountValue
  } else {
    // 비율 할인 (PERCENTAGE)
    discount = Math.round(orderAmount * coupon.discountValue / 100)
  }

  // 최대 할인 금액 제한
  if (coupon.maximumDiscountAmount && discount > coupon.maximumDiscountAmount) {
    discount = coupon.maximumDiscountAmount
  }

  // 할인 금액이 주문 금액을 초과하지 않도록
  if (discount > orderAmount) {
    discount = orderAmount
  }

  return discount
}
```

---

## 8. 핵심 정리

| 단계 | 기능 |
|------|------|
| **Address** | 배송지 입력, 쿠폰 선택, 유효성 검증 |
| **Payment** | 결제 수단 선택, 주문 요약 확인 |
| **Complete** | 주문 완료, 주문 번호 표시 |
| **쿠폰** | `calculateDiscount()` 함수 |
| **상태** | useState (local state) |
| **네비게이션** | 단계별 이동, 장바구니 비우기 |

---

## 다음 학습

- [Coupon UI](./coupon-ui.md)
- [Cart Implementation](./cart-implementation.md)
- [Form Handling](../react/form-handling.md)
