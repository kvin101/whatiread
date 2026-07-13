import type { ReadingStatus, ShelfVisibility } from '../api/types'

export const STORAGE_KEYS = {
  auth: 'whatiread.auth',
} as const

export const QUERY_KEYS = {
  setup: { required: ['setup', 'required'] as const },
  admin: {
    users: (page: number, q: string) => ['admin', 'users', page, q] as const,
  },
  goals: (year: number) => ['goals', year] as const,
  library: {
    all: ['library'] as const,
    list: (filter: string, shelfFilter: string, search: string) =>
      ['library', filter, shelfFilter, search] as const,
    byBook: (bookId: string) => ['library', 'by-book', bookId] as const,
    detail: (userBookId: string) => ['library', userBookId] as const,
    forRec: ['library', 'for-rec'] as const,
  },
  shelves: {
    all: ['shelves'] as const,
    detail: (shelfId: string) => ['shelves', shelfId] as const,
    books: (shelfId: string) => ['shelves', shelfId, 'books'] as const,
    members: (shelfId: string) => ['shelves', shelfId, 'members'] as const,
    shareLinks: (shelfId: string) => ['shelves', shelfId, 'share-links'] as const,
    shared: (token: string) => ['shelves', 'shared', token] as const,
    events: (shelfId: string) => ['shelves', shelfId, 'events'] as const,
    explore: ['shelves', 'explore'] as const,
    exploreHint: ['shelves', 'explore', 'library-hint'] as const,
    system: (status: string) => ['shelves', 'system', status] as const,
  },
  friends: {
    all: ['friends'] as const,
    incoming: ['friends', 'incoming'] as const,
    outgoing: ['friends', 'outgoing'] as const,
    blocked: ['friends', 'blocked'] as const,
  },
  conversations: {
    all: ['conversations'] as const,
    unreadCount: ['conversations', 'unread-count'] as const,
  },
  messages: (conversationId: string) => ['messages', conversationId] as const,
  recommendations: {
    inbox: ['recommendations', 'inbox'] as const,
    sent: ['recommendations', 'sent'] as const,
    suggestions: ['recommendations', 'suggestions'] as const,
    all: ['recommendations'] as const,
  },
  profile: {
    detail: (userId: string) => ['profile', userId] as const,
    shelves: (userId: string) => ['profile', userId, 'shelves'] as const,
  },
  comments: (targetType: string, targetId: string) => ['comments', targetType, targetId] as const,
} as const

export const STATUS_LABELS: Record<ReadingStatus, string> = {
  TO_READ: 'To read',
  READING: 'Reading',
  READ: 'Read',
  DNF: 'Did not finish',
}

export const STATUS_COLORS: Record<ReadingStatus, string> = {
  TO_READ: 'bg-white/10 text-ink-muted border border-white/10',
  READING: 'bg-sage/15 text-sage border border-sage/20',
  READ: 'bg-accent-dim text-accent border border-accent/25',
  DNF: 'bg-danger/15 text-danger border border-danger/20',
}

export const VISIBILITY_LABELS: Record<ShelfVisibility, string> = {
  SECRET: 'Secret',
  PRIVATE: 'Private',
  FRIENDS: 'Friends',
  PUBLIC: 'Public',
}

export const VISIBILITY_HINTS: Record<ShelfVisibility, string> = {
  SECRET: 'Only you and collaborators — hidden from your profile',
  PRIVATE: 'Only you and collaborators',
  FRIENDS: 'Visible to friends on your profile',
  PUBLIC: 'Anyone can find on Explore (public filter) and your profile',
}

export const SHELF_EMOJI_ICONS = [
  '📚',
  '📖',
  '✨',
  '🌙',
  '🔥',
  '💡',
  '🎯',
  '🌿',
  '🚀',
  '💜',
  '📝',
  '🏛️',
  '🎭',
  '🌍',
  '⭐',
  '📕',
  '🧠',
  '☕',
  '🦋',
  '🔖',
]
