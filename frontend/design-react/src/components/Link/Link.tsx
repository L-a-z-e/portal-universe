import { forwardRef, type AnchorHTMLAttributes, type ReactNode } from 'react';
import type { LinkProps } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface LinkComponentProps
  extends LinkProps,
    Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'href' | 'target'> {
  children?: ReactNode;
}

const variantClasses: Record<NonNullable<LinkProps['variant']>, string> = {
  default: 'text-text-link hover:text-text-linkHover',
  primary: 'text-brand-primary hover:text-brand-primaryHover',
  muted: 'text-text-muted hover:text-text-body',
  underline: 'text-text-link hover:text-text-linkHover underline underline-offset-2',
};

const sizeClasses: Record<NonNullable<LinkProps['size']>, string> = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-lg',
};

export const Link = forwardRef<HTMLAnchorElement, LinkComponentProps>(
  (
    {
      href,
      target = '_self',
      variant = 'default',
      external = false,
      disabled = false,
      size = 'md',
      className,
      children,
      ...props
    },
    ref
  ) => {
    const isExternal = external || target === '_blank';

    return (
      <a
        ref={ref}
        href={disabled ? undefined : href}
        target={target}
        rel={isExternal ? 'noopener noreferrer' : undefined}
        aria-disabled={disabled}
        className={cn(
          'inline-flex items-center gap-1 transition-colors duration-fast',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary focus-visible:ring-offset-2 rounded',
          variantClasses[variant],
          sizeClasses[size],
          disabled && 'opacity-50 cursor-not-allowed pointer-events-none',
          className
        )}
        {...props}
      >
        {children}
        {isExternal && (
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
            />
          </svg>
        )}
      </a>
    );
  }
);

Link.displayName = 'Link';

export default Link;
