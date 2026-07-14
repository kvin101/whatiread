import { Loader2 } from 'lucide-react'
import type { BookPreview } from '../../api/types'
import { formatAuthors } from '../../lib/utils'
import { BookCover } from './BookCover'

const SOURCE_LABELS: Record<NonNullable<BookPreview['source']>, string> = {
  OPEN_LIBRARY: 'Open Library',
  MANUAL: 'Catalog',
  GOODREADS: 'Goodreads',
}

export function BookPreviewPanel({
  preview,
  loading,
  error,
  compact = false,
}: {
  preview?: BookPreview
  loading?: boolean
  error?: boolean
  compact?: boolean
}) {
  if (loading) {
    return (
      <div className="flex items-center justify-center gap-3 rounded-2xl border border-border/60 bg-paper-elevated/60 px-4 py-8">
        <Loader2 className="h-5 w-5 animate-spin text-accent" />
        <span className="text-sm text-ink-muted">Loading preview…</span>
      </div>
    )
  }

  if (error || !preview) {
    return (
      <div className="rounded-2xl border border-border/60 bg-paper-elevated/60 px-4 py-6 text-sm text-ink-muted">
        Could not load a preview for this book.
      </div>
    )
  }

  return (
    <div
      className={
        compact
          ? 'space-y-4'
          : 'manga-panel rounded-2xl border border-accent/10 bg-paper-elevated/80 p-4 halftone-overlay'
      }
    >
      <div className="flex gap-4">
        <BookCover title={preview.title} coverUrl={preview.coverUrl} size="lg" />
        <div className="min-w-0 flex-1">
          {!compact && (
            <>
              <p className="font-display text-lg font-bold text-ink">{preview.title}</p>
              {preview.subtitle && (
                <p className="mt-1 text-sm text-ink-muted">{preview.subtitle}</p>
              )}
              <p className="mt-2 text-sm text-ink-muted">{formatAuthors(preview.authors)}</p>
            </>
          )}

          <div className={`flex flex-wrap items-center gap-2 text-xs text-ink-muted ${compact ? '' : 'mt-3'}`}>
            {preview.pageCount != null && <span>{preview.pageCount} pages</span>}
            {preview.publishYear != null && <span>Published {preview.publishYear}</span>}
            {preview.isbn && <span>ISBN {preview.isbn}</span>}
            {preview.source && (
              <span className="rounded-full bg-accent-dim px-2 py-0.5 font-medium text-accent">
                {SOURCE_LABELS[preview.source]}
              </span>
            )}
          </div>

          {preview.averageRating != null && (preview.ratingCount ?? 0) > 0 && (
            <span className="rating-pill mt-3 inline-flex">
              ★ {preview.averageRating} · {preview.ratingCount} ratings
            </span>
          )}
        </div>
      </div>

      {preview.description && (
        <p className="mt-4 text-sm leading-relaxed text-ink-muted line-clamp-6">
          {preview.description}
        </p>
      )}

      {preview.subjects && preview.subjects.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {preview.subjects.slice(0, 6).map((subject) => (
            <span
              key={subject}
              className="rounded-full border border-border bg-paper px-2.5 py-1 text-xs text-ink-muted"
            >
              {subject}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}
