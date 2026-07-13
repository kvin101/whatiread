import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { shelvesApi } from '../../api/shelves'
import type { Shelf, ShelfVisibility } from '../../api/types'
import { ApiError } from '../../api/client'
import { IconPicker } from './IconPicker'
import { Button } from '../ui/Button'
import { useConfirm } from '../ui/ConfirmDialog'
import { Input, Label } from '../ui/Input'
import { Modal } from '../ui/Modal'
import { Textarea } from '../ui/Textarea'
import { VISIBILITY_HINTS, VISIBILITY_LABELS } from '../../lib/constants'
import { cn } from '../../lib/utils'
import { QUERY_KEYS } from '../../lib/constants'

export function EditShelfModal({
  shelf,
  open,
  onClose,
  onDeleted,
}: {
  shelf: Shelf
  open: boolean
  onClose: () => void
  onDeleted?: () => void
}) {
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [name, setName] = useState(shelf.name)
  const [description, setDescription] = useState(shelf.description ?? '')
  const [icon, setIcon] = useState(shelf.icon ?? '📚')
  const [visibility, setVisibility] = useState<ShelfVisibility>(shelf.visibility)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setName(shelf.name)
      setDescription(shelf.description ?? '')
      setIcon(shelf.icon ?? '📚')
      setVisibility(shelf.visibility)
      setError(null)
    }
  }, [open, shelf])

  const updateMutation = useMutation({
    mutationFn: () =>
      shelvesApi.update(shelf.id, {
        name: name.trim(),
        description: description.trim() || undefined,
        icon,
        visibility,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.detail(shelf.id) })
      onClose()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Update failed'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => shelvesApi.remove(shelf.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      onClose()
      onDeleted?.()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Delete failed'),
  })

  return (
    <Modal open={open} onClose={onClose} title="Edit shelf" wide>
      <div className="space-y-5">
        <div>
          <Label htmlFor="editShelfName">Name</Label>
          <Input
            id="editShelfName"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1"
          />
        </div>
        <div>
          <Label htmlFor="editShelfDesc">Description</Label>
          <Textarea
            id="editShelfDesc"
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="mt-1"
          />
        </div>
        <div>
          <Label>Icon</Label>
          <div className="mt-2">
            <IconPicker value={icon} onChange={setIcon} />
          </div>
        </div>
        <div>
          <Label>Visibility</Label>
          <div className="mt-2 flex flex-col gap-2">
            {(['SECRET', 'PRIVATE', 'FRIENDS', 'PUBLIC'] as ShelfVisibility[]).map((v) => (
              <button
                key={v}
                type="button"
                onClick={() => setVisibility(v)}
                className={cn(
                  'rounded-xl border px-4 py-3 text-left transition-colors',
                  visibility === v
                    ? 'border-accent bg-accent/5'
                    : 'border-border hover:border-ink/20',
                )}
              >
                <span className="text-sm font-medium text-ink">{VISIBILITY_LABELS[v]}</span>
                <p className="mt-0.5 text-xs text-ink-muted">{VISIBILITY_HINTS[v]}</p>
              </button>
            ))}
          </div>
        </div>
        {error && <p className="text-sm text-danger">{error}</p>}
        <div className="flex flex-wrap gap-2 justify-between">
          <Button
            variant="danger"
            size="sm"
            disabled={deleteMutation.isPending}
            onClick={async () => {
              const ok = await confirm({
                title: 'Delete shelf?',
                description: `Delete "${shelf.name}"? This cannot be undone.`,
                variant: 'danger',
              })
              if (ok) deleteMutation.mutate()
            }}
          >
            Delete shelf
          </Button>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={onClose}>
              Cancel
            </Button>
            <Button
              disabled={!name.trim() || updateMutation.isPending}
              onClick={() => updateMutation.mutate()}
            >
              Save changes
            </Button>
          </div>
        </div>
      </div>
      {dialog}
    </Modal>
  )
}
