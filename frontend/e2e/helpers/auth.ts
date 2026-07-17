import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { api } from './api'

export type AuthSession = {
  accessToken: string
  refreshToken: string
  user: Record<string, unknown>
}

function asString(value: unknown, label: string): string {
  if (typeof value === 'string' && value.length > 0) return value
  throw new Error(`Missing ${label} in auth response`)
}

/** Refresh tokens via API (Node) — avoids browser fetch failures under rate limit. */
export async function refreshAuthViaApi(auth: AuthSession): Promise<AuthSession> {
  const res = await api('POST', '/auth/refresh', { body: { refreshToken: auth.refreshToken } })
  if (res.status !== 200 || !res.json) return auth
  return {
    accessToken: asString(res.json.accessToken, 'accessToken'),
    refreshToken: asString(res.json.refreshToken, 'refreshToken'),
    user: (res.json.user as Record<string, unknown>) ?? auth.user,
  }
}

export async function setAuthStorage(page: Page, auth: AuthSession) {
  await page.evaluate((stored) => localStorage.setItem('whatiread.auth', JSON.stringify(stored)), auth)
}

export async function refreshAuthInBrowser(page: Page, auth: AuthSession): Promise<AuthSession> {
  return page.evaluate(async (stored) => {
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: stored.refreshToken }),
    })
    if (!res.ok) return stored
    const data = (await res.json()) as {
      accessToken: string
      refreshToken: string
      user: Record<string, unknown>
    }
    const next = {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: data.user ?? stored.user,
    }
    localStorage.setItem('whatiread.auth', JSON.stringify(next))
    return next
  }, auth)
}

async function waitForAuthedShell(page: Page) {
  await expect(page.getByRole('button', { name: /sign out/i })).toBeVisible({ timeout: 20_000 })
}

/** Full bootstrap: set storage and load Home until the app shell appears. */
export async function injectAuth(page: Page, auth: AuthSession) {
  const refreshed = await refreshAuthViaApi(auth)
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await setAuthStorage(page, refreshed)
  await page.goto('/', { waitUntil: 'domcontentloaded' })
  if (await isLoginPage(page)) {
    await setAuthStorage(page, refreshed)
    await page.goto('/', { waitUntil: 'domcontentloaded' })
  }
  await waitForAuthedShell(page)
  return refreshed
}

export async function clearAuth(page: Page) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => localStorage.removeItem('whatiread.auth'))
}

export async function assertNoCrash(page: Page) {
  await expect(page.locator('body')).not.toContainText('Something went wrong')
}

export async function isLoginPage(page: Page) {
  return page.getByRole('heading', { name: /sign in/i }).isVisible()
}

/** Navigate with fresh tokens; WebSocket pages never reach networkidle. */
export async function gotoAuthed(page: Page, auth: AuthSession, path = '/') {
  let session = await refreshAuthViaApi(auth)
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await setAuthStorage(page, session)
  await page.goto(path, { waitUntil: 'domcontentloaded' })
  if (await isLoginPage(page)) {
    session = await refreshAuthViaApi(session)
    await setAuthStorage(page, session)
    await page.goto(path, { waitUntil: 'domcontentloaded' })
  }
  await waitForAuthedShell(page)
  return session
}
