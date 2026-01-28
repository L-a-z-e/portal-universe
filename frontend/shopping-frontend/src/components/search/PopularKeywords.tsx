/**
 * Popular Keywords Component
 * 인기 검색어 chip 리스트
 */
import React from 'react'
import { usePopularKeywords } from '@/hooks/useSearch'

interface PopularKeywordsProps {
  onSelect: (keyword: string) => void
}

const PopularKeywords: React.FC<PopularKeywordsProps> = ({ onSelect }) => {
  const { keywords, isLoading } = usePopularKeywords()

  if (isLoading || keywords.length === 0) return null

  return (
    <div className="space-y-2">
      <h3 className="text-sm font-medium text-text-meta flex items-center gap-1.5">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
        </svg>
        Popular Searches
      </h3>
      <div className="flex flex-wrap gap-2">
        {keywords.map((keyword, index) => (
          <button
            key={keyword}
            onClick={() => onSelect(keyword)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border border-border-default text-text-body hover:bg-brand-primary/10 hover:text-brand-primary hover:border-brand-primary/30 transition-colors"
          >
            <span className="text-xs font-bold text-text-meta w-4">{index + 1}</span>
            {keyword}
          </button>
        ))}
      </div>
    </div>
  )
}

export default PopularKeywords
