import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { visibleClick, visibleType } from './interactions'
import { pauseForViewer } from './timing'

export async function openBookDrawer(page: Page, bookTitle: string) {
  await visibleClick(page.getByRole('button', { name: new RegExp(bookTitle) }))
  const drawer = page.getByRole('dialog')
  await expect(drawer).toBeVisible()
  return drawer
}

export async function closeDrawer(page: Page) {
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0, { timeout: 5_000 })
  await pauseForViewer(page)
}

export async function exerciseBookDrawerTabs(page: Page, bookTitle: string, readerFirstName: string) {
  const drawer = await openBookDrawer(page, bookTitle)

  await visibleClick(drawer.getByRole('button', { name: 'notes' }))
  await pauseForViewer(page)
  const note = `${readerFirstName} finished the Ayemenem chapters — ${Date.now()}`
  await visibleType(drawer.getByPlaceholder('Add a reading note…'), note)
  await visibleClick(drawer.getByRole('button', { name: /save note/i }))
  await expect(drawer.getByText(note)).toBeVisible({ timeout: 10_000 })
  await pauseForViewer(page)

  await visibleClick(drawer.getByRole('button', { name: 'comments' }))
  await pauseForViewer(page)

  await visibleClick(drawer.getByRole('button', { name: 'details' }))
  await visibleClick(drawer.getByRole('button', { name: 'To read' }))
  await pauseForViewer(page)
  await visibleClick(drawer.getByRole('button', { name: 'Reading' }))
  await pauseForViewer(page)
  await visibleClick(drawer.getByRole('button', { name: '4 stars' }))
  await pauseForViewer(page)

  await visibleClick(drawer.getByRole('link', { name: /view book page/i }))
  await expect(page).toHaveURL(/\/books\//)
  await page.goBack({ waitUntil: 'domcontentloaded' })
  await pauseForViewer(page)
}

export async function clickFilterChip(
  page: Page,
  label: string | RegExp,
  group?: string,
  exact = false,
) {
  const scope = group
    ? page.getByRole('group', { name: group })
    : page.getByRole('main')
  await visibleClick(scope.getByRole('button', { name: label, exact }).first())
  await pauseForViewer(page)
}

export async function cancelModal(page: Page) {
  const cancel = page.getByRole('dialog').getByRole('button', { name: /cancel|done|close/i }).first()
  if (await cancel.isVisible().catch(() => false)) {
    await visibleClick(cancel)
  }
}
