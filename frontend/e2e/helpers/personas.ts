/** Shared Indian personas and catalogue fixtures for browser E2E runs. */

export const TEST_PASSWORD = 'Kolkata@2026'

export type TestPersona = {
  firstName: string
  lastName: string
  displayName: string
  email: (ts: number) => string
  username: (ts: number) => string
}

export const PRIYA: TestPersona = {
  firstName: 'Priya',
  lastName: 'Sharma',
  displayName: 'Priya Sharma',
  email: (ts) => `priya.sharma.${ts}@example.com`,
  username: (ts) => `priyasharma${ts}`,
}

export const ARJUN: TestPersona = {
  firstName: 'Arjun',
  lastName: 'Mehta',
  displayName: 'Arjun Mehta',
  email: (ts) => `arjun.mehta.${ts}@example.com`,
  username: (ts) => `arjunmehta${ts}`,
}

export const PRIYA_BOOK = {
  title: 'The God of Small Things',
  author: 'Arundhati Roy',
  authorSlug: 'arundhati-roy',
  pageCount: 340,
  progressPages: 118,
  librarySearch: 'God of Small',
  openLibraryQuery: "Midnight's Children",
} as const

export const ARJUN_BOOK = {
  title: 'The White Tiger',
  author: 'Aravind Adiga',
  pageCount: 320,
} as const

export const PRIYA_SHELF = {
  name: 'Monsoon Reading List',
  description: 'Priya’s shortlist for the rainy season in Bengaluru.',
} as const

export const ARJUN_SHELF = {
  name: 'Hyderabad Picks',
} as const

export const RECOMMENDATION_MESSAGE =
  'Arjun thinks you will enjoy this Booker winner from Delhi.'
