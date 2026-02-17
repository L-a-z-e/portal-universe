import { forwardRef, useState, type ImgHTMLAttributes } from 'react';
import type { AvatarProps } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface AvatarComponentProps
  extends AvatarProps,
    Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'> {}

const sizeClasses: Record<NonNullable<AvatarProps['size']>, string> = {
  xs: 'w-6 h-6 text-xs',
  sm: 'w-8 h-8 text-sm',
  md: 'w-10 h-10 text-base',
  lg: 'w-12 h-12 text-lg',
  xl: 'w-16 h-16 text-xl',
  '2xl': 'w-20 h-20 text-2xl',
};

const statusClasses: Record<NonNullable<AvatarProps['status']>, string> = {
  online: 'bg-status-success',
  offline: 'bg-text-muted',
  busy: 'bg-status-error',
  away: 'bg-status-warning',
};

const statusSizeClasses: Record<NonNullable<AvatarProps['size']>, string> = {
  xs: 'w-1.5 h-1.5',
  sm: 'w-2 h-2',
  md: 'w-2.5 h-2.5',
  lg: 'w-3 h-3',
  xl: 'w-3.5 h-3.5',
  '2xl': 'w-4 h-4',
};

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export const Avatar = forwardRef<HTMLDivElement, AvatarComponentProps>(
  (
    {
      src,
      alt,
      name,
      size = 'md',
      status,
      shape = 'circle',
      className,
      ...props
    },
    ref
  ) => {
    const [imgError, setImgError] = useState(false);

    const showFallback = !src || imgError;
    const initials = name ? getInitials(name) : '';

    return (
      <div ref={ref} className={cn('relative inline-flex', className)}>
        <div
          className={cn(
            'inline-flex items-center justify-center overflow-hidden bg-bg-muted',
            sizeClasses[size],
            shape === 'circle' ? 'rounded-full' : 'rounded-lg'
          )}
        >
          {showFallback ? (
            <span className="font-medium text-text-muted">{initials || '?'}</span>
          ) : (
            <img
              src={src}
              alt={alt || name || 'Avatar'}
              onError={() => setImgError(true)}
              className="w-full h-full object-cover"
              {...props}
            />
          )}
        </div>
        {status && (
          <span
            className={cn(
              'absolute bottom-0 right-0 rounded-full ring-2 ring-bg-card',
              statusClasses[status],
              statusSizeClasses[size]
            )}
          />
        )}
      </div>
    );
  }
);

Avatar.displayName = 'Avatar';

export default Avatar;
