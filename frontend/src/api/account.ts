import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { User } from './types'

export const accountApi = {
  updateProfile(body: {
    firstName?: string
    lastName?: string
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
}
