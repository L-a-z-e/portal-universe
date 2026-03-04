import React from 'react'
import { Spinner } from '@portal/design-react'

interface QuantityStepperProps {
  value: number
  min?: number
  max?: number
  onChange: (value: number) => void
  loading?: boolean
  variant?: 'default' | 'pill'
}

const QuantityStepper: React.FC<QuantityStepperProps> = ({
  value,
  min = 1,
  max = 99,
  onChange,
  loading = false,
  variant = 'default',
}) => {
  const handleDecrement = () => {
    if (value > min && !loading) onChange(value - 1)
  }

  const handleIncrement = () => {
    if (value < max && !loading) onChange(value + 1)
  }

  const isPill = variant === 'pill'
  const containerClass = isPill
    ? 'inline-flex items-center border-2 border-border-default rounded-full'
    : 'inline-flex items-center border border-border-default rounded-lg'

  const btnClass = isPill
    ? 'w-10 h-10 flex items-center justify-center text-text-body hover:bg-bg-hover disabled:opacity-40 disabled:cursor-not-allowed transition-colors'
    : 'w-8 h-8 flex items-center justify-center text-text-body hover:bg-bg-hover disabled:opacity-40 disabled:cursor-not-allowed transition-colors'

  const valueClass = isPill ? 'w-14 text-center font-semibold' : 'w-10 text-center font-medium'

  return (
    <div className={containerClass}>
      <button
        onClick={handleDecrement}
        disabled={value <= min || loading}
        className={btnClass}
        aria-label="Decrease quantity"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
        </svg>
      </button>

      <span className={`${valueClass} ${loading ? 'text-text-muted' : 'text-text-heading'}`}>
        {loading ? (
          <Spinner size="sm" />
        ) : (
          value
        )}
      </span>

      <button
        onClick={handleIncrement}
        disabled={value >= max || loading}
        className={btnClass}
        aria-label="Increase quantity"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
        </svg>
      </button>
    </div>
  )
}

export default QuantityStepper
