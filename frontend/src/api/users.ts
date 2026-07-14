import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Shelf, UserProfile, UserSuggestResult } from './types'

export const usersApi = {
  profile(userId: string) {
    return apiFetch<UserProfile>(API_PATHS.users.profile(userId))
  },

  shelves(userId: string) {
    return apiFetch<Shelf[]>(API_PATHS.users.shelves(userId))
  },

  suggest(query: string, scope: 'invite' | 'friends', limit = 8, signal?: AbortSignal) {
    const params = new URLSearchParams({ q: query, scope, limit: String(limit) })
    return apiFetch<UserSuggestResult[]>(`${API_PATHS.users.suggest}?${params}`, { signal })
  },
}
