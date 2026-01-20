import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { CardProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface CardComponentProps
  extends CardProps,
    HTMLAttributes<HTMLDivElement> {
  children?: ReactNode;
}

// Linear-inspired card styles - Dark mode first design
const variantClasses: Record<NonNullable<CardProps['variant']>, string> = {
  // Elevated: Default card with subtle shadow (Linear style)
  elevated: [
    'bg-[#0f1011]',
    'border border-[#2a2a2a]',
    'shadow-[0_1px_2px_rgba(0,0,0,0.3)]',
    'light:bg-white light:border-gray-200 light:shadow-sm'
  ].join(' '),

  // Outlined: Border emphasis only
  outlined: [
    'bg-transparent',
    'border border-[#2a2a2a]',
    'light:border-gray-200'
  ].join(' '),

  // Flat: No border, subtle background
  flat: [
    'bg-[#18191b]',
    'border border-transparent',
    'light:bg-gray-50'
  ].join(' '),

  // Glass: Glassmorphism effect
  glass: [
    'bg-[#0f1011]/80',
    'backdrop-blur-md',
    'border border-white/10',
    'light:bg-white/80 light:border-gray-200/50'
  ].join(' '),

  // Interactive: For clickable cards
  interactive: [
    'bg-[#0f1011]',
    'border border-[#2a2a2a]',
    'hover:border-[#3a3a3a] hover:bg-[#18191b]',
    'hover:-translate-y-0.5 hover:shadow-[0_8px_24px_rgba(0,0,0,0.4)]',
    'cursor-pointer',
    'light:bg-white light:border-gray-200',
    'light:hover:border-gray-300 light:hover:bg-gray-50',
    'light:hover:shadow-lg'
  ].join(' ')
};

const paddingClasses: Record<NonNullable<CardProps['padding']>, string> = {
  none: 'p-0',
  sm: 'p-3',
  md: 'p-4',
  lg: 'p-6',
  xl: 'p-8',
};

export const Card = forwardRef<HTMLDivElement, CardComponentProps>(
  (
    {
      variant = 'elevated',
      hoverable,
      padding = 'md',
      className,
      children,
      ...props
    },
    ref
  ) => {
    return (
      <div
        ref={ref}
        className={cn(
          'rounded-xl',
          'transition-all duration-150 ease-out',
          variantClasses[variant],
          paddingClasses[padding],
          hoverable &&
            variant !== 'interactive' &&
            'hover:-translate-y-0.5 hover:shadow-[0_4px_12px_rgba(0,0,0,0.5)] cursor-pointer light:hover:shadow-md',
          className
        )}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';

export default Card;
