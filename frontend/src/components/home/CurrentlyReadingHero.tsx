import { Link } from 'react-router-dom'
import { Minus, Plus } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { UserBook } from '../../api/types'
import { libraryApi } from '../../api/library'
import { BookCover } from '../books/BookCover'
import { Button } from '../ui/Button'
import { QUERY_KEYS } from '../../lib/constants'
import { formatAuthors } from '../../lib/utils'
import { APP_ROUTES } from '../../api/paths'

export function CurrentlyReadingHero({
  entries,
  onOpen,
  onUpdated,
}: {
  entries: UserBook[]
  onOpen: (id: string) => void
  onUpdated?: () => void
}) {
  const queryClient = useQueryClient()

  const updateMutation = useMutation({
    mutationFn: ({ id, pages }: { id: string; pages: number }) =>
      libraryApi.update(id, { progressPages: pages, status: 'READING' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.reading.streak })
      onUpdated?.()
    },
  })

  const finishMutation = useMutation({
    mutationFn: ({ id, total }: { id: string; total?: number }) =>
      libraryApi.update(id, {
        status: 'READ',
        progressPages: total,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.reading.streak })
      onUpdated?.()
    },
  })

  if (entries.length === 0) {
    return (
      <section className="rounded-2xl border border-dashed border-border bg-paper-elevated/50 p-6 text-center">
        <p className="text-ink-muted">Nothing marked as reading.</p>
        <Link to={`${APP_ROUTES.library}?status=READING`} className="mt-3 inline-block">
          <Button variant="secondary" size="sm">
            Browse to read
          </Button>
        </Link>
      </section>
    )
  }

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h2 className="font-display text-lg font-semibold text-ink">Currently reading</h2>
        <Link
          to={`${APP_ROUTES.library}?status=READING`}
          className="text-sm font-medium text-accent hover:underline"
        >
          See all
        </Link>
      </div>
      <div className="flex gap-4 overflow-x-auto pb-2 snap-x snap-mandatory">
        {entries.map((entry) => {
          const step = 10
          const pages = entry.progressPages ?? 0
          const total = entry.pageCount ?? entry.book.pageCount
          const pct = entry.progressPercent ?? (total ? Math.min(100, Math.round((pages / total) * 100)) : null)

          return (
            <article
              key={entry.id}
              className="snap-start min-w-[280px] max-w-[320px] shrink-0 rounded-2xl border border-white/10 bg-paper-elevated p-4"
            >
              <div className="flex gap-3">
                <button type="button" onClick={() => onOpen(entry.id)} className="shrink-0">
                  <BookCover title={entry.book.title} coverUrl={entry.book.coverUrl} size="md" />
                </button>
                <div className="min-w-0 flex-1">
                  <button
                    type="button"
                    onClick={() => onOpen(entry.id)}
                    className="text-left font-display font-semibold text-ink line-clamp-2 hover:text-accent"
                  >
                    {entry.book.title}
                  </button>
                  <p className="mt-0.5 text-xs text-ink-muted line-clamp-1">
                    {formatAuthors(entry.book.authors)}
                  </p>
                  {pct != null && (
                    <div className="mt-3">
                      <div className="flex justify-between text-xs text-ink-muted mb-1">
                        <span>{entry.progressDisplay ?? `${pages}${total ? ` / ${total} pp` : ''}`}</span>
                        <span>{pct}%</span>
                      </div>
                      <div className="h-1.5 overflow-hidden rounded-full bg-white/10">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-accent to-sage transition-all"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  )}
                  <div className="mt-3 flex items-center gap-2">
                    <Button
                      type="button"
                      size="sm"
                      variant="secondary"
                      disabled={updateMutation.isPending}
                      onClick={() =>
                        updateMutation.mutate({
                          id: entry.id,
                          pages: Math.max(0, pages - step),
                        })
                      }
                      aria-label="Decrease progress"
                    >
                      <Minus className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="secondary"
                      disabled={updateMutation.isPending}
                      onClick={() =>
                        updateMutation.mutate({
                          id: entry.id,
                          pages: pages + step,
                        })
                      }
                      aria-label="Increase progress"
                    >
                      <Plus className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      disabled={finishMutation.isPending}
                      onClick={() =>
                        finishMutation.mutate({ id: entry.id, total: total ?? undefined })
                      }
                    >
                      Finished
                    </Button>
                  </div>
                </div>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
