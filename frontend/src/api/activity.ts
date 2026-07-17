import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { ActivityItem, CursorPage } from './types'

export const activityApi = {
  list(params?: { cursor?: string; limit?: number }) {
    const q = new URLSearchParams()
    if (params?.cursor) q.set('cursor', params.cursor)
    if (params?.limit != null) q.set('limit', String(params.limit))
    const qs = q.toString()
    return apiFetch<CursorPage<ActivityItem>>(`${API_PATHS.activity}${qs ? `?${qs}` : ''}`)
  },
}
