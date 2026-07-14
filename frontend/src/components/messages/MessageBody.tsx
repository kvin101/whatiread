import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { APP_ROUTES } from '../../api/paths'
import type { MessageMention } from '../../api/types'

function mentionHref(mention: MessageMention): string {
  switch (mention.type) {
    case 'USER':
      return APP_ROUTES.userProfile(mention.targetId)
    case 'SHELF':
      return APP_ROUTES.shelf(mention.targetId)
    case 'BOOK':
      return APP_ROUTES.library
    default:
      return '#'
  }
}

/** Renders message text with @mention labels as deep links. */
export function MessageBody({
  body,
  mentions = [],
  outgoing = false,
}: {
  body: string
  mentions?: MessageMention[]
  outgoing?: boolean
}) {
  const mentionClass = outgoing
    ? 'font-semibold underline underline-offset-2 text-void/90'
    : 'font-semibold underline underline-offset-2 text-accent'
  const textClass = outgoing ? 'text-inherit' : 'panel-text'

  if (!mentions.length) {
    return <span className={textClass}>{body}</span>
  }

  const sorted = [...mentions].sort((a, b) => b.label.length - a.label.length)
  const parts: ReactNode[] = []
  let remaining = body
  let key = 0

  while (remaining.length > 0) {
    let earliest = -1
    let match: MessageMention | null = null
    for (const m of sorted) {
      const needle = `@${m.label}`
      const idx = remaining.indexOf(needle)
      if (idx !== -1 && (earliest === -1 || idx < earliest)) {
        earliest = idx
        match = m
      }
    }
    if (earliest === -1 || !match) {
      parts.push(remaining)
      break
    }
    if (earliest > 0) {
      parts.push(remaining.slice(0, earliest))
    }
    const needle = `@${match.label}`
    parts.push(
      <Link
        key={key++}
        to={mentionHref(match)}
        className={mentionClass}
      >
        {needle}
      </Link>,
    )
    remaining = remaining.slice(earliest + needle.length)
  }

  return <span className={textClass}>{parts}</span>
}
