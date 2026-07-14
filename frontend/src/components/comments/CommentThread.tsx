import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageSquare, Pencil, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { commentsApi } from '../../api/comments'
import type { CommentTargetType } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { QUERY_KEYS } from '../../lib/constants'
import { getApiErrorMessage } from '../../lib/api'
import { BookLoader } from '../ui/BookLoader'
import { Button } from '../ui/Button'
import { useConfirm } from '../ui/ConfirmDialog'
import { Textarea } from '../ui/Textarea'

export function CommentThread({
  targetType,
  targetId,
}: {
  targetType: CommentTargetType
  targetId: string
}) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [body, setBody] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editBody, setEditBody] = useState('')

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

  const comments = data?.content ?? []

  return (
    <section>
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <MessageSquare className="h-4 w-4 text-sage" />
        Comments
      </h3>
      {isLoading && (
        <div className="mt-2 manga-panel halftone-overlay rounded-xl px-4 py-3 flex items-center gap-3">
          <BookLoader className="scale-75 origin-left" />
          <p className="text-sm text-ink-muted panel-text">Loading comments…</p>
        </div>
      )}
      <ul className="mt-3 space-y-3">
        {comments.map((c) => (
          <li key={c.id} className="rounded-xl border border-border bg-paper px-3 py-2">
            <div className="flex items-start justify-between gap-2">
              <p className="text-xs font-medium text-ink-muted">
                {c.author.displayName?.trim() || 'Reader'}
                {c.author.id === user?.id && ' · you'}
              </p>
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
                className="mt-1 text-xs text-ink-muted hover:text-danger"
                onClick={() => commentsApi.report(c.id, 'Inappropriate')}
              >
                Report
              </button>
            )}
          </li>
        ))}
        {!isLoading && comments.length === 0 && (
          <p className="text-sm text-ink-muted">No comments yet.</p>
        )}
      </ul>
      <div className="mt-3">
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
      {dialog}
    </section>
  )
}
