import { useQuery } from '@tanstack/react-query'
import { BookMarked, Compass, Plus, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { libraryApi } from '../api/library'
import { shelvesApi } from '../api/shelves'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { CloneShelfButton } from '../components/shelves/CloneShelfDialog'
import type { ReadingStatus } from '../api/types'
import { BookCard } from '../components/books/BookCard'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { BookSearchModal } from '../components/books/BookSearchModal'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { FilterChips } from '../components/ui/FilterChips'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { PageHeader } from '../components/layout/PageHeader'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { STATUS_LABELS } from '../lib/constants'
import { copy } from '../lib/copy'
import { formatAuthors } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

const FILTERS: (ReadingStatus | 'ALL')[] = ['ALL', 'TO_READ', 'READING', 'READ', 'DNF']

export function LibraryPage() {
  const [filter, setFilter] = useState<ReadingStatus | 'ALL'>('ALL')
  const [shelfFilter, setShelfFilter] = useState<string>('ALL')
  const [search, setSearch] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const searchTrimmed = search.trim()

  const { data: myShelves = [] } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
  })

  const { data: explorePage } = useQuery({
    queryKey: QUERY_KEYS.shelves.exploreHint,
    queryFn: () => shelvesApi.explore(0, 6),
  })
  const publicShelves = explorePage?.content ?? []

  const { data, refetch, isLoading } = useQuery({
    queryKey: QUERY_KEYS.library.list(filter, shelfFilter, searchTrimmed),
    queryFn: () =>
      libraryApi.list({
        status: shelfFilter === 'ALL' && filter !== 'ALL' ? filter : undefined,
        shelfId: shelfFilter === 'ALL' ? undefined : shelfFilter,
        q: searchTrimmed || undefined,
        size: 100,
      }),
  })

  const entries = useMemo(() => {
    const raw = data?.content ?? []
    if (!searchTrimmed) return raw
    const needle = searchTrimmed.toLowerCase()
    return raw.filter(
      (e) =>
        e.book.title.toLowerCase().includes(needle) ||
        formatAuthors(e.book.authors).toLowerCase().includes(needle) ||
        (e.book.isbn?.toLowerCase().includes(needle) ?? false),
    )
  }, [data?.content, searchTrimmed])

  const filterOptions = FILTERS.map((f) => ({
    value: f,
    label: f === 'ALL' ? 'All' : STATUS_LABELS[f],
  }))

  return (
    <div>
      <PageHeader
        eyebrow="Home base"
        title={copy.library.title}
        description={copy.library.description(data?.totalElements ?? 0)}
        action={
          <Button onClick={() => setSearchOpen(true)}>
            <Plus className="h-4 w-4" />
            {copy.library.addBook}
          </Button>
        }
      />

      <div className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <FilterChips
          options={filterOptions}
          value={filter}
          onChange={setFilter}
          label="Reading status"
        />
        <div className="flex items-center gap-2">
          <label htmlFor="shelfFilter" className="text-sm text-ink-muted shrink-0">
            Shelf
          </label>
          <Select
            id="shelfFilter"
            value={shelfFilter}
            onChange={(e) => setShelfFilter(e.target.value)}
            className="min-w-[160px]"
          >
            <option value="ALL">All shelves</option>
            {myShelves.map((s) => (
              <option key={s.id} value={s.id}>
                {s.icon ? `${s.icon} ` : ''}{s.name}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <div className="mt-4 relative max-w-md">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted pointer-events-none" />
        <Input
          type="search"
          placeholder={copy.library.searchPlaceholder}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10"
          aria-label="Search library"
        />
      </div>

      {publicShelves.length > 0 && (
        <section className="mt-10">
          <div className="flex items-center justify-between gap-4">
            <h2 className="font-display text-xl font-semibold text-ink">{copy.library.discover}</h2>
            <Link
              to={APP_ROUTES.explore}
              className="inline-flex items-center gap-1 text-sm font-medium text-accent hover:underline shrink-0"
            >
              <Compass className="h-4 w-4" />
              {copy.library.seeAll}
            </Link>
          </div>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {publicShelves.map((s) => (
              <ShelfCard
                key={s.id}
                shelf={s}
                to={APP_ROUTES.shelf(s.id)}
                subtitle={`${s.source === 'PUBLIC' ? 'Public' : s.source === 'FRIEND' ? 'Friend' : 'Shared'} · ${s.ownerDisplayName}`}
                actions={<CloneShelfButton shelf={s} />}
              />
            ))}
          </div>
        </section>
      )}

      {isLoading && <BookSkeletonGrid className="mt-8" />}

      {!isLoading && entries.length === 0 && (
        <EmptyState
          className="mt-8"
          icon={BookMarked}
          title={searchTrimmed ? copy.library.noResults.title : copy.library.empty.title}
          description={
            searchTrimmed ? copy.library.noResults.description : copy.library.empty.description
          }
          action={
            !searchTrimmed ? (
              <Button onClick={() => setSearchOpen(true)}>
                <Plus className="h-4 w-4" />
                {copy.library.empty.cta}
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && entries.length > 0 && (
        <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {entries.map((entry) => (
            <BookCard key={entry.id} entry={entry} onClick={() => setSelectedId(entry.id)} />
          ))}
        </div>
      )}

      <BookSearchModal open={searchOpen} onClose={() => setSearchOpen(false)} onAdded={() => refetch()} />
      <BookDetailDrawer
        userBookId={selectedId}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        onUpdated={() => refetch()}
        onDeleted={() => refetch()}
      />
    </div>
  )
}
