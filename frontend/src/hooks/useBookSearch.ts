import { useCallback, useState } from 'react'
import { booksApi } from '../api/books'
import type { BookSearchResult } from '../api/types'
import { getApiErrorMessage } from '../lib/api'

export function useBookSearch(searchError = 'Search failed') {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<BookSearchResult[]>([])
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const search = useCallback(
    async (nextQuery = query) => {
      const trimmed = nextQuery.trim()
      if (!trimmed) return
      setSearching(true)
      setError(null)
      try {
        const page = await booksApi.search(trimmed)
        setResults(page.content)
      } catch (e) {
        setError(getApiErrorMessage(e, searchError))
        setResults([])
      } finally {
        setSearching(false)
      }
    },
    [query, searchError],
  )

  const reset = useCallback(() => {
    setQuery('')
    setResults([])
    setSearching(false)
    setError(null)
  }, [])

  return {
    query,
    setQuery,
    results,
    setResults,
    searching,
    error,
    setError,
    search,
    reset,
  }
}
