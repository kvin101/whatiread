import { useQuery } from '@tanstack/react-query'
import { BookOpen, FileText } from 'lucide-react'
import { readingApi } from '../../api/reading'
import { QUERY_KEYS } from '../../lib/constants'

const YEAR = new Date().getFullYear()

function paceLabel(booksRead: number, targetBooks: number | null | undefined): string | null {
  if (!targetBooks || targetBooks <= 0) return null
  const dayOfYear = Math.floor(
    (Date.now() - new Date(YEAR, 0, 1).getTime()) / (1000 * 60 * 60 * 24),
  ) + 1
  const expected = Math.floor((targetBooks * dayOfYear) / 365)
  const delta = booksRead - expected
  if (delta >= 0) return 'On pace'
  return `${Math.abs(delta)} behind`
}

export function ReadingStatsPanel({ className }: { className?: string }) {
  const { data: stats } = useQuery({
    queryKey: QUERY_KEYS.reading.stats(YEAR),
    queryFn: () => readingApi.getStats(YEAR),
  })

  if (!stats) return null

  const pace = paceLabel(stats.booksRead, stats.targetBooks)

  return (
    <section className={className}>
      <h2 className="text-sm font-semibold text-ink">{YEAR} in books</h2>
      <div className="mt-3 grid gap-3 sm:grid-cols-3">
        <div className="rounded-xl border border-border bg-paper-elevated px-4 py-3">
          <div className="flex items-center gap-2 text-ink-muted">
            <BookOpen className="h-4 w-4" />
            <span className="text-xs font-medium uppercase tracking-wide">Books read</span>
          </div>
          <p className="mt-2 font-display text-2xl font-bold text-ink">{stats.booksRead}</p>
          {stats.targetBooks != null && stats.targetBooks > 0 && (
            <p className="mt-1 text-xs text-ink-muted">Goal: {stats.targetBooks}</p>
          )}
        </div>
        <div className="rounded-xl border border-border bg-paper-elevated px-4 py-3">
          <div className="flex items-center gap-2 text-ink-muted">
            <FileText className="h-4 w-4" />
            <span className="text-xs font-medium uppercase tracking-wide">Pages read</span>
          </div>
          <p className="mt-2 font-display text-2xl font-bold text-ink">{stats.pagesRead.toLocaleString()}</p>
          {stats.targetPages != null && stats.targetPages > 0 && (
            <p className="mt-1 text-xs text-ink-muted">Goal: {stats.targetPages.toLocaleString()}</p>
          )}
        </div>
        <div className="rounded-xl border border-border bg-paper-elevated px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-ink-muted">Pace</p>
          <p className={`mt-2 font-display text-lg font-semibold ${pace === 'On pace' ? 'text-sage' : pace ? 'text-amber-300' : 'text-ink-muted'}`}>
            {pace ?? 'Set a goal to track pace'}
          </p>
          {stats.booksProgressPercent != null && (
            <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-gradient-to-r from-accent to-sage"
                style={{ width: `${Math.min(100, stats.booksProgressPercent)}%` }}
              />
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
