import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { BlockedUser, FriendRequest, FriendSummary } from './types'

export const friendsApi = {
  list() {
    return apiFetch<FriendSummary[]>(API_PATHS.friends.root)
  },

  listIncoming() {
    return apiFetch<FriendRequest[]>(API_PATHS.friends.requestsIncoming)
  },

  listOutgoing() {
    return apiFetch<FriendRequest[]>(API_PATHS.friends.requestsOutgoing)
  },

  sendRequest(body: { userId?: string; email?: string }) {
    return apiFetch<FriendRequest>(API_PATHS.friends.requests, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  accept(requestId: string) {
    return apiFetch<FriendRequest>(API_PATHS.friends.requestAccept(requestId), {
      method: 'POST',
    })
  },

  decline(requestId: string) {
    return apiFetch<FriendRequest>(API_PATHS.friends.requestDecline(requestId), {
      method: 'POST',
    })
  },

  cancel(requestId: string) {
    return apiFetch<void>(API_PATHS.friends.request(requestId), { method: 'DELETE' })
  },

  unfriend(friendUserId: string) {
    return apiFetch<void>(API_PATHS.friends.byId(friendUserId), { method: 'DELETE' })
  },

  block(userId: string) {
    return apiFetch<void>(API_PATHS.friends.block(userId), { method: 'POST' })
  },

  unblock(userId: string) {
    return apiFetch<void>(API_PATHS.friends.block(userId), { method: 'DELETE' })
  },

  listBlocked() {
    return apiFetch<BlockedUser[]>(API_PATHS.friends.blocked)
  },
}
