import { forwardRef, type HTMLAttributes } from 'react';
import type { SkeletonProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface SkeletonComponentProps
  extends SkeletonProps,
    HTMLAttributes<HTMLDivElement> {}

const variantClasses: Record<NonNullable<SkeletonProps['variant']>, string> = {
  text: 'rounded h-4',
  circular: 'rounded-full',
  rectangular: 'rounded-none',
  rounded: 'rounded-lg',
};

const animationClasses: Record<NonNullable<SkeletonProps['animation']>, string> = {
  pulse: 'animate-pulse',
  wave: 'overflow-hidden relative before:absolute before:inset-0 before:-translate-x-full before:animate-[shimmer_2s_infinite] before:bg-gradient-to-r before:from-transparent before:via-white/10 before:to-transparent',
  none: '',
};

export const Skeleton = forwardRef<HTMLDivElement, SkeletonComponentProps>(
  (
    {
      variant = 'text',
      width,
      height,
      animation = 'pulse',
      lines = 1,
      className,
      style,
      ...props
    },
    ref
  ) => {
    if (lines > 1 && variant === 'text') {
      return (
        <div ref={ref} className={cn('space-y-2', className)} {...props}>
          {Array.from({ length: lines }).map((_, index) => (
            <div
              key={index}
              className={cn(
                'bg-bg-muted',
                variantClasses[variant],
                animationClasses[animation],
                index === lines - 1 && 'w-3/4'
              )}
              style={{
                width: index === lines - 1 ? '75%' : width,
                height,
                ...style,
              }}
            />
          ))}
        </div>
      );
    }

    return (
      <div
        ref={ref}
        className={cn(
          'bg-bg-muted',
          variantClasses[variant],
          animationClasses[animation],
          className
        )}
        style={{
          width: width || (variant === 'circular' ? '40px' : undefined),
          height: height || (variant === 'circular' ? '40px' : undefined),
          ...style,
        }}
        {...props}
      />
    );
  }
);

Skeleton.displayName = 'Skeleton';

export default Skeleton;
