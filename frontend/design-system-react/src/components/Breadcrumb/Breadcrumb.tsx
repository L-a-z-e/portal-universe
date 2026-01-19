import { forwardRef, type HTMLAttributes } from 'react';
import type { BreadcrumbProps, BreadcrumbItem } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface BreadcrumbComponentProps
  extends BreadcrumbProps,
    HTMLAttributes<HTMLElement> {}

const sizeClasses: Record<NonNullable<BreadcrumbProps['size']>, string> = {
  sm: 'text-xs',
  md: 'text-sm',
  lg: 'text-base',
};

export const Breadcrumb = forwardRef<HTMLElement, BreadcrumbComponentProps>(
  (
    {
      items,
      separator = '/',
      maxItems,
      size = 'md',
      className,
      ...props
    },
    ref
  ) => {
    let displayItems = items;

    if (maxItems && items.length > maxItems) {
      const start = items.slice(0, 1);
      const end = items.slice(-(maxItems - 2));
      displayItems = [
        ...start,
        { label: '...', href: undefined },
        ...end,
      ];
    }

    return (
      <nav
        ref={ref}
        aria-label="Breadcrumb"
        className={cn(sizeClasses[size], className)}
        {...props}
      >
        <ol className="flex items-center flex-wrap gap-1">
          {displayItems.map((item: BreadcrumbItem, index: number) => {
            const isLast = index === displayItems.length - 1;

            return (
              <li key={index} className="flex items-center gap-1">
                {item.href ? (
                  <a
                    href={item.href}
                    className={cn(
                      'transition-colors duration-fast',
                      isLast
                        ? 'text-text-body font-medium'
                        : 'text-text-muted hover:text-text-body'
                    )}
                    aria-current={isLast ? 'page' : undefined}
                  >
                    {item.icon && <span className="mr-1">{item.icon}</span>}
                    {item.label}
                  </a>
                ) : (
                  <span
                    className={cn(
                      isLast ? 'text-text-body font-medium' : 'text-text-muted'
                    )}
                    aria-current={isLast ? 'page' : undefined}
                  >
                    {item.icon && <span className="mr-1">{item.icon}</span>}
                    {item.label}
                  </span>
                )}
                {!isLast && (
                  <span className="text-text-muted mx-1" aria-hidden="true">
                    {separator}
                  </span>
                )}
              </li>
            );
          })}
        </ol>
      </nav>
    );
  }
);

Breadcrumb.displayName = 'Breadcrumb';

export default Breadcrumb;
