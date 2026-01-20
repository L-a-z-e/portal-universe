import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import type { ButtonProps } from '@portal/design-types';
import { cn } from '../../utils/cn';
import { Spinner } from '../Spinner';

export interface ButtonComponentProps
  extends ButtonProps,
    Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  children?: ReactNode;
}

// Linear-inspired button styles - Dark mode first design
const variantClasses: Record<NonNullable<ButtonProps['variant']>, string> = {
  // Primary: Bright button on dark bg (Linear style)
  // Dark mode: white/light gray button with dark text
  // Light mode: brand color button with white text (handled via theme)
  primary: [
    // Dark mode (default)
    'bg-white/90 text-[#08090a]',
    'hover:bg-white',
    'active:bg-white/80 active:scale-[0.98]',
    // Light mode override
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
    'light:active:bg-brand-primary',
    'border border-transparent',
    'shadow-sm'
  ].join(' '),

  // Secondary: Ghost style with border
  secondary: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-[#2a2a2a]',
    'light:hover:bg-gray-100',
    'light:border-gray-200'
  ].join(' '),

  // Ghost: Minimal button without border
  ghost: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-transparent',
    'light:hover:bg-gray-100'
  ].join(' '),

  // Outline: Border emphasis
  outline: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:border-[#3a3a3a]',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-[#2a2a2a]',
    'light:border-gray-300 light:hover:border-gray-400',
    'light:hover:bg-gray-50'
  ].join(' '),

  // Danger: Destructive action - consistent across modes
  danger: [
    'bg-[#E03131] text-white',
    'hover:bg-[#C92A2A]',
    'active:bg-[#A51D1D] active:scale-[0.98]',
    'border border-transparent',
    'shadow-sm'
  ].join(' ')
};

const sizeClasses: Record<NonNullable<ButtonProps['size']>, string> = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2',
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
          'transition-all duration-150 ease-out',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-[#5e6ad2] focus-visible:ring-offset-2 focus-visible:ring-offset-[#08090a]',
          'light:focus-visible:ring-offset-white',
          // Variant
          variantClasses[variant],
          // Size
          sizeClasses[size],
          // Full width
          fullWidth && 'w-full',
          // Disabled state
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
