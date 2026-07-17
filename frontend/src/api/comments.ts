import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Comment, CommentTargetType, Page } from './types'

export const commentsApi = {
  list(targetType: CommentTargetType, targetId: string, page = 0, size = 50) {
    const params = new URLSearchParams({
      targetType,
      targetId,
      page: String(page),
      size: String(size),
    })
    return apiFetch<Page<Comment>>(`${API_PATHS.comments.root}?${params}`)
  },

  create(body: { targetType: CommentTargetType; targetId: string; body: string }) {
    return apiFetch<Comment>(API_PATHS.comments.root, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  update(commentId: string, body: string) {
    return apiFetch<Comment>(API_PATHS.comments.byId(commentId), {
      method: 'PATCH',
      body: JSON.stringify({ body }),
    })
  },

  remove(commentId: string) {
    return apiFetch<void>(API_PATHS.comments.byId(commentId), { method: 'DELETE' })
  },

  report(commentId: string, reason?: string) {
    return apiFetch<void>(API_PATHS.comments.report(commentId), {
      method: 'POST',
      body: JSON.stringify({ reason: reason ?? '' }),
    })
  },
}
