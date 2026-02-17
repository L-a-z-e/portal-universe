import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { CardProps } from '@portal/design-core';
import { cn, cardBase, cardVariants, cardPadding } from '@portal/design-core';

export interface CardComponentProps
  extends CardProps,
    HTMLAttributes<HTMLDivElement> {
  children?: ReactNode;
}

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
          cardBase,
          cardVariants[variant],
          cardPadding[padding],
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
