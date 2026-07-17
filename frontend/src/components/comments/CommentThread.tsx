import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Flag, MessageSquare, Pencil, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { commentsApi } from '../../api/comments'
import type { CommentTargetType } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { QUERY_KEYS } from '../../lib/constants'
import { getApiErrorMessage } from '../../lib/api'
import { cn, formatDateTime, formatRelativeTime } from '../../lib/utils'
import { BookLoader } from '../ui/BookLoader'
import { Button } from '../ui/Button'
import { useConfirm } from '../ui/ConfirmDialog'
import { Textarea } from '../ui/Textarea'

export function CommentThread({
  targetType,
  targetId,
  listMaxHeightClass,
}: {
  targetType: CommentTargetType
  targetId: string
  /** Tailwind max-height for the scrollable list (drawers use a shorter default). */
  listMaxHeightClass?: string
}) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [body, setBody] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editBody, setEditBody] = useState('')
  const listRef = useRef<HTMLDivElement>(null)

  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEYS.comments(targetType, targetId),
    queryFn: () => commentsApi.list(targetType, targetId),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.comments(targetType, targetId) })

  const createMutation = useMutation({
    mutationFn: () => commentsApi.create({ targetType, targetId, body: body.trim() }),
    onSuccess: () => {
      invalidate()
      setBody('')
      setError(null)
      listRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Failed to post')),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, text }: { id: string; text: string }) => commentsApi.update(id, text),
    onSuccess: () => {
      invalidate()
      setEditingId(null)
      setEditBody('')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => commentsApi.remove(id),
    onSuccess: invalidate,
  })

  const reportMutation = useMutation({
    mutationFn: (id: string) => commentsApi.report(id, 'Inappropriate content'),
    onSuccess: invalidate,
  })

  const comments = data?.content ?? []
  const totalComments = data?.totalElements ?? comments.length

  return (
    <section className="flex flex-col">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <MessageSquare className="h-4 w-4 text-sage" />
        Comments
        {!isLoading && totalComments > 0 && (
          <span className="font-normal text-ink-muted">({totalComments})</span>
        )}
      </h3>

      <div className="mt-3 shrink-0 rounded-xl border border-border bg-paper-elevated p-3">
        <Textarea
          rows={2}
          placeholder="Write a comment…"
          value={body}
          onChange={(e) => setBody(e.target.value)}
        />
        {error && <p className="mt-1 text-xs text-danger">{error}</p>}
        <Button
          size="sm"
          className="mt-2"
          disabled={!body.trim() || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          Post comment
        </Button>
      </div>

      {isLoading && (
        <div className="mt-3 manga-panel halftone-overlay rounded-xl px-4 py-3 flex items-center gap-3">
          <BookLoader className="scale-75 origin-left" />
          <p className="text-sm text-ink-muted panel-text">Loading comments…</p>
        </div>
      )}

      <div
        ref={listRef}
        className={cn(
          'mt-3 min-h-0 overflow-y-auto overscroll-contain pr-1',
          listMaxHeightClass ?? 'max-h-[min(50vh,28rem)]',
        )}
      >
        <ul className="space-y-3">
          {comments.map((c) => (
            <li key={c.id} className="rounded-xl border border-border bg-paper px-3 py-2">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-xs font-medium text-ink-muted">
                    {c.author.displayName?.trim() || 'Reader'}
                    {c.author.id === user?.id && ' · you'}
                  </p>
                  <time
                    className="text-[11px] text-ink-muted/80"
                    dateTime={c.createdAt}
                    title={formatDateTime(c.createdAt)}
                  >
                    {formatDateTime(c.createdAt)} · {formatRelativeTime(c.createdAt)}
                  </time>
                </div>
                {c.author.id === user?.id && editingId !== c.id && (
                  <div className="flex gap-2 shrink-0">
                    <button
                      type="button"
                      className="text-ink-muted hover:text-accent"
                      aria-label="Edit comment"
                      onClick={() => {
                        setEditingId(c.id)
                        setEditBody(c.body)
                      }}
                    >
                      <Pencil className="h-3.5 w-3.5" />
                    </button>
                    <button
                      type="button"
                      className="text-ink-muted hover:text-danger"
                      aria-label="Delete comment"
                      onClick={async () => {
                        const ok = await confirm({
                          title: 'Delete comment?',
                          description: 'Delete this comment?',
                          variant: 'danger',
                        })
                        if (ok) deleteMutation.mutate(c.id)
                      }}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )}
              </div>
              {editingId === c.id ? (
                <div className="mt-2">
                  <Textarea rows={2} value={editBody} onChange={(e) => setEditBody(e.target.value)} />
                  <div className="mt-2 flex gap-2">
                    <Button
                      size="sm"
                      disabled={!editBody.trim() || updateMutation.isPending}
                      onClick={() => updateMutation.mutate({ id: c.id, text: editBody.trim() })}
                    >
                      Save
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => setEditingId(null)}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : (
                <p className="mt-1 text-sm text-ink">{c.body}</p>
              )}
              {c.author.id !== user?.id && (
                <button
                  type="button"
                  className="mt-1 inline-flex items-center gap-1 text-xs text-ink-muted hover:text-danger disabled:opacity-50"
                  disabled={reportMutation.isPending}
                  onClick={() => reportMutation.mutate(c.id)}
                >
                  <Flag className="h-3 w-3" />
                  {reportMutation.isPending ? 'Reporting…' : 'Report'}
                </button>
              )}
            </li>
          ))}
          {!isLoading && comments.length === 0 && (
            <p className="text-sm text-ink-muted">No comments yet.</p>
          )}
        </ul>
      </div>
      {dialog}
    </section>
  )
}
