import { Check, MessageCircle, UserPlus, Users } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { Conversation, FriendSummary, UserSuggestResult } from '../../api/types'
import { displayName, initials } from '../../lib/utils'
import { UserSuggestField } from '../users/UserSuggestField'
import { Button } from '../ui/Button'
import { EmptyState } from '../ui/EmptyState'
import { Input } from '../ui/Input'
import { Modal } from '../ui/Modal'

const PAGE_SIZE = 50

type ChatMode = 'direct' | 'group'

/** Friends picker with search — direct chat or multi-select group creation. */
export function NewChatModal({
  open,
  onClose,
  friends,
  conversations,
  onPick,
  onCreateGroup,
  pending,
}: {
  open: boolean
  onClose: () => void
  friends: FriendSummary[]
  conversations: Conversation[]
  onPick: (friendUserId: string) => void
  onCreateGroup: (name: string, memberUserIds: string[]) => void
  pending?: boolean
}) {
  const [mode, setMode] = useState<ChatMode>('direct')
  const [search, setSearch] = useState('')
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  const [groupName, setGroupName] = useState('')
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())

  useEffect(() => {
    if (!open) {
      setMode('direct')
      setSearch('')
      setVisibleCount(PAGE_SIZE)
      setGroupName('')
      setSelectedIds(new Set())
    }
  }, [open])

  const existingThreadIds = useMemo(
    () =>
      new Set(
        conversations
          .filter((c) => c.type === 'DIRECT' && c.otherParticipant)
          .map((c) => String(c.otherParticipant!.id)),
      ),
    [conversations],
  )

  const filtered = useMemo(() => {
    return [...friends].sort((a, b) => displayName(a).localeCompare(displayName(b)))
  }, [friends])

  const selectFriend = (user: UserSuggestResult) => {
    if (mode === 'direct') {
      onPick(user.id)
      return
    }
    toggleMember(user.id)
    setSearch(user.displayName)
  }

  const visible = filtered.slice(0, visibleCount)
  const hasMore = visibleCount < filtered.length

  const toggleMember = (friendId: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(friendId)) next.delete(friendId)
      else next.add(friendId)
      return next
    })
  }

  const canCreateGroup = groupName.trim().length > 0 && selectedIds.size > 0

  return (
    <Modal open={open} onClose={onClose} title="Start a chat">
      <div className="mb-4 flex gap-2">
        <Button
          type="button"
          size="sm"
          variant={mode === 'direct' ? 'primary' : 'secondary'}
          className="flex-1"
          onClick={() => setMode('direct')}
        >
          <MessageCircle className="h-4 w-4" />
          Direct
        </Button>
        <Button
          type="button"
          size="sm"
          variant={mode === 'group' ? 'primary' : 'secondary'}
          className="flex-1"
          onClick={() => setMode('group')}
        >
          <Users className="h-4 w-4" />
          New group
        </Button>
      </div>

      <p className="text-sm text-ink-muted mb-4">
        {mode === 'direct'
          ? 'Pick a friend from your crew. Search by name or email.'
          : 'Name your group and pick friends to add.'}
      </p>

      {mode === 'group' && (
        <Input
          placeholder="Group name"
          value={groupName}
          onChange={(e) => setGroupName(e.target.value)}
          className="mb-4"
          maxLength={255}
        />
      )}

      {friends.length > 0 && (
        <div className="mb-4">
          <UserSuggestField
            value={search}
            onValueChange={(value) => {
              setSearch(value)
              setVisibleCount(PAGE_SIZE)
            }}
            onSelect={selectFriend}
            scope="friends"
            placeholder="Search friends by username or name…"
            autoFocus
          />
        </div>
      )}

      {friends.length === 0 ? (
        <EmptyState
          icon={UserPlus}
          title="No friends yet"
          description="Add friends first — then you can gossip about books here."
          className="py-10"
        />
      ) : search.trim().length < 2 ? (
        <>
          <ul className="space-y-2 max-h-[50vh] overflow-y-auto">
            {visible.map((f) => {
              const name = displayName(f)
              const hasThread = mode === 'direct' && existingThreadIds.has(String(f.id))
              const selected = selectedIds.has(String(f.id))
              return (
                <li key={f.id}>
                  <button
                    type="button"
                    disabled={pending}
                    onClick={() =>
                      mode === 'direct' ? onPick(f.id) : toggleMember(String(f.id))
                    }
                    className={`flex w-full items-center gap-3 rounded-2xl border px-4 py-3 text-left transition-colors disabled:opacity-50 ${
                      mode === 'group' && selected
                        ? 'border-accent/50 bg-accent/10'
                        : 'border-white/10 bg-white/[0.03] hover:border-accent/30 hover:bg-accent-dim'
                    }`}
                  >
                    {mode === 'group' && (
                      <span
                        className={`flex h-5 w-5 shrink-0 items-center justify-center rounded border ${
                          selected
                            ? 'border-accent bg-accent text-void'
                            : 'border-white/20 bg-transparent'
                        }`}
                      >
                        {selected && <Check className="h-3 w-3" />}
                      </span>
                    )}
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-sage/15 text-sm font-semibold text-sage">
                      {initials(name)}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium text-ink">{name}</p>
                      <p className="truncate text-xs text-ink-muted">{f.email}</p>
                    </div>
                    {hasThread && (
                      <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-paper-elevated px-2 py-0.5 text-[10px] font-medium text-ink-muted">
                        <MessageCircle className="h-3 w-3" />
                        Has thread
                      </span>
                    )}
                  </button>
                </li>
              )
            })}
          </ul>
          {hasMore && (
            <Button
              variant="secondary"
              size="sm"
              className="mt-3 w-full"
              onClick={() => setVisibleCount((n) => n + PAGE_SIZE)}
            >
              Load more ({filtered.length - visibleCount} remaining)
            </Button>
          )}
        </>
      ) : null}

      {mode === 'group' && friends.length > 0 && (
        <Button
          className="w-full mt-4"
          disabled={!canCreateGroup || pending}
          onClick={() => onCreateGroup(groupName.trim(), [...selectedIds])}
        >
          Create group ({selectedIds.size} member{selectedIds.size === 1 ? '' : 's'})
        </Button>
      )}
      <Button variant="secondary" className="w-full mt-4" onClick={onClose}>
        Cancel
      </Button>
    </Modal>
  )
}
