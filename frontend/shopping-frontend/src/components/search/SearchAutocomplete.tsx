/**
 * Search Autocomplete Component
 * 검색 입력 시 자동완성 드롭다운 표시
 */
import React, { useState, useRef, useEffect, useCallback } from 'react'
import { useSearchSuggest } from '@/hooks/useSearch'

interface SearchAutocompleteProps {
  value: string
  onChange: (value: string) => void
  onSearch: (keyword: string) => void
  placeholder?: string
}

const SearchAutocomplete: React.FC<SearchAutocompleteProps> = ({
  value,
  onChange,
  onSearch,
  placeholder = 'Search products...'
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)
  const { suggestions, isLoading } = useSearchSuggest(value)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Show dropdown when suggestions available
  useEffect(() => {
    if (suggestions.length > 0 && value.length >= 2) {
      setIsOpen(true)
      setActiveIndex(-1)
    } else {
      setIsOpen(false)
    }
  }, [suggestions, value])

  const handleSelect = useCallback((keyword: string) => {
    onChange(keyword)
    onSearch(keyword)
    setIsOpen(false)
    inputRef.current?.blur()
  }, [onChange, onSearch])

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen || suggestions.length === 0) {
      if (e.key === 'Enter') {
        onSearch(value)
      }
      return
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setActiveIndex(prev => (prev < suggestions.length - 1 ? prev + 1 : 0))
        break
      case 'ArrowUp':
        e.preventDefault()
        setActiveIndex(prev => (prev > 0 ? prev - 1 : suggestions.length - 1))
        break
      case 'Enter':
        e.preventDefault()
        if (activeIndex >= 0) {
          handleSelect(suggestions[activeIndex])
        } else {
          onSearch(value)
          setIsOpen(false)
        }
        break
      case 'Escape':
        setIsOpen(false)
        break
    }
  }

  return (
    <div ref={wrapperRef} className="relative w-full max-w-md">
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => { if (suggestions.length > 0) setIsOpen(true) }}
          placeholder={placeholder}
          className="w-full px-4 py-2 pr-10 border border-border-default rounded-lg bg-bg-card text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent"
        />
        {/* Search icon */}
        <button
          type="button"
          onClick={() => onSearch(value)}
          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-text-meta hover:text-brand-primary transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </button>
        {isLoading && (
          <div className="absolute right-10 top-1/2 -translate-y-1/2">
            <div className="w-4 h-4 border-2 border-brand-primary border-t-transparent rounded-full animate-spin" />
          </div>
        )}
      </div>

      {/* Dropdown */}
      {isOpen && suggestions.length > 0 && (
        <ul className="absolute z-50 w-full mt-1 bg-bg-card border border-border-default rounded-lg shadow-lg overflow-hidden">
          {suggestions.map((suggestion, index) => (
            <li
              key={suggestion}
              onClick={() => handleSelect(suggestion)}
              className={`px-4 py-2.5 cursor-pointer text-sm transition-colors ${
                index === activeIndex
                  ? 'bg-brand-primary/10 text-brand-primary'
                  : 'text-text-body hover:bg-bg-hover'
              }`}
            >
              <span className="flex items-center gap-2">
                <svg className="w-4 h-4 text-text-meta flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                {suggestion}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default SearchAutocomplete
