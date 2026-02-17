import { forwardRef, type HTMLAttributes, type ReactNode, useId } from 'react';
import type { FormFieldProps } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface FormFieldComponentProps
  extends FormFieldProps,
    Omit<HTMLAttributes<HTMLDivElement>, 'id'> {
  children?: ReactNode;
}

export const FormField = forwardRef<HTMLDivElement, FormFieldComponentProps>(
  (
    {
      label,
      required,
      error,
      errorMessage,
      helperText,
      id: providedId,
      disabled,
      size = 'md',
      className,
      children,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const id = providedId || generatedId;

    return (
      <div ref={ref} className={cn('w-full', className)} {...props}>
        {label && (
          <label
            htmlFor={id}
            className={cn(
              'block mb-1.5 font-medium text-text-body',
              size === 'sm' && 'text-xs',
              size === 'md' && 'text-sm',
              size === 'lg' && 'text-base',
              disabled && 'opacity-50'
            )}
          >
            {label}
            {required && <span className="text-status-error ml-0.5">*</span>}
          </label>
        )}
        {children}
        {helperText && !error && (
          <p
            className={cn(
              'mt-1.5 text-text-muted',
              size === 'sm' && 'text-xs',
              size === 'md' && 'text-sm',
              size === 'lg' && 'text-base'
            )}
          >
            {helperText}
          </p>
        )}
        {error && errorMessage && (
          <p
            id={`${id}-error`}
            className={cn(
              'mt-1.5 text-status-error',
              size === 'sm' && 'text-xs',
              size === 'md' && 'text-sm',
              size === 'lg' && 'text-base'
            )}
          >
            {errorMessage}
          </p>
        )}
      </div>
    );
  }
);

FormField.displayName = 'FormField';

export default FormField;
