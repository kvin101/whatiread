import { useQuery } from '@tanstack/react-query'
import {
  BookMarked,
  BookX,
  Eye,
  Loader2,
  UserMinus,
  UserPlus,
  Users,
} from 'lucide-react'
import { shelvesApi } from '../../api/shelves'
import type { ShelfEvent, ShelfEventType } from '../../api/types'
import { formatRelativeTime } from '../../lib/utils'
import { QUERY_KEYS } from '../../lib/constants'

function eventMessage(event: ShelfEvent): string {
  const p = event.payload ?? {}
  switch (event.eventType) {
    case 'SHELF_CREATED':
      return `Created shelf “${p.name ?? 'Untitled'}”`
    case 'SHELF_UPDATED':
      return 'Updated shelf details'
    case 'VISIBILITY_CHANGED':
      return p.to
        ? `Changed visibility to ${p.to.toLowerCase()}`
        : `Changed visibility`
    case 'BOOK_ADDED':
      return `Added “${p.bookTitle ?? 'a book'}”`
    case 'BOOK_REMOVED':
      return `Removed “${p.bookTitle ?? 'a book'}”`
    case 'MEMBER_ADDED':
      return `Invited ${p.memberName ?? 'someone'} as ${p.role ?? 'member'}`
    case 'MEMBER_REMOVED':
      return `Removed ${p.memberName ?? 'a member'}`
    case 'MEMBER_ROLE_CHANGED':
      return `Changed ${p.memberName ?? 'member'} role to ${p.role ?? 'member'}`
    default:
      return 'Shelf activity'
  }
}

const EVENT_ICONS: Record<ShelfEventType, typeof BookMarked> = {
  SHELF_CREATED: BookMarked,
  SHELF_UPDATED: Eye,
  VISIBILITY_CHANGED: Eye,
  BOOK_ADDED: BookMarked,
  BOOK_REMOVED: BookX,
  MEMBER_ADDED: UserPlus,
  MEMBER_REMOVED: UserMinus,
  MEMBER_ROLE_CHANGED: Users,
}

export function ShelfActivityFeed({ shelfId }: { shelfId: string }) {
  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.events(shelfId),
    queryFn: () => shelvesApi.listEvents(shelfId),
  })

  const events = data?.content ?? []

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-accent" />
      </div>
    )
  }

  if (events.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-border p-8 text-center text-ink-muted">
        No updates yet. Activity appears when books or members change.
      </p>
    )
  }

  return (
    <ul className="space-y-3">
      {events.map((event) => {
        const Icon = EVENT_ICONS[event.eventType] ?? BookMarked
        return (
          <li
            key={event.id}
            className="flex gap-3 rounded-xl border border-border bg-paper-elevated px-4 py-3"
          >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sage/10 text-sage">
              <Icon className="h-4 w-4" />
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm text-ink">
                <span className="font-medium">{event.actorDisplayName}</span>{' '}
                {eventMessage(event)}
              </p>
              <p className="mt-0.5 text-xs text-ink-muted">
                {formatRelativeTime(event.createdAt)}
              </p>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
