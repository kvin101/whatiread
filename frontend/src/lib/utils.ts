import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatAuthors(authors: string[] | undefined): string {
  if (!authors?.length) return 'Unknown author'
  if (authors.length <= 2) return authors.join(', ')
  return `${authors[0]} et al.`
}

export function displayName(user: {
  displayName?: string | null
  firstName?: string | null
  lastName?: string | null
  email?: string
}): string {
  if (user.displayName?.trim()) return user.displayName.trim()
  const parts = [user.firstName, user.lastName].filter(Boolean)
  if (parts.length) return parts.join(' ')
  return user.email ?? 'Reader'
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('')
}

export function formatRelativeTime(iso: string): string {
  const date = new Date(iso)
  const diffMs = Date.now() - date.getTime()
  const diffSec = Math.round(diffMs / 1000)
  if (diffSec < 60) return 'just now'
  const diffMin = Math.round(diffSec / 60)
  if (diffMin < 60) return `${diffMin}m ago`
  const diffHr = Math.round(diffMin / 60)
  if (diffHr < 24) return `${diffHr}h ago`
  const diffDay = Math.round(diffHr / 24)
  if (diffDay < 7) return `${diffDay}d ago`
  return date.toLocaleDateString()
}
