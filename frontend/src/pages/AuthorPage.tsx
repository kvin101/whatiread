import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { authorsApi } from '../api/authors'
import { BookCard } from '../components/books/BookCard'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { UserAvatar } from '../components/ui/UserAvatar'
import { AuthorLink } from '../components/books/AuthorLink'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { PageHeader } from '../components/layout/PageHeader'
import { FilterChips } from '../components/ui/FilterChips'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

export function AuthorPage() {
  const { slug } = useParams<{ slug: string }>()
  const [tab, setTab] = useState<'library' | 'catalog'>('library')
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data: author, isLoading: authorLoading } = useQuery({
    queryKey: QUERY_KEYS.authors.detail(slug!),
    queryFn: () => authorsApi.get(slug!),
    enabled: !!slug,
  })

  const { data: libraryPage, isLoading: libLoading } = useQuery({
    queryKey: QUERY_KEYS.authors.library(slug!),
    queryFn: () => authorsApi.listLibrary(slug!, 0, 48),
    enabled: !!slug && tab === 'library',
  })

  const { data: booksPage, isLoading: booksLoading } = useQuery({
    queryKey: QUERY_KEYS.authors.books(slug!),
    queryFn: () => authorsApi.listBooks(slug!, 0, 48),
    enabled: !!slug && tab === 'catalog',
  })

  if (!slug) return null
  if (authorLoading) return <BookLoaderCenter className="min-h-[40vh]" />
  if (!author) return <p className="p-8 text-ink-muted">Author not found.</p>

  const libraryEntries = libraryPage?.content ?? []
  const catalogBooks = booksPage?.content ?? []

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-5xl pb-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
          {author.photoUrl ? (
            <img
              src={author.photoUrl}
              alt=""
              className="h-24 w-24 rounded-2xl object-cover mx-auto sm:mx-0"
            />
          ) : (
            <UserAvatar name={author.name} size="xl" className="h-24 w-24 text-2xl mx-auto sm:mx-0" />
          )}
          <div className="min-w-0 flex-1">
            <PageHeader title={author.name} />
            {author.bio && <p className="mt-2 max-w-2xl text-sm text-ink-muted">{author.bio}</p>}
            <div className="mt-4 flex flex-wrap gap-2 text-sm text-ink-muted">
              <span>{booksPage?.totalElements ?? catalogBooks.length} books in catalog</span>
              <span>·</span>
              <span>{libraryPage?.totalElements ?? libraryEntries.length} in your library</span>
              {author.id && (
                <>
                  <span>·</span>
                  <Link
                    to={`${APP_ROUTES.library}?authorId=${author.id}&authorName=${encodeURIComponent(author.name)}`}
                    className="text-accent hover:underline"
                  >
                    Filter my library
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="mt-6">
          <FilterChips
            options={[
              { value: 'library', label: 'Your books' },
              { value: 'catalog', label: 'All books' },
            ]}
            value={tab}
            onChange={(v) => setTab(v as 'library' | 'catalog')}
            label="View"
          />
        </div>

        {(libLoading || booksLoading) && <BookLoaderCenter className="mt-8 min-h-[20vh]" />}

        {tab === 'library' && !libLoading && (
          <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {libraryEntries.map((entry) => (
              <BookCard key={entry.id} entry={entry} onClick={() => setSelectedId(entry.id)} />
            ))}
          </div>
        )}

        {tab === 'catalog' && !booksLoading && (
          <ul className="mt-6 space-y-2">
            {catalogBooks.map((book) => (
              <li key={book.id}>
                <Link
                  to={APP_ROUTES.book(book.id)}
                  className="block rounded-xl border border-border px-4 py-3 hover:border-accent/30"
                >
                  <p className="font-medium text-ink">{book.title}</p>
                  <p className="text-sm text-ink-muted">
                    <AuthorLink names={book.authors} />
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      <BookDetailDrawer
        userBookId={selectedId}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
      />
    </ScrollablePage>
  )
}
