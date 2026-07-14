import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { User, UsernameAvailability } from './types'

export const accountApi = {
  updateProfile(body: {
    firstName?: string
    lastName?: string
    username?: string
    phoneNumber?: string
    avatarUrl?: string
    addressLine1?: string
    addressLine2?: string
    city?: string
    state?: string
    postalCode?: string
    country?: string
    writer?: boolean
    writerBio?: string
    acceptRecommendations?: boolean
  }) {
    return apiFetch<User>(API_PATHS.me, {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
  },

  checkUsernameAvailable(username: string) {
    const params = new URLSearchParams({ username })
    return apiFetch<UsernameAvailability>(`${API_PATHS.meUsernameAvailable}?${params}`)
  },

  uploadAvatar(file: File) {
    const body = new FormData()
    body.append('file', file)
    return apiFetch<User>(API_PATHS.meAvatar, {
      method: 'POST',
      body,
    })
  },

  removeAvatar() {
    return apiFetch<void>(API_PATHS.meAvatar, {
      method: 'DELETE',
    })
  },
}
