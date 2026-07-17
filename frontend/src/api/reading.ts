import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { ReadingGoal, ReadingStats, ReadingStreak } from './types'

export const readingApi = {
  getGoal(year?: number) {
    const q = year != null ? `?year=${year}` : ''
    return apiFetch<ReadingGoal>(`${API_PATHS.reading.goal}${q}`)
  },

  upsertGoal(body: { year?: number; targetBooks?: number | null; targetPages?: number | null }) {
    return apiFetch<ReadingGoal>(API_PATHS.reading.goal, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
  },

  getStats(year?: number) {
    const q = year != null ? `?year=${year}` : ''
    return apiFetch<ReadingStats>(`${API_PATHS.reading.stats}${q}`)
  },

  getStreak() {
    return apiFetch<ReadingStreak>(API_PATHS.reading.streak)
  },
}
