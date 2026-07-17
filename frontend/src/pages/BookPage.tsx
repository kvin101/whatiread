import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { booksApi } from '../api/books'
import { libraryApi } from '../api/library'
import { AuthorLink } from '../components/books/AuthorLink'
import { BookCover } from '../components/books/BookCover'
import { StarRating } from '../components/books/StarRating'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { STATUS_COLORS, STATUS_LABELS, QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { CommentThread } from '../components/comments/CommentThread'
import { formatRelativeTime } from '../lib/utils'
import { useState } from 'react'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'

export function BookPage() {
  const { bookId } = useParams<{ bookId: string }>()
  const [drawerOpen, setDrawerOpen] = useState(false)

  const { data: book, isLoading } = useQuery({
    queryKey: ['books', bookId],
    queryFn: () => booksApi.get(bookId!),
    enabled: !!bookId,
  })

  const { data: myEntry, refetch } = useQuery({
    queryKey: QUERY_KEYS.library.byBook(bookId!),
    queryFn: () => libraryApi.getByBookId(bookId!),
    enabled: !!bookId,
    retry: false,
  })

  if (!bookId) return null
  if (isLoading) return <BookLoaderCenter className="min-h-[40vh]" />
  if (!book) return <p className="p-8">Book not found.</p>

  return (
    <ScrollablePage>
      <div className="w-full pb-8">
        <div className="flex flex-col gap-6 sm:flex-row">
          <BookCover title={book.title} coverUrl={book.coverUrl} size="lg" className="mx-auto sm:mx-0" />
          <div className="min-w-0 flex-1">
            <h1 className="font-display text-2xl font-bold text-ink">{book.title}</h1>
            <p className="mt-2 text-ink-muted">
              <AuthorLink names={book.authors} />
            </p>
            {book.pageCount != null && (
              <p className="mt-1 text-sm text-ink-muted">{book.pageCount} pages</p>
            )}
            {book.averageRating != null && (book.ratingCount ?? 0) > 0 && (
              <p className="mt-2 text-sm text-ink-muted">
                Community ★ {book.averageRating} · {book.ratingCount} ratings
              </p>
            )}
            {book.description && (
              <p className="mt-4 text-sm text-ink-muted">{book.description}</p>
            )}
            <div className="mt-6 flex flex-wrap gap-2">
              {myEntry ? (
                <Button onClick={() => setDrawerOpen(true)}>Open in library</Button>
              ) : (
                <Button
                  onClick={async () => {
                    await libraryApi.add({ bookId: book.id, status: 'TO_READ' })
                    refetch()
                    setDrawerOpen(true)
                  }}
                >
                  Add to library
                </Button>
              )}
              <Link to={APP_ROUTES.library}>
                <Button variant="secondary">My Books</Button>
              </Link>
            </div>
          </div>
        </div>

        {myEntry && (
          <section className="mt-8 rounded-2xl border border-border bg-paper-elevated p-4">
            <h2 className="text-sm font-semibold text-ink">Your reading</h2>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <Badge className={STATUS_COLORS[myEntry.status]}>{STATUS_LABELS[myEntry.status]}</Badge>
              {myEntry.rating != null && (
                <StarRating value={myEntry.rating} readonly />
              )}
            </div>
            {myEntry.progressDisplay && (
              <p className="mt-2 text-sm text-ink-muted">{myEntry.progressDisplay}</p>
            )}
            {myEntry.progressPercent != null && (
              <div className="mt-2 h-1.5 max-w-xs overflow-hidden rounded-full bg-white/10">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-accent to-sage"
                  style={{ width: `${Math.min(100, myEntry.progressPercent)}%` }}
                />
              </div>
            )}
            <div className="mt-3 flex flex-wrap gap-4 text-xs text-ink-muted">
              {myEntry.startedAt && <span>Started {formatRelativeTime(myEntry.startedAt)}</span>}
              {myEntry.finishedAt && <span>Finished {formatRelativeTime(myEntry.finishedAt)}</span>}
            </div>
            {(myEntry.notes?.length ?? 0) > 0 && (
              <ul className="mt-4 space-y-2">
                {myEntry.notes!.map((note) => (
                  <li key={note.id} className="rounded-lg bg-paper px-3 py-2 text-sm text-ink">
                    {note.body}
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}

        <div className="mt-10">
          <CommentThread targetType="BOOK" targetId={book.id} />
        </div>
      </div>
      {myEntry && (
        <BookDetailDrawer
          userBookId={myEntry.id}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          onUpdated={() => refetch()}
        />
      )}
    </ScrollablePage>
  )
}
