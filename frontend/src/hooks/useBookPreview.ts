import { useQuery } from '@tanstack/react-query'
import { booksApi } from '../api/books'
import type { Book, BookPreview, BookSearchResult } from '../api/types'
import { QUERY_KEYS } from '../lib/constants'

function fromSearchResult(book: BookSearchResult): BookPreview {
  return {
    title: book.title,
    authors: book.authors,
    isbn: book.isbn,
    pageCount: book.pageCount,
    coverUrl: book.coverUrl,
    source: book.source,
    externalId: book.externalId,
  }
}

function fromCatalogBook(book: Book): BookPreview {
  return {
    title: book.title,
    subtitle: book.subtitle,
    authors: book.authors,
    isbn: book.isbn,
    pageCount: book.pageCount,
    coverUrl: book.coverUrl,
    description: book.description,
    averageRating: book.averageRating,
    ratingCount: book.ratingCount,
  }
}

function mergePreview(remote: BookPreview, fallback: BookSearchResult): BookPreview {
  return {
    title: remote.title || fallback.title,
    subtitle: remote.subtitle,
    authors: fallback.authors.length > 0 ? fallback.authors : remote.authors,
    isbn: remote.isbn ?? fallback.isbn,
    pageCount: remote.pageCount ?? fallback.pageCount,
    coverUrl: remote.coverUrl ?? fallback.coverUrl,
    description: remote.description,
    publishYear: remote.publishYear,
    subjects: remote.subjects,
    averageRating: remote.averageRating,
    ratingCount: remote.ratingCount,
    source: remote.source ?? fallback.source,
    externalId: remote.externalId ?? fallback.externalId,
  }
}

async function fetchPreview(book: BookSearchResult): Promise<BookPreview> {
  if (book.id?.trim()) {
    return fromCatalogBook(await booksApi.get(book.id))
  }
  if (book.externalId?.trim()) {
    const remote = await booksApi.externalPreview(book.externalId)
    return mergePreview(remote, book)
  }
  return fromSearchResult(book)
}

export function useBookPreview(book: BookSearchResult | null, enabled: boolean) {
  return useQuery({
    queryKey: QUERY_KEYS.books.preview(book?.id, book?.externalId),
    queryFn: () => fetchPreview(book!),
    enabled: enabled && !!book,
    staleTime: 60_000,
    retry: false,
  })
}
