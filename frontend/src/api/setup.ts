import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { AuthResponse } from './types'

export interface SetupRequired {
  setupRequired: boolean
  registrationEnabled: boolean
}

export const setupApi = {
  required() {
    return apiFetch<SetupRequired>(API_PATHS.setup.required)
  },

  createAdmin(body: {
    email: string
    username: string
    password: string
    firstName: string
    lastName?: string
    registrationEnabled?: boolean
  }) {
    return apiFetch<AuthResponse>(API_PATHS.setup.admin, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
}
