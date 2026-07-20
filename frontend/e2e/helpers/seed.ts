import { api } from './api'
import type { AuthSession } from './auth'
import {
  ARJUN,
  ARJUN_BOOK,
  ARJUN_SHELF,
  PRIYA,
  PRIYA_BOOK,
  PRIYA_SHELF,
  RECOMMENDATION_MESSAGE,
  TEST_PASSWORD,
} from './personas'

export type SeedContext = {
  auth: AuthSession
  userId: string
  userDisplayName: string
  userFirstName: string
  loginEmail: string
  loginUsername: string
  password: string
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
  friendFirstName: string
  friendBookTitle: string
  librarySearch: string
  openLibraryQuery: string
  readingProgressPages: number
  bookPageCount: number
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

  const reg = await api('POST', '/auth/register', {
    body: {
      email: PRIYA.email(ts),
      username: PRIYA.username(ts),
      password: TEST_PASSWORD,
      firstName: PRIYA.firstName,
      lastName: PRIYA.lastName,
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
    body: {
      title: PRIYA_BOOK.title,
      authors: [PRIYA_BOOK.author],
      pageCount: PRIYA_BOOK.pageCount,
    },
  })
  const bookId = asString(book.json?.id, 'book.id')

  const authorCheck = await api('GET', `/authors/${PRIYA_BOOK.authorSlug}`)
  if (authorCheck.status !== 200) {
    throw new Error(
      `Author "${PRIYA_BOOK.author}" not linked after book create (GET /authors/${PRIYA_BOOK.authorSlug} → ${authorCheck.status}). ` +
        'Redeploy API after BookServiceImpl author sync fix.',
    )
  }

  const entry = await api('POST', '/library', {
    token,
    body: {
      bookId,
      status: 'READING',
      progressPages: PRIYA_BOOK.progressPages,
    },
  })
  const userBookId = asString(entry.json?.id, 'library entry id')

  const shelf = await api('POST', '/shelves', {
    token,
    body: {
      name: PRIYA_SHELF.name,
      visibility: 'PUBLIC',
      description: PRIYA_SHELF.description,
    },
  })
  const shelfId = asString(shelf.json?.id, 'shelf.id')
  const shelfSlug = asString(shelf.json?.slug, 'shelf.slug')

  await api('POST', `/shelves/${shelfId}/books`, { token, body: { userBookId } })

  const friendReg = await api('POST', '/auth/register', {
    body: {
      email: ARJUN.email(ts),
      username: ARJUN.username(ts),
      password: TEST_PASSWORD,
      firstName: ARJUN.firstName,
      lastName: ARJUN.lastName,
    },
  })
  const friendUser = asRecord(friendReg.json?.user)
  const friendId = asString(friendUser.id, 'friend.id')
  const friendToken = asString(friendReg.json?.accessToken, 'friend accessToken')

  const req = await api('POST', '/friends/requests', { token, body: { userId: friendId } })
  const reqId = asString(req.json?.id, 'friend request id')
  await api('POST', `/friends/requests/${reqId}/accept`, { token: friendToken })

  const friendBook = await api('POST', '/books', {
    token: friendToken,
    body: {
      title: ARJUN_BOOK.title,
      authors: [ARJUN_BOOK.author],
      pageCount: ARJUN_BOOK.pageCount,
    },
  })
  const friendBookId = asString(friendBook.json?.id, 'friend book id')
  const fub = await api('POST', '/library', {
    token: friendToken,
    body: { bookId: friendBookId },
  })
  const fShelf = await api('POST', '/shelves', {
    token: friendToken,
    body: { name: ARJUN_SHELF.name, visibility: 'PUBLIC' },
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
    body: {
      toUserId: userId,
      bookId: friendBookId,
      message: RECOMMENDATION_MESSAGE,
    },
  })

  return {
    auth,
    userId,
    userDisplayName: PRIYA.displayName,
    userFirstName: PRIYA.firstName,
    loginEmail: PRIYA.email(ts),
    loginUsername: PRIYA.username(ts),
    password: TEST_PASSWORD,
    bookId,
    bookTitle: PRIYA_BOOK.title,
    authorName: PRIYA_BOOK.author,
    authorSlug: PRIYA_BOOK.authorSlug,
    userBookId,
    shelfId,
    shelfName: PRIYA_SHELF.name,
    shelfSlug,
    friendId,
    friendDisplayName: ARJUN.displayName,
    friendFirstName: ARJUN.firstName,
    friendBookTitle: ARJUN_BOOK.title,
    librarySearch: PRIYA_BOOK.librarySearch,
    openLibraryQuery: PRIYA_BOOK.openLibraryQuery,
    readingProgressPages: PRIYA_BOOK.progressPages,
    bookPageCount: PRIYA_BOOK.pageCount,
  }
}
