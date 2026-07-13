import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useState } from 'react'
import { shelvesApi } from '../api/shelves'
import type { ReadingStatus } from '../api/types'
import { BookCard } from '../components/books/BookCard'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { STATUS_LABELS, QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

const VALID: ReadingStatus[] = ['TO_READ', 'READING', 'READ', 'DNF']

export function SystemShelfPage() {
  const { status } = useParams<{ status: string }>()
  const readingStatus = VALID.includes(status as ReadingStatus)
    ? (status as ReadingStatus)
    : 'TO_READ'
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data, isLoading, refetch } = useQuery({
    queryKey: QUERY_KEYS.shelves.system(readingStatus),
    queryFn: () => shelvesApi.listSystemBooks(readingStatus, 0, 100),
  })

  const entries = data?.content ?? []

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
      <p className="mt-1 text-ink-muted">System shelf · {entries.length} books</p>

      {isLoading && <BookSkeletonGrid className="mt-8" />}
      {!isLoading && (
        <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {entries.map((entry) => (
            <BookCard key={entry.id} entry={entry} onClick={() => setSelectedId(entry.id)} />
          ))}
        </div>
      )}

      <BookDetailDrawer
        userBookId={selectedId}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        onUpdated={() => refetch()}
      />
    </div>
  )
}
