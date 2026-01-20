import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { InputProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface InputComponentProps
  extends Omit<InputProps, 'value'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'size' | 'type'> {}

// Linear-inspired sizing
const sizeClasses: Record<NonNullable<InputProps['size']>, string> = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-9 px-3 text-sm',
  lg: 'h-11 px-4 text-base',
};

export const Input = forwardRef<HTMLInputElement, InputComponentProps>(
  (
    {
      type = 'text',
      placeholder,
      disabled,
      error,
      errorMessage,
      label,
      required,
      size = 'md',
      id: providedId,
      className,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const id = providedId || generatedId;

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={id}
            className={cn(
              'block mb-1.5 text-sm font-medium text-[#b4b4b4]',
              'light:text-gray-700',
              disabled && 'opacity-50'
            )}
          >
            {label}
            {required && <span className="text-[#E03131] ml-0.5">*</span>}
          </label>
        )}
        <input
          ref={ref}
          type={type}
          id={id}
          disabled={disabled}
          placeholder={placeholder}
          aria-invalid={error}
          aria-describedby={error && errorMessage ? `${id}-error` : undefined}
          className={cn(
            // Base styles - Linear dark mode first
            'w-full rounded-md border',
            'bg-[#0f1011]',
            'text-[#b4b4b4] placeholder:text-[#6b6b6b]',
            // Light mode
            'light:bg-white light:text-gray-900 light:placeholder:text-gray-400',
            // Transitions
            'transition-all duration-150 ease-out',
            // Focus state
            'focus:outline-none focus:ring-2 focus:ring-[#5e6ad2]/30 focus:border-[#5e6ad2]',
            'light:focus:ring-[#5e6ad2]/20',
            // Size
            sizeClasses[size],
            // Error state
            error
              ? 'border-[#E03131] focus:border-[#E03131] focus:ring-[#E03131]/30'
              : 'border-[#2a2a2a] hover:border-[#3a3a3a] light:border-gray-200 light:hover:border-gray-300',
            // Disabled state
            disabled && 'bg-[#18191b] cursor-not-allowed opacity-50 light:bg-gray-100',
            className
          )}
          {...props}
        />
        {error && errorMessage && (
          <p id={`${id}-error`} className="mt-1.5 text-sm text-[#E03131]">
            {errorMessage}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
