import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import type { TagProps } from '@portal/design-core';
import { cn, tagBase, tagVariants, tagSizes } from '@portal/design-core';

export interface TagComponentProps
  extends TagProps,
    HTMLAttributes<HTMLSpanElement> {
  children?: ReactNode;
  onRemove?: () => void;
}

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
          tagBase,
          tagVariants[variant],
          tagSizes[size],
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
