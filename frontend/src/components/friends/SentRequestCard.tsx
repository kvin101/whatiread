import { Link } from 'react-router-dom'
import type { FriendRequest } from '../../api/types'
import { APP_ROUTES } from '../../api/paths'
import { UserAvatar } from '../ui/UserAvatar'
import { Button } from '../ui/Button'
import { displayName } from '../../lib/utils'

export function SentRequestCard({
  request,
  onCancel,
  cancelling,
}: {
  request: FriendRequest
  onCancel: () => void
  cancelling?: boolean
}) {
  const name = displayName(request.addressee)
  const user = request.addressee

  return (
    <li className="flex items-center gap-3 rounded-2xl border border-border/80 bg-paper-elevated/60 px-3 py-3 transition-colors hover:border-border hover:bg-paper-elevated">
      <Link to={APP_ROUTES.userProfile(user.id)} className="shrink-0">
        <UserAvatar name={name} avatarUrl={user.avatarUrl} size="md" variant="pending" />
      </Link>
      <div className="min-w-0 flex-1">
        <Link
          to={APP_ROUTES.userProfile(user.id)}
          className="block truncate font-semibold text-ink hover:text-accent"
        >
          {name}
        </Link>
        <p className="truncate text-xs text-ink-muted">Request pending</p>
      </div>
      <Button size="sm" variant="secondary" disabled={cancelling} onClick={onCancel}>
        Withdraw
      </Button>
    </li>
  )
}
