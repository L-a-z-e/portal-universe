import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { CheckboxProps } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface CheckboxComponentProps
  extends Omit<CheckboxProps, 'checked'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'size' | 'type' | 'value'> {
  checked?: boolean;
  value?: string | number;
}

const sizeClasses: Record<NonNullable<CheckboxProps['size']>, { box: string; label: string }> = {
  sm: { box: 'w-4 h-4', label: 'text-sm' },
  md: { box: 'w-5 h-5', label: 'text-base' },
  lg: { box: 'w-6 h-6', label: 'text-lg' },
};

export const Checkbox = forwardRef<HTMLInputElement, CheckboxComponentProps>(
  (
    {
      checked,
      indeterminate,
      disabled,
      label,
      error,
      errorMessage,
      size = 'md',
      id: providedId,
      className,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const id = providedId || generatedId;
    const sizes = sizeClasses[size];

    return (
      <div className={cn('flex flex-col', className)}>
        <label
          htmlFor={id}
          className={cn(
            'inline-flex items-center gap-2 cursor-pointer',
            disabled && 'opacity-50 cursor-not-allowed'
          )}
        >
          <div className="relative flex items-center justify-center">
            <input
              ref={ref}
              type="checkbox"
              id={id}
              checked={checked}
              disabled={disabled}
              aria-invalid={error}
              aria-describedby={error && errorMessage ? `${id}-error` : undefined}
              className={cn(
                'appearance-none rounded border bg-bg-card',
                'transition-all duration-normal ease-linear-ease',
                'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:ring-offset-2',
                'checked:bg-brand-primary checked:border-brand-primary',
                sizes.box,
                error
                  ? 'border-status-error'
                  : 'border-border-default hover:border-border-hover',
                disabled && 'cursor-not-allowed'
              )}
              {...props}
            />
            <svg
              className={cn(
                'absolute pointer-events-none text-white',
                sizes.box,
                'opacity-0 transition-opacity',
                checked && 'opacity-100'
              )}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={3}
            >
              {indeterminate ? (
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 12h14" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              )}
            </svg>
          </div>
          {label && (
            <span className={cn('text-text-body', sizes.label)}>{label}</span>
          )}
        </label>
        {error && errorMessage && (
          <p id={`${id}-error`} className="mt-1 text-sm text-status-error ml-7">
            {errorMessage}
          </p>
        )}
      </div>
    );
  }
);

Checkbox.displayName = 'Checkbox';

export default Checkbox;
