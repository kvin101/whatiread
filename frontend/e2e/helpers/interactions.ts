import type { Locator } from '@playwright/test'
import {
  animateInteractions,
  clickDelayMs,
  pauseBetweenActions,
  typingDelayMs,
} from './timing'

/** Scroll into view, pause, click with visible press duration, pause again. */
export async function visibleClick(locator: Locator) {
  await locator.scrollIntoViewIfNeeded()
  if (animateInteractions) {
    await pauseBetweenActions(locator.page())
    await locator.click({ delay: clickDelayMs })
    await pauseBetweenActions(locator.page())
    return
  }
  await locator.click()
}

/** Click to focus, then type character-by-character so you can watch it happen. */
export async function visibleType(locator: Locator, text: string) {
  await locator.scrollIntoViewIfNeeded()
  await locator.click()
  if (animateInteractions) {
    await pauseBetweenActions(locator.page())
    await locator.fill('')
    await locator.pressSequentially(text, { delay: typingDelayMs })
    await pauseBetweenActions(locator.page())
    return
  }
  await locator.fill(text)
}

/** Replace numeric input value with visible keystrokes. */
export async function visibleTypeNumber(locator: Locator, value: string) {
  await locator.scrollIntoViewIfNeeded()
  await locator.click()
  if (animateInteractions) {
    await pauseBetweenActions(locator.page())
    await locator.press(process.platform === 'darwin' ? 'Meta+a' : 'Control+a')
    await locator.pressSequentially(value, { delay: typingDelayMs })
    await pauseBetweenActions(locator.page())
    return
  }
  await locator.fill(value)
}
