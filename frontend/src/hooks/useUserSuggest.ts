import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { usersApi } from '../api/users'
import { QUERY_KEYS } from '../lib/constants'
import { useDebouncedValue } from './useDebouncedValue'

const MIN_LENGTH = 2
const DEBOUNCE_MS = 300
const SUGGEST_LIMIT = 8

export type UserSuggestScope = 'invite' | 'friends'

export function useUserSuggest(raw: string, scope: UserSuggestScope, enabled = true) {
  const debounced = useDebouncedValue(raw.trim(), DEBOUNCE_MS)
  const meetsMinLength = debounced.length >= MIN_LENGTH

  return useQuery({
    queryKey: QUERY_KEYS.users.suggest(scope, debounced),
    queryFn: ({ signal }) => usersApi.suggest(debounced, scope, SUGGEST_LIMIT, signal),
    enabled: enabled && meetsMinLength,
    staleTime: 30_000,
    placeholderData: keepPreviousData,
    retry: false,
  })
}
