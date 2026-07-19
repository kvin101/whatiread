import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { CursorPage, LibrarySortParam, Page, ReadingStatus, UserBook } from './types'

const SORT_MAP: Record<string, LibrarySortParam> = {
  updated: 'UPDATED_DESC',
  title: 'TITLE_ASC',
  added: 'UPDATED_DESC',
  author: 'TITLE_ASC',
  rating: 'FINISHED_DESC',
}

export function toLibrarySortParam(sort: string): LibrarySortParam {
  return SORT_MAP[sort] ?? 'UPDATED_DESC'
}

/** Backend cursor mode requires a cursor query param; space means first page. */
const CURSOR_FIRST_PAGE = ' '

function normalizeLibraryListPage(raw: unknown): CursorPage<UserBook> {
  if (raw && typeof raw === 'object') {
    const page = raw as Record<string, unknown>
    if (Array.isArray(page.items)) {
      return {
        items: page.items as UserBook[],
        nextCursor: (page.nextCursor as string | null | undefined) ?? null,
        hasMore: Boolean(page.hasMore),
      }
    }
    if (Array.isArray(page.content)) {
      return {
        items: page.content as UserBook[],
        nextCursor: null,
        hasMore: page.last === false,
      }
    }
  }
  return { items: [], nextCursor: null, hasMore: false }
}

export const libraryApi = {
  list(params?: {
    status?: ReadingStatus
    shelfId?: string
    authorId?: string
    q?: string
    sort?: LibrarySortParam
    page?: number
    size?: number
  }) {
    const q = new URLSearchParams()
    if (params?.status) q.set('status', params.status)
    if (params?.shelfId) q.set('shelfId', params.shelfId)
    if (params?.authorId) q.set('authorId', params.authorId)
    if (params?.q?.trim()) q.set('q', params.q.trim())
    if (params?.sort) q.set('sort', params.sort)
    if (params?.page != null) q.set('page', String(params.page))
    if (params?.size != null) q.set('size', String(params.size))
    const qs = q.toString()
    return apiFetch<Page<UserBook>>(`${API_PATHS.library.root}${qs ? `?${qs}` : ''}`)
  },

  listCursor(params?: {
    status?: ReadingStatus
    shelfId?: string
    authorId?: string
    q?: string
    sort?: LibrarySortParam
    cursor?: string
    limit?: number
  }) {
    const q = new URLSearchParams()
    if (params?.status) q.set('status', params.status)
    if (params?.shelfId) q.set('shelfId', params.shelfId)
    if (params?.authorId) q.set('authorId', params.authorId)
    if (params?.q?.trim()) q.set('q', params.q.trim())
    if (params?.sort) q.set('sort', params.sort)
    q.set('cursor', params?.cursor?.trim() ? params.cursor : CURSOR_FIRST_PAGE)
    if (params?.limit != null) q.set('limit', String(params.limit))
    const qs = q.toString()
    return apiFetch<unknown>(`${API_PATHS.library.root}?${qs}`).then(normalizeLibraryListPage)
  },

  get(userBookId: string) {
    return apiFetch<UserBook>(API_PATHS.library.byId(userBookId))
  },

  getByBookId(bookId: string) {
    return apiFetch<UserBook>(API_PATHS.library.byBook(bookId))
  },

  add(body: { bookId: string; status?: ReadingStatus }) {
    return apiFetch<UserBook>(API_PATHS.library.root, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  update(
    userBookId: string,
    body: {
      status?: ReadingStatus
      rating?: number
      clearRating?: boolean
      progressPages?: number
      progressPercent?: number
    },
  ) {
    return apiFetch<UserBook>(API_PATHS.library.byId(userBookId), {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
  },

  remove(userBookId: string) {
    return apiFetch<void>(API_PATHS.library.byId(userBookId), { method: 'DELETE' })
  },

  listNotes(userBookId: string) {
    return apiFetch<import('./types').UserBookNote[]>(API_PATHS.library.notes(userBookId))
  },

  addNote(userBookId: string, body: string) {
    return apiFetch<import('./types').UserBookNote>(API_PATHS.library.notes(userBookId), {
      method: 'POST',
      body: JSON.stringify({ body }),
    })
  },

  updateNote(userBookId: string, noteId: string, body: string) {
    return apiFetch<import('./types').UserBookNote>(API_PATHS.library.note(userBookId, noteId), {
      method: 'PATCH',
      body: JSON.stringify({ body }),
    })
  },

  deleteNote(userBookId: string, noteId: string) {
    return apiFetch<void>(API_PATHS.library.note(userBookId, noteId), {
      method: 'DELETE',
    })
  },
}
