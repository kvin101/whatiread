import { Link } from 'react-router-dom'
import { MessageCircle, ThumbsUp, Users } from 'lucide-react'
import { APP_ROUTES } from '../../api/paths'

export function SocialPulseCards({
  unreadMessages,
  pendingRecs,
  pendingFriendRequests,
}: {
  unreadMessages: number
  pendingRecs: number
  pendingFriendRequests: number
}) {
  const total = unreadMessages + pendingRecs + pendingFriendRequests
  if (total === 0) {
    return (
      <p className="rounded-xl border border-border bg-paper-elevated/50 px-4 py-3 text-sm text-ink-muted">
        All caught up — no pending friend or rec actions.
      </p>
    )
  }

  const cards = [
    {
      to: APP_ROUTES.recommendations,
      icon: ThumbsUp,
      label: 'Recommendations',
      count: pendingRecs,
      color: 'text-accent',
    },
    {
      to: APP_ROUTES.friends,
      icon: Users,
      label: 'Friend requests',
      count: pendingFriendRequests,
      color: 'text-sage',
    },
    {
      to: APP_ROUTES.messages,
      icon: MessageCircle,
      label: 'Messages',
      count: unreadMessages,
      color: 'text-ink',
    },
  ].filter((c) => c.count > 0)

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {cards.map(({ to, icon: Icon, label, count, color }) => (
        <Link
          key={to}
          to={to}
          className="flex items-center gap-3 rounded-xl border border-white/10 bg-paper-elevated px-4 py-3 transition-colors hover:border-accent/30"
        >
          <Icon className={`h-5 w-5 shrink-0 ${color}`} />
          <div className="min-w-0">
            <p className="text-sm font-medium text-ink">{label}</p>
            <p className="text-xs text-ink-muted">{count} waiting</p>
          </div>
        </Link>
      ))}
    </div>
  )
}
