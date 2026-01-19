import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import type { ButtonProps } from '@portal/design-types';
import { cn } from '../../utils/cn';
import { Spinner } from '../Spinner';

export interface ButtonComponentProps
  extends ButtonProps,
    Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  children?: ReactNode;
}

const variantClasses: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary:
    'bg-brand-primary text-text-inverse hover:bg-brand-primaryHover active:bg-brand-primaryHover',
  secondary:
    'bg-bg-muted text-text-body hover:bg-bg-hover border border-border-default',
  ghost: 'bg-transparent text-text-body hover:bg-bg-hover',
  outline:
    'bg-transparent text-text-body border border-border-default hover:bg-bg-hover hover:border-border-hover',
  danger:
    'bg-status-error text-white hover:bg-red-600 active:bg-red-700',
};

const sizeClasses: Record<NonNullable<ButtonProps['size']>, string> = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-base gap-2',
  lg: 'h-11 px-6 text-lg gap-2',
};

export const Button = forwardRef<HTMLButtonElement, ButtonComponentProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      disabled,
      loading,
      fullWidth,
      type = 'button',
      className,
      children,
      ...props
    },
    ref
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        ref={ref}
        type={type}
        disabled={isDisabled}
        className={cn(
          // Base styles
          'inline-flex items-center justify-center font-medium rounded-md',
          'transition-all duration-normal ease-linear-ease',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary focus-visible:ring-offset-2',
          // Variant
          variantClasses[variant],
          // Size
          sizeClasses[size],
          // Full width
          fullWidth && 'w-full',
          // Disabled state
          isDisabled && 'opacity-50 cursor-not-allowed',
          className
        )}
        {...props}
      >
        {loading && <Spinner size="sm" color="current" className="shrink-0" />}
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';

export default Button;
