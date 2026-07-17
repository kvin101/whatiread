import { useMemo, useState } from 'react'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Compass } from 'lucide-react'
import { shelvesApi } from '../api/shelves'
import type { ExploreShelf, ExploreShelfSource } from '../api/types'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { CloneShelfButton } from '../components/shelves/CloneShelfDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { FilterChips } from '../components/ui/FilterChips'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { InfiniteScrollSentinel } from '../components/ui/InfiniteScrollSentinel'
import { ListPageLayout } from '../components/layout/ListPageLayout'
import { PageHeader } from '../components/layout/PageHeader'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

type SourceFilter = 'ALL' | ExploreShelfSource
type SortOption = 'UPDATED' | 'BOOKS' | 'NAME'

const FILTER_TABS: SourceFilter[] = ['ALL', 'PUBLIC', 'FRIEND', 'SHARED']
const PAGE_SIZE = 24

const SOURCE_LABELS: Record<ExploreShelfSource, string> = {
  PUBLIC: 'Public',
  FRIEND: 'Friend',
  SHARED: 'Shared with you',
}

export function ExplorePage() {
  const [filter, setFilter] = useState<SourceFilter>('ALL')
  const [sort, setSort] = useState<SortOption>('UPDATED')

  const infinite = useInfiniteQuery({
    queryKey: [...QUERY_KEYS.shelves.explore, 'infinite'],
    queryFn: ({ pageParam = 0 }) => shelvesApi.explore(pageParam, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (page) => (page.last ? undefined : page.number + 1),
  })

  const shelves = useMemo(() => {
    const raw = infinite.data?.pages.flatMap((p) => p.content) ?? []
    const byId = new Map<string, ExploreShelf>()
    for (const shelf of raw) {
      if (!byId.has(shelf.id)) byId.set(shelf.id, shelf)
    }
    return [...byId.values()]
  }, [infinite.data?.pages])

  const filtered = useMemo(() => {
    let list = filter === 'ALL' ? shelves : shelves.filter((s) => s.source === filter)
    list = [...list]
    if (sort === 'UPDATED') {
      list.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    } else if (sort === 'BOOKS') {
      list.sort((a, b) => b.bookCount - a.bookCount)
    } else {
      list.sort((a, b) => a.name.localeCompare(b.name))
    }
    return list
  }, [shelves, filter, sort])

  const filterOptions = FILTER_TABS.map((tab) => ({
    value: tab,
    label: tab === 'ALL' ? 'All' : SOURCE_LABELS[tab],
  }))

  return (
    <ListPageLayout
      toolbar={
        <>
          <PageHeader title={copy.explore.title} />
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <FilterChips options={filterOptions} value={filter} onChange={setFilter} label="Shelf source" />
            <label className="flex items-center gap-2 text-sm text-ink-muted">
              Sort
              <select
                value={sort}
                onChange={(e) => setSort(e.target.value as SortOption)}
                className="rounded-lg border border-border bg-paper px-2 py-1 text-sm text-ink"
              >
                <option value="UPDATED">Recently updated</option>
                <option value="BOOKS">Most books</option>
                <option value="NAME">Name</option>
              </select>
            </label>
          </div>
        </>
      }
    >
      {infinite.isLoading && <BookSkeletonGrid />}

      {!infinite.isLoading && filtered.length === 0 && (
        <EmptyState
          icon={Compass}
          title={
            filter === 'ALL'
              ? copy.explore.empty.title
              : `No ${SOURCE_LABELS[filter as ExploreShelfSource].toLowerCase()} shelves`
          }
          description={
            filter === 'ALL'
              ? copy.explore.empty.description
              : 'Try another filter — or create a public shelf and add friends.'
          }
        />
      )}

      {!infinite.isLoading && filtered.length > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((shelf) => (
              <ShelfCard
                key={shelf.id}
                shelf={shelf}
                to={APP_ROUTES.shelf(shelf.id)}
                subtitle={`${SOURCE_LABELS[shelf.source]} · ${shelf.ownerDisplayName}`}
                showUpdated
                actions={<CloneShelfButton shelf={shelf} />}
              />
            ))}
          </div>
          <InfiniteScrollSentinel
            disabled={!infinite.hasNextPage || infinite.isFetchingNextPage}
            onIntersect={() => infinite.fetchNextPage()}
          />
          {infinite.isFetchingNextPage && <BookSkeletonGrid count={3} className="mt-4" />}
        </>
      )}
    </ListPageLayout>
  )
}
