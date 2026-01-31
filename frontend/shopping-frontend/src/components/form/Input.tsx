/**
 * Input Component
 * Admin 폼에서 사용하는 Input 컴포넌트
 */
import React from 'react'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: string
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helpText, className = '', required, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-medium text-text-heading mb-2">
            {label}
            {required && <span className="text-status-error ml-1">*</span>}
          </label>
        )}

        <input
          ref={ref}
          className={`
            w-full px-4 py-3
            border rounded-lg
            bg-bg-card text-text-body
            placeholder:text-text-placeholder
            focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary
            disabled:bg-bg-muted disabled:text-text-muted disabled:cursor-not-allowed
            transition-colors
            ${error ? 'border-status-error' : 'border-border-default'}
            ${className}
          `}
          {...props}
        />

        {error && (
          <p className="mt-2 text-sm text-status-error flex items-center gap-1">
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            {error}
          </p>
        )}

        {helpText && !error && (
          <p className="mt-2 text-xs text-text-meta">{helpText}</p>
        )}
      </div>
    )
  }
)

Input.displayName = 'Input'

export default Input
