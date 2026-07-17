/**
 * WhatIRead — full product demo for GitHub screen recordings.
 * Serial suite: meaningful names, every major button and flow exercised.
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

let ctx: SeedContext

test.describe.configure({ mode: 'serial' })

test.beforeAll(async () => {
  ctx = await seedTestUser()
})

test.describe('01 — Authentication pages', () => {
  test('Login page shows email, password, and Sign in button', async ({ page }) => {
    await visitLoggedOut(page, '/login')
    await expect(page.getByLabel(/email or username/i)).toBeVisible()
    await expect(page.getByLabel(/^password$/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
    await visibleClick(page.getByRole('link', { name: /create account/i }))
    await expect(page).toHaveURL(/\/register/)
  })

  test('Register page shows account form fields', async ({ page }) => {
    await visitLoggedOut(page, '/register')
    await expect(page.getByRole('heading', { name: /create account/i })).toBeVisible()
    await expect(page.getByLabel(/first name/i)).toBeVisible()
    await expect(page.getByLabel(/email/i)).toBeVisible()
    await visibleClick(page.getByRole('link', { name: /sign in/i }))
    await expect(page).toHaveURL(/\/login/)
  })
})

test.describe('02 — App navigation (sidebar)', () => {
  test('Every sidebar link opens the correct page', async ({ page }) => {
    await visitAuthed(page, ctx, '/')
    await visitEverySidebarDestination(page)
  })

  test('Profile link in sidebar opens your user profile', async ({ page }) => {
    await visitAuthed(page, ctx, '/')
    await visibleClick(page.getByRole('complementary').getByRole('link', { name: /e2e user/i }))
    await expect(page).toHaveURL(new RegExp(`/users/${ctx.userId}`))
    await visibleClick(page.getByRole('link', { name: /edit profile/i }))
    await expect(page).toHaveURL(/\/settings/)
  })
})

test.describe('03 — Home dashboard', () => {
  test('Home — open Add book modal, search, and close', async ({ page }) => {
    await visitAuthed(page, ctx, '/')
    await visibleClick(page.getByRole('button', { name: 'Add book' }))
    await expect(page.getByRole('heading', { name: 'Add a book' })).toBeVisible()
    await visibleType(page.getByPlaceholder('Start typing a book title…'), 'Dune')
    await visibleClick(page.getByRole('button', { name: 'Search' }))
    await expect(page.getByText(/Dune|No Open Library results|Add to library/i).first()).toBeVisible({
      timeout: 30_000,
    })
    await visibleClick(page.getByRole('button', { name: 'Done' }))
  })

  test('Home — reading goal, streak badge, and activity preview links work', async ({ page }) => {
    await visitAuthed(page, ctx, '/')
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

test.describe('04 — My Books (library)', () => {
  test('Library — filter by every reading status chip', async ({ page }) => {
    await visitAuthed(page, ctx, '/library')
    await clickFilterChip(page, /Reading \(\d+\)/, 'Reading status')
    await clickFilterChip(page, 'To read', 'Reading status', true)
    await clickFilterChip(page, 'Read', 'Reading status', true)
    await clickFilterChip(page, 'Did not finish', 'Reading status', true)
    await clickFilterChip(page, 'All', 'Reading status', true)
  })

  test('Library — search books and change sort order', async ({ page }) => {
    await visitAuthed(page, ctx, '/library')
    await visibleType(page.getByLabel('Search library'), 'E2E')
    await pauseForViewer(page)
    await visibleClick(page.getByLabel('Sort order'))
    await page.getByLabel('Sort order').selectOption('title')
    await pauseForViewer(page)
    const clearBtn = page.getByRole('button', { name: 'Clear' })
    if (await clearBtn.isVisible()) await visibleClick(clearBtn)
  })

  test('Library — open book drawer and update reading progress', async ({ page }) => {
    await visitAuthed(page, ctx, '/library')
    await clickFilterChip(page, /Reading \(\d+\)/, 'Reading status')
    const drawer = await openBookDrawer(page, ctx.bookTitle)
    await visibleTypeNumber(drawer.getByRole('spinbutton'), '120')
    await drawer.getByRole('spinbutton').blur()
    await expect(drawer.getByText(/120\s*\/\s*300/)).toBeVisible({ timeout: 10_000 })
    await closeDrawer(page)
  })

  test('Library — book drawer exercises Details, Notes, Comments, and rating', async ({ page }) => {
    await visitAuthed(page, ctx, '/library')
    await exerciseBookDrawerTabs(page, ctx.bookTitle)
  })
})

test.describe('05 — Book & author pages', () => {
  test('Book page — post a comment that appears with timestamp', async ({ page }) => {
    await visitAuthed(page, ctx, `/books/${ctx.bookId}`)
    await expect(page.getByRole('heading', { name: ctx.bookTitle })).toBeVisible()
    const comment = `E2E comment ${Date.now()}`
    const postBtn = page.getByRole('button', { name: 'Post comment' })
    await visibleType(page.getByPlaceholder('Write a comment…'), comment)
    await expect(postBtn).toBeEnabled({ timeout: 10_000 })
    await visibleClick(postBtn)
    await expect(page.getByRole('list').getByText(comment)).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('time').first()).toBeVisible()
  })

  test('Author page — open from drawer, browse Your books and All books tabs', async ({ page }) => {
    await visitAuthed(page, ctx, '/library')
    await visibleClick(page.getByRole('button', { name: new RegExp(ctx.bookTitle) }))
    await visibleClick(page.getByRole('dialog').getByRole('link', { name: ctx.authorName }))
    await expect(page.getByRole('heading', { name: ctx.authorName })).toBeVisible()
    await visibleClick(page.getByRole('button', { name: 'All books' }))
    await expect(page.getByRole('link', { name: ctx.bookTitle })).toBeVisible()
    await clickFilterChip(page, 'Your books', 'View')
    await pauseForViewer(page)
    const filterLink = page.getByRole('link', { name: /filter my library/i })
    if (await filterLink.isVisible()) await visibleClick(filterLink)
  })
})

test.describe('06 — Shelves', () => {
  test('Shelves list — visibility filters and system shelf shortcuts', async ({ page }) => {
    await visitAuthed(page, ctx, '/shelves')
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

  test('Shelves list — create a new shelf from the modal', async ({ page }) => {
    await visitAuthed(page, ctx, '/shelves')
    await visibleClick(page.getByRole('button', { name: 'New shelf' }))
    const name = `Demo Shelf ${Date.now()}`
    await visibleType(page.getByLabel('Name'), name)
    await visibleType(page.getByLabel('Short note'), 'Created during product demo')
    await visibleClick(page.getByRole('button', { name: 'Create shelf' }))
    await expect(page.getByRole('heading', { name })).toBeVisible({ timeout: 15_000 })
  })

  test('Shelves list — open Start book club wizard and cancel', async ({ page }) => {
    await visitAuthed(page, ctx, '/shelves')
    await visibleClick(page.getByRole('button', { name: 'Start book club' }))
    await expect(page.getByRole('heading', { name: 'Start a book club' })).toBeVisible()
    await cancelModal(page)
  })

  test('Shelf detail — Books, Updates, Sharing tabs and action buttons', async ({ page }) => {
    await visitAuthed(page, ctx, `/shelves/${ctx.shelfId}`)
    await expect(page.getByRole('heading', { name: ctx.shelfName })).toBeVisible()
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

  test('Shelf detail — post a discussion comment', async ({ page }) => {
    await visitAuthed(page, ctx, `/shelves/${ctx.shelfId}`)
    const comment = `Shelf discussion ${Date.now()}`
    await visibleType(page.getByPlaceholder('Write a comment…'), comment)
    await visibleClick(page.getByRole('button', { name: 'Post comment' }))
    await expect(page.getByRole('list').getByText(comment)).toBeVisible({ timeout: 15_000 })
  })

  test('Public shelf URL — view shelf by owner slug', async ({ page }) => {
    await visitAuthed(page, ctx, `/u/${ctx.userId}/s/${ctx.shelfSlug}`)
    await expect(page.getByRole('heading', { name: ctx.shelfName })).toBeVisible()
    await expect(page.locator('body')).toContainText(ctx.bookTitle)
  })
})

test.describe('07 — Explore & Activity', () => {
  test('Explore — filter by source and change sort order', async ({ page }) => {
    await visitAuthed(page, ctx, '/explore')
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

  test('Activity feed — filter by Books and Shelves', async ({ page }) => {
    await visitAuthed(page, ctx, '/activity')
    await clickFilterChip(page, 'Books', 'Filter')
    await clickFilterChip(page, 'Shelves', 'Filter')
    await clickFilterChip(page, 'All', 'Filter')
  })
})

test.describe('08 — Friends & profiles', () => {
  test('Friends — search, message friend, and open sent requests', async ({ page }) => {
    await visitAuthed(page, ctx, '/friends')
    await visibleType(page.getByPlaceholder('Search friends…'), 'Friend')
    await pauseForViewer(page)
    await visibleClick(page.getByRole('button', { name: 'Message' }).first())
    await expect(page).toHaveURL(/\/messages/)
    await visitAuthed(page, ctx, '/friends')
    await visibleClick(page.getByRole('link', { name: /sent requests/i }))
    await expect(page).toHaveURL(/\/friends\/requests\/sent/)
    await visibleClick(page.getByRole('link', { name: /back to friends/i }))
  })

  test('Friends — expand blocked users section', async ({ page }) => {
    await visitAuthed(page, ctx, '/friends')
    const blockedToggle = page.getByRole('button', { name: /blocked accounts/i })
    if (await blockedToggle.isVisible()) await visibleClick(blockedToggle)
  })

  test('User profile — view friend profile and click Message', async ({ page }) => {
    await visitAuthed(page, ctx, `/users/${ctx.friendId}`)
    await expect(page.getByRole('heading', { name: ctx.friendDisplayName })).toBeVisible()
    const messageBtn = page.getByRole('button', { name: /message/i })
    if (await messageBtn.isVisible()) await visibleClick(messageBtn)
  })
})

test.describe('09 — Messages', () => {
  test('Messages — start new chat with friend and send a message', async ({ page }) => {
    await visitAuthed(page, ctx, '/messages')
    await visibleClick(page.getByRole('button', { name: 'New chat' }).first())
    const startChat = page.getByRole('dialog')
    await expect(startChat.getByRole('heading', { name: 'Start a chat' })).toBeVisible()
    await startChat.locator('button').filter({ hasText: ctx.friendDisplayName }).first().click()
    const composer = page.getByPlaceholder(/type a message/i)
    await expect(composer).toBeVisible({ timeout: 20_000 })
    const msg = `Hello from demo ${Date.now()}`
    await visibleType(composer, msg)
    await composer.press('Enter')
    await expect(page.getByRole('main').getByText(msg).first()).toBeVisible({ timeout: 20_000 })
  })
})

test.describe('10 — Recommendations', () => {
  test('Recommendations — open Recommend modal, pick friend and book, then cancel', async ({ page }) => {
    await visitAuthed(page, ctx, '/recommendations')
    await visibleClick(page.getByRole('main').getByRole('button', { name: 'Recommend', exact: true }))
    const modal = page.getByRole('dialog')
    await expect(modal.getByRole('heading', { name: /recommend books/i })).toBeVisible()
    await modal.locator('button').filter({ hasText: ctx.friendDisplayName }).first().click()
    await pauseForViewer(page)
    await visibleClick(modal.getByRole('button', { name: 'Cancel' }))
  })

  test('Recommendations — accept incoming recommendation from friend', async ({ page }) => {
    await visitAuthed(page, ctx, '/recommendations')
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

test.describe('11 — Notifications & Settings', () => {
  test('Notifications page loads inbox or empty state', async ({ page }) => {
    await visitAuthed(page, ctx, '/notifications')
    await expect(page.getByRole('main').getByRole('heading', { name: 'Notifications', exact: true })).toBeVisible()
    const markAll = page.getByRole('button', { name: /mark all read/i })
    if (await markAll.isVisible()) await visibleClick(markAll)
  })

  test('Settings — update writer bio and save profile', async ({ page }) => {
    await visitAuthed(page, ctx, '/settings')
    await expect(page.getByRole('heading', { name: 'Account' })).toBeVisible()
    await visibleClick(page.getByRole('link', { name: /public profile/i }))
    await expect(page).toHaveURL(new RegExp(`/users/${ctx.userId}`))
    await page.goBack({ waitUntil: 'domcontentloaded' })
    const bio = page.getByLabel('Bio')
    if (await bio.isVisible()) {
      await visibleType(bio, `Demo bio updated ${Date.now()}`)
      await visibleClick(page.getByRole('button', { name: 'Save changes' }))
      await expect(page.getByText('Saved')).toBeVisible({ timeout: 10_000 })
    }
  })
})

test.describe('12 — Sign out', () => {
  test('Sign out returns to the login page', async ({ page }) => {
    await visitAuthed(page, ctx, '/')
    await visibleClick(page.getByRole('button', { name: /sign out/i }))
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible({ timeout: 15_000 })
  })
})
