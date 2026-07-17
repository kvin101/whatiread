import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Users } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { friendsApi } from '../../api/friends'
import { conversationsApi } from '../../api/conversations'
import { shelvesApi } from '../../api/shelves'
import type { FriendSummary } from '../../api/types'
import { APP_ROUTES } from '../../api/paths'
import { QUERY_KEYS } from '../../lib/constants'
import { displayName } from '../../lib/utils'
import { getApiErrorMessage } from '../../lib/api'
import { DEFAULT_SHELF_ICON } from '../../lib/shelfIcons'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'
import { Input, Label } from '../ui/Input'
import { Textarea } from '../ui/Textarea'
import { IconPicker } from './IconPicker'

type Invite = { userId: string; name: string; role: 'EDITOR' | 'VIEWER' }

export function ShelfCircleWizard({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [step, setStep] = useState(0)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [icon, setIcon] = useState(DEFAULT_SHELF_ICON)
  const [invites, setInvites] = useState<Invite[]>([])
  const [startChat, setStartChat] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
    enabled: open,
  })

  const reset = () => {
    setStep(0)
    setName('')
    setDescription('')
    setIcon(DEFAULT_SHELF_ICON)
    setInvites([])
    setStartChat(true)
    setError(null)
  }

  const createMutation = useMutation({
    mutationFn: async () => {
      const shelf = await shelvesApi.create({
        name: name.trim(),
        description: description.trim() || undefined,
        icon,
        visibility: 'FRIENDS',
      })
      for (const invite of invites) {
        await shelvesApi.addMember(shelf.id, { userId: invite.userId, role: invite.role })
      }
      if (startChat && invites.length > 0) {
        await conversationsApi.createGroup(
          name.trim(),
          invites.map((i) => i.userId),
        )
      }
      return shelf
    },
    onSuccess: (shelf) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      reset()
      onClose()
      navigate(`${APP_ROUTES.shelf(shelf.id)}?addBooks=1`)
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Could not create book club shelf')),
  })

  const toggleFriend = (friend: FriendSummary, role: 'EDITOR' | 'VIEWER') => {
    setInvites((prev) => {
      const existing = prev.find((i) => i.userId === friend.id)
      if (existing) {
        return prev.filter((i) => i.userId !== friend.id)
      }
      return [...prev, { userId: friend.id, name: displayName(friend), role }]
    })
  }

  const handleClose = () => {
    reset()
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title="Start a book club" wide>
      <div className="space-y-5">
        <p className="text-sm text-ink-muted">
          Step {step + 1} of 3 — shared shelf, member roles, and optional group chat in one flow.
        </p>

        {step === 0 && (
          <>
            <div>
              <Label htmlFor="circleName">Club / shelf name</Label>
              <Input
                id="circleName"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="July book club"
                className="mt-1"
              />
            </div>
            <div>
              <Label htmlFor="circleDesc">Description</Label>
              <Textarea
                id="circleDesc"
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What are you reading together?"
                className="mt-1"
              />
            </div>
            <div>
              <Label>Icon</Label>
              <div className="mt-2">
                <IconPicker value={icon} onChange={setIcon} />
              </div>
            </div>
          </>
        )}

        {step === 1 && (
          <>
            <p className="text-sm text-ink-muted">Invite friends as editors (can add books) or viewers.</p>
            {friends.length === 0 ? (
              <p className="rounded-xl border border-dashed border-border p-4 text-sm text-ink-muted">
                Add friends first — then come back to invite them to your club shelf.
              </p>
            ) : (
              <ul className="max-h-56 space-y-2 overflow-y-auto">
                {friends.map((f) => {
                  const selected = invites.find((i) => i.userId === f.id)
                  return (
                    <li
                      key={f.id}
                      className="flex items-center justify-between gap-2 rounded-xl border border-border px-3 py-2"
                    >
                      <span className="text-sm text-ink">{displayName(f)}</span>
                      <div className="flex gap-1">
                        <Button
                          size="sm"
                          variant={selected?.role === 'EDITOR' ? 'primary' : 'secondary'}
                          onClick={() => toggleFriend(f, 'EDITOR')}
                        >
                          Editor
                        </Button>
                        <Button
                          size="sm"
                          variant={selected?.role === 'VIEWER' ? 'primary' : 'secondary'}
                          onClick={() => toggleFriend(f, 'VIEWER')}
                        >
                          Viewer
                        </Button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
            <label className="flex items-center gap-2 text-sm text-ink">
              <input
                type="checkbox"
                checked={startChat}
                onChange={(e) => setStartChat(e.target.checked)}
                className="rounded border-border"
              />
              Start a group chat with members
            </label>
          </>
        )}

        {step === 2 && (
          <div className="rounded-xl border border-border bg-paper-elevated p-4 space-y-2 text-sm">
            <p className="font-medium text-ink flex items-center gap-2">
              <Users className="h-4 w-4 text-accent" />
              {name.trim()}
            </p>
            <p className="text-ink-muted">Friends-only shelf · {invites.length} member(s) invited</p>
            {startChat && invites.length > 0 && (
              <p className="text-ink-muted">Group chat will be created</p>
            )}
          </div>
        )}

        {error && <p className="text-sm text-danger">{error}</p>}

        <div className="flex justify-between gap-2">
          <Button variant="secondary" onClick={step === 0 ? handleClose : () => setStep((s) => s - 1)}>
            {step === 0 ? 'Cancel' : 'Back'}
          </Button>
          {step < 2 ? (
            <Button disabled={step === 0 && !name.trim()} onClick={() => setStep((s) => s + 1)}>
              Next
            </Button>
          ) : (
            <Button disabled={!name.trim() || createMutation.isPending} onClick={() => createMutation.mutate()}>
              Create club shelf
            </Button>
          )}
        </div>
      </div>
    </Modal>
  )
}
