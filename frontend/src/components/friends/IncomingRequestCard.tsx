import { Link } from 'react-router-dom'
import { Check, X } from 'lucide-react'
import type { FriendRequest } from '../../api/types'
import { APP_ROUTES } from '../../api/paths'
import { UserAvatar } from '../ui/UserAvatar'
import { Button } from '../ui/Button'
import { displayName } from '../../lib/utils'

export function IncomingRequestCard({
  request,
  onAccept,
  onDecline,
  accepting,
  declining,
}: {
  request: FriendRequest
  onAccept: () => void
  onDecline: () => void
  accepting?: boolean
  declining?: boolean
}) {
  const name = displayName(request.requester)
  const user = request.requester

  return (
    <li className="overflow-hidden rounded-2xl border border-border/80 bg-paper-elevated/60">
      <div className="flex items-start gap-3 p-4">
        <Link to={APP_ROUTES.userProfile(user.id)} className="shrink-0">
          <UserAvatar name={name} avatarUrl={user.avatarUrl} size="md" variant="pending" />
        </Link>
        <div className="min-w-0 flex-1 pt-0.5">
          <Link
            to={APP_ROUTES.userProfile(user.id)}
            className="font-semibold text-ink hover:text-accent"
          >
            {name}
          </Link>
          <p className="mt-0.5 text-sm text-ink-muted">Sent you a friend request</p>
        </div>
      </div>
      <div className="grid grid-cols-2 border-t border-border/80">
        <Button
          variant="ghost"
          className="h-11 rounded-none border-r border-border/80 font-semibold text-ink hover:bg-white/5"
          disabled={declining}
          onClick={onDecline}
        >
          <X className="h-4 w-4" />
          Decline
        </Button>
        <Button
          variant="ghost"
          className="h-11 rounded-none font-semibold text-accent hover:bg-accent/10"
          disabled={accepting}
          onClick={onAccept}
        >
          <Check className="h-4 w-4" />
          Confirm
        </Button>
      </div>
    </li>
  )
}
