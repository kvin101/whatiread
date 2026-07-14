import { useInfiniteQuery, useMutation, useQuery, useQueryClient, type InfiniteData } from '@tanstack/react-query'
import { Clock, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { friendsApi } from '../api/friends'
import { libraryApi } from '../api/library'
import { recommendationsApi } from '../api/recommendations'
import { shelvesApi } from '../api/shelves'
import type { Book, Page, RecommendationTargetType, Shelf, UserBook } from '../api/types'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { PageHeader } from '../components/layout/PageHeader'
import { RecommendationCard } from '../components/recommendations/RecommendationCard'
import { RecommendModal } from '../components/recommendations/RecommendModal'
import { Button } from '../components/ui/Button'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { BookCover } from '../components/books/BookCover'
import { copy } from '../lib/copy'
import { formatAuthors } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'
import { getApiErrorMessage } from '../lib/api'

const LIBRARY_PAGE_SIZE = 50

export function RecommendationsPage() {
  const queryClient = useQueryClient()
  const { confirm, dialog: confirmDialog } = useConfirm()
  const [recommendOpen, setRecommendOpen] = useState(false)
  const [bookSearch, setBookSearch] = useState('')
  const [debouncedBookSearch, setDebouncedBookSearch] = useState('')
  const [shelves, setShelves] = useState<Shelf[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedBookSearch(bookSearch), 300)
    return () => clearTimeout(timer)
  }, [bookSearch])

  useEffect(() => {
    if (!recommendOpen) {
      setBookSearch('')
      setDebouncedBookSearch('')
      setError(null)
    }
  }, [recommendOpen])

  const { data: inbox = [], isLoading: inboxLoading } = useQuery({
    queryKey: QUERY_KEYS.recommendations.inbox,
    queryFn: recommendationsApi.inbox,
    refetchInterval: 15_000,
    staleTime: 5_000,
  })

  const { data: sent = [], isLoading: sentLoading } = useQuery({
    queryKey: QUERY_KEYS.recommendations.sent,
    queryFn: recommendationsApi.sent,
    refetchInterval: 15_000,
    staleTime: 5_000,
  })

  const { data: suggestions = [] } = useQuery({
    queryKey: QUERY_KEYS.recommendations.suggestions,
    queryFn: recommendationsApi.suggestions,
  })

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
  })

  const {
    data: libraryPages,
    isFetching: libraryLoading,
    hasNextPage: libraryHasMore,
    fetchNextPage: loadMoreBooks,
  } = useInfiniteQuery<Page<UserBook>, Error, InfiniteData<Page<UserBook>, number>, string[], number>({
    queryKey: [...QUERY_KEYS.library.forRec, debouncedBookSearch],
    queryFn: ({ pageParam = 0 }) =>
      libraryApi.list({
        q: debouncedBookSearch || undefined,
        size: LIBRARY_PAGE_SIZE,
        page: pageParam,
      }),
    initialPageParam: 0,
    getNextPageParam: (page) => (page.last ? undefined : page.number + 1),
    enabled: recommendOpen,
  })

  const { data: myShelves = [], isLoading: shelvesLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
    enabled: recommendOpen,
  })

  useEffect(() => {
    if (myShelves.length > 0) setShelves(myShelves)
  }, [myShelves])

  const libraryBooks = useMemo(() => {
    const books = libraryPages?.pages.flatMap((page: Page<UserBook>) =>
      page.content.map((entry: UserBook) => entry.book),
    ) ?? []
    const byId = new Map<string, Book>()
    books.forEach((book: Book) => {
      byId.set(book.id, book)
    })
    return [...byId.values()]
  }, [libraryPages])

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.recommendations.all })
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
  }

  const acceptMutation = useMutation({
    mutationFn: recommendationsApi.accept,
    onSuccess: invalidate,
  })

  const dismissMutation = useMutation({
    mutationFn: recommendationsApi.dismiss,
    onSuccess: invalidate,
  })

  const deleteMutation = useMutation({
    mutationFn: recommendationsApi.delete,
    onSuccess: invalidate,
  })

  const createMutation = useMutation({
    mutationFn: (payload: {
      friendId: string
      targetType: RecommendationTargetType
      bookIds?: string[]
      shelfIds?: string[]
      message: string
    }) =>
      recommendationsApi.createBatch({
        toUserId: payload.friendId,
        targetType: payload.targetType,
        bookIds: payload.bookIds,
        shelfIds: payload.shelfIds,
        message: payload.message || undefined,
      }),
    onSuccess: () => {
      setRecommendOpen(false)
      setError(null)
      invalidate()
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Rec failed to launch.')),
  })

  const handleDelete = async (recId: string, variant: 'inbox' | 'sent') => {
    const ok = await confirm({
      title: variant === 'sent' ? 'Withdraw recommendation?' : 'Remove recommendation?',
      description:
        variant === 'sent'
          ? 'This pending recommendation will be cancelled.'
          : 'This recommendation will be removed from your inbox.',
      confirmLabel: variant === 'sent' ? 'Withdraw' : 'Remove',
      variant: 'danger',
    })
    if (ok) deleteMutation.mutate(recId)
  }

  return (
    <div>
      <PageHeader
        eyebrow="Social reading"
        title={copy.recommendations.title}
        description={copy.recommendations.description}
        action={<Button onClick={() => setRecommendOpen(true)}>{copy.recommendations.recommend}</Button>}
      />

      {(inboxLoading || sentLoading) && <BookLoaderCenter className="mt-8" />}

      {!inboxLoading && (
        <section className="mt-8">
          <h2 className="section-header-manga">{copy.recommendations.inbox(inbox.length)}</h2>
          {inbox.length === 0 ? (
            <EmptyState
              className="mt-4 list-enter"
              icon={Sparkles}
              title={copy.recommendations.emptyInbox.title}
              description={copy.recommendations.emptyInbox.description}
            />
          ) : (
            <ul className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {inbox.map((rec, i) => (
                <li key={rec.id} style={{ animationDelay: `${i * 30}ms` }}>
                  <RecommendationCard
                    rec={rec}
                    variant="inbox"
                    acceptPending={acceptMutation.isPending}
                    dismissPending={dismissMutation.isPending}
                    deletePending={deleteMutation.isPending}
                    onAccept={() => acceptMutation.mutate(rec.id)}
                    onDismiss={() => dismissMutation.mutate(rec.id)}
                    onDelete={() => handleDelete(rec.id, 'inbox')}
                  />
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {!sentLoading && (
        <section className="mt-12">
          <h2 className="section-header-manga">{copy.recommendations.sent(sent.length)}</h2>
          {sent.length === 0 ? (
            <EmptyState
              className="mt-4 list-enter"
              icon={Clock}
              title={copy.recommendations.emptySent.title}
              description={copy.recommendations.emptySent.description}
            />
          ) : (
            <ul className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {sent.map((rec, i) => (
                <li key={rec.id} style={{ animationDelay: `${i * 30}ms` }}>
                  <RecommendationCard
                    rec={rec}
                    variant="sent"
                    deletePending={deleteMutation.isPending}
                    onDelete={() => handleDelete(rec.id, 'sent')}
                  />
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <section className="mt-12">
        <h2 className="section-header-manga">{copy.recommendations.suggested}</h2>
        {suggestions.length === 0 ? (
          <EmptyState
            className="mt-4"
            icon={Sparkles}
            title={copy.recommendations.emptySuggestions.title}
            description={copy.recommendations.emptySuggestions.description}
          />
        ) : (
          <ul className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {suggestions.map((s, i) => (
              <li
                key={`${s.book.id}-${i}`}
                className="flex gap-3 rounded-2xl border-2 border-border bg-paper-elevated p-3 card-hover manga-panel list-enter"
                style={{ animationDelay: `${i * 30}ms` }}
              >
                <BookCover title={s.book.title} coverUrl={s.book.coverUrl} size="sm" />
                <div className="min-w-0">
                  <h3 className="text-sm font-medium text-ink line-clamp-2">{s.book.title}</h3>
                  <p className="text-xs text-ink-muted line-clamp-1">{formatAuthors(s.book.authors)}</p>
                  <p className="mt-1.5 text-[11px] text-sage line-clamp-2">{s.reason}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <RecommendModal
        open={recommendOpen}
        onClose={() => setRecommendOpen(false)}
        friends={friends}
        libraryBooks={libraryBooks}
        libraryLoading={libraryLoading}
        libraryHasMore={Boolean(libraryHasMore)}
        onLoadMoreBooks={() => loadMoreBooks()}
        bookSearch={bookSearch}
        onBookSearchChange={setBookSearch}
        shelves={shelves}
        shelvesLoading={shelvesLoading}
        pending={createMutation.isPending}
        error={error}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />

      {confirmDialog}
    </div>
  )
}
