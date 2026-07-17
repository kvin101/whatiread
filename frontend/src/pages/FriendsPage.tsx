import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ChevronRight, Search, ShieldBan, UserPlus, Users } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { friendsApi } from '../api/friends'
import { conversationsApi } from '../api/conversations'
import type { UserSuggestResult } from '../api/types'
import { IncomingRequestCard } from '../components/friends/IncomingRequestCard'
import { FriendListItem } from '../components/friends/FriendListItem'
import { Button } from '../components/ui/Button'
import { UserSuggestField } from '../components/users/UserSuggestField'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { InfiniteScrollSentinel } from '../components/ui/InfiniteScrollSentinel'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { PageHeader } from '../components/layout/PageHeader'
import { Input, Label } from '../components/ui/Input'
import { copy } from '../lib/copy'
import { displayName } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { getApiErrorMessage } from '../lib/api'
import { useClientInfiniteScroll } from '../hooks/useClientInfiniteScroll'
import { useFriendsMutations } from '../hooks/useFriendsMutations'
import { UserAvatar } from '../components/ui/UserAvatar'

const FRIENDS_PAGE_SIZE = 16

export function FriendsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [query, setQuery] = useState('')
  const [selectedUser, setSelectedUser] = useState<UserSuggestResult | null>(null)
  const [friendFilter, setFriendFilter] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [blockedOpen, setBlockedOpen] = useState(false)
  const [messagingUserId, setMessagingUserId] = useState<string | null>(null)

  const {
    sendMutation,
    acceptMutation,
    declineMutation,
    unfriendMutation,
    blockMutation,
    unblockMutation,
  } = useFriendsMutations()

  const { data: friends = [], isLoading: friendsLoading } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
  })

  const { data: incoming = [], isLoading: incomingLoading } = useQuery({
    queryKey: QUERY_KEYS.friends.incoming,
    queryFn: friendsApi.listIncoming,
  })

  const { data: outgoing = [], isLoading: outgoingLoading } = useQuery({
    queryKey: QUERY_KEYS.friends.outgoing,
    queryFn: friendsApi.listOutgoing,
  })

  const { data: blocked = [], isLoading: blockedLoading } = useQuery({
    queryKey: QUERY_KEYS.friends.blocked,
    queryFn: friendsApi.listBlocked,
    enabled: blockedOpen,
  })

  const filteredFriends = useMemo(() => {
    const needle = friendFilter.trim().toLowerCase()
    if (!needle) return friends
    return friends.filter((friend) => {
      const name = displayName(friend).toLowerCase()
      return name.includes(needle) || friend.email.toLowerCase().includes(needle)
    })
  }, [friends, friendFilter])

  const {
    visibleItems: visibleFriends,
    hasMore: hasMoreFriends,
    loadMore: loadMoreFriends,
    total: totalFriends,
  } = useClientInfiniteScroll(filteredFriends, FRIENDS_PAGE_SIZE)

  const messageMutation = useMutation({
    mutationFn: (friendUserId: string) => conversationsApi.withFriend(friendUserId),
    onMutate: (friendUserId) => setMessagingUserId(friendUserId),
    onSuccess: (conversation) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      navigate(APP_ROUTES.messages, { state: { conversationId: conversation.id } })
    },
    onSettled: () => setMessagingUserId(null),
  })

  const loading = friendsLoading || incomingLoading || outgoingLoading

  const handleSendRequest = () => {
    if (!selectedUser) return
    sendMutation.mutate(selectedUser.id, {
      onSuccess: () => {
        setQuery('')
        setSelectedUser(null)
        setError(null)
      },
      onError: (e) => setError(getApiErrorMessage(e, 'Could not send request.')),
    })
  }

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-2xl">
        {dialog}
        <PageHeader title={copy.friends.title} />

        {loading && <BookLoaderCenter className="mt-6" />}

        {!loading && (
          <div className="mt-6 space-y-5">
            <section className="rounded-2xl border border-border/80 bg-paper-elevated/50 p-4 shadow-sm">
              <Label htmlFor="friendSearch">{copy.friends.inviteLabel}</Label>
              <div className="mt-2 flex flex-col gap-2 sm:flex-row">
                <UserSuggestField
                  inputId="friendSearch"
                  value={query}
                  onValueChange={(value) => {
                    setQuery(value)
                    setSelectedUser(null)
                    setError(null)
                  }}
                  onSelect={(user) => {
                    setSelectedUser(user)
                    setQuery(user.displayName)
                    sendMutation.mutate(user.id, {
                      onSuccess: () => {
                        setQuery('')
                        setSelectedUser(null)
                        setError(null)
                      },
                      onError: (e) => setError(getApiErrorMessage(e, 'Could not send request.')),
                    })
                  }}
                  placeholder={copy.friends.invitePlaceholder}
                />
                <Button
                  className="shrink-0"
                  disabled={!selectedUser || sendMutation.isPending}
                  onClick={handleSendRequest}
                >
                  <UserPlus className="h-4 w-4" />
                  {copy.friends.send}
                </Button>
              </div>
              {error && <p className="mt-2 text-sm text-danger">{error}</p>}
            </section>

            {incoming.length > 0 && (
              <section>
                <div className="mb-3 flex items-center justify-between gap-3">
                  <h2 className="text-base font-semibold text-ink">
                    {copy.friends.incoming}
                  </h2>
                  <span className="rounded-full bg-accent/15 px-2.5 py-0.5 text-xs font-semibold text-accent">
                    {incoming.length}
                  </span>
                </div>
                <ul className="space-y-3">
                  {incoming.map((request) => {
                    const name = displayName(request.requester)
                    return (
                      <IncomingRequestCard
                        key={request.id}
                        request={request}
                        accepting={acceptMutation.isPending}
                        declining={declineMutation.isPending}
                        onAccept={() => acceptMutation.mutate(request.id)}
                        onDecline={async () => {
                          const ok = await confirm({
                            title: copy.friends.declineConfirm.title,
                            description: copy.friends.declineConfirm.description(name),
                            confirmLabel: copy.friends.declineConfirm.confirm,
                            variant: 'danger',
                          })
                          if (ok) declineMutation.mutate(request.id)
                        }}
                      />
                    )
                  })}
                </ul>
              </section>
            )}

            <Link
              to={APP_ROUTES.friendsSentRequests}
              className="flex items-center justify-between gap-3 rounded-2xl border border-border/80 bg-paper-elevated/40 px-4 py-3.5 transition-colors hover:border-border hover:bg-paper-elevated"
            >
              <div>
                <p className="font-medium text-ink">{copy.friends.outgoing}</p>
                <p className="text-sm text-ink-muted">{copy.friends.sentRequestsHint}</p>
              </div>
              <div className="flex items-center gap-2">
                {outgoing.length > 0 && (
                  <span className="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-semibold text-ink">
                    {outgoing.length}
                  </span>
                )}
                <ChevronRight className="h-5 w-5 text-ink-muted" />
              </div>
            </Link>

            <section>
              <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <h2 className="text-base font-semibold text-ink">
                  {copy.friends.yourFriends(totalFriends)}
                </h2>
                {friends.length > 0 && (
                  <div className="relative w-full sm:max-w-xs">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
                    <Input
                      value={friendFilter}
                      onChange={(e) => setFriendFilter(e.target.value)}
                      placeholder={copy.friends.searchFriends}
                      className="pl-9"
                    />
                  </div>
                )}
              </div>

              {friends.length === 0 ? (
                <EmptyState
                  icon={Users}
                  title={copy.friends.empty.title}
                  description={copy.friends.empty.description}
                />
              ) : filteredFriends.length === 0 ? (
                <p className="rounded-2xl border border-dashed border-border px-4 py-8 text-center text-sm text-ink-muted">
                  {copy.friends.noSearchResults}
                </p>
              ) : (
                <>
                  <ul className="space-y-2">
                    {visibleFriends.map((friend) => {
                      const name = displayName(friend)
                      return (
                        <FriendListItem
                          key={friend.id}
                          friend={friend}
                          messaging={messagingUserId === friend.id}
                          onMessage={() => messageMutation.mutate(friend.id)}
                          onUnfriend={async () => {
                            const ok = await confirm({
                              title: copy.friends.unfriend.title,
                              description: copy.friends.unfriend.description(name),
                              confirmLabel: copy.friends.unfriend.confirm,
                              variant: 'danger',
                            })
                            if (ok) unfriendMutation.mutate(friend.id)
                          }}
                          onBlock={async () => {
                            const ok = await confirm({
                              title: copy.friends.blockConfirm.title,
                              description: copy.friends.blockConfirm.description(name),
                              confirmLabel: copy.friends.blockConfirm.confirm,
                              variant: 'danger',
                            })
                            if (ok) blockMutation.mutate(friend.id)
                          }}
                        />
                      )
                    })}
                  </ul>
                  <InfiniteScrollSentinel
                    disabled={!hasMoreFriends}
                    onIntersect={loadMoreFriends}
                  />
                  {hasMoreFriends && (
                    <p className="pt-3 text-center text-xs text-ink-muted">Loading more friends…</p>
                  )}
                </>
              )}
            </section>

            <section className="rounded-2xl border border-border/60 bg-paper-elevated/30">
              <button
                type="button"
                onClick={() => setBlockedOpen((open) => !open)}
                className="flex w-full items-center justify-between gap-3 px-4 py-3.5 text-left"
              >
                <div className="flex items-center gap-2">
                  <ShieldBan className="h-4 w-4 text-ink-muted" />
                  <span className="font-medium text-ink">{copy.friends.blockedManage}</span>
                </div>
                <ChevronRight
                  className={`h-5 w-5 text-ink-muted transition-transform ${blockedOpen ? 'rotate-90' : ''}`}
                />
              </button>
              {blockedOpen && (
                <div className="border-t border-border/60 px-4 py-4">
                  {blockedLoading && <BookLoaderCenter />}
                  {!blockedLoading && blocked.length === 0 && (
                    <p className="text-sm text-ink-muted">{copy.friends.blockedEmpty.description}</p>
                  )}
                  {!blockedLoading && blocked.length > 0 && (
                    <ul className="space-y-2">
                      {blocked.map((user) => {
                        const name = displayName(user)
                        return (
                          <li
                            key={user.id}
                            className="flex items-center justify-between gap-3 rounded-xl border border-border/80 bg-paper-elevated/50 px-3 py-2.5"
                          >
                            <div className="flex min-w-0 items-center gap-3">
                              <UserAvatar name={name} avatarUrl={user.avatarUrl} variant="danger" size="sm" />
                              <p className="truncate font-medium text-ink">{name}</p>
                            </div>
                            <Button
                              size="sm"
                              variant="secondary"
                              disabled={unblockMutation.isPending}
                              onClick={() => unblockMutation.mutate(user.id)}
                            >
                              {copy.friends.unblock}
                            </Button>
                          </li>
                        )
                      })}
                    </ul>
                  )}
                </div>
              )}
            </section>
          </div>
        )}
      </div>
    </ScrollablePage>
  )
}
