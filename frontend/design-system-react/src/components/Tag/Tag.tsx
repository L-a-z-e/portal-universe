import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { TagProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface TagComponentProps
  extends TagProps,
    HTMLAttributes<HTMLSpanElement> {
  children?: ReactNode;
  onRemove?: () => void;
}

const variantClasses: Record<NonNullable<TagProps['variant']>, string> = {
  default: 'bg-bg-muted text-text-body border border-border-default',
  primary: 'bg-brand-primary/10 text-brand-primary',
  success: 'bg-status-successBg text-status-success',
  error: 'bg-status-errorBg text-status-error',
  warning: 'bg-status-warningBg text-status-warning',
  info: 'bg-status-infoBg text-status-info',
};

const sizeClasses: Record<NonNullable<TagProps['size']>, string> = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
  lg: 'px-3 py-1.5 text-base',
};

export const Tag = forwardRef<HTMLSpanElement, TagComponentProps>(
  (
    {
      variant = 'default',
      size = 'md',
      removable = false,
      clickable = false,
      onClick,
      onRemove,
      className,
      children,
      ...props
    },
    ref
  ) => {
    return (
      <span
        ref={ref}
        role={clickable ? 'button' : undefined}
        tabIndex={clickable ? 0 : undefined}
        onClick={clickable ? onClick : undefined}
        className={cn(
          'inline-flex items-center gap-1 rounded-md font-medium',
          variantClasses[variant],
          sizeClasses[size],
          clickable && 'cursor-pointer hover:opacity-80 transition-opacity',
          className
        )}
        {...props}
      >
        {children}
        {removable && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onRemove?.();
            }}
            className="ml-1 -mr-1 p-0.5 rounded hover:bg-black/10 transition-colors"
            aria-label="Remove"
          >
            <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        )}
      </span>
    );
  }
);

Tag.displayName = 'Tag';

export default Tag;
