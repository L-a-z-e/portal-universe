import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { CardProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface CardComponentProps
  extends CardProps,
    HTMLAttributes<HTMLDivElement> {
  children?: ReactNode;
}

const variantClasses: Record<NonNullable<CardProps['variant']>, string> = {
  elevated: 'bg-bg-card shadow-md border border-border-default',
  outlined: 'bg-bg-card border border-border-default',
  flat: 'bg-bg-card',
  glass: 'bg-bg-card/80 backdrop-blur-glass border border-border-default/50',
  interactive:
    'bg-bg-card border border-border-default hover:border-border-hover hover:shadow-md transition-all duration-normal cursor-pointer',
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
          'rounded-lg',
          variantClasses[variant],
          paddingClasses[padding],
          hoverable &&
            variant !== 'interactive' &&
            'hover:shadow-lg transition-shadow duration-normal',
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
