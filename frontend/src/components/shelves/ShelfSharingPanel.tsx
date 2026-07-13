import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Copy, Link2, Loader2, UserPlus, X } from 'lucide-react'
import { useState } from 'react'
import { friendsApi } from '../../api/friends'
import { shelvesApi } from '../../api/shelves'
import { APP_ROUTES } from '../../api/paths'
import { displayName } from '../../lib/utils'
import { Button } from '../ui/Button'
import { Label } from '../ui/Input'
import { Select } from '../ui/Select'
import { useConfirm } from '../ui/ConfirmDialog'
import { copy } from '../../lib/copy'
import { QUERY_KEYS } from '../../lib/constants'

const ROLES = ['VIEWER', 'EDITOR', 'ADMIN'] as const

export function ShelfSharingPanel({
  shelfId,
  ownerId,
  canManage,
}: {
  shelfId: string
  ownerId: string
  canManage: boolean
}) {
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [inviteUserId, setInviteUserId] = useState('')
  const [inviteRole, setInviteRole] = useState<'VIEWER' | 'EDITOR' | 'ADMIN'>('VIEWER')
  const [error, setError] = useState<string | null>(null)
  const [copiedToken, setCopiedToken] = useState<string | null>(null)

  const { data: members = [], isLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.members(shelfId),
    queryFn: () => shelvesApi.listMembers(shelfId),
  })

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
    enabled: canManage,
  })

  const { data: shareLinks = [], isLoading: shareLinksLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.shareLinks(shelfId),
    queryFn: () => shelvesApi.listShareLinks(shelfId),
    enabled: canManage,
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.members(shelfId) })
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.shareLinks(shelfId) })
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.events(shelfId) })
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.detail(shelfId) })
  }

  const addMutation = useMutation({
    mutationFn: () => shelvesApi.addMember(shelfId, { userId: inviteUserId, role: inviteRole }),
    onSuccess: () => {
      setInviteUserId('')
      setError(null)
      invalidate()
    },
    onError: (e: Error) => setError(e.message),
  })

  const updateMutation = useMutation({
    mutationFn: ({
      userId,
      role,
    }: {
      userId: string
      role: 'ADMIN' | 'EDITOR' | 'VIEWER'
    }) => shelvesApi.updateMember(shelfId, userId, { role }),
    onSuccess: invalidate,
  })

  const removeMutation = useMutation({
    mutationFn: (userId: string) => shelvesApi.removeMember(shelfId, userId),
    onSuccess: invalidate,
  })

  const createShareLinkMutation = useMutation({
    mutationFn: () => shelvesApi.createShareLink(shelfId),
    onSuccess: invalidate,
    onError: (e: Error) => setError(e.message),
  })

  const revokeShareLinkMutation = useMutation({
    mutationFn: (linkId: string) => shelvesApi.revokeShareLink(shelfId, linkId),
    onSuccess: invalidate,
  })

  const shareUrl = (token: string) =>
    `${window.location.origin}${APP_ROUTES.sharedShelf(token)}`

  const copyShareLink = async (token: string) => {
    await navigator.clipboard.writeText(shareUrl(token))
    setCopiedToken(token)
    window.setTimeout(() => setCopiedToken(null), 2000)
  }

  const activeShareLinks = shareLinks.filter((link) => link.active)

  const memberRows = members.filter((m) => m.userId !== ownerId)
  const friendOptions = friends.filter(
    (f) => f.id !== ownerId && !members.some((m) => m.userId === f.id),
  )

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <Loader2 className="h-8 w-8 animate-spin text-accent" />
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-lg">
      {canManage && (
        <section className="rounded-2xl border border-border bg-paper p-4 space-y-3">
          <div className="flex items-center gap-2">
            <Link2 className="h-4 w-4 text-accent" />
            <h3 className="text-sm font-semibold text-ink">Share link</h3>
          </div>
          <p className="text-sm text-ink-muted">
            Anyone with the link can view this shelf and clone it to their account. Revoke links
            anytime.
          </p>
          {shareLinksLoading ? (
            <div className="flex justify-center py-4">
              <Loader2 className="h-6 w-6 animate-spin text-accent" />
            </div>
          ) : (
            <>
              {activeShareLinks.map((link) => (
                <div
                  key={link.id}
                  className="flex flex-wrap items-center gap-2 rounded-xl border border-border bg-paper-elevated px-3 py-2"
                >
                  <span className="flex-1 truncate text-xs text-ink-muted font-mono">
                    {shareUrl(link.token)}
                  </span>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => copyShareLink(link.token)}
                  >
                    {copiedToken === link.token ? (
                      <Check className="h-4 w-4" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                    {copiedToken === link.token ? 'Copied' : 'Copy'}
                  </Button>
                  <button
                    type="button"
                    className="rounded-lg p-1.5 text-ink-muted hover:bg-danger/10 hover:text-danger"
                    onClick={() => revokeShareLinkMutation.mutate(link.id)}
                    disabled={revokeShareLinkMutation.isPending}
                    aria-label="Revoke link"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              ))}
              <Button
                variant="secondary"
                onClick={() => createShareLinkMutation.mutate()}
                disabled={createShareLinkMutation.isPending}
              >
                <Link2 className="h-4 w-4" />
                {activeShareLinks.length > 0 ? 'Create new link' : 'Create share link'}
              </Button>
            </>
          )}
        </section>
      )}

      <p className="text-sm text-ink-muted">
        Private and secret shelves are visible only to people you invite. Assign roles to control
        who can edit books or manage sharing.
      </p>

      <ul className="space-y-2">
        {memberRows.length === 0 && (
          <li className="text-sm text-ink-muted rounded-xl border border-dashed border-border p-4 text-center">
            Only you have access so far.
          </li>
        )}
        {memberRows.map((m) => (
          <li
            key={m.id}
            className="flex flex-wrap items-center gap-2 rounded-xl border border-border bg-paper-elevated px-3 py-2"
          >
            <span className="flex-1 text-sm font-medium text-ink">
              {m.displayName ?? 'Member'}
            </span>
            {canManage ? (
              <>
                <Select
                  className="w-auto rounded-lg px-2 py-1"
                  value={m.role}
                  onChange={(e) =>
                    updateMutation.mutate({
                      userId: m.userId,
                      role: e.target.value as 'ADMIN' | 'EDITOR' | 'VIEWER',
                    })
                  }
                  disabled={updateMutation.isPending}
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r.charAt(0) + r.slice(1).toLowerCase()}
                    </option>
                  ))}
                </Select>
                <button
                  type="button"
                  className="rounded-lg p-1.5 text-ink-muted hover:bg-danger/10 hover:text-danger"
                  onClick={async () => {
                    const ok = await confirm({
                      title: 'Remove member?',
                      description: `Remove ${m.displayName ?? 'this member'} from this shelf?`,
                      confirmLabel: copy.confirm.remove,
                      variant: 'danger',
                    })
                    if (ok) removeMutation.mutate(m.userId)
                  }}
                  aria-label="Remove member"
                >
                  <X className="h-4 w-4" />
                </button>
              </>
            ) : (
              <span className="text-xs text-ink-muted">{m.role.toLowerCase()}</span>
            )}
          </li>
        ))}
      </ul>

      {canManage && (
        <section className="rounded-2xl border border-border bg-paper p-4 space-y-3">
          <Label>Invite a friend</Label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Select
              className="flex-1"
              value={inviteUserId}
              onChange={(e) => setInviteUserId(e.target.value)}
            >
              <option value="">Select friend…</option>
              {friendOptions.map((f) => (
                <option key={f.id} value={f.id}>
                  {displayName(f)}
                </option>
              ))}
            </Select>
            <Select
              className="w-auto sm:w-32"
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value as typeof inviteRole)}
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.charAt(0) + r.slice(1).toLowerCase()}
                </option>
              ))}
            </Select>
            <Button
              onClick={() => addMutation.mutate()}
              disabled={!inviteUserId || addMutation.isPending}
            >
              <UserPlus className="h-4 w-4" />
              Invite
            </Button>
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
        </section>
      )}
      {dialog}
    </div>
  )
}
