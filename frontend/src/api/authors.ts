import { apiFetch } from './client'
import { API_PATHS } from './paths'
import type { Author, Book, Page, UserBook } from './types'

export const authorsApi = {
  get(slug: string) {
    return apiFetch<Author>(API_PATHS.authors.bySlug(slug))
  },

  listBooks(slug: string, page = 0, size = 24) {
    const q = new URLSearchParams({ page: String(page), size: String(size) })
    return apiFetch<Page<Book>>(`${API_PATHS.authors.books(slug)}?${q}`)
  },

  listLibrary(slug: string, page = 0, size = 24) {
    const q = new URLSearchParams({ page: String(page), size: String(size) })
    return apiFetch<Page<UserBook>>(`${API_PATHS.authors.library(slug)}?${q}`)
  },
}
