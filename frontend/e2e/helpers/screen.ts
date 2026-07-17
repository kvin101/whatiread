import { execSync } from 'node:child_process'

/** Headless CI viewport — stable, not tied to the developer machine. */
export const HEADLESS_VIEWPORT = { width: 1512, height: 982 }

/** Detect primary display size for headed runs (macOS Finder bounds, env override, fallback). */
export function resolveScreenSize(): { width: number; height: number } {
  const envW = process.env.E2E_WINDOW_WIDTH
  const envH = process.env.E2E_WINDOW_HEIGHT
  if (envW && envH) {
    return { width: Number(envW), height: Number(envH) }
  }

  if (process.platform === 'darwin') {
    try {
      const raw = execSync(
        'osascript -e \'tell application "Finder" to get bounds of window of desktop\'',
        { encoding: 'utf8' },
      ).trim()
      const parts = raw.split(',').map((s) => parseInt(s.trim(), 10))
      if (parts.length === 4 && parts[2] > 0 && parts[3] > 0) {
        return { width: parts[2], height: parts[3] }
      }
    } catch {
      /* fall through */
    }
  }

  return { width: 2560, height: 1440 }
}
