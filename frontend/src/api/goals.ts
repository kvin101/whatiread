import { apiFetch } from './client'
import { API_PATHS } from './paths'

export interface ReadingGoal {
  year: number
  targetBooks: number
  booksRead: number
}

export const goalsApi = {
  get(year: number) {
    return apiFetch<ReadingGoal>(API_PATHS.goals.byYear(year))
  },

  upsert(year: number, targetBooks: number) {
    return apiFetch<ReadingGoal>(API_PATHS.goals.byYear(year), {
      method: 'PUT',
      body: JSON.stringify({ targetBooks }),
    })
  },
}
