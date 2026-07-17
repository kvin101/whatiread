import { useMutation, useQuery } from '@tanstack/react-query'
import type { Recommendation, Shelf } from '../../api/types'
import { recommendationsApi } from '../../api/recommendations'
import { shelvesApi } from '../../api/shelves'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'
import { Label } from '../ui/Input'
import { QUERY_KEYS } from '../../lib/constants'
import { getApiErrorMessage } from '../../lib/api'
import { useState } from 'react'

export function AcceptRecommendationModal({
  rec,
  open,
  onClose,
  onAccepted,
}: {
  rec: Recommendation | null
  open: boolean
  onClose: () => void
  onAccepted: () => void
}) {
  const [shelfId, setShelfId] = useState<string>('')
  const [error, setError] = useState<string | null>(null)

  const { data: shelves = [] } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
    enabled: open && rec?.targetType === 'BOOK',
  })

  const acceptMutation = useMutation({
    mutationFn: async () => {
      if (!rec) return
      await recommendationsApi.accept(rec.id)
      if (rec.targetType === 'BOOK' && shelfId && rec.book) {
        await shelvesApi.addBook(shelfId, { bookId: rec.book.id })
      }
    },
    onSuccess: () => {
      setError(null)
      setShelfId('')
      onAccepted()
      onClose()
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Could not accept recommendation')),
  })

  if (!rec) return null

  const isBook = rec.targetType === 'BOOK'

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isBook ? 'Add to your library' : 'Accept shelf recommendation'}
    >
      <div className="space-y-4">
        {isBook ? (
          <>
            <p className="text-sm text-ink-muted">
              This book will be added to your library. Optionally place it on a shelf too.
            </p>
            <div>
              <Label htmlFor="acceptShelf">Add to shelf (optional)</Label>
              <select
                id="acceptShelf"
                value={shelfId}
                onChange={(e) => setShelfId(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-paper px-3 py-2 text-sm text-ink"
              >
                <option value="">Library only</option>
                {shelves.map((s: Shelf) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </>
        ) : (
          <p className="text-sm text-ink-muted">
            A copy of this shelf will be added to your account with full book list.
          </p>
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
