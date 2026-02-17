import { forwardRef, type HTMLAttributes } from 'react';
import type { SkeletonProps } from '@portal/design-core';
import { cn, skeletonBase, skeletonVariants, skeletonAnimations } from '@portal/design-core';

export interface SkeletonComponentProps
  extends SkeletonProps,
    HTMLAttributes<HTMLDivElement> {}

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
                skeletonBase,
                skeletonVariants[variant],
                skeletonAnimations[animation],
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
          skeletonBase,
          skeletonVariants[variant],
          skeletonAnimations[animation],
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
