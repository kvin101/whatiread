import { useSearchParams } from 'react-router-dom'
import { useMemo } from 'react'
import type { ReadingStatus } from '../api/types'
import type { LibraryFilters, LibrarySort } from '../components/library/LibraryFilterBar'
import { toLibrarySortParam } from '../api/library'

const VALID_STATUS: (ReadingStatus | 'ALL')[] = ['ALL', 'READING', 'TO_READ', 'READ', 'DNF']
const VALID_SORT: LibrarySort[] = ['updated', 'title', 'author', 'added', 'rating']

export function useLibraryFilters(defaultStatus: ReadingStatus | 'ALL' = 'ALL') {
  const [params, setParams] = useSearchParams()

  const filters: LibraryFilters = useMemo(() => {
    const statusParam = params.get('status') ?? defaultStatus
    const status = VALID_STATUS.includes(statusParam as ReadingStatus | 'ALL')
      ? (statusParam as ReadingStatus | 'ALL')
      : defaultStatus
    const sortParam = params.get('sort') ?? 'updated'
    const sort = VALID_SORT.includes(sortParam as LibrarySort) ? (sortParam as LibrarySort) : 'updated'
    return {
      status,
      shelfId: params.get('shelfId') ?? 'ALL',
      search: params.get('q') ?? '',
      sort,
      authorId: params.get('authorId') ?? '',
      authorName: params.get('authorName') ?? '',
    }
  }, [params, defaultStatus])

  const setFilters = (patch: Partial<LibraryFilters>) => {
    setParams((prev) => {
      const next = new URLSearchParams(prev)
      const merged = { ...filters, ...patch }
      if (merged.status === 'ALL') next.delete('status')
      else next.set('status', merged.status)
      if (merged.shelfId === 'ALL') next.delete('shelfId')
      else next.set('shelfId', merged.shelfId)
      if (!merged.search.trim()) next.delete('q')
      else next.set('q', merged.search.trim())
      if (merged.sort === 'updated') next.delete('sort')
      else next.set('sort', merged.sort)
      if (!merged.authorId) {
        next.delete('authorId')
        next.delete('authorName')
      } else {
        next.set('authorId', merged.authorId)
        if (merged.authorName) next.set('authorName', merged.authorName)
        else next.delete('authorName')
      }
      return next
    })
  }

  return {
    filters,
    sortParam: toLibrarySortParam(filters.sort),
    setFilters,
  }
}
