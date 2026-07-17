import { useMemo, useState } from 'react'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { activityApi } from '../api/activity'
import type { ActivityItem } from '../api/types'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { PageHeader } from '../components/layout/PageHeader'
import { FilterChips } from '../components/ui/FilterChips'
import { InfiniteScrollSentinel } from '../components/ui/InfiniteScrollSentinel'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { EmptyState } from '../components/ui/EmptyState'
import { QUERY_KEYS } from '../lib/constants'
import { formatRelativeTime } from '../lib/utils'
import { APP_ROUTES } from '../api/paths'
import { Activity } from 'lucide-react'

type ActivityFilter = 'ALL' | 'BOOKS' | 'SHELVES'

function activityText(item: ActivityItem): string {
  const p = item.payload ?? {}
  switch (item.eventType) {
    case 'BOOK_ADDED':
      return `added “${p.bookTitle ?? 'a book'}” to ${item.shelfName}`
    case 'BOOK_REMOVED':
      return `removed “${p.bookTitle ?? 'a book'}” from ${item.shelfName}`
    case 'SHELF_CREATED':
      return `created shelf “${p.name ?? item.shelfName}”`
    default:
      return `updated ${item.shelfName}`
  }
}

function matchesFilter(item: ActivityItem, filter: ActivityFilter): boolean {
  if (filter === 'ALL') return true
  if (filter === 'BOOKS') {
    return item.eventType === 'BOOK_ADDED' || item.eventType === 'BOOK_REMOVED'
  }
  return item.eventType === 'SHELF_CREATED' || item.eventType === 'SHELF_UPDATED'
}

export function ActivityPage() {
  const [filter, setFilter] = useState<ActivityFilter>('ALL')

  const { data, isLoading, isError, refetch, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: QUERY_KEYS.activity.infinite,
    queryFn: ({ pageParam }) => activityApi.list({ cursor: pageParam as string | undefined, limit: 20 }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) => (last.hasMore ? last.nextCursor ?? undefined : undefined),
  })

  const items = useMemo(() => {
    const raw = data?.pages.flatMap((p) => p.items ?? []) ?? []
    return raw.filter((item) => item?.id && matchesFilter(item, filter))
  }, [data?.pages, filter])

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-2xl">
        <PageHeader title="Activity" description="Updates from friends' shelves" />
        <div className="mt-4">
          <FilterChips
            options={[
              { value: 'ALL', label: 'All' },
              { value: 'BOOKS', label: 'Books' },
              { value: 'SHELVES', label: 'Shelves' },
            ]}
            value={filter}
            onChange={(v) => setFilter(v as ActivityFilter)}
            label="Filter"
          />
        </div>
        {isLoading && <BookLoaderCenter className="min-h-[30vh]" />}
        {isError && (
          <EmptyState
            icon={Activity}
            title="Could not load activity"
            description="Try again in a moment."
            action={
              <button
                type="button"
                className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-void"
                onClick={() => refetch()}
              >
                Retry
              </button>
            }
          />
        )}
        {!isLoading && !isError && items.length === 0 && (
          <EmptyState
            icon={Activity}
            title="No activity yet"
            description="Friend shelf updates will appear here."
          />
        )}
        <ul className="mt-6 space-y-3">
          {!isError &&
            items.map((item) => (
            <li key={item.id}>
              <Link
                to={APP_ROUTES.shelf(item.shelfId)}
                className="block rounded-xl border border-border bg-paper-elevated px-4 py-3 hover:border-accent/30"
              >
                <p className="text-sm text-ink">
                  <span className="font-medium">{item.actorDisplayName}</span> {activityText(item)}
                </p>
                <p className="mt-0.5 text-xs text-ink-muted">{formatRelativeTime(item.createdAt)}</p>
              </Link>
            </li>
          ))}
        </ul>
        <InfiniteScrollSentinel
          disabled={!hasNextPage || isFetchingNextPage}
          onIntersect={() => fetchNextPage()}
        />
      </div>
    </ScrollablePage>
  )
}
