import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { notificationsApi } from '../api/notifications'
import type { AppNotification } from '../api/types'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { PageHeader } from '../components/layout/PageHeader'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { QUERY_KEYS } from '../lib/constants'
import { formatRelativeTime } from '../lib/utils'
import { APP_ROUTES } from '../api/paths'

function notificationLink(n: AppNotification): string {
  const p = n.payload ?? {}
  switch (n.type) {
    case 'FRIEND_REQUEST':
      return APP_ROUTES.friends
    case 'RECOMMENDATION':
      return APP_ROUTES.recommendations
    case 'MESSAGE':
    case 'MENTION':
      return p.conversationId
        ? APP_ROUTES.messages
        : APP_ROUTES.messages
    default:
      return APP_ROUTES.home
  }
}

function notificationLabel(n: AppNotification): string {
  const p = n.payload ?? {}
  switch (n.type) {
    case 'FRIEND_REQUEST':
      return `${p.fromName ?? 'Someone'} sent a friend request`
    case 'RECOMMENDATION':
      return `${p.fromName ?? 'Someone'} recommended a book`
    case 'MENTION':
      return `${p.fromName ?? 'Someone'} mentioned you`
    case 'MESSAGE':
      return `New message from ${p.fromName ?? 'someone'}`
    default:
      return 'Notification'
  }
}

export function NotificationsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: items = [], isLoading } = useQuery({
    queryKey: QUERY_KEYS.notifications.all,
    queryFn: notificationsApi.list,
  })

  const markAllMutation = useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notifications.all }),
  })

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notifications.all }),
  })

  const open = (n: AppNotification) => {
    if (!n.readAt) markReadMutation.mutate(n.id)
    navigate(notificationLink(n))
  }

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-2xl">
        <PageHeader
          title="Notifications"
          action={
            items.some((n) => !n.readAt) ? (
              <Button size="sm" variant="secondary" onClick={() => markAllMutation.mutate()}>
                Mark all read
              </Button>
            ) : undefined
          }
        />
        {isLoading && <BookLoaderCenter className="min-h-[30vh]" />}
        {!isLoading && items.length === 0 && (
          <EmptyState icon={Bell} title="No notifications" description="You're all caught up." />
        )}
        <ul className="mt-6 space-y-2">
          {items.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                onClick={() => open(n)}
                className={`w-full rounded-xl border px-4 py-3 text-left transition-colors hover:border-accent/30 ${
                  n.readAt ? 'border-border bg-paper-elevated/50' : 'border-accent/30 bg-accent/5'
                }`}
              >
                <p className="text-sm text-ink">{notificationLabel(n)}</p>
                <p className="mt-0.5 text-xs text-ink-muted">{formatRelativeTime(n.createdAt)}</p>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </ScrollablePage>
  )
}
