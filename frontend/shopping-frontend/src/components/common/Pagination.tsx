/**
 * Pagination Component
 * 페이지네이션 컴포넌트
 */
import React from 'react'
import type { PaginationProps } from '@/types/admin'

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  disabled = false
}) => {
  const getPageNumbers = (): number[] => {
    const pages: number[] = []
    const showPages = 5
    const half = Math.floor(showPages / 2)

    let start = Math.max(0, currentPage - half)
    let end = Math.min(totalPages - 1, start + showPages - 1)

    if (end - start < showPages - 1) {
      start = Math.max(0, end - showPages + 1)
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }

    return pages
  }

  const pageNumbers = getPageNumbers()

  return (
    <div className="flex items-center justify-center gap-2 mt-6">
      {/* Previous Button */}
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={disabled || currentPage === 0}
        className="px-4 py-2 rounded-lg font-medium bg-bg-subtle text-text-body hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        Previous
      </button>

      {/* Page Numbers */}
      <div className="flex items-center gap-1">
        {pageNumbers.map((page) => (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            disabled={disabled}
            className={`
              w-10 h-10 rounded-lg font-medium transition-all
              ${page === currentPage
                ? 'bg-brand-primary text-white shadow-sm'
                : 'bg-bg-subtle text-text-body hover:bg-bg-hover'
              }
              disabled:opacity-50 disabled:cursor-not-allowed
            `}
          >
            {page + 1}
          </button>
        ))}
      </div>

      {/* Next Button */}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={disabled || currentPage === totalPages - 1}
        className="px-4 py-2 rounded-lg font-medium bg-bg-subtle text-text-body hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        Next
      </button>
    </div>
  )
}

export default Pagination
