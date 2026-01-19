import { forwardRef, type HTMLAttributes } from 'react';
import type { SpinnerProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface SpinnerComponentProps
  extends SpinnerProps,
    Omit<HTMLAttributes<HTMLDivElement>, 'color'> {}

const sizeClasses: Record<NonNullable<SpinnerProps['size']>, string> = {
  xs: 'w-3 h-3',
  sm: 'w-4 h-4',
  md: 'w-6 h-6',
  lg: 'w-8 h-8',
  xl: 'w-12 h-12',
};

const colorClasses: Record<NonNullable<SpinnerProps['color']>, string> = {
  primary: 'text-brand-primary',
  current: 'text-current',
  white: 'text-white',
};

export const Spinner = forwardRef<HTMLDivElement, SpinnerComponentProps>(
  ({ size = 'md', color = 'primary', label = 'Loading', className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        role="status"
        aria-label={label}
        className={cn('inline-flex', className)}
        {...props}
      >
        <svg
          className={cn('animate-spin', sizeClasses[size], colorClasses[color])}
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
        <span className="sr-only">{label}</span>
      </div>
    );
  }
);

Spinner.displayName = 'Spinner';

export default Spinner;
