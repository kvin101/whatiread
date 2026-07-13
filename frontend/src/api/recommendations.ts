import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Recommendation, RecommendationSuggestion, RecommendationTargetType } from './types'

export const recommendationsApi = {
  inbox() {
    return apiFetch<Recommendation[]>(API_PATHS.recommendations.inbox)
  },

  sent() {
    return apiFetch<Recommendation[]>(API_PATHS.recommendations.sent)
  },

  suggestions() {
    return apiFetch<RecommendationSuggestion[]>(API_PATHS.recommendations.suggestions)
  },

  create(body: {
    toUserId: string
    targetType?: RecommendationTargetType
    bookId?: string
    shelfId?: string
    message?: string
  }) {
    return apiFetch<Recommendation>(API_PATHS.recommendations.root, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  createBatch(body: {
    toUserId: string
    targetType?: RecommendationTargetType
    bookIds?: string[]
    shelfIds?: string[]
    message?: string
  }) {
    return apiFetch<Recommendation[]>(API_PATHS.recommendations.batch, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  accept(recommendationId: string) {
    return apiFetch<Recommendation>(API_PATHS.recommendations.accept(recommendationId), {
      method: 'POST',
    })
  },

  dismiss(recommendationId: string) {
    return apiFetch<Recommendation>(API_PATHS.recommendations.dismiss(recommendationId), {
      method: 'POST',
    })
  },

  delete(recommendationId: string) {
    return apiFetch<void>(API_PATHS.recommendations.delete(recommendationId), {
      method: 'DELETE',
    })
  },
}
