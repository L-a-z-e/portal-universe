/**
 * Recent Keywords Component
 * 최근 검색어 목록 (삭제 가능)
 */
import React from 'react'
import { useRecentKeywords } from '@/hooks/useSearch'

interface RecentKeywordsProps {
  onSelect: (keyword: string) => void
}

const RecentKeywords: React.FC<RecentKeywordsProps> = ({ onSelect }) => {
  const { keywords, isLoading, deleteKeyword, clearAll } = useRecentKeywords()

  if (isLoading || keywords.length === 0) return null

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium text-text-meta flex items-center gap-1.5">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Recent Searches
        </h3>
        <button
          onClick={clearAll}
          className="text-xs text-text-meta hover:text-status-error transition-colors"
        >
          Clear all
        </button>
      </div>
      <div className="flex flex-wrap gap-2">
        {keywords.map((keyword) => (
          <span
            key={keyword}
            className="inline-flex items-center gap-1 px-3 py-1.5 text-sm rounded-full border border-border-default text-text-body group"
          >
            <button
              onClick={() => onSelect(keyword)}
              className="hover:text-brand-primary transition-colors"
            >
              {keyword}
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation()
                deleteKeyword(keyword)
              }}
              className="ml-0.5 text-text-meta hover:text-status-error transition-colors opacity-0 group-hover:opacity-100"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </span>
        ))}
      </div>
    </div>
  )
}

export default RecentKeywords
