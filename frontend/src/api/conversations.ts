import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Conversation, CursorPage, Message } from './types'

export const conversationsApi = {
  list() {
    return apiFetch<Conversation[]>(API_PATHS.conversations.root)
  },

  listGroups() {
    return apiFetch<Conversation[]>(API_PATHS.conversations.groups)
  },

  unreadCount() {
    return apiFetch<number>(API_PATHS.conversations.unreadCount)
  },

  withFriend(friendUserId: string) {
    return apiFetch<Conversation>(API_PATHS.conversations.withFriend(friendUserId), {
      method: 'POST',
    })
  },

  createGroup(name: string, memberUserIds: string[]) {
    return apiFetch<Conversation>(API_PATHS.conversations.groups, {
      method: 'POST',
      body: JSON.stringify({ name, memberUserIds }),
    })
  },

  renameGroup(conversationId: string, name: string) {
    return apiFetch<Conversation>(API_PATHS.conversations.byId(conversationId), {
      method: 'PUT',
      body: JSON.stringify({ name }),
    })
  },

  addMember(conversationId: string, userId: string) {
    return apiFetch<Conversation>(API_PATHS.conversations.members(conversationId), {
      method: 'POST',
      body: JSON.stringify({ userId }),
    })
  },

  removeMember(conversationId: string, memberUserId: string) {
    return apiFetch<void>(API_PATHS.conversations.member(conversationId, memberUserId), {
      method: 'DELETE',
    })
  },

  leave(conversationId: string) {
    return apiFetch<void>(API_PATHS.conversations.leave(conversationId), { method: 'POST' })
  },

  messages(conversationId: string, params?: { cursor?: string; limit?: number }) {
    const q = new URLSearchParams()
    if (params?.cursor) q.set('cursor', params.cursor)
    if (params?.limit != null) q.set('limit', String(params.limit))
    const qs = q.toString()
    return apiFetch<CursorPage<Message>>(
      `${API_PATHS.conversations.messages(conversationId)}${qs ? `?${qs}` : ''}`,
    )
  },

  markRead(conversationId: string) {
    return apiFetch<void>(API_PATHS.conversations.markRead(conversationId), { method: 'POST' })
  },
}
