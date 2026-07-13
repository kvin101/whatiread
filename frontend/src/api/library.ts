import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Page, ReadingStatus, UserBook, UserBookNote } from './types'

export const libraryApi = {
  list(params?: {
    status?: ReadingStatus
    shelfId?: string
    q?: string
    page?: number
    size?: number
  }) {
    const q = new URLSearchParams()
    if (params?.status) q.set('status', params.status)
    if (params?.shelfId) q.set('shelfId', params.shelfId)
    if (params?.q?.trim()) q.set('q', params.q.trim())
    if (params?.page != null) q.set('page', String(params.page))
    if (params?.size != null) q.set('size', String(params.size))
    const qs = q.toString()
    return apiFetch<Page<UserBook>>(`${API_PATHS.library.root}${qs ? `?${qs}` : ''}`)
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
    return apiFetch<UserBookNote[]>(API_PATHS.library.notes(userBookId))
  },

  addNote(userBookId: string, body: string) {
    return apiFetch<UserBookNote>(API_PATHS.library.notes(userBookId), {
      method: 'POST',
      body: JSON.stringify({ body }),
    })
  },

  updateNote(userBookId: string, noteId: string, body: string) {
    return apiFetch<UserBookNote>(API_PATHS.library.note(userBookId, noteId), {
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
