import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BookMarked, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { libraryApi } from '../../api/library'
import { ApiError } from '../../api/client'
import type { ReadingStatus, UserBook } from '../../api/types'
import { STATUS_LABELS, QUERY_KEYS } from '../../lib/constants'
import { formatAuthors } from '../../lib/utils'
import { CommentThread } from '../comments/CommentThread'
import { BookLoader } from '../ui/BookLoader'
import { Button } from '../ui/Button'
import { useConfirm } from '../ui/ConfirmDialog'
import { copy } from '../../lib/copy'
import { Drawer } from '../ui/Drawer'
import { Input, Label } from '../ui/Input'
import { Textarea } from '../ui/Textarea'
import { BookCover } from './BookCover'
import { StarRating } from './StarRating'
import { getApiErrorMessage } from '../../lib/api'

const STATUSES: ReadingStatus[] = ['TO_READ', 'READING', 'READ', 'DNF']

export function BookDetailDrawer({
  userBookId,
  viewEntry,
  open,
  onClose,
  onUpdated,
  onDeleted,
}: {
  userBookId: string | null
  /** Curator's shelf entry when viewing someone else's shelf. */
  viewEntry?: UserBook | null
  open: boolean
  onClose: () => void
  onUpdated?: () => void
  onDeleted?: () => void
}) {
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [progressPages, setProgressPages] = useState('')
  const [noteBody, setNoteBody] = useState('')
  const [error, setError] = useState<string | null>(null)

  const sharedContext = !!viewEntry
  const bookId = viewEntry?.book.id

  const { data: myEntryByBook, refetch: refetchMyByBook } = useQuery({
    queryKey: QUERY_KEYS.library.byBook(bookId!),
    queryFn: () => libraryApi.getByBookId(bookId!),
    enabled: open && sharedContext && !!bookId,
    retry: false,
  })

  const myEntry = sharedContext ? myEntryByBook : undefined
  const inMyLibrary = sharedContext ? !!myEntry : true

  const activeUserBookId = sharedContext ? (myEntry?.id ?? null) : userBookId

  const { data: fetchedEntry, isLoading } = useQuery({
    queryKey: QUERY_KEYS.library.detail(activeUserBookId!),
    queryFn: () => libraryApi.get(activeUserBookId!),
    enabled: open && !!activeUserBookId && inMyLibrary,
  })

  const displayEntry =
    sharedContext && !inMyLibrary ? viewEntry : (fetchedEntry ?? viewEntry)

  const bookIdForComments = displayEntry?.book.id ?? bookId
  const commentTargetType = bookIdForComments ? ('BOOK' as const) : undefined
  const commentTargetId = bookIdForComments

  useEffect(() => {
    if (displayEntry?.progressPages != null) {
      setProgressPages(String(displayEntry.progressPages))
    } else {
      setProgressPages('')
    }
  }, [displayEntry?.progressPages, activeUserBookId])

  useEffect(() => {
    if (!open) {
      setError(null)
    }
  }, [open])

  const updateMutation = useMutation({
    mutationFn: (body: Parameters<typeof libraryApi.update>[1]) =>
      libraryApi.update(activeUserBookId!, body),
    onSuccess: (updated) => {
      queryClient.setQueryData(QUERY_KEYS.library.detail(activeUserBookId!), updated)
      if (bookId) {
        queryClient.setQueryData(QUERY_KEYS.library.byBook(bookId!), updated)
      }
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      onUpdated?.()
      setError(null)
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Update failed')),
  })

  const rateWithoutLibraryMutation = useMutation({
    mutationFn: async (rating: number | null) => {
      const added = await libraryApi.add({ bookId: bookId!, status: 'TO_READ' })
      if (rating == null) {
        return libraryApi.update(added.id, { clearRating: true })
      }
      return libraryApi.update(added.id, { rating })
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(QUERY_KEYS.library.byBook(bookId!), updated)
      queryClient.setQueryData(QUERY_KEYS.library.detail(updated.id), updated)
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      refetchMyByBook()
      onUpdated?.()
      setError(null)
    },
    onError: (e) => {
      if (e instanceof ApiError && e.status === 409) {
        refetchMyByBook()
        return
      }
      setError(getApiErrorMessage(e, 'Could not save rating'))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => libraryApi.remove(activeUserBookId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      if (bookId) {
        queryClient.removeQueries({ queryKey: QUERY_KEYS.library.byBook(bookId) })
      }
      onDeleted?.()
      onClose()
    },
  })

  const addToLibraryMutation = useMutation({
    mutationFn: () =>
      libraryApi.add({ bookId: viewEntry!.book.id, status: 'TO_READ' }),
    onSuccess: (added) => {
      queryClient.setQueryData(QUERY_KEYS.library.byBook(viewEntry!.book.id), added)
      queryClient.setQueryData(QUERY_KEYS.library.detail(added.id), added)
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      refetchMyByBook()
      setError(null)
    },
    onError: (e) => {
      if (e instanceof ApiError && e.status === 409) {
        refetchMyByBook()
        return
      }
      setError(getApiErrorMessage(e, 'Could not add to your library'))
    },
  })

  const addNoteMutation = useMutation({
    mutationFn: (body: string) => libraryApi.addNote(activeUserBookId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.detail(activeUserBookId!) })
      setNoteBody('')
    },
  })

  const previewProgress = (e: UserBook | undefined) => {
    if (!e?.book.pageCount) return null
    const pages = parseInt(progressPages, 10)
    if (Number.isNaN(pages) || pages < 0) return null
    const pct = Math.min(100, Math.round((pages / e.book.pageCount) * 100))
    return `${pages} / ${e.book.pageCount} pages (${pct}%)`
  }

  const loading =
    (sharedContext && inMyLibrary && !!activeUserBookId && !fetchedEntry) ||
    (!sharedContext && !!userBookId && !fetchedEntry && isLoading)

  const handleRatingChange: (rating: number | undefined) => void = (rating) => {
    if (inMyLibrary && activeUserBookId) {
      if (rating == null) {
        updateMutation.mutate({ clearRating: true })
      } else {
        updateMutation.mutate({ rating })
      }
      return
    }
    if (rating == null) return
    rateWithoutLibraryMutation.mutate(rating)
  }

  return (
    <>
    <Drawer open={open} onClose={onClose} title={displayEntry?.book.title ?? 'Book details'}>
      {loading && (
        <div className="flex justify-center py-12">
          <BookLoader />
        </div>
      )}
      {displayEntry && !loading && (
        <div className="space-y-8">
          <div className="flex gap-4 manga-panel rounded-2xl p-4 halftone-overlay">
            <BookCover title={displayEntry.book.title} coverUrl={displayEntry.book.coverUrl} size="lg" />
            <div>
              <p className="text-sm text-ink-muted panel-text">{formatAuthors(displayEntry.book.authors)}</p>
              {displayEntry.book.pageCount && (
                <p className="mt-1 text-sm text-ink-muted">{displayEntry.book.pageCount} pages</p>
              )}
              {displayEntry.book.averageRating != null && (displayEntry.book.ratingCount ?? 0) > 0 && (
                <span className="rating-pill mt-2 inline-flex">
                  ★ {displayEntry.book.averageRating} · {displayEntry.book.ratingCount} ratings
                </span>
              )}
              {displayEntry.book.description && (
                <p className="mt-3 text-sm text-ink-muted">{displayEntry.book.description}</p>
              )}
            </div>
          </div>

          {sharedContext && !inMyLibrary ? (
            <section className="space-y-6">
              {error && (
                <p
                  className={`text-sm ${error.includes('already') ? 'text-ink-muted' : 'text-danger'}`}
                >
                  {error}
                </p>
              )}

              <section>
                <Label>Your rating</Label>
                <p className="mt-1 text-xs text-ink-muted mb-2">
                  Saved to your library when you rate — no need to add the book first.
                </p>
                <StarRating
                  value={undefined}
                  onChange={handleRatingChange}
                  readonly={rateWithoutLibraryMutation.isPending}
                />
              </section>

              {commentTargetId && commentTargetType && (
                <CommentThread targetType={commentTargetType} targetId={commentTargetId} />
              )}

              <Button
                onClick={() => {
                  setError(null)
                  addToLibraryMutation.mutate()
                }}
                disabled={addToLibraryMutation.isPending}
              >
                <BookMarked className="h-4 w-4" />
                Add to my library
              </Button>
            </section>
          ) : inMyLibrary && activeUserBookId && fetchedEntry ? (
            <>
              {error && <p className="text-sm text-danger">{error}</p>}

              <section>
                <Label>Reading status</Label>
                <div className="mt-2 flex flex-wrap gap-2">
                  {STATUSES.map((s) => (
                    <Button
                      key={s}
                      size="sm"
                      variant={fetchedEntry.status === s ? 'primary' : 'secondary'}
                      onClick={() => updateMutation.mutate({ status: s })}
                      disabled={updateMutation.isPending}
                    >
                      {STATUS_LABELS[s]}
                    </Button>
                  ))}
                </div>
              </section>

              <section>
                <Label>Your rating</Label>
                <div className="mt-2">
                  <StarRating
                    value={fetchedEntry.rating}
                    onChange={handleRatingChange}
                    readonly={updateMutation.isPending || rateWithoutLibraryMutation.isPending}
                  />
                </div>
              </section>

              {fetchedEntry.book.pageCount && (
                <section>
                  <Label>Pages read</Label>
                  <Input
                    type="number"
                    min={0}
                    max={fetchedEntry.book.pageCount}
                    value={progressPages}
                    onChange={(e) => setProgressPages(e.target.value)}
                    onBlur={() => {
                      const n = parseInt(progressPages, 10)
                      if (!Number.isNaN(n)) {
                        updateMutation.mutate({
                          status: fetchedEntry.status === 'TO_READ' ? 'READING' : fetchedEntry.status,
                          progressPages: n,
                        })
                      }
                    }}
                  />
                  <p className="mt-2 text-sm text-sage font-medium">
                    {previewProgress(fetchedEntry) ??
                      fetchedEntry.progressDisplay ??
                      'Enter pages to see progress'}
                  </p>
                </section>
              )}

              <section>
                <Label>Notes</Label>
                <ul className="mt-2 space-y-2">
                  {(fetchedEntry.notes ?? []).map((note) => (
                    <li
                      key={note.id}
                      className="rounded-xl bg-paper border border-border px-3 py-2 text-sm flex justify-between gap-2"
                    >
                      <span>{note.body}</span>
                      <button
                        type="button"
                        className="text-ink-muted hover:text-danger shrink-0"
                        aria-label="Delete note"
                        onClick={async () => {
                          const ok = await confirm({
                            title: 'Delete note?',
                            description: 'Delete this reading note?',
                            variant: 'danger',
                          })
                          if (!ok) return
                          libraryApi.deleteNote(activeUserBookId!, note.id).then(() => {
                            queryClient.invalidateQueries({
                              queryKey: QUERY_KEYS.library.detail(activeUserBookId!),
                            })
                          })
                        }}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </li>
                  ))}
                </ul>
                <div className="mt-3 flex gap-2">
                  <Textarea
                    placeholder="Add a reading note…"
                    value={noteBody}
                    onChange={(e) => setNoteBody(e.target.value)}
                    rows={2}
                  />
                </div>
                <Button
                  className="mt-2"
                  size="sm"
                  variant="secondary"
                  disabled={!noteBody.trim() || addNoteMutation.isPending}
                  onClick={() => addNoteMutation.mutate(noteBody.trim())}
                >
                  Save note
                </Button>
              </section>

              {commentTargetId && commentTargetType && (
                <CommentThread targetType={commentTargetType} targetId={commentTargetId} />
              )}

              <Button
                variant="danger"
                size="sm"
                onClick={async () => {
                  const ok = await confirm({
                    title: 'Remove from library?',
                    description: 'Remove this book from your library?',
                    confirmLabel: copy.confirm.remove,
                    variant: 'danger',
                  })
                  if (ok) deleteMutation.mutate()
                }}
              >
                <Trash2 className="h-4 w-4" />
                Remove from library
              </Button>
            </>
          ) : null}
        </div>
      )}
    </Drawer>
    {dialog}
    </>
  )
}
