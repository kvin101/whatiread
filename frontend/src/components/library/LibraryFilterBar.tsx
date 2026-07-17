import { Search, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { ReadingStatus } from '../../api/types'
import type { Shelf } from '../../api/types'
import { FilterChips } from '../ui/FilterChips'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { STATUS_LABELS } from '../../lib/constants'
import { APP_ROUTES } from '../../api/paths'
import { cn } from '../../lib/utils'

const STATUS_FILTERS: (ReadingStatus | 'ALL')[] = ['READING', 'TO_READ', 'READ', 'DNF', 'ALL']

export type LibrarySort = 'updated' | 'title' | 'author' | 'added' | 'rating'

export interface LibraryFilters {
  status: ReadingStatus | 'ALL'
  shelfId: string
  search: string
  sort: LibrarySort
  authorId: string
  authorName: string
}

export function LibraryFilterBar({
  filters,
  onChange,
  shelves,
  readingCount,
  className,
}: {
  filters: LibraryFilters
  onChange: (patch: Partial<LibraryFilters>) => void
  shelves: Shelf[]
  readingCount?: number
  className?: string
}) {
  const statusOptions = STATUS_FILTERS.map((f) => ({
    value: f,
    label: f === 'ALL' ? 'All' : f === 'READING' && readingCount != null && readingCount > 0
      ? `${STATUS_LABELS[f]} (${readingCount})`
      : STATUS_LABELS[f],
  }))

  const hasActive =
    filters.status !== 'ALL' ||
    filters.shelfId !== 'ALL' ||
    filters.search.trim() !== '' ||
    filters.sort !== 'updated' ||
    filters.authorId !== ''

  return (
    <div className={cn('space-y-3', className)}>
      <div className="flex flex-wrap items-center gap-2">
        <FilterChips
          options={statusOptions}
          value={filters.status}
          onChange={(v) => onChange({ status: v as ReadingStatus | 'ALL' })}
          label="Reading status"
        />
        {readingCount != null && readingCount > 0 && (
          <Link
            to={`${APP_ROUTES.library}?status=READING`}
            className="inline-flex items-center gap-1 rounded-full bg-sage/15 px-3 py-1 text-xs font-medium text-sage hover:bg-sage/25 lg:hidden"
          >
            📖 Reading ({readingCount})
          </Link>
        )}
      </div>

      {filters.authorId && (
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded-full bg-accent/15 px-3 py-1 text-xs font-medium text-accent">
            Author: {filters.authorName || 'Selected'}
            <button
              type="button"
              className="hover:text-ink"
              aria-label="Clear author filter"
              onClick={() => onChange({ authorId: '', authorName: '' })}
            >
              <X className="h-3 w-3" />
            </button>
          </span>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
        <div className="relative min-w-0 flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted pointer-events-none" />
          <Input
            type="search"
            placeholder="Search by title, author, or ISBN…"
            value={filters.search}
            onChange={(e) => onChange({ search: e.target.value })}
            className="pl-10"
            aria-label="Search library"
          />
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select
            id="shelfFilter"
            value={filters.shelfId}
            onChange={(e) => onChange({ shelfId: e.target.value })}
            className="min-w-[140px]"
            aria-label="Filter by shelf"
          >
            <option value="ALL">All shelves</option>
            {shelves.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </Select>
          <Select
            id="sortFilter"
            value={filters.sort}
            onChange={(e) => onChange({ sort: e.target.value as LibrarySort })}
            className="min-w-[140px]"
            aria-label="Sort order"
          >
            <option value="updated">Recently updated</option>
            <option value="title">Title</option>
            <option value="author">Author</option>
            <option value="added">Date added</option>
            <option value="rating">Rating</option>
          </Select>
          {hasActive && (
            <button
              type="button"
              onClick={() =>
                onChange({ status: 'ALL', shelfId: 'ALL', search: '', sort: 'updated', authorId: '', authorName: '' })
              }
              className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-sm text-ink-muted hover:text-accent"
            >
              <X className="h-4 w-4" />
              Clear
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
