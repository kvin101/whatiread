import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, MessageCircle, User, UserMinus, UserPlus, UserX } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { usersApi } from '../api/users'
import { friendsApi } from '../api/friends'
import { conversationsApi } from '../api/conversations'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter, BookSkeletonGrid } from '../components/ui/BookLoader'
import { displayName, initials } from '../lib/utils'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { copy } from '../lib/copy'
import { useAuth } from '../auth/AuthContext'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

export function UserProfilePage() {
  const { userId } = useParams<{ userId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user: me } = useAuth()
  const { confirm, dialog } = useConfirm()

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
  const isSelf = profile?.self ?? userId === me?.id

  return (
    <div>
      {dialog}
      <Link
        to={isSelf ? APP_ROUTES.settings : APP_ROUTES.friends}
        className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-accent mb-4"
      >
        <ArrowLeft className="h-4 w-4" />
        Back
      </Link>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-4 rounded-2xl p-6 flex-1 manga-panel halftone-overlay">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-sage/15 text-xl font-semibold text-sage">
            {profile?.avatarUrl ? (
              <img src={profile.avatarUrl} alt="" className="h-full w-full rounded-2xl object-cover" />
            ) : (
              initials(name)
            )}
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold text-ink">{name}</h1>
            {profile?.writer && profile.writerBio && (
              <p className="mt-2 text-sm text-ink-muted max-w-lg">{profile.writerBio}</p>
            )}
            <p className="mt-2 text-sm text-ink-muted">
              {profile?.friend && 'Friend · '}
              {profile?.blocked && 'Blocked · '}
              {isSelf && 'Your profile · '}
              {shelves.length} {shelves.length === 1 ? 'shelf' : 'shelves'} visible to you
            </p>
          </div>
        </div>

        {!isSelf && profile && !profile.blocked && (
          <div className="flex flex-wrap gap-2">
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
                  <UserMinus className="h-4 w-4" />
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
              variant="danger"
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
          </div>
        )}

        {!isSelf && profile?.blockedByViewer && (
          <div className="flex flex-col gap-3 sm:items-end">
            <p className="text-sm text-ink-muted max-w-xs sm:text-right">{copy.profile.blockedBanner}</p>
            <Button
              size="sm"
              variant="secondary"
              onClick={() => unblockMutation.mutate()}
              disabled={unblockMutation.isPending}
            >
              <UserX className="h-4 w-4" />
              {copy.profile.unblock}
            </Button>
          </div>
        )}
      </div>

      {profile?.blockedByViewer && (
        <p className="mt-4 rounded-xl border border-danger/20 bg-danger/10 px-4 py-3 text-sm text-danger lg:hidden">
          {copy.profile.blockedBanner}
        </p>
      )}

      <section className="mt-10">
        <h2 className="section-header-manga">
          Shelves you can view
        </h2>
        {isLoading && <BookSkeletonGrid count={3} className="mt-4" />}
        {!isLoading && shelves.length === 0 && (
          <EmptyState
            className="mt-4"
            icon={User}
            title="No shelves to show"
            description={
              isSelf
                ? 'Create shelves and set visibility to Friends or Public to share them.'
                : 'This reader has not shared any shelves with you yet. Add them as a friend to see Friends-only shelves.'
            }
          />
        )}
        {!isLoading && shelves.length > 0 && (
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
    </div>
  )
}
