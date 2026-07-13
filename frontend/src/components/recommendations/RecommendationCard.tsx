import { Check, Clock, Trash2, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { Recommendation } from '../../api/types'
import { APP_ROUTES } from '../../api/paths'
import { displayName, formatAuthors } from '../../lib/utils'
import { BookCover } from '../books/BookCover'
import { Button } from '../ui/Button'

type RecommendationCardProps = {
  rec: Recommendation
  variant: 'inbox' | 'sent'
  onAccept?: () => void
  onDismiss?: () => void
  onDelete?: () => void
  acceptPending?: boolean
  dismissPending?: boolean
  deletePending?: boolean
}

export function RecommendationCard({
  rec,
  variant,
  onAccept,
  onDismiss,
  onDelete,
  acceptPending,
  dismissPending,
  deletePending,
}: RecommendationCardProps) {
  const isShelf = rec.targetType === 'SHELF' && rec.shelf
  const peer = variant === 'inbox' ? rec.fromUser : rec.toUser
  const peerLabel =
    variant === 'inbox'
      ? `${displayName(peer)} recommends`
      : `Waiting on ${displayName(peer)}`

  return (
    <article className="flex flex-col rounded-2xl border-2 border-border bg-paper-elevated p-3 card-hover manga-panel list-enter">
      <div className="flex min-h-0 flex-1 gap-3">
        {isShelf ? (
          <Link
            to={APP_ROUTES.shelf(rec.shelf!.id)}
            className="flex h-16 w-12 shrink-0 items-center justify-center rounded-lg bg-accent-dim text-2xl ring-1 ring-accent/20 transition-colors hover:ring-accent/40"
            aria-label={rec.shelf!.name}
          >
            {rec.shelf!.icon ?? '📚'}
          </Link>
        ) : rec.book ? (
          <BookCover title={rec.book.title} coverUrl={rec.book.coverUrl} size="sm" />
        ) : null}
        <div className="min-w-0 flex-1">
          <p className="text-[11px] font-medium uppercase tracking-wide text-ink-muted line-clamp-1">
            {peerLabel}
          </p>
          {isShelf ? (
            <>
              <h3 className="font-display text-sm font-semibold leading-snug text-ink line-clamp-2">
                <Link to={APP_ROUTES.shelf(rec.shelf!.id)} className="hover:text-accent">
                  {rec.shelf!.name}
                </Link>
              </h3>
              <p className="text-xs text-ink-muted">
                {rec.shelf!.bookCount} {rec.shelf!.bookCount === 1 ? 'book' : 'books'}
              </p>
            </>
          ) : rec.book ? (
            <>
              <h3 className="font-display text-sm font-semibold leading-snug text-ink line-clamp-2">
                {rec.book.title}
              </h3>
              <p className="text-xs text-ink-muted line-clamp-1">{formatAuthors(rec.book.authors)}</p>
            </>
          ) : null}
          {rec.message && (
            <p className="mt-1.5 text-xs italic text-ink-muted line-clamp-2">&ldquo;{rec.message}&rdquo;</p>
          )}
        </div>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-1.5 border-t border-ink/8 pt-2">
        {variant === 'inbox' ? (
          <>
            <Button size="sm" className="h-7 px-2 text-xs" disabled={acceptPending} onClick={onAccept}>
              <Check className="h-3.5 w-3.5" />
              Add
            </Button>
            <Button
              size="sm"
              variant="secondary"
              className="h-7 px-2 text-xs"
              disabled={dismissPending}
              onClick={onDismiss}
            >
              <X className="h-3.5 w-3.5" />
              Pass
            </Button>
            <Button
              size="sm"
              variant="secondary"
              className="ml-auto h-7 px-2 text-xs text-danger"
              disabled={deletePending}
              onClick={onDelete}
              aria-label="Remove recommendation"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </>
        ) : (
          <>
            <span className="inline-flex items-center gap-1 rounded-full bg-accent-dim px-2 py-0.5 text-[11px] font-medium text-accent">
              <Clock className="h-3 w-3" />
              Pending
            </span>
            <Button
              size="sm"
              variant="secondary"
              className="ml-auto h-7 px-2 text-xs text-danger"
              disabled={deletePending}
              onClick={onDelete}
            >
              <Trash2 className="h-3.5 w-3.5" />
              Withdraw
            </Button>
          </>
        )}
      </div>
    </article>
  )
}
