/**
 * Search Hooks
 * 검색 자동완성, 인기 검색어, 최근 검색어 관련 hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { searchApi } from '@/api'

/**
 * 검색 자동완성 (debounced)
 */
export function useSearchSuggest(keyword: string, delay = 300) {
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (!keyword || keyword.length < 2) {
      setSuggestions([])
      return
    }

    const timer = setTimeout(async () => {
      setIsLoading(true)
      try {
        const response = await searchApi.suggest(keyword)
        if (response.success) {
          setSuggestions(response.data)
        }
      } catch {
        setSuggestions([])
      } finally {
        setIsLoading(false)
      }
    }, delay)

    return () => clearTimeout(timer)
  }, [keyword, delay])

  return { suggestions, isLoading }
}

/**
 * 인기 검색어
 */
export function usePopularKeywords(size = 10) {
  const [keywords, setKeywords] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await searchApi.getPopularKeywords(size)
      if (response.success) {
        setKeywords(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch popular keywords'))
    } finally {
      setIsLoading(false)
    }
  }, [size])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { keywords, isLoading, error, refetch: fetch }
}

/**
 * 최근 검색어
 */
export function useRecentKeywords(size = 10) {
  const [keywords, setKeywords] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await searchApi.getRecentKeywords(size)
      if (response.success) {
        setKeywords(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch recent keywords'))
    } finally {
      setIsLoading(false)
    }
  }, [size])

  useEffect(() => {
    fetch()
  }, [fetch])

  const addKeyword = useCallback(async (keyword: string) => {
    try {
      await searchApi.addRecentKeyword(keyword)
      await fetch()
    } catch {
      // silently fail
    }
  }, [fetch])

  const deleteKeyword = useCallback(async (keyword: string) => {
    try {
      await searchApi.deleteRecentKeyword(keyword)
      setKeywords(prev => prev.filter(k => k !== keyword))
    } catch {
      // silently fail
    }
  }, [])

  const clearAll = useCallback(async () => {
    try {
      await searchApi.clearRecentKeywords()
      setKeywords([])
    } catch {
      // silently fail
    }
  }, [])

  return { keywords, isLoading, error, refetch: fetch, addKeyword, deleteKeyword, clearAll }
}
