import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { adminApi } from '../api/admin'
import { QUERY_KEYS } from '../lib/constants'
import { useDebouncedValue } from './useDebouncedValue'

const MIN_LENGTH = 2
const DEBOUNCE_MS = 300
const SUGGEST_LIMIT = 8

export function useAdminUserSuggest(raw: string, enabled = true) {
  const debounced = useDebouncedValue(raw.trim(), DEBOUNCE_MS)
  const meetsMinLength = debounced.length >= MIN_LENGTH

  return useQuery({
    queryKey: QUERY_KEYS.admin.suggest(debounced),
    queryFn: ({ signal }) => adminApi.suggestUsers(debounced, SUGGEST_LIMIT, signal),
    enabled: enabled && meetsMinLength,
    staleTime: 30_000,
    placeholderData: keepPreviousData,
    retry: false,
  })
}
