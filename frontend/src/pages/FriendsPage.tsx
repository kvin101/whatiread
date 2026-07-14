import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Ban,
  Check,
  ChevronRight,
  Clock,
  UserMinus,
  UserPlus,
  Users,
  UserX,
  X,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { friendsApi } from '../api/friends'
import type { UserSuggestResult } from '../api/types'
import { Button } from '../components/ui/Button'
import { UserSuggestField } from '../components/users/UserSuggestField'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { PageHeader } from '../components/layout/PageHeader'
import { Label } from '../components/ui/Input'
import { copy } from '../lib/copy'
import { displayName, initials } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { getApiErrorMessage } from '../lib/api'

function UserAvatar({
  name,
  variant = 'default',
}: {
  name: string
  variant?: 'default' | 'danger' | 'pending'
}) {
  const styles = {
    default: 'bg-sage/15 text-sage ring-sage/20',
    danger: 'bg-danger/15 text-danger ring-danger/20',
    pending: 'bg-accent-dim text-accent ring-accent/20',
  }
  return (
    <div
      className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-sm font-semibold ring-2 ${styles[variant]}`}
    >
      {initials(name)}
    </div>
  )
}

function RequestCard({
  name,
  subtitle,
  badge,
  children,
}: {
  name: string
  subtitle?: string
  badge?: string
  children: React.ReactNode
}) {
  return (
    <li className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-paper-elevated px-4 py-3 card-hover list-enter">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <UserAvatar name={name} variant="pending" />
        <div className="min-w-0">
          <p className="font-medium text-ink truncate">{name}</p>
          {subtitle && <p className="text-xs text-ink-muted truncate">{subtitle}</p>}
          {badge && (
            <span className="mt-1 inline-flex items-center gap-1 rounded-full bg-accent-dim px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-accent">
              <Clock className="h-3 w-3" />
              {badge}
            </span>
          )}
        </div>
      </div>
      <div className="flex shrink-0 gap-2">{children}</div>
    </li>
  )
}

export function FriendsPage() {
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [query, setQuery] = useState('')
  const [selectedUser, setSelectedUser] = useState<UserSuggestResult | null>(null)
  const [error, setError] = useState<string | null>(null)

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
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['friends'] })
  }

  const sendMutation = useMutation({
    mutationFn: (userId: string) => friendsApi.sendRequest({ userId }),
    onSuccess: () => {
      setQuery('')
      setSelectedUser(null)
      setError(null)
      invalidate()
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Request bounced — try another reader.')),
  })

  const acceptMutation = useMutation({
    mutationFn: friendsApi.accept,
    onSuccess: invalidate,
  })

  const declineMutation = useMutation({
    mutationFn: friendsApi.decline,
    onSuccess: invalidate,
  })

  const cancelMutation = useMutation({
    mutationFn: friendsApi.cancel,
    onSuccess: invalidate,
  })

  const unfriendMutation = useMutation({
    mutationFn: friendsApi.unfriend,
    onSuccess: invalidate,
  })

  const blockMutation = useMutation({
    mutationFn: friendsApi.block,
    onSuccess: invalidate,
  })

  const unblockMutation = useMutation({
    mutationFn: friendsApi.unblock,
    onSuccess: invalidate,
  })

  const loading = friendsLoading || incomingLoading || outgoingLoading || blockedLoading

  return (
    <div>
      {dialog}
      <PageHeader
        eyebrow="Social shelf"
        title={copy.friends.title}
        description={copy.friends.description}
      />

      {loading && <BookLoaderCenter className="mt-8" />}

      {!loading && (
        <>
          <section className="mt-8 rounded-2xl border border-border bg-paper-elevated p-6 shadow-sm list-enter">
            <Label htmlFor="friendSearch">{copy.friends.inviteLabel}</Label>
            <div className="mt-2 flex flex-col gap-2 sm:flex-row">
              <UserSuggestField
                inputId="friendSearch"
                value={query}
                onValueChange={(value) => {
                  setQuery(value)
                  setSelectedUser(null)
                }}
                onSelect={(user) => {
                  setSelectedUser(user)
                  setQuery(user.displayName)
                  sendMutation.mutate(user.id)
                }}
                placeholder={copy.friends.invitePlaceholder}
              />
              <Button
                className="shrink-0"
                disabled={!selectedUser || sendMutation.isPending}
                onClick={() => selectedUser && sendMutation.mutate(selectedUser.id)}
              >
                <UserPlus className="h-4 w-4" />
                {copy.friends.send}
              </Button>
            </div>
            {error && <p className="mt-2 text-sm text-danger">{error}</p>}
          </section>

          <section className="mt-10">
            <h2 className="section-header-manga">
              {copy.friends.incoming} ({incoming.length})
            </h2>
            {incoming.length === 0 ? (
              <p className="mt-3 text-sm text-ink-muted">No pending requests — your DMs are safe for now.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {incoming.map((req) => {
                  const name = displayName(req.requester)
                  return (
                    <RequestCard key={req.id} name={name} subtitle={req.requester.email} badge="Incoming">
                      <Button size="sm" className="comic-btn-quiet shadow-none" onClick={() => acceptMutation.mutate(req.id)}>
                        <Check className="h-4 w-4" />
                        Accept
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={async () => {
                          const ok = await confirm({
                            title: copy.friends.declineConfirm.title,
                            description: copy.friends.declineConfirm.description(name),
                            confirmLabel: copy.friends.declineConfirm.confirm,
                            variant: 'danger',
                          })
                          if (ok) declineMutation.mutate(req.id)
                        }}
                      >
                        <X className="h-4 w-4" />
                        {copy.friends.decline}
                      </Button>
                    </RequestCard>
                  )
                })}
              </ul>
            )}
          </section>

          <section className="mt-10">
            <h2 className="section-header-manga">
              {copy.friends.outgoing} ({outgoing.length})
            </h2>
            {outgoing.length === 0 ? (
              <p className="mt-3 text-sm text-ink-muted">No outgoing requests waiting on a reply.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {outgoing.map((req) => {
                  const name = displayName(req.addressee)
                  return (
                    <RequestCard key={req.id} name={name} subtitle={req.addressee.email} badge="Sent">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={async () => {
                          const ok = await confirm({
                            title: copy.friends.cancelConfirm.title,
                            description: copy.friends.cancelConfirm.description(name),
                            confirmLabel: copy.friends.cancelConfirm.confirm,
                          })
                          if (ok) cancelMutation.mutate(req.id)
                        }}
                      >
                        {copy.friends.cancelRequest}
                      </Button>
                    </RequestCard>
                  )
                })}
              </ul>
            )}
          </section>

          <section className="mt-10">
            <h2 className="section-header-manga">
              {copy.friends.yourFriends(friends.length)}
            </h2>
            {friends.length === 0 ? (
              <EmptyState
                className="mt-4"
                icon={Users}
                title={copy.friends.empty.title}
                description={copy.friends.empty.description}
              />
            ) : (
              <ul className="mt-4 grid gap-3 sm:grid-cols-2">
                {friends.map((f, i) => {
                  const name = displayName(f)
                  return (
                    <li
                      key={f.id}
                      className="flex items-center gap-3 rounded-2xl border border-border bg-paper-elevated p-4 card-hover list-enter"
                      style={{ animationDelay: `${i * 30}ms` }}
                    >
                      <Link
                        to={APP_ROUTES.userProfile(f.id)}
                        className="flex min-w-0 flex-1 items-center gap-3 hover:opacity-90"
                      >
                        <UserAvatar name={name} />
                        <div className="min-w-0 flex-1">
                          <p className="font-medium text-ink truncate">{name}</p>
                          <p className="text-xs text-ink-muted truncate">{f.email}</p>
                          <p className="text-xs text-accent mt-0.5">Peek their shelves →</p>
                        </div>
                        <ChevronRight className="h-4 w-4 shrink-0 text-ink-muted" />
                      </Link>
                      <div className="flex shrink-0 flex-col gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label={`Unfriend ${name}`}
                          onClick={async () => {
                            const ok = await confirm({
                              title: copy.friends.unfriend.title,
                              description: copy.friends.unfriend.description(name),
                              confirmLabel: copy.friends.unfriend.confirm,
                              variant: 'danger',
                            })
                            if (ok) unfriendMutation.mutate(f.id)
                          }}
                        >
                          <UserMinus className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label={`Block ${name}`}
                          onClick={async () => {
                            const ok = await confirm({
                              title: copy.friends.blockConfirm.title,
                              description: copy.friends.blockConfirm.description(name),
                              confirmLabel: copy.friends.blockConfirm.confirm,
                              variant: 'danger',
                            })
                            if (ok) blockMutation.mutate(f.id)
                          }}
                        >
                          <Ban className="h-4 w-4" />
                        </Button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>

          <section className="mt-10">
            <h2 className="section-header-manga">
              {copy.friends.blocked(blocked.length)}
            </h2>
            {blocked.length === 0 ? (
              <p className="mt-3 text-sm text-ink-muted">{copy.friends.blockedEmpty.description}</p>
            ) : (
              <ul className="mt-4 space-y-2">
                {blocked.map((b) => {
                  const name = displayName(b)
                  return (
                    <li
                      key={b.id}
                      className="flex items-center justify-between gap-3 rounded-2xl border border-white/8 bg-white/[0.03] p-4 card-hover list-enter"
                    >
                      <div className="flex min-w-0 flex-1 items-center gap-3">
                        <UserAvatar name={name} variant="danger" />
                        <div className="min-w-0">
                          <p className="font-medium text-ink truncate">{name}</p>
                          <p className="text-xs text-ink-muted">Blocked</p>
                        </div>
                      </div>
                      <Button
                        size="sm"
                        variant="secondary"
                        disabled={unblockMutation.isPending}
                        onClick={() => unblockMutation.mutate(b.id)}
                      >
                        <UserX className="h-4 w-4" />
                        {copy.friends.unblock}
                      </Button>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
