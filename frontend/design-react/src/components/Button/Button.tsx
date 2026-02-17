import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import type { ButtonProps } from '@portal/design-core';
import { cn, buttonBase, buttonVariants, buttonSizes } from '@portal/design-core';
import { Spinner } from '../Spinner';

export interface ButtonComponentProps
  extends ButtonProps,
    Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  children?: ReactNode;
  asChild?: boolean;
}

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
          buttonBase,
          buttonVariants[variant],
          buttonSizes[size],
          fullWidth && 'w-full',
          isDisabled && 'opacity-50 cursor-not-allowed pointer-events-none',
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
