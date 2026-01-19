import { forwardRef, type HTMLAttributes } from 'react';
import type { ProgressProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface ProgressComponentProps
  extends ProgressProps,
    HTMLAttributes<HTMLDivElement> {}

const sizeClasses: Record<NonNullable<ProgressProps['size']>, string> = {
  sm: 'h-1',
  md: 'h-2',
  lg: 'h-3',
};

const variantClasses: Record<NonNullable<ProgressProps['variant']>, string> = {
  default: 'bg-brand-primary',
  info: 'bg-status-info',
  success: 'bg-status-success',
  warning: 'bg-status-warning',
  error: 'bg-status-error',
};

export const Progress = forwardRef<HTMLDivElement, ProgressComponentProps>(
  (
    {
      value,
      max = 100,
      size = 'md',
      showLabel = false,
      variant = 'default',
      className,
      ...props
    },
    ref
  ) => {
    const percentage = Math.min(Math.max((value / max) * 100, 0), 100);

    return (
      <div ref={ref} className={cn('w-full', className)} {...props}>
        <div
          className={cn(
            'w-full rounded-full bg-bg-muted overflow-hidden',
            sizeClasses[size]
          )}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={max}
        >
          <div
            className={cn(
              'h-full rounded-full transition-all duration-normal ease-linear-ease',
              variantClasses[variant]
            )}
            style={{ width: `${percentage}%` }}
          />
        </div>
        {showLabel && (
          <div className="mt-1 text-sm text-text-muted text-right">
            {Math.round(percentage)}%
          </div>
        )}
      </div>
    );
  }
);

Progress.displayName = 'Progress';

export default Progress;
