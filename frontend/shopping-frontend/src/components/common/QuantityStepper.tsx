import React from 'react'

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
          <svg className="w-4 h-4 mx-auto animate-spin" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
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
