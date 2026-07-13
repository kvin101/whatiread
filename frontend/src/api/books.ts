import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Book, BookSearchResult, Page } from './types'

export const booksApi = {
  search(query: string, page = 0, size = 12) {
    const params = new URLSearchParams({ q: query, page: String(page), size: String(size) })
    return apiFetch<Page<BookSearchResult>>(`${API_PATHS.books.search}?${params}`)
  },

  get(bookId: string) {
    return apiFetch<Book>(API_PATHS.books.byId(bookId))
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
