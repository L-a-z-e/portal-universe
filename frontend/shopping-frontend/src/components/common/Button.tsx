/**
 * Button Component
 * Admin UI에서 사용하는 버튼 컴포넌트
 */
import React from 'react'

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  loading?: boolean
  icon?: React.ReactNode
  fullWidth?: boolean
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  loading = false,
  icon,
  fullWidth = false,
  className = '',
  disabled,
  ...props
}) => {
  const baseStyles = 'px-6 py-3 rounded-lg font-medium transition-all duration-200 flex items-center justify-center gap-2'

  const variantStyles = {
    primary: 'bg-brand-primary text-white hover:bg-brand-primaryHover shadow-sm hover:shadow-md',
    secondary: 'border-2 border-brand-primary bg-transparent text-brand-primary hover:bg-brand-primary/5',
    ghost: 'bg-transparent text-text-body hover:bg-bg-hover',
    danger: 'bg-status-error text-white hover:bg-status-error/90 shadow-sm hover:shadow-md'
  }

  const disabledStyles = 'disabled:opacity-50 disabled:cursor-not-allowed'
  const fullWidthStyles = fullWidth ? 'w-full' : ''

  return (
    <button
      className={`${baseStyles} ${variantStyles[variant]} ${disabledStyles} ${fullWidthStyles} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <>
          <div className="w-5 h-5 border-2 border-current border-t-transparent rounded-full animate-spin" />
          <span>Loading...</span>
        </>
      ) : (
        <>
          {icon && icon}
          {children}
        </>
      )}
    </button>
  )
}

export default Button
