import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { assertNoCrash, clearAuth, gotoAuthed } from './auth'
import { visibleClick, visibleType } from './interactions'
import type { SeedContext } from './seed'
import { pauseForViewer } from './timing'

export async function visitAuthed(page: Page, ctx: SeedContext, path: string) {
  ctx.auth = await gotoAuthed(page, ctx.auth, path)
  await assertNoCrash(page)
  await pauseForViewer(page)
}

export async function visitLoggedOut(page: Page, path: string) {
  await clearAuth(page)
  await page.goto(path, { waitUntil: 'domcontentloaded' })
  await pauseForViewer(page)
}

/** Sign in through the login form with visible keystrokes (demo recordings). */
export async function signInViaForm(page: Page, loginId: string, password: string) {
  await visibleType(page.getByLabel(/email or username/i), loginId)
  await visibleType(page.getByLabel(/^password$/i), password)
  await visibleClick(page.getByRole('button', { name: /sign in/i }))
  await expect(page.getByRole('button', { name: /sign out/i })).toBeVisible({ timeout: 20_000 })
  await pauseForViewer(page)
}
