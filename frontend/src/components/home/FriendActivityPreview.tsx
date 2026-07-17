import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { activityApi } from '../../api/activity'
import { QUERY_KEYS } from '../../lib/constants'
import { formatRelativeTime } from '../../lib/utils'
import { APP_ROUTES } from '../../api/paths'
import type { ActivityItem } from '../../api/types'

function activityText(item: ActivityItem): string {
  const p = item.payload ?? {}
  switch (item.eventType) {
    case 'BOOK_ADDED':
      return `added “${p.bookTitle ?? 'a book'}” to ${item.shelfName}`
    case 'BOOK_REMOVED':
      return `removed “${p.bookTitle ?? 'a book'}” from ${item.shelfName}`
    case 'SHELF_CREATED':
      return `created shelf “${p.name ?? item.shelfName}”`
    case 'SHELF_UPDATED':
      return `updated ${item.shelfName}`
    case 'VISIBILITY_CHANGED':
      return `changed ${item.shelfName} visibility`
    case 'MEMBER_ADDED':
      return `invited someone to ${item.shelfName}`
    case 'MEMBER_REMOVED':
      return `removed a member from ${item.shelfName}`
    case 'MEMBER_ROLE_CHANGED':
      return `changed a role on ${item.shelfName}`
    default:
      return `updated ${item.shelfName}`
  }
}

export function FriendActivityPreview() {
  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEYS.activity.preview,
    queryFn: () => activityApi.list({ limit: 5 }),
  })

  const items = data?.items ?? []

  if (isLoading) {
    return <div className="h-24 animate-pulse rounded-xl bg-white/5" />
  }

  if (items.length === 0) {
    return (
      <p className="rounded-xl border border-dashed border-border px-4 py-6 text-center text-sm text-ink-muted">
        Add friends to see shelf activity here.
      </p>
    )
  }

  return (
    <section>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-ink">Friend activity</h2>
        <Link to={APP_ROUTES.activity} className="text-sm font-medium text-accent hover:underline">
          See all
        </Link>
      </div>
      <ul className="space-y-2">
        {items.map((item) => (
          <li key={item.id}>
            <Link
              to={APP_ROUTES.shelf(item.shelfId)}
              className="block rounded-xl border border-border bg-paper-elevated px-4 py-3 transition-colors hover:border-accent/30"
            >
              <p className="text-sm text-ink">
                <span className="font-medium">{item.actorDisplayName}</span>{' '}
                {activityText(item)}
              </p>
              <p className="mt-0.5 text-xs text-ink-muted">{formatRelativeTime(item.createdAt)}</p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
