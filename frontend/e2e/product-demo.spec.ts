/**
 * WhatIRead — product demo suite with Indian personas (Priya Sharma & Arjun Mehta).
 * Serial run: meaningful journey names, every major flow exercised for recordings.
 */
import { test, expect } from '@playwright/test'
import { visitAuthed, visitLoggedOut } from './helpers/fixtures'
import { visitEverySidebarDestination } from './helpers/navigation'
import {
  cancelModal,
  clickFilterChip,
  closeDrawer,
  exerciseBookDrawerTabs,
  openBookDrawer,
} from './helpers/pages'
import { visibleClick, visibleType, visibleTypeNumber } from './helpers/interactions'
import { seedTestUser, type SeedContext } from './helpers/seed'
import { pauseForViewer } from './helpers/timing'

let priya: SeedContext

test.describe.configure({ mode: 'serial' })

test.beforeAll(async () => {
  priya = await seedTestUser()
})

test.describe('01 — Welcome & account access', () => {
  test('Visitor opens Sign in, then hops to Create account', async ({ page }) => {
    await visitLoggedOut(page, '/login')
    await expect(page.getByLabel(/email or username/i)).toBeVisible()
    await expect(page.getByLabel(/^password$/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
    await visibleClick(page.getByRole('link', { name: /create account/i }))
    await expect(page).toHaveURL(/\/register/)
  })

  test('Visitor opens Create account, then returns to Sign in', async ({ page }) => {
    await visitLoggedOut(page, '/register')
    await expect(page.getByRole('heading', { name: /create account/i })).toBeVisible()
    await expect(page.getByLabel(/first name/i)).toBeVisible()
    await expect(page.getByLabel(/email/i)).toBeVisible()
    await visibleClick(page.getByRole('link', { name: /sign in/i }))
    await expect(page).toHaveURL(/\/login/)
  })
})

test.describe('02 — Priya tours the app', () => {
  test('Priya visits every section from the sidebar', async ({ page }) => {
    await visitAuthed(page, priya, '/')
    await visitEverySidebarDestination(page)
  })

  test('Priya opens her public profile and jumps to account settings', async ({ page }) => {
    await visitAuthed(page, priya, '/')
    await visibleClick(
      page.getByRole('complementary').getByRole('link', { name: new RegExp(priya.userDisplayName, 'i') }),
    )
    await expect(page).toHaveURL(new RegExp(`/users/${priya.userId}`))
    await visibleClick(page.getByRole('link', { name: /edit profile/i }))
    await expect(page).toHaveURL(/\/settings/)
  })
})

test.describe('03 — Priya’s home dashboard', () => {
  test('Priya searches Open Library from Add book and closes the modal', async ({ page }) => {
    await visitAuthed(page, priya, '/')
    await visibleClick(page.getByRole('button', { name: 'Add book' }))
    await expect(page.getByRole('heading', { name: 'Add a book' })).toBeVisible()
    await visibleType(page.getByPlaceholder('Start typing a book title…'), priya.openLibraryQuery)
    await visibleClick(page.getByRole('button', { name: 'Search' }))
    await expect(
      page.getByText(new RegExp(priya.openLibraryQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i')).first(),
    ).toBeVisible({ timeout: 30_000 })
    await visibleClick(page.getByRole('button', { name: 'Done' }))
  })

  test('Priya checks reading goal, streak, and activity shortcuts', async ({ page }) => {
    await visitAuthed(page, priya, '/')
    const readingLink = page.getByRole('link', { name: /reading \(\d+\)/i })
    if (await readingLink.isVisible()) {
      await visibleClick(readingLink)
      await expect(page).toHaveURL(/status=READING/)
      await page.goBack({ waitUntil: 'domcontentloaded' })
      await pauseForViewer(page)
    }
    await visibleClick(page.getByRole('button', { name: /books|goal/i }).first())
    await pauseForViewer(page)
    await page.keyboard.press('Escape')
    await visibleClick(page.getByTitle('Reading streak'))
    await pauseForViewer(page)
    await page.keyboard.press('Escape')
    const activityLink = page.getByRole('link', { name: /^activity$/i })
    if (await activityLink.isVisible()) {
      await visibleClick(activityLink)
      await expect(page).toHaveURL(/\/activity/)
    }
  })
})

test.describe('04 — Priya’s library (My Books)', () => {
  test('Priya filters her shelf by every reading status', async ({ page }) => {
    await visitAuthed(page, priya, '/library')
    await clickFilterChip(page, /Reading \(\d+\)/, 'Reading status')
    await clickFilterChip(page, 'To read', 'Reading status', true)
    await clickFilterChip(page, 'Read', 'Reading status', true)
    await clickFilterChip(page, 'Did not finish', 'Reading status', true)
    await clickFilterChip(page, 'All', 'Reading status', true)
  })

  test('Priya searches for God of Small Things and sorts by title', async ({ page }) => {
    await visitAuthed(page, priya, '/library')
    await visibleType(page.getByLabel('Search library'), priya.librarySearch)
    await pauseForViewer(page)
    await visibleClick(page.getByLabel('Sort order'))
    await page.getByLabel('Sort order').selectOption('title')
    await pauseForViewer(page)
    const clearBtn = page.getByRole('button', { name: 'Clear' })
    if (await clearBtn.isVisible()) await visibleClick(clearBtn)
  })

  test('Priya logs reading progress on The God of Small Things', async ({ page }) => {
    await visitAuthed(page, priya, '/library')
    await clickFilterChip(page, /Reading \(\d+\)/, 'Reading status')
    const drawer = await openBookDrawer(page, priya.bookTitle)
    const updatedPages = String(priya.readingProgressPages + 2)
    await visibleTypeNumber(drawer.getByRole('spinbutton'), updatedPages)
    await drawer.getByRole('spinbutton').blur()
    await expect(
      drawer.getByText(new RegExp(`${updatedPages}\\s*\\/\\s*${priya.bookPageCount}`)),
    ).toBeVisible({ timeout: 10_000 })
    await closeDrawer(page)
  })

  test('Priya reviews notes, comments, rating, and status in the book drawer', async ({ page }) => {
    await visitAuthed(page, priya, '/library')
    await exerciseBookDrawerTabs(page, priya.bookTitle, priya.userFirstName)
  })
})

test.describe('05 — Book page & Arundhati Roy', () => {
  test('Priya leaves a comment on The God of Small Things', async ({ page }) => {
    await visitAuthed(page, priya, `/books/${priya.bookId}`)
    await expect(page.getByRole('heading', { name: priya.bookTitle })).toBeVisible()
    const comment = `Kerala monsoon atmosphere is unforgettable — Priya, ${Date.now()}`
    const postBtn = page.getByRole('button', { name: 'Post comment' })
    await visibleType(page.getByPlaceholder('Write a comment…'), comment)
    await expect(postBtn).toBeEnabled({ timeout: 10_000 })
    await visibleClick(postBtn)
    await expect(page.getByRole('list').getByText(comment)).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('time').first()).toBeVisible()
  })

  test('Priya opens Arundhati Roy’s author page from the drawer', async ({ page }) => {
    await visitAuthed(page, priya, '/library')
    await visibleClick(page.getByRole('button', { name: new RegExp(priya.bookTitle) }))
    await visibleClick(page.getByRole('dialog').getByRole('link', { name: priya.authorName }))
    await expect(page.getByRole('heading', { name: priya.authorName })).toBeVisible()
    await visibleClick(page.getByRole('button', { name: 'All books' }))
    await expect(page.getByRole('link', { name: priya.bookTitle })).toBeVisible()
    await clickFilterChip(page, 'Your books', 'View')
    await pauseForViewer(page)
    const filterLink = page.getByRole('link', { name: /filter my library/i })
    if (await filterLink.isVisible()) await visibleClick(filterLink)
  })
})

test.describe('06 — Priya’s shelves', () => {
  test('Priya filters shelves and opens system shortcuts', async ({ page }) => {
    await visitAuthed(page, priya, '/shelves')
    await clickFilterChip(page, 'Public', 'Visibility')
    await clickFilterChip(page, 'Friends', 'Visibility')
    await clickFilterChip(page, 'Private', 'Visibility')
    await clickFilterChip(page, 'All', 'Visibility')
    await visibleClick(page.getByRole('main').getByRole('link', { name: 'To read', exact: true }))
    await expect(page).toHaveURL(/\/shelves\/system\/TO_READ/)
    await page.goBack({ waitUntil: 'domcontentloaded' })
    await visibleClick(page.getByRole('link', { name: 'Explore shelves' }))
    await expect(page).toHaveURL(/\/explore/)
  })

  test('Priya creates Chennai Weekend Reads shelf', async ({ page }) => {
    await visitAuthed(page, priya, '/shelves')
    await visibleClick(page.getByRole('button', { name: 'New shelf' }))
    const name = `Chennai Weekend Reads ${Date.now()}`
    await visibleType(page.getByLabel('Name'), name)
    await visibleType(page.getByLabel('Short note'), 'Shortlist for train journeys down south.')
    await visibleClick(page.getByRole('button', { name: 'Create shelf' }))
    await expect(page.getByRole('heading', { name })).toBeVisible({ timeout: 15_000 })
  })

  test('Priya opens Start book club wizard and steps back', async ({ page }) => {
    await visitAuthed(page, priya, '/shelves')
    await visibleClick(page.getByRole('button', { name: 'Start book club' }))
    await expect(page.getByRole('heading', { name: 'Start a book club' })).toBeVisible()
    await cancelModal(page)
  })

  test('Priya manages Monsoon Reading List — tabs, edit, add books, members', async ({ page }) => {
    await visitAuthed(page, priya, `/shelves/${priya.shelfId}`)
    await expect(page.getByRole('heading', { name: priya.shelfName })).toBeVisible()
    await visibleClick(page.getByRole('main').getByRole('button', { name: 'Updates', exact: true }))
    await pauseForViewer(page)
    await visibleClick(page.getByRole('main').getByRole('button', { name: 'Sharing', exact: true }))
    await pauseForViewer(page)
    await visibleClick(page.getByRole('main').getByRole('button', { name: 'Books', exact: true }))
    await visibleClick(page.getByRole('button', { name: 'Edit shelf' }))
    await cancelModal(page)
    await visibleClick(page.getByRole('button', { name: 'Add books' }))
    await visibleClick(page.getByRole('button', { name: 'Done' }))
    const cloneBtn = page.getByRole('button', { name: 'Clone to my shelves' })
    if (await cloneBtn.isVisible().catch(() => false)) {
      await visibleClick(cloneBtn)
      await cancelModal(page)
    }
    await visibleClick(page.getByRole('button', { name: /manage access|members/i }))
    await page.keyboard.press('Escape')
  })

  test('Priya posts a discussion note on Monsoon Reading List', async ({ page }) => {
    await visitAuthed(page, priya, `/shelves/${priya.shelfId}`)
    const comment = `Who is joining the Bengaluru meetup? — Priya, ${Date.now()}`
    await visibleType(page.getByPlaceholder('Write a comment…'), comment)
    await visibleClick(page.getByRole('button', { name: 'Post comment' }))
    await expect(page.getByRole('list').getByText(comment)).toBeVisible({ timeout: 15_000 })
  })

  test('Public link shows Priya’s Monsoon Reading List with The God of Small Things', async ({ page }) => {
    await visitAuthed(page, priya, `/u/${priya.userId}/s/${priya.shelfSlug}`)
    await expect(page.getByRole('heading', { name: priya.shelfName })).toBeVisible()
    await expect(page.locator('body')).toContainText(priya.bookTitle)
  })
})

test.describe('07 — Explore & Activity', () => {
  test('Priya browses public shelves and tries clone', async ({ page }) => {
    await visitAuthed(page, priya, '/explore')
    await clickFilterChip(page, 'Public', 'Shelf source')
    await clickFilterChip(page, 'Friend', 'Shelf source')
    await clickFilterChip(page, 'All', 'Shelf source')
    await page.locator('select').first().selectOption('NAME')
    await pauseForViewer(page)
    const cloneBtn = page.getByRole('button', { name: /clone/i }).first()
    if (await cloneBtn.isVisible()) {
      await visibleClick(cloneBtn)
      await cancelModal(page)
    }
  })

  test('Priya filters her activity feed by books and shelves', async ({ page }) => {
    await visitAuthed(page, priya, '/activity')
    await clickFilterChip(page, 'Books', 'Filter')
    await clickFilterChip(page, 'Shelves', 'Filter')
    await clickFilterChip(page, 'All', 'Filter')
  })
})

test.describe('08 — Priya & Arjun (friends)', () => {
  test('Priya searches friends, messages Arjun, and checks sent requests', async ({ page }) => {
    await visitAuthed(page, priya, '/friends')
    await visibleType(page.getByPlaceholder('Search friends…'), priya.friendFirstName)
    await pauseForViewer(page)
    await visibleClick(page.getByRole('button', { name: 'Message' }).first())
    await expect(page).toHaveURL(/\/messages/)
    await visitAuthed(page, priya, '/friends')
    await visibleClick(page.getByRole('link', { name: /sent requests/i }))
    await expect(page).toHaveURL(/\/friends\/requests\/sent/)
    await visibleClick(page.getByRole('link', { name: /back to friends/i }))
  })

  test('Priya opens the blocked accounts section', async ({ page }) => {
    await visitAuthed(page, priya, '/friends')
    const blockedToggle = page.getByRole('button', { name: /blocked accounts/i })
    if (await blockedToggle.isVisible()) await visibleClick(blockedToggle)
  })

  test('Priya views Arjun Mehta’s profile and starts a message', async ({ page }) => {
    await visitAuthed(page, priya, `/users/${priya.friendId}`)
    await expect(page.getByRole('heading', { name: priya.friendDisplayName })).toBeVisible()
    const messageBtn = page.getByRole('button', { name: /message/i })
    if (await messageBtn.isVisible()) await visibleClick(messageBtn)
  })
})

test.describe('09 — Priya messages Arjun', () => {
  test('Priya starts a chat with Arjun and sends a note about The White Tiger', async ({ page }) => {
    await visitAuthed(page, priya, '/messages')
    await visibleClick(page.getByRole('button', { name: 'New chat' }).first())
    const startChat = page.getByRole('dialog')
    await expect(startChat.getByRole('heading', { name: 'Start a chat' })).toBeVisible()
    await startChat.locator('button').filter({ hasText: priya.friendDisplayName }).first().click()
    const composer = page.getByPlaceholder(/type a message/i)
    await expect(composer).toBeVisible({ timeout: 20_000 })
    const msg = `Arjun, should we read ${priya.friendBookTitle} next month? — Priya ${Date.now()}`
    await visibleType(composer, msg)
    await composer.press('Enter')
    await expect(page.getByRole('main').getByText(msg).first()).toBeVisible({ timeout: 20_000 })
  })
})

test.describe('10 — Book recommendations', () => {
  test('Priya opens Recommend modal for Arjun and cancels', async ({ page }) => {
    await visitAuthed(page, priya, '/recommendations')
    await visibleClick(page.getByRole('main').getByRole('button', { name: 'Recommend', exact: true }))
    const modal = page.getByRole('dialog')
    await expect(modal.getByRole('heading', { name: /recommend books/i })).toBeVisible()
    await modal.locator('button').filter({ hasText: priya.friendDisplayName }).first().click()
    await pauseForViewer(page)
    await visibleClick(modal.getByRole('button', { name: 'Cancel' }))
  })

  test('Priya accepts Arjun’s recommendation of The White Tiger', async ({ page }) => {
    await visitAuthed(page, priya, '/recommendations')
    const addBtn = page.getByRole('button', { name: 'Add' }).first()
    await expect(addBtn).toBeVisible({ timeout: 15_000 })
    await visibleClick(addBtn)
    const acceptBtn = page.getByRole('button', { name: 'Accept' })
    if (await acceptBtn.isVisible()) {
      await visibleClick(acceptBtn)
      await pauseForViewer(page)
    }
  })
})

test.describe('11 — Notifications & settings', () => {
  test('Priya opens notifications and marks all read when available', async ({ page }) => {
    await visitAuthed(page, priya, '/notifications')
    await expect(page.getByRole('main').getByRole('heading', { name: 'Notifications', exact: true })).toBeVisible()
    const markAll = page.getByRole('button', { name: /mark all read/i })
    if (await markAll.isVisible()) await visibleClick(markAll)
  })

  test('Priya updates her writer bio on the account page', async ({ page }) => {
    await visitAuthed(page, priya, '/settings')
    await expect(page.getByRole('heading', { name: 'Account' })).toBeVisible()
    await visibleClick(page.getByRole('link', { name: /public profile/i }))
    await expect(page).toHaveURL(new RegExp(`/users/${priya.userId}`))
    await page.goBack({ waitUntil: 'domcontentloaded' })
    const bio = page.getByLabel('Bio')
    if (await bio.isVisible()) {
      await visibleType(bio, `Bengaluru reader tracking Indian fiction — updated ${Date.now()}`)
      await visibleClick(page.getByRole('button', { name: 'Save changes' }))
      await expect(page.getByText('Saved')).toBeVisible({ timeout: 10_000 })
    }
  })
})

test.describe('12 — Sign out', () => {
  test('Priya signs out and returns to the login page', async ({ page }) => {
    await visitAuthed(page, priya, '/')
    await visibleClick(page.getByRole('button', { name: /sign out/i }))
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible({ timeout: 15_000 })
  })
})
