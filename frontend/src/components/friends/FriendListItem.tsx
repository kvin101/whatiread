import { Link, useNavigate } from 'react-router-dom'
import { MessageCircle } from 'lucide-react'
import type { FriendSummary } from '../../api/types'
import { APP_ROUTES } from '../../api/paths'
import { UserAvatar } from '../ui/UserAvatar'
import { Button } from '../ui/Button'
import { FriendActionsMenu } from './FriendActionsMenu'
import { displayName, formatRelativeTime } from '../../lib/utils'

export function FriendListItem({
  friend,
  onUnfriend,
  onBlock,
  onMessage,
  messaging,
}: {
  friend: FriendSummary
  onUnfriend: () => void
  onBlock: () => void
  onMessage: () => void
  messaging?: boolean
}) {
  const navigate = useNavigate()
  const name = displayName(friend)

  return (
    <li className="flex items-center gap-3 rounded-2xl border border-border/80 bg-paper-elevated/60 px-3 py-3 transition-colors hover:border-border hover:bg-paper-elevated">
      <Link to={APP_ROUTES.userProfile(friend.id)} className="shrink-0">
        <UserAvatar name={name} avatarUrl={friend.avatarUrl} size="md" />
      </Link>
      <div className="min-w-0 flex-1">
        <Link
          to={APP_ROUTES.userProfile(friend.id)}
          className="block truncate font-semibold text-ink hover:text-accent"
        >
          {name}
        </Link>
        <p className="truncate text-xs text-ink-muted">
          Friends since {formatRelativeTime(friend.friendsSince)}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <Button
          size="sm"
          variant="secondary"
          className="hidden sm:inline-flex"
          disabled={messaging}
          onClick={onMessage}
        >
          <MessageCircle className="h-4 w-4" />
          Message
        </Button>
        <Button
          size="sm"
          variant="ghost"
          className="sm:hidden"
          aria-label={`Message ${name}`}
          disabled={messaging}
          onClick={onMessage}
        >
          <MessageCircle className="h-4 w-4" />
        </Button>
        <FriendActionsMenu
          items={[
            { label: 'View profile', onClick: () => navigate(APP_ROUTES.userProfile(friend.id)) },
            { label: 'Unfriend', onClick: onUnfriend, danger: true },
            { label: 'Block', onClick: onBlock, danger: true },
          ]}
        />
      </div>
    </li>
  )
}
