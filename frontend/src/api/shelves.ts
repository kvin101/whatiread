import { apiFetch } from './client'
import { API_PATHS, PUBLIC_API_PATHS } from './paths'
import type {
  ExploreShelf,
  Page,
  ReadingStatus,
  SharedShelf,
  Shelf,
  ShelfBook,
  ShelfEvent,
  ShelfMember,
  ShelfShareLink,
  ShelfVisibility,
  SystemShelf,
  UserBook,
} from './types'

export const shelvesApi = {
  listMine() {
    return apiFetch<Shelf[]>(API_PATHS.shelves.root)
  },

  listSystem() {
    return apiFetch<SystemShelf[]>(API_PATHS.shelves.system)
  },

  listSystemBooks(status: ReadingStatus, page = 0, size = 24) {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    return apiFetch<Page<UserBook>>(`${API_PATHS.shelves.systemBooks(status)}?${params}`)
  },

  get(shelfId: string) {
    return apiFetch<Shelf>(API_PATHS.shelves.byId(shelfId))
  },

  explore(page = 0, size = 24) {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    return apiFetch<Page<ExploreShelf>>(`${API_PATHS.shelves.explore}?${params}`)
  },

  create(body: {
    name: string
    visibility?: ShelfVisibility
    description?: string
    icon?: string
  }) {
    return apiFetch<Shelf>(API_PATHS.shelves.root, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  update(
    shelfId: string,
    body: {
      name?: string
      visibility?: ShelfVisibility
      description?: string
      icon?: string
    },
  ) {
    return apiFetch<Shelf>(API_PATHS.shelves.byId(shelfId), {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
  },

  remove(shelfId: string) {
    return apiFetch<void>(API_PATHS.shelves.byId(shelfId), { method: 'DELETE' })
  },

  listMembers(shelfId: string) {
    return apiFetch<ShelfMember[]>(API_PATHS.shelves.members(shelfId))
  },

  addMember(shelfId: string, body: { userId: string; role: 'ADMIN' | 'EDITOR' | 'VIEWER' }) {
    return apiFetch<ShelfMember>(API_PATHS.shelves.members(shelfId), {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  updateMember(
    shelfId: string,
    memberUserId: string,
    body: { role: 'ADMIN' | 'EDITOR' | 'VIEWER' },
  ) {
    return apiFetch<ShelfMember>(API_PATHS.shelves.member(shelfId, memberUserId), {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
  },

  removeMember(shelfId: string, memberUserId: string) {
    return apiFetch<void>(API_PATHS.shelves.member(shelfId, memberUserId), {
      method: 'DELETE',
    })
  },

  listEvents(shelfId: string, page = 0, size = 30) {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    return apiFetch<Page<ShelfEvent>>(`${API_PATHS.shelves.events(shelfId)}?${params}`)
  },

  clone(
    shelfId: string,
    body: { name: string; includeBooks?: boolean; visibility?: ShelfVisibility },
  ) {
    return apiFetch<Shelf>(API_PATHS.shelves.clone(shelfId), {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  listBooks(shelfId: string) {
    return apiFetch<ShelfBook[]>(API_PATHS.shelves.books(shelfId))
  },

  addBook(shelfId: string, body: { userBookId?: string; bookId?: string }) {
    return apiFetch<ShelfBook>(API_PATHS.shelves.books(shelfId), {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  removeBook(shelfId: string, userBookId: string) {
    return apiFetch<void>(API_PATHS.shelves.book(shelfId, userBookId), {
      method: 'DELETE',
    })
  },

  listShareLinks(shelfId: string) {
    return apiFetch<ShelfShareLink[]>(API_PATHS.shelves.shareLinks(shelfId))
  },

  createShareLink(shelfId: string, body?: { expiresAt?: string }) {
    return apiFetch<ShelfShareLink>(API_PATHS.shelves.shareLinks(shelfId), {
      method: 'POST',
      body: JSON.stringify(body ?? {}),
    })
  },

  revokeShareLink(shelfId: string, linkId: string) {
    return apiFetch<void>(API_PATHS.shelves.shareLink(shelfId, linkId), { method: 'DELETE' })
  },

  getShared(token: string) {
    return apiFetch<SharedShelf>(PUBLIC_API_PATHS.sharedShelf(token))
  },

  cloneFromShare(
    token: string,
    body: { name: string; includeBooks?: boolean; visibility?: ShelfVisibility },
  ) {
    return apiFetch<Shelf>(API_PATHS.shelves.shareClone(token), {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
}
