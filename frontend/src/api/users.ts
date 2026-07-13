import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Shelf, UserProfile } from './types'

export const usersApi = {
  profile(userId: string) {
    return apiFetch<UserProfile>(API_PATHS.users.profile(userId))
  },

  shelves(userId: string) {
    return apiFetch<Shelf[]>(API_PATHS.users.shelves(userId))
  },
}
