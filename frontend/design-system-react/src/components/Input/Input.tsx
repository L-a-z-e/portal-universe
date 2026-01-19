import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { InputProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface InputComponentProps
  extends Omit<InputProps, 'value'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'size' | 'type'> {}

const sizeClasses: Record<NonNullable<InputProps['size']>, string> = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-9 px-3 text-base',
  lg: 'h-11 px-4 text-lg',
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
              'block mb-1.5 text-sm font-medium text-text-body',
              disabled && 'opacity-50'
            )}
          >
            {label}
            {required && <span className="text-status-error ml-0.5">*</span>}
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
            // Base styles
            'w-full rounded-md border bg-bg-card text-text-body placeholder:text-text-muted',
            'transition-all duration-normal ease-linear-ease',
            'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent',
            // Size
            sizeClasses[size],
            // Error state
            error
              ? 'border-status-error focus:ring-status-error'
              : 'border-border-default hover:border-border-hover',
            // Disabled state
            disabled && 'opacity-50 cursor-not-allowed bg-bg-muted',
            className
          )}
          {...props}
        />
        {error && errorMessage && (
          <p id={`${id}-error`} className="mt-1.5 text-sm text-status-error">
            {errorMessage}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
