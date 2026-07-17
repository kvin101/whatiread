import { useInfiniteQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useState } from 'react'
import { shelvesApi } from '../api/shelves'
import type { ReadingStatus, UserBook } from '../api/types'
import { BookCard } from '../components/books/BookCard'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { InfiniteScrollSentinel } from '../components/ui/InfiniteScrollSentinel'
import { STATUS_LABELS, QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

const VALID: ReadingStatus[] = ['TO_READ', 'READING', 'READ', 'DNF']
const PAGE_SIZE = 24

export function SystemShelfPage() {
  const { status } = useParams<{ status: string }>()
  const readingStatus = VALID.includes(status as ReadingStatus)
    ? (status as ReadingStatus)
    : 'TO_READ'
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const infinite = useInfiniteQuery({
    queryKey: [...QUERY_KEYS.shelves.system(readingStatus), 'infinite'],
    queryFn: ({ pageParam = 0 }) =>
      shelvesApi.listSystemBooks(readingStatus, pageParam, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (page) => (page.last ? undefined : page.number + 1),
  })

  const entries = infinite.data?.pages.flatMap((p) => p.content) ?? []
  const total = infinite.data?.pages[0]?.totalElements ?? entries.length

  return (
    <div>
      <Link
        to={APP_ROUTES.shelves}
        className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-accent mb-4"
      >
        <ArrowLeft className="h-4 w-4" />
        All shelves
      </Link>
      <h1 className="font-display text-3xl font-bold text-ink">
        {STATUS_LABELS[readingStatus]}
      </h1>
      <p className="mt-1 text-ink-muted">System shelf · {total} books</p>

      {infinite.isLoading && <BookSkeletonGrid className="mt-8" />}
      {!infinite.isLoading && (
        <>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {entries.map((entry: UserBook) => (
              <BookCard key={entry.id} entry={entry} onClick={() => setSelectedId(entry.id)} />
            ))}
          </div>
          <InfiniteScrollSentinel
            disabled={!infinite.hasNextPage || infinite.isFetchingNextPage}
            onIntersect={() => infinite.fetchNextPage()}
          />
          {infinite.isFetchingNextPage && <BookSkeletonGrid count={3} className="mt-4" />}
        </>
      )}

      <BookDetailDrawer
        userBookId={selectedId}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        onUpdated={() => infinite.refetch()}
      />
    </div>
  )
}
