import { defineConfig } from '@playwright/test'
import { DEMO_RECORD_VIEWPORT, HEADLESS_VIEWPORT, resolveScreenSize } from './e2e/helpers/screen'
import { isVisibleMode, slowMoMs } from './e2e/helpers/timing'

const baseURL =
  process.env.BASE_URL ?? process.env.SMOKE_BASE_URL ?? 'http://localhost'

/** Record every test video in CI, headed runs, or explicit RECORD_DEMO_VIDEOS=1. */
const recordVideos =
  process.env.CI === 'true' ||
  process.env.RECORD_DEMO_VIDEOS === '1' ||
  isVisibleMode

/** 2K for demo capture; Full HD for quick headless checks. */
const viewport = isVisibleMode
  ? resolveScreenSize()
  : recordVideos
    ? DEMO_RECORD_VIEWPORT
    : HEADLESS_VIEWPORT

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  // Serial suites retry the whole file from test 1 — never use retries here.
  retries: 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ...(process.env.CI === 'true'
      ? ([['json', { outputFile: 'playwright-results.json' }]] as const)
      : []),
  ],
  timeout: isVisibleMode ? 600_000 : 240_000,
  expect: { timeout: isVisibleMode ? 30_000 : 15_000 },
  use: {
    baseURL,
    viewport,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: recordVideos
      ? { mode: 'on', size: DEMO_RECORD_VIEWPORT }
      : 'retain-on-failure',
    launchOptions: {
      slowMo: slowMoMs,
      args: [
        `--window-size=${viewport.width},${viewport.height}`,
        '--window-position=0,0',
        '--force-device-scale-factor=1',
        '--font-render-hinting=medium',
      ],
    },
  },
  // Do NOT spread devices['Desktop Chrome'] — it forces 1280×720 and leaves gray bars.
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
