import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Book, BookPreview, BookSearchResult, BookSuggestResult, Page } from './types'

export const booksApi = {
  suggest(query: string, limit = 8, signal?: AbortSignal) {
    const params = new URLSearchParams({ q: query, limit: String(limit) })
    return apiFetch<BookSuggestResult[]>(`${API_PATHS.books.suggest}?${params}`, { signal })
  },

  search(query: string, page = 0, size = 12, signal?: AbortSignal) {
    const params = new URLSearchParams({ q: query, page: String(page), size: String(size) })
    return apiFetch<Page<BookSearchResult>>(`${API_PATHS.books.search}?${params}`, { signal })
  },

  get(bookId: string) {
    return apiFetch<Book>(API_PATHS.books.byId(bookId))
  },

  externalPreview(externalId: string, signal?: AbortSignal) {
    const params = new URLSearchParams({ externalId })
    return apiFetch<BookPreview>(`${API_PATHS.books.externalPreview}?${params}`, { signal })
  },

  createManual(body: {
    title: string
    authors: string[]
    pageCount?: number
    isbn?: string
    coverUrl?: string
    description?: string
    externalId?: string
    source?: 'MANUAL' | 'OPEN_LIBRARY' | 'GOODREADS'
  }) {
    return apiFetch<Book>(API_PATHS.books.root, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
}
