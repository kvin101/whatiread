import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { AppNotification } from './types'

export const notificationsApi = {
  list() {
    return apiFetch<AppNotification[]>(API_PATHS.notifications.root)
  },

  markRead(id: string) {
    return apiFetch<void>(API_PATHS.notifications.read(id), { method: 'POST' })
  },

  markAllRead() {
    return apiFetch<void>(API_PATHS.notifications.readAll, { method: 'POST' })
  },
}
