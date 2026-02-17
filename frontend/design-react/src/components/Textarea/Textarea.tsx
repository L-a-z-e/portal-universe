import { forwardRef, type TextareaHTMLAttributes, useId } from 'react';
import type { TextareaProps } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface TextareaComponentProps
  extends Omit<TextareaProps, 'value'>,
    Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'rows'> {}

const sizeClasses: Record<NonNullable<TextareaProps['size']>, string> = {
  sm: 'px-3 py-2 text-sm',
  md: 'px-3 py-2.5 text-base',
  lg: 'px-4 py-3 text-lg',
};

const resizeClasses: Record<NonNullable<TextareaProps['resize']>, string> = {
  none: 'resize-none',
  vertical: 'resize-y',
  horizontal: 'resize-x',
  both: 'resize',
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaComponentProps>(
  (
    {
      placeholder,
      disabled,
      error,
      errorMessage,
      label,
      required,
      size = 'md',
      rows = 4,
      resize = 'vertical',
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
        <textarea
          ref={ref}
          id={id}
          disabled={disabled}
          rows={rows}
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
            // Resize
            resizeClasses[resize],
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

Textarea.displayName = 'Textarea';

export default Textarea;
