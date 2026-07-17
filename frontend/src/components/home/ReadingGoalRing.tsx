import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Target } from 'lucide-react'
import { readingApi } from '../../api/reading'
import { QUERY_KEYS } from '../../lib/constants'
import { Button } from '../ui/Button'
import { Input, Label } from '../ui/Input'
import { Modal } from '../ui/Modal'
import { useState } from 'react'

const YEAR = new Date().getFullYear()

export function ReadingGoalRing({ onEdit }: { onEdit?: () => void }) {
  const [editOpen, setEditOpen] = useState(false)
  const [target, setTarget] = useState('')
  const queryClient = useQueryClient()

  const { data: stats } = useQuery({
    queryKey: QUERY_KEYS.reading.stats(YEAR),
    queryFn: () => readingApi.getStats(YEAR),
  })

  const saveMutation = useMutation({
    mutationFn: (books: number) => readingApi.upsertGoal({ year: YEAR, targetBooks: books }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.reading.stats(YEAR) })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.reading.goal(YEAR) })
      setEditOpen(false)
    },
  })

  const goal = stats?.targetBooks
  const read = stats?.booksRead ?? 0
  const pct = goal && goal > 0 ? Math.min(100, Math.round((read / goal) * 100)) : null
  const dayOfYear = Math.floor((Date.now() - new Date(YEAR, 0, 1).getTime()) / (1000 * 60 * 60 * 24)) + 1
  const expected = goal && goal > 0 ? Math.floor((goal * dayOfYear) / 365) : null
  const pace =
    expected != null ? (read >= expected ? 'On pace' : `${expected - read} behind`) : null

  const openEdit = () => {
    setTarget(goal != null ? String(goal) : '')
    setEditOpen(true)
    onEdit?.()
  }

  return (
    <>
      <button
        type="button"
        onClick={openEdit}
        className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-paper-elevated px-3 py-1.5 text-sm transition-colors hover:border-accent/40"
      >
        <Target className="h-4 w-4 text-accent" />
        {goal != null && goal > 0 ? (
          <span className="text-ink">
            {read}/{goal} books {pct != null ? `(${pct}%)` : ''}
            {pace && <span className="text-ink-muted"> · {pace}</span>}
          </span>
        ) : (
          <span className="text-ink-muted">Set {YEAR} goal</span>
        )}
      </button>

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title={`${YEAR} reading goal`}>
        <div className="space-y-4">
          <div>
            <Label htmlFor="goalBooks">Books to read this year</Label>
            <Input
              id="goalBooks"
              type="number"
              min={1}
              max={999}
              value={target}
              onChange={(e) => setTarget(e.target.value)}
              placeholder="e.g. 24"
            />
          </div>
          <Button
            type="button"
            disabled={saveMutation.isPending || !target.trim()}
            onClick={() => saveMutation.mutate(parseInt(target, 10))}
          >
            Save goal
          </Button>
        </div>
      </Modal>
    </>
  )
}
