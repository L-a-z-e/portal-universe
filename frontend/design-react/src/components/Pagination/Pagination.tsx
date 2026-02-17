import { forwardRef, type HTMLAttributes } from 'react';
import type { PaginationProps } from '@portal/design-core';
import { cn } from '@portal/design-core';
import { Button } from '../Button';

export interface PaginationComponentProps
  extends PaginationProps,
    Omit<HTMLAttributes<HTMLElement>, 'onChange'> {
  onChange?: (page: number) => void;
}

const sizeClasses: Record<NonNullable<PaginationProps['size']>, string> = {
  sm: 'text-xs',
  md: 'text-sm',
  lg: 'text-base',
};

export const Pagination = forwardRef<HTMLElement, PaginationComponentProps>(
  (
    {
      page,
      totalPages,
      siblingCount = 1,
      showFirstLast = true,
      size = 'md',
      onChange,
      className,
      ...props
    },
    ref
  ) => {
    const getPageNumbers = () => {
      const pages: (number | 'ellipsis')[] = [];
      const leftSiblingIndex = Math.max(page - siblingCount, 1);
      const rightSiblingIndex = Math.min(page + siblingCount, totalPages);

      const showLeftEllipsis = leftSiblingIndex > 2;
      const showRightEllipsis = rightSiblingIndex < totalPages - 1;

      if (showFirstLast || !showLeftEllipsis) {
        pages.push(1);
      }

      if (showLeftEllipsis) {
        pages.push('ellipsis');
      }

      for (let i = leftSiblingIndex; i <= rightSiblingIndex; i++) {
        if (i !== 1 && i !== totalPages) {
          pages.push(i);
        }
      }

      if (showRightEllipsis) {
        pages.push('ellipsis');
      }

      if ((showFirstLast || !showRightEllipsis) && totalPages > 1) {
        pages.push(totalPages);
      }

      return pages;
    };

    const buttonSize = size === 'sm' ? 'xs' : size === 'lg' ? 'md' : 'sm';

    return (
      <nav
        ref={ref}
        role="navigation"
        aria-label="Pagination"
        className={cn('flex items-center gap-1', sizeClasses[size], className)}
        {...props}
      >
        <Button
          variant="ghost"
          size={buttonSize}
          disabled={page <= 1}
          onClick={() => onChange?.(page - 1)}
          aria-label="Previous page"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </Button>

        {getPageNumbers().map((pageNumber, index) => {
          if (pageNumber === 'ellipsis') {
            return (
              <span
                key={`ellipsis-${index}`}
                className="px-2 text-text-muted"
              >
                ...
              </span>
            );
          }

          return (
            <Button
              key={pageNumber}
              variant={page === pageNumber ? 'primary' : 'ghost'}
              size={buttonSize}
              onClick={() => onChange?.(pageNumber)}
              aria-label={`Page ${pageNumber}`}
              aria-current={page === pageNumber ? 'page' : undefined}
            >
              {pageNumber}
            </Button>
          );
        })}

        <Button
          variant="ghost"
          size={buttonSize}
          disabled={page >= totalPages}
          onClick={() => onChange?.(page + 1)}
          aria-label="Next page"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </Button>
      </nav>
    );
  }
);

Pagination.displayName = 'Pagination';

export default Pagination;
