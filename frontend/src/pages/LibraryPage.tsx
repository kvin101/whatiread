import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { BookMarked, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { libraryApi } from '../api/library'
import { shelvesApi } from '../api/shelves'
import type { ReadingStatus, UserBook } from '../api/types'
import { BookCard } from '../components/books/BookCard'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { BookSearchModal } from '../components/books/BookSearchModal'
import { LibraryFilterBar } from '../components/library/LibraryFilterBar'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { InfiniteScrollSentinel } from '../components/ui/InfiniteScrollSentinel'
import { ListPageLayout } from '../components/layout/ListPageLayout'
import { PageHeader } from '../components/layout/PageHeader'
import { copy } from '../lib/copy'
import { formatAuthors } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'
import { useLibraryFilters } from '../hooks/useLibraryFilters'

const PAGE_SIZE = 24

export function LibraryPage() {
  const { filters, sortParam, setFilters } = useLibraryFilters('ALL')
  const [searchOpen, setSearchOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const searchTrimmed = filters.search.trim()
  const useCursor =
    filters.shelfId === 'ALL' && !searchTrimmed && !filters.authorId

  const { data: shelvesData } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
  })
  const myShelves = Array.isArray(shelvesData) ? shelvesData : []

  const { data: readingHint } = useQuery({
    queryKey: QUERY_KEYS.library.reading,
    queryFn: () => libraryApi.list({ status: 'READING', size: 1 }),
  })
  const readingCount = readingHint?.totalElements ?? 0

  const listParams = {
    status: filters.status !== 'ALL' ? (filters.status as ReadingStatus) : undefined,
    shelfId: filters.shelfId !== 'ALL' ? filters.shelfId : undefined,
    authorId: filters.authorId || undefined,
    q: searchTrimmed || undefined,
    sort: sortParam,
  }

  const infinite = useInfiniteQuery({
    queryKey: QUERY_KEYS.library.list(
      filters.status,
      filters.shelfId,
      searchTrimmed,
      filters.sort,
      filters.authorId,
    ),
    queryFn: ({ pageParam }) => {
      if (useCursor) {
        return libraryApi.listCursor({
          ...listParams,
          cursor: pageParam as string | undefined,
          limit: PAGE_SIZE,
        })
      }
      return libraryApi
        .list({
          ...listParams,
          page: pageParam as number,
          size: PAGE_SIZE,
        })
        .then((page) => ({
          items: page.content,
          nextCursor: page.last ? null : String((pageParam as number) + 1),
          hasMore: !page.last,
        }))
    },
    initialPageParam: useCursor ? (undefined as string | undefined) : 0,
    getNextPageParam: (lastPage, _pages, lastParam) => {
      if (!lastPage.hasMore) return undefined
      if (useCursor) return lastPage.nextCursor ?? undefined
      return (lastParam as number) + 1
    },
  })

  const entries = useMemo(() => {
    const raw =
      infinite.data?.pages.flatMap((p) => {
        if (Array.isArray(p?.items)) return p.items
        const content = (p as { content?: UserBook[] } | undefined)?.content
        if (Array.isArray(content)) return content
        return []
      }) ?? []
    const deduped = raw.filter((e): e is UserBook => Boolean(e?.id && e?.book?.title))
    if (!searchTrimmed || useCursor) return deduped
    const needle = searchTrimmed.toLowerCase()
    return deduped.filter(
      (e) =>
        e.book.title.toLowerCase().includes(needle) ||
        formatAuthors(e.book.authors).toLowerCase().includes(needle) ||
        (e.book.isbn?.toLowerCase().includes(needle) ?? false),
    )
  }, [infinite.data?.pages, searchTrimmed, useCursor])

  const refetch = () => infinite.refetch()

  return (
    <>
      <ListPageLayout
        toolbar={
          <>
            <PageHeader
              title={copy.library.title}
              description={copy.library.description(entries.length)}
              action={
                <Button onClick={() => setSearchOpen(true)}>
                  <Plus className="h-4 w-4" />
                  {copy.library.addBook}
                </Button>
              }
            />
            <LibraryFilterBar
              className="mt-4"
              filters={filters}
              onChange={setFilters}
              shelves={myShelves}
              readingCount={readingCount}
            />
          </>
        }
      >
        {infinite.isLoading && <BookSkeletonGrid />}

        {infinite.isError && (
          <EmptyState
            icon={BookMarked}
            title="Could not load library"
            description="Try refreshing the page."
            action={
              <Button onClick={() => infinite.refetch()}>Retry</Button>
            }
          />
        )}

        {!infinite.isLoading && !infinite.isError && entries.length === 0 && (
          <EmptyState
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

        {!infinite.isLoading && !infinite.isError && entries.length > 0 && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {entries.map((entry) => (
                <BookCard key={entry.id} entry={entry} onClick={() => setSelectedId(entry.id)} />
              ))}
            </div>
            <InfiniteScrollSentinel
              disabled={!infinite.hasNextPage || infinite.isFetchingNextPage}
              onIntersect={() => infinite.fetchNextPage()}
            />
            {infinite.isFetchingNextPage && <BookSkeletonGrid className="mt-4" />}
          </>
        )}
      </ListPageLayout>

      <BookSearchModal open={searchOpen} onClose={() => setSearchOpen(false)} onAdded={() => refetch()} />
      <BookDetailDrawer
        userBookId={selectedId}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        onUpdated={() => refetch()}
        onDeleted={() => refetch()}
      />
    </>
  )
}
