import { APP_ROUTES } from '../api/paths'

/** Reject open-redirect targets; only same-app relative paths. */
export function safeAppPath(path: string | undefined | null, fallback = APP_ROUTES.library): string {
  if (!path || typeof path !== 'string') return fallback
  if (!path.startsWith('/') || path.startsWith('//') || path.includes('://')) {
    return fallback
  }
  return path
}
