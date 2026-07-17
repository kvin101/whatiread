import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { visibleClick } from './interactions'
import { pauseForViewer } from './timing'

const SIDEBAR_LINKS: { name: RegExp | string; url?: RegExp; heading?: RegExp }[] = [
  { name: 'Home', heading: /good (morning|afternoon|evening)/i },
  { name: 'My Books', url: /\/library/ },
  { name: 'Shelves', url: /\/shelves$/ },
  { name: 'Explore', url: /\/explore/ },
  { name: 'Messages', url: /\/messages/ },
  { name: 'Friends', url: /\/friends/ },
  { name: 'Recommendations', url: /\/recommendations/ },
  { name: 'Activity', url: /\/activity/ },
  { name: 'Notifications', url: /\/notifications/ },
  { name: 'Account', url: /\/settings/ },
]

export async function clickSidebarLink(page: Page, name: RegExp | string) {
  const link =
    typeof name === 'string'
      ? page.getByRole('complementary').getByRole('link', { name: new RegExp(name, 'i') })
      : page.getByRole('complementary').getByRole('link', { name })
  await visibleClick(link.first())
  await pauseForViewer(page)
}

export async function visitEverySidebarDestination(page: Page) {
  for (const link of SIDEBAR_LINKS) {
    await clickSidebarLink(page, link.name)
    if (link.url) {
      await expect(page).toHaveURL(link.url)
    } else if (link.heading) {
      await expect(page.getByRole('heading', { name: link.heading })).toBeVisible()
    }
  }
}
