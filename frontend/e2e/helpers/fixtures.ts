import type { Page } from '@playwright/test'
import { assertNoCrash, clearAuth, gotoAuthed } from './auth'
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
