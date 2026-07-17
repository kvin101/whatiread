import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { API_PATHS } from '../api/paths'
import type { Shelf, ShelfBook } from '../api/types'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { PageHeader } from '../components/layout/PageHeader'
import { BookCover } from '../components/books/BookCover'
import { formatAuthors } from '../lib/utils'

export function PublicShelfPage() {
  const { ownerId, slug } = useParams<{ ownerId: string; slug: string }>()

  const { data: shelf, isLoading } = useQuery({
    queryKey: ['public-shelf', ownerId, slug],
    queryFn: () => apiFetch<Shelf>(API_PATHS.publicShelves.bySlug(ownerId!, slug!)),
    enabled: !!ownerId && !!slug,
  })

  const { data: books = [] } = useQuery({
    queryKey: ['public-shelf-books', ownerId, slug],
    queryFn: () => apiFetch<ShelfBook[]>(API_PATHS.publicShelves.books(ownerId!, slug!)),
    enabled: !!ownerId && !!slug && !!shelf,
  })

  if (!ownerId || !slug) return null
  if (isLoading) return <BookLoaderCenter className="min-h-[40vh]" />
  if (!shelf) return <p className="p-8 text-ink-muted">Shelf not found or not public.</p>

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-5xl pb-8">
        <PageHeader
          title={shelf.name}
          description={shelf.description ?? `Public shelf · ${books.length} books`}
        />
        <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {books.map((sb) => (
            <article
              key={sb.userBookId}
              className="flex gap-3 rounded-xl border border-border bg-paper-elevated p-3"
            >
              <BookCover title={sb.userBook.book.title} coverUrl={sb.userBook.book.coverUrl} size="sm" />
              <div className="min-w-0">
                <p className="font-medium text-ink line-clamp-2">{sb.userBook.book.title}</p>
                <p className="text-xs text-ink-muted">{formatAuthors(sb.userBook.book.authors)}</p>
              </div>
            </article>
          ))}
        </div>
      </div>
    </ScrollablePage>
  )
}
