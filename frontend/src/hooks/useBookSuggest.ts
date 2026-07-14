import { useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { booksApi } from '../api/books'
import { QUERY_KEYS } from '../lib/constants'

const MIN_LENGTH = 2
const DEBOUNCE_MS = 300
const SUGGEST_LIMIT = 8

export function useBookSuggest(raw: string, enabled = true) {
  const [debounced, setDebounced] = useState('')

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(raw.trim()), DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [raw])

  const meetsMinLength = debounced.length >= MIN_LENGTH

  return useQuery({
    queryKey: QUERY_KEYS.books.suggest(debounced),
    queryFn: ({ signal }) => booksApi.suggest(debounced, SUGGEST_LIMIT, signal),
    enabled: enabled && meetsMinLength,
    staleTime: 30_000,
    placeholderData: keepPreviousData,
    retry: false,
  })
}
