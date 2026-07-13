import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { AuthResponse, User } from './types'

export const authApi = {
  register(body: {
    email: string
    password: string
    firstName: string
    lastName?: string
    phoneNumber?: string
  }) {
    return apiFetch<AuthResponse>(API_PATHS.auth.register, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  login(body: { email: string; password: string }) {
    return apiFetch<AuthResponse>(API_PATHS.auth.login, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  logout(refreshToken: string) {
    return apiFetch<void>(API_PATHS.auth.logout, {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    })
  },

  me() {
    return apiFetch<User>(API_PATHS.me)
  },
}
