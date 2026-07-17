import type { UserBook } from '../../api/types'
import { STATUS_COLORS, STATUS_LABELS } from '../../lib/constants'
import { cn } from '../../lib/utils'
import { Badge } from '../ui/Badge'
import { BookCover } from './BookCover'
import { AuthorLink } from './AuthorLink'

export function BookCard({
  entry,
  onClick,
  compact,
}: {
  entry: UserBook
  onClick?: () => void
  compact?: boolean
}) {
  const { book, status, progressDisplay, progressPercent, rating } = entry

  return (
    <article
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={(e) => {
        if (onClick && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault()
          onClick()
        }
      }}
      className={cn(
        'group flex gap-4 rounded-2xl border-2 border-white/8 bg-paper-warm/80 p-4 backdrop-blur-sm transition-all duration-300 ease-out halftone-overlay',
        'manga-panel manga-panel-hover',
        onClick && 'cursor-pointer card-hover',
        compact && 'p-3 gap-3',
      )}
    >
      <BookCover title={book.title} coverUrl={book.coverUrl} size={compact ? 'sm' : 'md'} />
      <div className="min-w-0 flex-1">
        <h3 className="font-display text-lg font-semibold leading-snug tracking-tight text-ink line-clamp-2 group-hover:text-accent transition-colors duration-300">
          {book.title}
        </h3>
        <p className="mt-0.5 text-sm text-ink-muted line-clamp-1 panel-text">
          <AuthorLink names={book.authors} />
        </p>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <Badge className={STATUS_COLORS[status]}>{STATUS_LABELS[status]}</Badge>
          {rating != null && <span className="rating-pill">★ {rating}</span>}
        </div>
        {progressDisplay && (
          <div className="mt-3">
            <p className="text-xs text-ink-muted mb-1">{progressDisplay}</p>
            {progressPercent != null && (
              <div className="h-1.5 overflow-hidden rounded-full bg-white/10">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-accent to-sage transition-all duration-500"
                  style={{ width: `${Math.min(100, progressPercent)}%` }}
                />
              </div>
            )}
          </div>
        )}
        {book.averageRating != null && (book.ratingCount ?? 0) > 0 && (
          <p className="mt-2 text-xs text-ink-muted">
            Community {book.averageRating} · {book.ratingCount} ratings
          </p>
        )}
      </div>
    </article>
  )
}
