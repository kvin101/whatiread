import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { accountApi } from '../api/account'
import { authApi } from '../api/auth'
import type { UsernameAvailability } from '../api/types'
import { QUERY_KEYS } from '../lib/constants'
import { UsernameUtils } from '../lib/username'

type Options = {
  enabled?: boolean
  /** When set, uses authenticated /me check and allows keeping the current handle. */
  currentUser?: boolean
  /** When unchanged from this value, skip the availability check (e.g. profile settings). */
  savedUsername?: string
}

export function useUsernameAvailability(raw: string, options: Options = {}) {
  const [debounced, setDebounced] = useState('')
  const enabled = options.enabled ?? true

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(raw.trim()), 300)
    return () => clearTimeout(timer)
  }, [raw])

  const meetsMinLength = debounced.length >= UsernameUtils.MIN_LENGTH
  const unchanged =
    options.savedUsername != null &&
    options.savedUsername !== '' &&
    UsernameUtils.equals(debounced, options.savedUsername)

  return useQuery({
    queryKey: [...QUERY_KEYS.usernameAvailability(debounced, options.currentUser)],
    queryFn: (): Promise<UsernameAvailability> =>
      options.currentUser
        ? accountApi.checkUsernameAvailable(debounced)
        : authApi.checkUsernameAvailable(debounced),
    enabled: enabled && meetsMinLength && !unchanged,
    staleTime: 30_000,
    retry: false,
  })
}
