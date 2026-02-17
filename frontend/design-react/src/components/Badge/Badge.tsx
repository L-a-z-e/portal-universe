import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { BadgeProps } from '@portal/design-core';
import { cn, badgeBase, badgeVariants, badgeSizes } from '@portal/design-core';

export interface BadgeComponentProps
  extends BadgeProps,
    HTMLAttributes<HTMLSpanElement> {
  children?: ReactNode;
}

export const Badge = forwardRef<HTMLSpanElement, BadgeComponentProps>(
  ({ variant = 'default', size = 'md', className, children, ...props }, ref) => {
    return (
      <span
        ref={ref}
        className={cn(
          badgeBase,
          badgeVariants[variant],
          badgeSizes[size],
          className
        )}
        {...props}
      >
        {children}
      </span>
    );
  }
);

Badge.displayName = 'Badge';

export default Badge;
