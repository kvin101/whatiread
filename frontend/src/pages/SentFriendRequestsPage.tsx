import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import { friendsApi } from '../api/friends'
import { SentRequestCard } from '../components/friends/SentRequestCard'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { InfiniteScrollSentinel } from '../components/ui/InfiniteScrollSentinel'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { PageHeader } from '../components/layout/PageHeader'
import { EmptyState } from '../components/ui/EmptyState'
import { copy } from '../lib/copy'
import { displayName } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { useClientInfiniteScroll } from '../hooks/useClientInfiniteScroll'
import { useFriendsMutations } from '../hooks/useFriendsMutations'
import { Clock } from 'lucide-react'

const SENT_PAGE_SIZE = 20

export function SentFriendRequestsPage() {
  const { confirm, dialog } = useConfirm()
  const { cancelMutation } = useFriendsMutations()

  const { data: outgoing = [], isLoading } = useQuery({
    queryKey: QUERY_KEYS.friends.outgoing,
    queryFn: friendsApi.listOutgoing,
  })

  const {
    visibleItems: visibleOutgoing,
    hasMore,
    loadMore,
    total,
  } = useClientInfiniteScroll(outgoing, SENT_PAGE_SIZE)

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-2xl">
        {dialog}
        <Link
          to={APP_ROUTES.friends}
          className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-accent"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to friends
        </Link>

        <PageHeader
          title={copy.friends.outgoing}
          className="mt-4"
        />

        <p className="mt-1 text-sm text-ink-muted">
          {total === 0
            ? copy.friends.sentRequestsEmpty
            : copy.friends.sentRequestsSubtitle(total)}
        </p>

        {isLoading && <BookLoaderCenter className="mt-6" />}

        {!isLoading && outgoing.length === 0 && (
          <EmptyState
            className="mt-6"
            icon={Clock}
            title={copy.friends.sentRequestsEmptyTitle}
            description={copy.friends.sentRequestsEmpty}
          />
        )}

        {!isLoading && outgoing.length > 0 && (
          <ul className="mt-6 space-y-2">
            {visibleOutgoing.map((request) => {
              const name = displayName(request.addressee)
              return (
                <SentRequestCard
                  key={request.id}
                  request={request}
                  cancelling={cancelMutation.isPending}
                  onCancel={async () => {
                    const ok = await confirm({
                      title: copy.friends.cancelConfirm.title,
                      description: copy.friends.cancelConfirm.description(name),
                      confirmLabel: copy.friends.cancelConfirm.confirm,
                    })
                    if (ok) cancelMutation.mutate(request.id)
                  }}
                />
              )
            })}
          </ul>
        )}

        {!isLoading && hasMore && (
          <>
            <InfiniteScrollSentinel onIntersect={loadMore} />
            <p className="pt-3 text-center text-xs text-ink-muted">Loading more requests…</p>
          </>
        )}
      </div>
    </ScrollablePage>
  )
}
