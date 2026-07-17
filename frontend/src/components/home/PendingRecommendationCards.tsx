import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, X } from 'lucide-react'
import { recommendationsApi } from '../../api/recommendations'
import { shelvesApi } from '../../api/shelves'
import type { Recommendation } from '../../api/types'
import { BookCover } from '../books/BookCover'
import { ShelfIcon } from '../shelves/ShelfIcon'
import { Button } from '../ui/Button'
import { Modal } from '../ui/Modal'
import { Label } from '../ui/Input'
import { QUERY_KEYS } from '../../lib/constants'
import { displayName, formatAuthors } from '../../lib/utils'
import { getApiErrorMessage } from '../../lib/api'
import { useState } from 'react'
import { removeArrayQueryItem } from '../../lib/queryCache'

function RecAcceptModal({
  rec,
  open,
  onClose,
  onDone,
}: {
  rec: Recommendation
  open: boolean
  onClose: () => void
  onDone: () => void
}) {
  const [shelfId, setShelfId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const { data: shelves = [] } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
    enabled: open && rec.targetType === 'BOOK',
  })

  const acceptMutation = useMutation({
    mutationFn: async () => {
      await recommendationsApi.accept(rec.id)
      if (rec.targetType === 'BOOK' && shelfId && rec.book) {
        await shelvesApi.addBook(shelfId, { bookId: rec.book.id })
      }
    },
    onSuccess: () => {
      onDone()
      onClose()
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Could not accept')),
  })

  return (
    <Modal open={open} onClose={onClose} title="Accept recommendation">
      <div className="space-y-4">
        {rec.targetType === 'BOOK' && (
          <>
            <p className="text-sm text-ink-muted">Add to your library and optionally place on a shelf.</p>
            <div>
              <Label htmlFor="homeAcceptShelf">Shelf (optional)</Label>
              <select
                id="homeAcceptShelf"
                value={shelfId}
                onChange={(e) => setShelfId(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-paper px-3 py-2 text-sm"
              >
                <option value="">Library only</option>
                {shelves.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </>
        )}
        {error && <p className="text-sm text-danger">{error}</p>}
        <div className="flex gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={acceptMutation.isPending} onClick={() => acceptMutation.mutate()}>
            Accept
          </Button>
        </div>
      </div>
    </Modal>
  )
}

export function PendingRecommendationCards({
  recs,
}: {
  recs: Recommendation[]
}) {
  const queryClient = useQueryClient()
  const [acceptRec, setAcceptRec] = useState<Recommendation | null>(null)

  const dismissMutation = useMutation({
    mutationFn: recommendationsApi.dismiss,
    onSuccess: (_, id) => removeArrayQueryItem(queryClient, QUERY_KEYS.recommendations.inbox, id),
  })

  if (recs.length === 0) return null

  return (
    <>
      <div className="grid gap-3 sm:grid-cols-2">
        {recs.slice(0, 4).map((rec) => (
          <article
            key={rec.id}
            className="flex gap-3 rounded-xl border border-accent/20 bg-paper-elevated p-3"
          >
            {rec.targetType === 'SHELF' && rec.shelf ? (
              <ShelfIcon icon={rec.shelf.icon} size="sm" />
            ) : rec.book ? (
              <BookCover title={rec.book.title} coverUrl={rec.book.coverUrl} size="sm" />
            ) : null}
            <div className="min-w-0 flex-1">
              <p className="text-xs text-ink-muted">{displayName(rec.fromUser)} recommends</p>
              <p className="text-sm font-medium text-ink line-clamp-2">
                {rec.shelf?.name ?? rec.book?.title}
              </p>
              {rec.book && (
                <p className="text-xs text-ink-muted line-clamp-1">{formatAuthors(rec.book.authors)}</p>
              )}
              <div className="mt-2 flex gap-1">
                <Button size="sm" className="h-7 px-2 text-xs" onClick={() => setAcceptRec(rec)}>
                  <Check className="h-3.5 w-3.5" />
                  Accept
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  className="h-7 px-2 text-xs"
                  disabled={dismissMutation.isPending}
                  onClick={() => dismissMutation.mutate(rec.id)}
                >
                  <X className="h-3.5 w-3.5" />
                  Pass
                </Button>
              </div>
            </div>
          </article>
        ))}
      </div>
      {acceptRec && (
        <RecAcceptModal
          rec={acceptRec}
          open={!!acceptRec}
          onClose={() => setAcceptRec(null)}
          onDone={() => {
            removeArrayQueryItem(queryClient, QUERY_KEYS.recommendations.inbox, acceptRec.id)
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
          }}
        />
      )}
    </>
  )
}
