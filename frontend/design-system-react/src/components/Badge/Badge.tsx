import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { BadgeProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface BadgeComponentProps
  extends BadgeProps,
    HTMLAttributes<HTMLSpanElement> {
  children?: ReactNode;
}

const variantClasses: Record<NonNullable<BadgeProps['variant']>, string> = {
  default: 'bg-bg-muted text-text-body border border-border-default',
  primary: 'bg-brand-primary/10 text-brand-primary',
  success: 'bg-status-successBg text-status-success',
  warning: 'bg-status-warningBg text-status-warning',
  danger: 'bg-status-errorBg text-status-error',
  info: 'bg-status-infoBg text-status-info',
  outline: 'bg-transparent text-text-body border border-border-default',
};

const sizeClasses: Record<NonNullable<BadgeProps['size']>, string> = {
  xs: 'px-1.5 py-0.5 text-[10px]',
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
  lg: 'px-3 py-1.5 text-base',
};

export const Badge = forwardRef<HTMLSpanElement, BadgeComponentProps>(
  ({ variant = 'default', size = 'md', className, children, ...props }, ref) => {
    return (
      <span
        ref={ref}
        className={cn(
          'inline-flex items-center font-medium rounded-full',
          variantClasses[variant],
          sizeClasses[size],
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
