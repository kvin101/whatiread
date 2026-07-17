import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { ExploreShelf, Shelf } from '../../api/types'
import { VISIBILITY_LABELS } from '../../lib/constants'
import { cn, formatRelativeTime } from '../../lib/utils'
import { triggerComicBurst } from '../ui/ComicBurst'
import { ShelfIcon } from './ShelfIcon'
import { Globe, EyeOff, Lock, Users } from 'lucide-react'

const visibilityIcons = {
  SECRET: EyeOff,
  PRIVATE: Lock,
  FRIENDS: Users,
  PUBLIC: Globe,
}

export function ShelfCard({
  shelf,
  to,
  subtitle,
  showUpdated,
  actions,
}: {
  shelf: Shelf | ExploreShelf
  to: string
  subtitle?: string
  showUpdated?: boolean
  actions?: ReactNode
}) {
  const Icon = visibilityIcons[shelf.visibility]

  return (
    <div className="group flex flex-col rounded-2xl backdrop-blur-sm card-hover manga-panel manga-panel-hover halftone-overlay transition-all duration-300">
      <Link
        to={to}
        onClick={(e) => triggerComicBurst(e.currentTarget)}
        className={cn('comic-btn block min-w-0 flex-1 p-5', actions && 'pb-3')}
      >
        <div className="flex items-start gap-3">
          <ShelfIcon icon={shelf.icon} size="md" />
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2">
              <h3 className="font-display text-lg font-semibold text-ink group-hover:text-accent transition-colors line-clamp-2">
                {shelf.name}
              </h3>
              <div className="flex shrink-0 items-center gap-1.5">
                <Icon
                  className="h-4 w-4 text-ink-muted"
                  aria-label={VISIBILITY_LABELS[shelf.visibility]}
                />
                {'currentUserRole' in shelf && shelf.currentUserRole && shelf.currentUserRole !== 'OWNER' && (
                  <span className="rounded-full bg-sage/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-sage">
                    {shelf.currentUserRole.toLowerCase()}
                  </span>
                )}
                {'requiresPin' in shelf && shelf.requiresPin && (
                  <span className="rounded-full bg-accent/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-accent">
                    PIN
                  </span>
                )}
              </div>
            </div>
            {shelf.description && (
              <p className="mt-1 text-sm text-ink-muted line-clamp-2 panel-text">{shelf.description}</p>
            )}
            <p className="mt-2 text-xs text-ink-muted line-clamp-2">
              {shelf.bookCount} {shelf.bookCount === 1 ? 'book' : 'books'}
              {subtitle && ` · ${subtitle}`}
              {'ownerDisplayName' in shelf && (
                <span> · by {shelf.ownerDisplayName}</span>
              )}
              {showUpdated && shelf.updatedAt && (
                <span> · updated {formatRelativeTime(shelf.updatedAt)}</span>
              )}
            </p>
          </div>
        </div>
      </Link>
      {actions && (
        <div className="flex items-center justify-end gap-2 border-t border-ink/8 px-5 py-3">
          {actions}
        </div>
      )}
    </div>
  )
}

export function ShelfVisibilityBadge({
  visibility,
  className,
}: {
  visibility: Shelf['visibility']
  className?: string
}) {
  const Icon = visibilityIcons[visibility]
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full bg-ink/5 px-2 py-0.5 text-xs font-medium text-ink-muted',
        className,
      )}
    >
      <Icon className="h-3 w-3" />
      {VISIBILITY_LABELS[visibility]}
    </span>
  )
}
