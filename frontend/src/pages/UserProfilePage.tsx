import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { LayoutGrid, MessageCircle, Settings, Target, UserPlus, UserX } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { usersApi } from '../api/users'
import { friendsApi } from '../api/friends'
import { conversationsApi } from '../api/conversations'
import { libraryApi } from '../api/library'
import { readingApi } from '../api/reading'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { BookCard } from '../components/books/BookCard'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { ReadingGoalRing } from '../components/home/ReadingGoalRing'
import { ReadingStatsPanel } from '../components/home/ReadingStatsPanel'
import { StreakBadge } from '../components/home/StreakBadge'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter, BookSkeletonGrid } from '../components/ui/BookLoader'
import { displayName } from '../lib/utils'
import { UserAvatar } from '../components/ui/UserAvatar'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { copy } from '../lib/copy'
import { useAuth } from '../auth/AuthContext'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { useState } from 'react'

export function UserProfilePage() {
  const { userId } = useParams<{ userId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user: me } = useAuth()
  const { confirm, dialog } = useConfirm()
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null)
  const year = new Date().getFullYear()

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: QUERY_KEYS.profile.detail(userId!),
    queryFn: () => usersApi.profile(userId!),
    enabled: !!userId,
  })

  const { data: shelves = [], isLoading } = useQuery({
    queryKey: QUERY_KEYS.profile.shelves(userId!),
    queryFn: () => usersApi.shelves(userId!),
    enabled: !!userId,
  })

  const isSelf = profile?.self ?? userId === me?.id

  const { data: readingPage } = useQuery({
    queryKey: QUERY_KEYS.library.reading,
    queryFn: () => libraryApi.list({ status: 'READING', size: 6, sort: 'UPDATED_DESC' }),
    enabled: !!userId && isSelf,
  })

  const { data: finishedPage } = useQuery({
    queryKey: [...QUERY_KEYS.library.all, 'finished-profile'],
    queryFn: () => libraryApi.list({ status: 'READ', size: 12, sort: 'FINISHED_DESC' }),
    enabled: !!userId && isSelf,
  })

  const { data: stats } = useQuery({
    queryKey: QUERY_KEYS.reading.stats(year),
    queryFn: () => readingApi.getStats(year),
    enabled: !!userId && isSelf,
  })

  const unfriendMutation = useMutation({
    mutationFn: () => friendsApi.unfriend(userId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.profile.detail(userId!) })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.friends.all })
    },
  })

  const blockMutation = useMutation({
    mutationFn: () => friendsApi.block(userId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.profile.detail(userId!) })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.friends.all })
    },
  })

  const unblockMutation = useMutation({
    mutationFn: () => friendsApi.unblock(userId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.profile.detail(userId!) })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.friends.all })
    },
  })

  const messageMutation = useMutation({
    mutationFn: () => conversationsApi.withFriend(userId!),
    onSuccess: (conv) => navigate(APP_ROUTES.messages, { state: { conversationId: conv.id } }),
  })

  const sendRequestMutation = useMutation({
    mutationFn: () => friendsApi.sendRequest({ userId: userId! }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.profile.detail(userId!) })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.friends.all })
    },
  })

  if (!userId) return null

  if (profileLoading) {
    return <BookLoaderCenter className="min-h-[40vh]" />
  }

  const name = profile ? displayName(profile) : 'Reader'
  const readingEntries = readingPage?.content ?? []
  const finishedEntries = finishedPage?.content ?? []

  return (
    <ScrollablePage>
    <div className="mx-auto max-w-5xl">
      {dialog}

      <section className="rounded-xl border border-white/10 bg-paper-elevated/60 p-4 md:p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:gap-5">
          <UserAvatar
            name={name}
            avatarUrl={profile?.avatarUrl}
            size="xl"
            className="h-20 w-20 text-2xl shrink-0 mx-auto sm:mx-0"
          />

          <div className="min-w-0 flex-1 text-center sm:text-left">
            <h1 className="font-display text-2xl font-semibold text-ink">{name}</h1>
            {profile?.username && (
              <p className="mt-1 text-base text-ink-muted">@{profile.username}</p>
            )}
            {profile?.writerBio && (
              <p className="mt-2 max-w-2xl text-sm text-ink-muted">{profile.writerBio}</p>
            )}

            <div className="mt-3 flex flex-wrap items-center justify-center gap-2 sm:justify-start">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/5 px-3 py-1 text-sm text-ink-muted">
                <LayoutGrid className="h-4 w-4" />
                {shelves.length} {shelves.length === 1 ? 'shelf' : 'shelves'}
              </span>
              {isSelf && stats && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-white/5 px-3 py-1 text-sm text-ink-muted">
                  <Target className="h-4 w-4" />
                  {stats.booksRead} read in {year}
                </span>
              )}
              {profile?.friend && (
                <span className="rounded-full bg-sage/15 px-3 py-1 text-sm font-medium text-sage">Friend</span>
              )}
              {isSelf && (
                <span className="rounded-full bg-accent/15 px-3 py-1 text-sm font-medium text-accent">You</span>
              )}
            </div>

            <div className="mt-4 flex flex-wrap justify-center gap-2 sm:justify-start">
              {isSelf && (
                <Link to={APP_ROUTES.settings}>
                  <Button size="sm" variant="secondary">
                    <Settings className="h-4 w-4" />
                    Edit profile
                  </Button>
                </Link>
              )}

              {!isSelf && profile && !profile.blocked && (
                <>
                  {profile.friend && (
                    <>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => messageMutation.mutate()}
                        disabled={messageMutation.isPending}
                      >
                        <MessageCircle className="h-4 w-4" />
                        Message
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={async () => {
                          const ok = await confirm({
                            title: copy.friends.unfriend.title,
                            description: copy.friends.unfriend.description(name),
                            confirmLabel: copy.friends.unfriend.confirm,
                            variant: 'danger',
                          })
                          if (ok) unfriendMutation.mutate()
                        }}
                        disabled={unfriendMutation.isPending}
                      >
                        Unfriend
                      </Button>
                    </>
                  )}
                  {!profile.friend && !profile.blockedByViewer && (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => sendRequestMutation.mutate()}
                      disabled={sendRequestMutation.isPending}
                    >
                      <UserPlus className="h-4 w-4" />
                      {copy.profile.addFriend}
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={async () => {
                      const ok = await confirm({
                        title: `Block ${name}?`,
                        description: copy.profile.blockConfirm(name),
                        confirmLabel: copy.profile.block,
                        variant: 'danger',
                      })
                      if (ok) blockMutation.mutate()
                    }}
                    disabled={blockMutation.isPending}
                  >
                    <UserX className="h-4 w-4" />
                    Block
                  </Button>
                </>
              )}

              {!isSelf && profile?.blockedByViewer && (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => unblockMutation.mutate()}
                  disabled={unblockMutation.isPending}
                >
                  {copy.profile.unblock}
                </Button>
              )}
            </div>

            {!isSelf && profile?.blockedByViewer && (
              <p className="mt-4 text-sm text-danger">{copy.profile.blockedBanner}</p>
            )}
          </div>
        </div>
      </section>

      {isSelf && (
        <section className="mt-6 space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <ReadingGoalRing />
            <StreakBadge />
          </div>
          <ReadingStatsPanel />
        </section>
      )}

      {isSelf && readingEntries.length > 0 && (
        <section className="mt-6">
          <h2 className="text-sm font-semibold text-ink">Reading now</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {readingEntries.map((entry) => (
              <BookCard key={entry.id} entry={entry} compact onClick={() => setSelectedBookId(entry.id)} />
            ))}
          </div>
        </section>
      )}

      {isSelf && finishedEntries.length > 0 && (
        <section className="mt-6">
          <h2 className="text-sm font-semibold text-ink">Recently finished</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {finishedEntries.map((entry) => (
              <BookCard key={entry.id} entry={entry} compact onClick={() => setSelectedBookId(entry.id)} />
            ))}
          </div>
        </section>
      )}

      <section className="mt-6">
        <h2 className="text-sm font-semibold text-ink">Shelves</h2>
        {isLoading && <BookSkeletonGrid count={3} className="mt-4" />}
        {!isLoading && shelves.length === 0 && (
          <EmptyState
            className="mt-4"
            icon={LayoutGrid}
            title="No shelves to show"
            description={
              isSelf
                ? 'Create shelves and set visibility to Friends or Public to share them.'
                : 'This reader has not shared any shelves with you yet.'
            }
          />
        )}
        {!isLoading && shelves.length > 0 && (
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {shelves.map((shelf) => (
              <ShelfCard
                key={shelf.id}
                shelf={shelf}
                to={APP_ROUTES.shelf(shelf.id)}
                subtitle={isSelf ? undefined : 'Shared shelf'}
              />
            ))}
          </div>
        )}
      </section>

      <BookDetailDrawer
        userBookId={selectedBookId}
        open={!!selectedBookId}
        onClose={() => setSelectedBookId(null)}
        onUpdated={() => {
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.reading })
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
        }}
      />
    </div>
    </ScrollablePage>
  )
}
