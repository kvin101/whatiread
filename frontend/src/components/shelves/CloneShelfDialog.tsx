import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { shelvesApi } from '../../api/shelves'
import type { Shelf } from '../../api/types'
import { Button } from '../ui/Button'
import { QUERY_KEYS } from '../../lib/constants'
import { APP_ROUTES } from '../../api/paths'

export function CloneShelfDialog({
  shelf,
  open,
  onClose,
}: {
  shelf: Pick<Shelf, 'id' | 'name'>
  open: boolean
  onClose: () => void
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')

  useEffect(() => {
    if (open) {
      setName(`Copy of ${shelf.name}`)
    }
  }, [open, shelf.name])

  const cloneMutation = useMutation({
    mutationFn: () =>
      shelvesApi.clone(shelf.id, {
        name: name.trim() || `Copy of ${shelf.name}`,
        includeBooks: true,
        visibility: 'PRIVATE',
      }),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      onClose()
      navigate(APP_ROUTES.shelf(created.id))
    },
  })

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-paper-elevated border border-border p-6 shadow-xl">
        <h2 className="font-display text-lg font-semibold text-ink">Clone shelf</h2>
        <p className="mt-1 text-sm text-ink-muted">
          Creates a private copy in your account with all books from this shelf.
        </p>
        <input
          className="mt-4 w-full rounded-lg border border-border bg-paper px-3 py-2 text-sm"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Shelf name"
        />
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={() => cloneMutation.mutate()}
            disabled={cloneMutation.isPending || !name.trim()}
          >
            Clone
          </Button>
        </div>
      </div>
    </div>
  )
}

export function CloneShelfButton({
  shelf,
  className,
}: {
  shelf: Pick<Shelf, 'id' | 'name'>
  className?: string
}) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button
        type="button"
        size="sm"
        variant="secondary"
        className={className}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setOpen(true)
        }}
      >
        Clone
      </Button>
      <CloneShelfDialog shelf={shelf} open={open} onClose={() => setOpen(false)} />
    </>
  )
}
