import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Compass } from 'lucide-react'
import { shelvesApi } from '../api/shelves'
import type { ExploreShelfSource } from '../api/types'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { CloneShelfButton } from '../components/shelves/CloneShelfDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { FilterChips } from '../components/ui/FilterChips'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { PageHeader } from '../components/layout/PageHeader'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

type SourceFilter = 'ALL' | ExploreShelfSource

const FILTER_TABS: SourceFilter[] = ['ALL', 'PUBLIC', 'FRIEND', 'SHARED']

const SOURCE_LABELS: Record<ExploreShelfSource, string> = {
  PUBLIC: 'Public',
  FRIEND: 'Friend',
  SHARED: 'Shared with you',
}

export function ExplorePage() {
  const [filter, setFilter] = useState<SourceFilter>('ALL')

  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.explore,
    queryFn: () => shelvesApi.explore(0, 48),
  })

  const shelves = data?.content ?? []

  const filtered = useMemo(() => {
    if (filter === 'ALL') return shelves
    return shelves.filter((s) => s.source === filter)
  }, [shelves, filter])

  const filterOptions = FILTER_TABS.map((tab) => ({
    value: tab,
    label: tab === 'ALL' ? 'All' : SOURCE_LABELS[tab],
  }))

  return (
    <div>
      <PageHeader
        eyebrow="Wander"
        title={copy.explore.title}
        description={copy.explore.description}
      />

      <div className="mt-6">
        <FilterChips options={filterOptions} value={filter} onChange={setFilter} label="Shelf source" />
      </div>

      {isLoading && <BookSkeletonGrid className="mt-12" />}

      {!isLoading && filtered.length === 0 && (
        <EmptyState
          className="mt-8"
          icon={Compass}
          title={
            filter === 'ALL'
              ? copy.explore.empty.title
              : `No ${SOURCE_LABELS[filter as ExploreShelfSource].toLowerCase()} shelves`
          }
          description={
            filter === 'ALL'
              ? copy.explore.empty.description
              : 'Try another filter — or bribe a friend to share their shelf.'
          }
        />
      )}

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((shelf) => (
          <ShelfCard
            key={shelf.id}
            shelf={shelf}
            to={APP_ROUTES.shelf(shelf.id)}
            subtitle={SOURCE_LABELS[shelf.source]}
            showUpdated
            actions={<CloneShelfButton shelf={shelf} />}
          />
        ))}
      </div>
    </div>
  )
}
