import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { AdminCreateUserRequest, AdminUser, Page } from './types'

export const adminApi = {
  listUsers(page = 0, size = 20, q?: string) {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (q?.trim()) params.set('q', q.trim())
    return apiFetch<Page<AdminUser>>(`${API_PATHS.admin.users}?${params}`)
  },

  createUser(body: AdminCreateUserRequest) {
    return apiFetch<AdminUser>(API_PATHS.admin.users, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  resetPassword(userId: string, password: string) {
    return apiFetch<AdminUser>(API_PATHS.admin.userPassword(userId), {
      method: 'PATCH',
      body: JSON.stringify({ password }),
    })
  },

  setEnabled(userId: string, enabled: boolean) {
    return apiFetch<AdminUser>(API_PATHS.admin.userEnabled(userId), {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },

  deleteUser(userId: string) {
    return apiFetch<void>(API_PATHS.admin.user(userId), { method: 'DELETE' })
  },

  setRegistrationEnabled(enabled: boolean) {
    return apiFetch<void>(API_PATHS.admin.registration, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },
}
