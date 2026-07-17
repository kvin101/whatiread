import { api } from './api'
import type { AuthSession } from './auth'

export type SeedContext = {
  auth: AuthSession
  userId: string
  bookId: string
  bookTitle: string
  authorName: string
  authorSlug: string
  userBookId: string
  shelfId: string
  shelfName: string
  shelfSlug: string
  friendId: string
  friendDisplayName: string
  friendBookTitle: string
}

function asRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object') return value as Record<string, unknown>
  throw new Error('Unexpected API response shape')
}

function asString(value: unknown, label: string): string {
  if (typeof value === 'string' && value.length > 0) return value
  throw new Error(`Missing ${label} in API response`)
}

export async function seedTestUser(): Promise<SeedContext> {
  const ts = Date.now()
  const bookTitle = `E2E Audit Book ${ts}`
  const authorName = 'Jane Author'
  const authorSlug = 'jane-author'
  const shelfName = 'E2E Audit Shelf'

  const reg = await api('POST', '/auth/register', {
    body: {
      email: `e2e-${ts}@test.com`,
      username: `e2e${ts}`,
      password: 'TestPass123!',
      firstName: 'E2E',
      lastName: 'User',
    },
  })
  if (reg.status !== 201 || !reg.json) throw new Error(`register failed: ${reg.status}`)

  const token = asString(reg.json.accessToken, 'accessToken')
  const user = asRecord(reg.json.user)
  const userId = asString(user.id, 'user.id')

  const auth: AuthSession = {
    accessToken: token,
    refreshToken: asString(reg.json.refreshToken, 'refreshToken'),
    user,
  }

  const book = await api('POST', '/books', {
    token,
    body: { title: bookTitle, authors: [authorName], pageCount: 300 },
  })
  const bookId = asString(book.json?.id, 'book.id')

  const authorCheck = await api('GET', `/authors/${authorSlug}`)
  if (authorCheck.status !== 200) {
    throw new Error(
      `Author "${authorName}" not linked after book create (GET /authors/${authorSlug} → ${authorCheck.status}). ` +
        'Redeploy API after BookServiceImpl author sync fix.',
    )
  }

  const entry = await api('POST', '/library', {
    token,
    body: { bookId, status: 'READING', progressPages: 42 },
  })
  const userBookId = asString(entry.json?.id, 'library entry id')

  const shelf = await api('POST', '/shelves', {
    token,
    body: { name: shelfName, visibility: 'PUBLIC', description: 'Browser test shelf' },
  })
  const shelfId = asString(shelf.json?.id, 'shelf.id')
  const shelfSlug = asString(shelf.json?.slug, 'shelf.slug')

  await api('POST', `/shelves/${shelfId}/books`, { token, body: { userBookId } })

  const friendReg = await api('POST', '/auth/register', {
    body: {
      email: `e2e-friend-${ts}@test.com`,
      username: `e2efriend${ts}`,
      password: 'TestPass123!',
      firstName: 'Friend',
      lastName: 'User',
    },
  })
  const friendUser = asRecord(friendReg.json?.user)
  const friendId = asString(friendUser.id, 'friend.id')
  const friendToken = asString(friendReg.json?.accessToken, 'friend accessToken')

  const req = await api('POST', '/friends/requests', { token, body: { userId: friendId } })
  const reqId = asString(req.json?.id, 'friend request id')
  await api('POST', `/friends/requests/${reqId}/accept`, { token: friendToken })

  const friendBookTitle = 'Friend Book'
  const friendBook = await api('POST', '/books', {
    token: friendToken,
    body: { title: friendBookTitle, authors: ['Friend Author'], pageCount: 200 },
  })
  const friendBookId = asString(friendBook.json?.id, 'friend book id')
  const fub = await api('POST', '/library', {
    token: friendToken,
    body: { bookId: friendBookId },
  })
  const fShelf = await api('POST', '/shelves', {
    token: friendToken,
    body: { name: 'Friend Public', visibility: 'PUBLIC' },
  })
  await api('POST', `/shelves/${asString(fShelf.json?.id, 'friend shelf id')}/books`, {
    token: friendToken,
    body: { userBookId: asString(fub.json?.id, 'friend library entry id') },
  })

  await api('PUT', '/me/reading-goal', {
    token,
    body: { year: 2026, targetBooks: 12, targetPages: 6000 },
  })

  await api('POST', '/recommendations', {
    token: friendToken,
    body: { toUserId: userId, bookId: friendBookId, message: 'Try this book from your friend!' },
  })

  return {
    auth,
    userId,
    bookId,
    bookTitle,
    authorName,
    authorSlug,
    userBookId,
    shelfId,
    shelfName,
    shelfSlug,
    friendId,
    friendDisplayName: 'Friend User',
    friendBookTitle,
  }
}
