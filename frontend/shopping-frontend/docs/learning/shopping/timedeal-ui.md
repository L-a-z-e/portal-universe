# 타임딜 UI 구현

## 학습 목표
- 타임딜 카운트다운 타이머 구현
- 실시간 상태 업데이트 (진행 중/종료/예정)
- 타임딜 목록 및 상세 페이지 구현

---

## 1. 타임딜 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          TIME DEAL                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Countdown      ─────►  실시간 타이머 (HH:MM:SS)                            │
│   Status Badge   ─────►  진행 중, 종료, 예정                                 │
│   Progress Bar   ─────►  판매 진행률                                          │
│   Stock          ─────►  남은 재고 표시                                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. CountdownTimer 컴포넌트

### 2.1 Portal Universe 코드 분석

**components/timedeal/CountdownTimer.tsx**
```tsx
import React, { useState, useEffect } from 'react'

interface Props {
  endTime: string // ISO 8601 format
  onExpire?: () => void
}

export const CountdownTimer: React.FC<Props> = ({ endTime, onExpire }) => {
  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft())

  function calculateTimeLeft() {
    const difference = new Date(endTime).getTime() - new Date().getTime()

    if (difference <= 0) {
      return { hours: 0, minutes: 0, seconds: 0, expired: true }
    }

    const hours = Math.floor(difference / (1000 * 60 * 60))
    const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((difference % (1000 * 60)) / 1000)

    return { hours, minutes, seconds, expired: false }
  }

  useEffect(() => {
    const timer = setInterval(() => {
      const newTimeLeft = calculateTimeLeft()
      setTimeLeft(newTimeLeft)

      if (newTimeLeft.expired) {
        clearInterval(timer)
        onExpire?.()
      }
    }, 1000)

    return () => clearInterval(timer)
  }, [endTime, onExpire])

  if (timeLeft.expired) {
    return (
      <div className="text-status-error font-bold">
        종료됨
      </div>
    )
  }

  return (
    <div className="flex items-center gap-1 text-brand-primary font-bold">
      <span className="text-2xl">
        {String(timeLeft.hours).padStart(2, '0')}
      </span>
      <span>:</span>
      <span className="text-2xl">
        {String(timeLeft.minutes).padStart(2, '0')}
      </span>
      <span>:</span>
      <span className="text-2xl">
        {String(timeLeft.seconds).padStart(2, '0')}
      </span>
    </div>
  )
}
```

---

## 3. TimeDealCard 컴포넌트

### 3.1 Portal Universe 코드 분석

**components/timedeal/TimeDealCard.tsx**
```tsx
import React from 'react'
import { Link } from 'react-router-dom'
import { CountdownTimer } from './CountdownTimer'
import type { TimeDeal } from '@/types'

interface Props {
  timeDeal: TimeDeal
}

export const TimeDealCard: React.FC<Props> = ({ timeDeal }) => {
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const getDiscountRate = () => {
    if (!timeDeal.originalPrice) return 0
    return Math.round(
      ((timeDeal.originalPrice - timeDeal.dealPrice) / timeDeal.originalPrice) * 100
    )
  }

  const getSoldPercentage = () => {
    return (timeDeal.soldCount / timeDeal.totalStock) * 100
  }

  const getStatusBadge = () => {
    const now = new Date()
    const startTime = new Date(timeDeal.startTime)
    const endTime = new Date(timeDeal.endTime)

    if (now < startTime) {
      return (
        <span className="px-3 py-1 bg-status-info-bg text-status-info text-xs font-medium rounded-full">
          예정
        </span>
      )
    }

    if (now > endTime) {
      return (
        <span className="px-3 py-1 bg-status-error-bg text-status-error text-xs font-medium rounded-full">
          종료
        </span>
      )
    }

    return (
      <span className="px-3 py-1 bg-status-success-bg text-status-success text-xs font-medium rounded-full animate-pulse">
        진행 중
      </span>
    )
  }

  const isActive = () => {
    const now = new Date()
    return now >= new Date(timeDeal.startTime) && now <= new Date(timeDeal.endTime)
  }

  return (
    <Link to={`/time-deals/${timeDeal.id}`}>
      <div className={`
        bg-bg-card border rounded-lg overflow-hidden transition-all
        ${isActive()
          ? 'border-brand-primary shadow-lg hover:shadow-xl'
          : 'border-border-default hover:shadow-lg'
        }
      `}>
        {/* Image */}
        <div className="aspect-square bg-bg-subtle relative">
          {timeDeal.product.imageUrl ? (
            <img
              src={timeDeal.product.imageUrl}
              alt={timeDeal.product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-text-meta">
              No Image
            </div>
          )}

          {/* Discount Badge */}
          {getDiscountRate() > 0 && (
            <div className="absolute top-3 right-3 px-3 py-1 bg-status-error text-white text-sm font-bold rounded-full">
              {getDiscountRate()}% OFF
            </div>
          )}

          {/* Status Badge */}
          <div className="absolute top-3 left-3">
            {getStatusBadge()}
          </div>
        </div>

        {/* Content */}
        <div className="p-4 space-y-3">
          {/* Product Name */}
          <h3 className="text-lg font-semibold text-text-heading line-clamp-2">
            {timeDeal.product.name}
          </h3>

          {/* Countdown or Status */}
          {isActive() ? (
            <div>
              <p className="text-xs text-text-meta mb-1">남은 시간</p>
              <CountdownTimer endTime={timeDeal.endTime} />
            </div>
          ) : (
            <div className="text-sm text-text-meta">
              {new Date() < new Date(timeDeal.startTime)
                ? `${new Date(timeDeal.startTime).toLocaleString()} 시작`
                : '종료됨'
              }
            </div>
          )}

          {/* Price */}
          <div className="flex items-center gap-2">
            {timeDeal.originalPrice && (
              <span className="text-sm text-text-meta line-through">
                {formatPrice(timeDeal.originalPrice)}
              </span>
            )}
            <span className="text-xl font-bold text-brand-primary">
              {formatPrice(timeDeal.dealPrice)}
            </span>
          </div>

          {/* Progress Bar */}
          <div>
            <div className="flex justify-between text-xs text-text-meta mb-1">
              <span>판매 진행률</span>
              <span>{timeDeal.soldCount} / {timeDeal.totalStock}</span>
            </div>
            <div className="h-2 bg-bg-subtle rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-brand-primary to-brand-secondary transition-all duration-300"
                style={{ width: `${getSoldPercentage()}%` }}
              />
            </div>
          </div>

          {/* Stock Warning */}
          {isActive() && timeDeal.remainingStock < 10 && timeDeal.remainingStock > 0 && (
            <p className="text-xs text-status-warning font-medium">
              ⚠️ 재고가 {timeDeal.remainingStock}개 남았습니다!
            </p>
          )}

          {timeDeal.remainingStock === 0 && (
            <p className="text-xs text-status-error font-medium">
              품절되었습니다
            </p>
          )}
        </div>
      </div>
    </Link>
  )
}
```

---

## 4. TimeDealListPage

### 4.1 목록 페이지

```tsx
// pages/timedeal/TimeDealListPage.tsx
import React, { useState } from 'react'
import { useTimeDeals } from '@/hooks/useTimeDeals'
import { TimeDealCard } from '@/components/timedeal/TimeDealCard'

const TimeDealListPage: React.FC = () => {
  const [status, setStatus] = useState<'ACTIVE' | 'UPCOMING' | 'ENDED' | undefined>(undefined)
  const { data, isLoading, error } = useTimeDeals({ status, page: 0, size: 20 })

  const tabs = [
    { value: undefined, label: '전체' },
    { value: 'ACTIVE', label: '진행 중' },
    { value: 'UPCOMING', label: '예정' },
    { value: 'ENDED', label: '종료' }
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-text-heading mb-2">
          ⚡ Time Deals
        </h1>
        <p className="text-text-meta">
          한정된 시간 동안만 제공되는 특별한 할인 혜택!
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-border-default">
        {tabs.map((tab) => (
          <button
            key={String(tab.value)}
            onClick={() => setStatus(tab.value)}
            className={`
              px-4 py-2 font-medium transition-colors
              ${status === tab.value
                ? 'text-brand-primary border-b-2 border-brand-primary'
                : 'text-text-meta hover:text-text-body'
              }
            `}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Loading */}
      {isLoading && <div>Loading...</div>}

      {/* Error */}
      {error && <Alert variant="error">{error.message}</Alert>}

      {/* Grid */}
      {data && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {data.content.map((timeDeal) => (
            <TimeDealCard key={timeDeal.id} timeDeal={timeDeal} />
          ))}
        </div>
      )}

      {/* Empty State */}
      {data && data.content.length === 0 && (
        <div className="text-center py-12">
          <p className="text-text-meta">타임딜이 없습니다.</p>
        </div>
      )}
    </div>
  )
}

export default TimeDealListPage
```

---

## 5. TimeDealDetailPage

### 5.1 상세 페이지

```tsx
// pages/timedeal/TimeDealDetailPage.tsx
import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { timeDealApi } from '@/api/endpoints'
import { CountdownTimer } from '@/components/timedeal/CountdownTimer'
import type { TimeDeal } from '@/types'

const TimeDealDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [timeDeal, setTimeDeal] = useState<TimeDeal | null>(null)
  const [loading, setLoading] = useState(true)
  const [quantity, setQuantity] = useState(1)

  useEffect(() => {
    const fetchTimeDeal = async () => {
      try {
        const response = await timeDealApi.getTimeDeal(Number(id))
        setTimeDeal(response.data)
      } catch (error) {
        console.error('Failed to load time deal', error)
      } finally {
        setLoading(false)
      }
    }

    fetchTimeDeal()
  }, [id])

  const handlePurchase = () => {
    if (!timeDeal) return
    // 구매 로직 (장바구니 또는 바로 구매)
    navigate(`/products/${timeDeal.product.id}`)
  }

  if (loading || !timeDeal) {
    return <div>Loading...</div>
  }

  const isActive = () => {
    const now = new Date()
    return now >= new Date(timeDeal.startTime) && now <= new Date(timeDeal.endTime)
  }

  return (
    <div className="max-w-6xl mx-auto">
      {/* Time Deal Banner */}
      <div className="bg-gradient-to-r from-brand-primary to-brand-secondary text-white rounded-lg p-6 mb-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold mb-2">⚡ Time Deal</h1>
            {isActive() ? (
              <>
                <p className="text-sm mb-2">지금 바로 특별 할인을 받으세요!</p>
                <CountdownTimer endTime={timeDeal.endTime} />
              </>
            ) : (
              <p>이 타임딜은 종료되었습니다.</p>
            )}
          </div>

          {timeDeal.originalPrice && (
            <div className="text-right">
              <div className="text-4xl font-bold">
                {Math.round(((timeDeal.originalPrice - timeDeal.dealPrice) / timeDeal.originalPrice) * 100)}%
              </div>
              <div className="text-sm">OFF</div>
            </div>
          )}
        </div>
      </div>

      {/* Product Detail */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Image */}
        <div className="bg-bg-subtle rounded-lg aspect-square">
          {timeDeal.product.imageUrl && (
            <img
              src={timeDeal.product.imageUrl}
              alt={timeDeal.product.name}
              className="w-full h-full object-cover rounded-lg"
            />
          )}
        </div>

        {/* Info */}
        <div className="space-y-6">
          <h2 className="text-3xl font-bold text-text-heading">
            {timeDeal.product.name}
          </h2>

          <p className="text-text-meta leading-relaxed">
            {timeDeal.product.description}
          </p>

          {/* Price */}
          <div className="border-t border-b border-border-default py-6">
            {timeDeal.originalPrice && (
              <div className="text-lg text-text-meta line-through mb-2">
                {formatPrice(timeDeal.originalPrice)}
              </div>
            )}
            <div className="text-4xl font-bold text-brand-primary">
              {formatPrice(timeDeal.dealPrice)}
            </div>
          </div>

          {/* Stock */}
          <div>
            <div className="flex justify-between text-sm mb-2">
              <span className="text-text-meta">남은 재고</span>
              <span className="font-medium text-text-heading">
                {timeDeal.remainingStock} / {timeDeal.totalStock}
              </span>
            </div>
            <div className="h-2 bg-bg-subtle rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-brand-primary to-brand-secondary"
                style={{ width: `${(timeDeal.soldCount / timeDeal.totalStock) * 100}%` }}
              />
            </div>
          </div>

          {/* Actions */}
          <Button
            onClick={handlePurchase}
            disabled={!isActive() || timeDeal.remainingStock === 0}
            variant="primary"
            size="lg"
            className="w-full"
          >
            {isActive()
              ? timeDeal.remainingStock > 0
                ? '지금 구매하기'
                : '품절'
              : '종료됨'
            }
          </Button>
        </div>
      </div>
    </div>
  )
}

export default TimeDealDetailPage
```

---

## 6. 핵심 정리

| 컴포넌트 | 역할 |
|----------|------|
| `CountdownTimer` | 실시간 타이머 (HH:MM:SS) |
| `TimeDealCard` | 타임딜 카드 (목록용) |
| `TimeDealListPage` | 타임딜 목록 (상태별 필터) |
| `TimeDealDetailPage` | 타임딜 상세 (구매 버튼) |
| **Progress Bar** | 판매 진행률 표시 |
| **Status Badge** | 진행 중/예정/종료 표시 |

---

## 다음 학습

- [Coupon UI](./coupon-ui.md)
- [Product Detail](./product-detail.md)
- [Checkout Flow](./checkout-flow.md)
