import type { Page } from '@playwright/test'

/** Headed / visible mode — slower pacing so you can follow each step. */
export const isVisibleMode = process.env.E2E_VISIBLE === '1'

/** Pause after each test step / assertion block (ms). */
export const stepPauseMs = Number(
  process.env.E2E_STEP_PAUSE_MS ?? (isVisibleMode ? 3_000 : 400),
)

/** Playwright slowMo — delays every Playwright action (click, type, navigate). */
export const slowMoMs = Number(
  process.env.E2E_SLOW_MO_MS ?? (isVisibleMode ? 400 : 0),
)

/** Pause before and after each deliberate click (ms). */
export const actionPauseMs = Number(
  process.env.E2E_ACTION_PAUSE_MS ?? (isVisibleMode ? 800 : 0),
)

/** Per-character delay when typing in visible mode (ms). */
export const typingDelayMs = Number(
  process.env.E2E_TYPING_DELAY_MS ?? (isVisibleMode ? 100 : 0),
)

/** Mouse-down duration on click in visible mode (ms). */
export const clickDelayMs = Number(
  process.env.E2E_CLICK_DELAY_MS ?? (isVisibleMode ? 200 : 0),
)

/** Extra pause after each page loads so content is readable before the next step. */
export async function pauseForViewer(page: Page) {
  if (stepPauseMs > 0) {
    await page.waitForTimeout(stepPauseMs)
  }
}

/** Short pause between sub-steps inside a test (visible mode only). */
export async function pauseBetweenActions(page: Page) {
  if (actionPauseMs > 0) {
    await page.waitForTimeout(actionPauseMs)
  }
}
