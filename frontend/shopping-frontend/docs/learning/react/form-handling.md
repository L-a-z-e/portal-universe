# Form Handling

## 학습 목표
- Controlled Components 패턴 이해
- Form 상태 관리 및 검증 기법
- Shopping Frontend의 폼 처리 패턴 분석

---

## 1. Controlled vs Uncontrolled Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CONTROLLED COMPONENTS                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   React 상태로 관리  ─────►  value + onChange                                │
│   즉각적인 검증     ─────►  입력 즉시 유효성 검사                             │
│   조건부 활성화     ─────►  버튼 활성/비활성 제어                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Controlled Component

```tsx
function ControlledInput() {
  const [value, setValue] = useState('')

  return (
    <input
      value={value} // React 상태와 동기화
      onChange={(e) => setValue(e.target.value)}
    />
  )
}
```

### 1.2 Uncontrolled Component

```tsx
import { useRef } from 'react'

function UncontrolledInput() {
  const inputRef = useRef<HTMLInputElement>(null)

  const handleSubmit = () => {
    console.log(inputRef.current?.value) // DOM에서 직접 읽기
  }

  return (
    <>
      <input ref={inputRef} defaultValue="Initial" />
      <button onClick={handleSubmit}>Submit</button>
    </>
  )
}
```

---

## 2. 기본 Form 패턴

### 2.1 단일 Input 관리

```tsx
function SearchBar() {
  const [keyword, setKeyword] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    console.log('Search for:', keyword)
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="Search products..."
      />
      <button type="submit">Search</button>
    </form>
  )
}
```

### 2.2 여러 Input 관리

```tsx
interface FormData {
  username: string
  email: string
  password: string
}

function SignupForm() {
  const [formData, setFormData] = useState<FormData>({
    username: '',
    email: '',
    password: ''
  })

  const handleChange = (field: keyof FormData) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [field]: e.target.value
    }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    console.log('Submit:', formData)
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={formData.username}
        onChange={handleChange('username')}
      />
      <input
        type="email"
        value={formData.email}
        onChange={handleChange('email')}
      />
      <input
        type="password"
        value={formData.password}
        onChange={handleChange('password')}
      />
      <button type="submit">Sign Up</button>
    </form>
  )
}
```

---

## 3. Portal Universe 코드 분석

### 3.1 CheckoutPage - 배송 주소 폼

```tsx
// pages/CheckoutPage.tsx
import { useState } from 'react'
import type { AddressRequest } from '@/types'

function CheckoutPage() {
  // 주소 폼 상태
  const [address, setAddress] = useState<AddressRequest>({
    receiverName: '',
    receiverPhone: '',
    zipCode: '',
    address1: '',
    address2: ''
  })

  // 필드 변경 핸들러
  const handleAddressChange = (field: keyof AddressRequest, value: string) => {
    setAddress(prev => ({ ...prev, [field]: value }))
  }

  // 유효성 검증
  const isAddressValid = () => {
    return (
      address.receiverName.trim() !== '' &&
      address.receiverPhone.trim() !== '' &&
      address.zipCode.trim() !== '' &&
      address.address1.trim() !== ''
    )
  }

  const handleCreateOrder = async () => {
    if (!isAddressValid()) {
      setError('Please fill in all required address fields')
      return
    }

    try {
      const response = await orderApi.createOrder({
        shippingAddress: address,
        userCouponId: selectedCoupon?.id
      })
      setOrder(response.data)
      setStep('payment')
    } catch (err: any) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h2>Shipping Address</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* 수신자 이름 */}
        <div>
          <label className="block text-sm font-medium mb-1">
            Receiver Name *
          </label>
          <input
            type="text"
            value={address.receiverName}
            onChange={(e) => handleAddressChange('receiverName', e.target.value)}
            placeholder="Enter receiver name"
            className="w-full px-4 py-3 border rounded-lg"
          />
        </div>

        {/* 전화번호 */}
        <div>
          <label className="block text-sm font-medium mb-1">
            Phone Number *
          </label>
          <input
            type="tel"
            value={address.receiverPhone}
            onChange={(e) => handleAddressChange('receiverPhone', e.target.value)}
            placeholder="010-0000-0000"
            className="w-full px-4 py-3 border rounded-lg"
          />
        </div>

        {/* 우편번호 */}
        <div>
          <label className="block text-sm font-medium mb-1">
            Zip Code *
          </label>
          <input
            type="text"
            value={address.zipCode}
            onChange={(e) => handleAddressChange('zipCode', e.target.value)}
            placeholder="12345"
            className="w-full px-4 py-3 border rounded-lg"
          />
        </div>

        {/* 주소 */}
        <div className="md:col-span-2">
          <label className="block text-sm font-medium mb-1">
            Address *
          </label>
          <input
            type="text"
            value={address.address1}
            onChange={(e) => handleAddressChange('address1', e.target.value)}
            placeholder="Street address"
            className="w-full px-4 py-3 border rounded-lg"
          />
        </div>

        {/* 상세 주소 (선택) */}
        <div className="md:col-span-2">
          <label className="block text-sm font-medium mb-1">
            Detail Address
          </label>
          <input
            type="text"
            value={address.address2}
            onChange={(e) => handleAddressChange('address2', e.target.value)}
            placeholder="Apartment, suite, etc. (optional)"
            className="w-full px-4 py-3 border rounded-lg"
          />
        </div>
      </div>

      <button
        onClick={handleCreateOrder}
        disabled={!isAddressValid()}
        className="btn-primary"
      >
        Continue to Payment
      </button>
    </div>
  )
}
```

### 3.2 ProductListPage - 검색 폼

```tsx
// pages/ProductListPage.tsx
function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const searchKeyword = searchParams.get('keyword') || ''
  const [searchInput, setSearchInput] = useState(searchKeyword)

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const params = new URLSearchParams()
    if (searchInput) {
      params.set('keyword', searchInput)
    }
    params.set('page', '0')
    setSearchParams(params)
  }

  const clearSearch = () => {
    setSearchInput('')
    setSearchParams({})
  }

  return (
    <form onSubmit={handleSearch} className="flex items-center gap-2">
      <Input
        type="text"
        value={searchInput}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchInput(e.target.value)}
        placeholder="Search products..."
      />
      <Button type="submit" variant="primary">
        Search
      </Button>
      {searchKeyword && (
        <Button type="button" onClick={clearSearch} variant="secondary">
          Clear
        </Button>
      )}
    </form>
  )
}
```

---

## 4. Form 유효성 검증

### 4.1 실시간 검증

```tsx
interface Errors {
  username?: string
  email?: string
  password?: string
}

function SignupForm() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: ''
  })
  const [errors, setErrors] = useState<Errors>({})
  const [touched, setTouched] = useState<Record<string, boolean>>({})

  const validate = (field: string, value: string): string | undefined => {
    switch (field) {
      case 'username':
        if (value.length < 3) return 'Username must be at least 3 characters'
        break
      case 'email':
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Invalid email format'
        break
      case 'password':
        if (value.length < 8) return 'Password must be at least 8 characters'
        break
    }
  }

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setFormData(prev => ({ ...prev, [field]: value }))

    // 터치된 필드만 검증
    if (touched[field]) {
      const error = validate(field, value)
      setErrors(prev => ({ ...prev, [field]: error }))
    }
  }

  const handleBlur = (field: string) => () => {
    setTouched(prev => ({ ...prev, [field]: true }))
    const error = validate(field, formData[field as keyof typeof formData])
    setErrors(prev => ({ ...prev, [field]: error }))
  }

  const isValid = () => {
    return Object.keys(formData).every(field =>
      !validate(field, formData[field as keyof typeof formData])
    )
  }

  return (
    <form>
      <div>
        <input
          value={formData.username}
          onChange={handleChange('username')}
          onBlur={handleBlur('username')}
        />
        {touched.username && errors.username && (
          <span className="text-red-500 text-sm">{errors.username}</span>
        )}
      </div>

      <button type="submit" disabled={!isValid()}>
        Sign Up
      </button>
    </form>
  )
}
```

### 4.2 제출 시 검증

```tsx
function CheckoutForm() {
  const [formData, setFormData] = useState({ /* ... */ })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const validateAll = () => {
    const newErrors: Record<string, string> = {}

    if (!formData.receiverName.trim()) {
      newErrors.receiverName = 'Receiver name is required'
    }

    if (!formData.receiverPhone.trim()) {
      newErrors.receiverPhone = 'Phone number is required'
    } else if (!/^\d{3}-\d{4}-\d{4}$/.test(formData.receiverPhone)) {
      newErrors.receiverPhone = 'Invalid phone format (010-0000-0000)'
    }

    if (!formData.zipCode.trim()) {
      newErrors.zipCode = 'Zip code is required'
    }

    return newErrors
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    const validationErrors = validateAll()
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }

    try {
      await submitOrder(formData)
    } catch (error) {
      console.error('Submit failed', error)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* 폼 필드들 */}
      {errors.receiverName && <span className="error">{errors.receiverName}</span>}
      <button type="submit">Submit</button>
    </form>
  )
}
```

---

## 5. 커스텀 Form Hook

### 5.1 useForm Hook

```tsx
interface UseFormOptions<T> {
  initialValues: T
  validate?: (values: T) => Partial<Record<keyof T, string>>
  onSubmit: (values: T) => void | Promise<void>
}

function useForm<T extends Record<string, any>>({
  initialValues,
  validate,
  onSubmit
}: UseFormOptions<T>) {
  const [values, setValues] = useState<T>(initialValues)
  const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({})
  const [touched, setTouched] = useState<Partial<Record<keyof T, boolean>>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (field: keyof T) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setValues(prev => ({ ...prev, [field]: e.target.value }))

    if (touched[field] && validate) {
      const validationErrors = validate({ ...values, [field]: e.target.value })
      setErrors(prev => ({ ...prev, [field]: validationErrors[field] }))
    }
  }

  const handleBlur = (field: keyof T) => () => {
    setTouched(prev => ({ ...prev, [field]: true }))
    if (validate) {
      const validationErrors = validate(values)
      setErrors(prev => ({ ...prev, [field]: validationErrors[field] }))
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    // 모든 필드 터치 표시
    const allTouched = Object.keys(values).reduce((acc, key) => ({
      ...acc,
      [key]: true
    }), {})
    setTouched(allTouched)

    // 검증
    if (validate) {
      const validationErrors = validate(values)
      setErrors(validationErrors)

      if (Object.keys(validationErrors).length > 0) {
        return
      }
    }

    // 제출
    setIsSubmitting(true)
    try {
      await onSubmit(values)
    } finally {
      setIsSubmitting(false)
    }
  }

  const reset = () => {
    setValues(initialValues)
    setErrors({})
    setTouched({})
  }

  return {
    values,
    errors,
    touched,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit,
    reset
  }
}
```

### 5.2 사용 예시

```tsx
function SignupForm() {
  const form = useForm({
    initialValues: {
      username: '',
      email: '',
      password: ''
    },
    validate: (values) => {
      const errors: any = {}

      if (values.username.length < 3) {
        errors.username = 'Username must be at least 3 characters'
      }

      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) {
        errors.email = 'Invalid email format'
      }

      if (values.password.length < 8) {
        errors.password = 'Password must be at least 8 characters'
      }

      return errors
    },
    onSubmit: async (values) => {
      await signupApi(values)
      alert('Signup successful!')
    }
  })

  return (
    <form onSubmit={form.handleSubmit}>
      <div>
        <input
          value={form.values.username}
          onChange={form.handleChange('username')}
          onBlur={form.handleBlur('username')}
        />
        {form.touched.username && form.errors.username && (
          <span className="text-red-500">{form.errors.username}</span>
        )}
      </div>

      <button type="submit" disabled={form.isSubmitting}>
        {form.isSubmitting ? 'Submitting...' : 'Sign Up'}
      </button>
    </form>
  )
}
```

---

## 6. Design System 통합

### 6.1 Reusable Input Component

```tsx
// components/form/Input.tsx
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
}

export function Input({ label, error, helperText, ...props }: InputProps) {
  return (
    <div>
      {label && (
        <label className="block text-sm font-medium text-text-heading mb-1">
          {label}
        </label>
      )}

      <input
        {...props}
        className={`
          w-full px-4 py-3 border rounded-lg
          bg-bg-input text-text-body
          placeholder:text-text-placeholder
          focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary
          transition-colors
          ${error ? 'border-status-error' : 'border-border-default'}
          ${props.className || ''}
        `}
      />

      {error && (
        <p className="mt-1 text-sm text-status-error">{error}</p>
      )}

      {helperText && !error && (
        <p className="mt-1 text-sm text-text-meta">{helperText}</p>
      )}
    </div>
  )
}
```

### 6.2 사용 예시

```tsx
function AddressForm() {
  const form = useForm({ /* ... */ })

  return (
    <form onSubmit={form.handleSubmit}>
      <Input
        label="Receiver Name *"
        placeholder="Enter receiver name"
        value={form.values.receiverName}
        onChange={form.handleChange('receiverName')}
        onBlur={form.handleBlur('receiverName')}
        error={form.touched.receiverName ? form.errors.receiverName : undefined}
      />
    </form>
  )
}
```

---

## 7. 핵심 정리

| 패턴 | 설명 |
|------|------|
| **Controlled** | React 상태로 value 관리 |
| **onChange** | 입력값 즉시 상태 업데이트 |
| **Validation** | 실시간 또는 제출 시 검증 |
| **touched** | 필드 방문 여부 추적 |
| **Custom Hook** | 폼 로직 재사용 |
| **Design System** | 일관된 UI 컴포넌트 사용 |

---

## 다음 학습

- [API Integration](./api-integration.md)
- [Custom Hooks](./custom-hooks.md)
- [Checkout Flow](../shopping/checkout-flow.md)
