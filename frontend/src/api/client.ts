import type { AuthResponse, ProblemDetail } from './types'
import { API_PATHS } from './paths'
import { STORAGE_KEYS } from '../lib/constants'
import { loadStoredAuth } from '../auth/storage'

const API_BASE = import.meta.env.VITE_API_URL ?? ''

/** Refresh access token at ~80% of the 15-minute TTL while the tab is active. */
const PROACTIVE_REFRESH_MS = 12 * 60 * 1000

export class ApiError extends Error {
  status: number
  problem?: ProblemDetail

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

type TokenPair = {
  accessToken: string
  refreshToken: string
}

let tokens: TokenPair | null = null
let refreshPromise: Promise<TokenPair> | null = null
type AuthRefreshListener = (data: AuthResponse) => void
let authRefreshListener: AuthRefreshListener | null = null
const tokenRefreshListeners = new Set<() => void>()

export function setAuthRefreshListener(listener: AuthRefreshListener | null) {
  authRefreshListener = listener
}

export function onTokensRefreshed(listener: () => void) {
  tokenRefreshListeners.add(listener)
  return () => tokenRefreshListeners.delete(listener)
}

function notifyTokenRefresh() {
  tokenRefreshListeners.forEach((listener) => listener())
}

export function setTokens(pair: TokenPair | null) {
  tokens = pair
}

export function getAccessToken(): string | null {
  return tokens?.accessToken ?? null
}

export function getRefreshToken(): string | null {
  return tokens?.refreshToken ?? null
}

export function syncTokensFromStorage(): boolean {
  const stored = loadStoredAuth()
  if (!stored) {
    return false
  }
  setTokens({
    accessToken: stored.accessToken,
    refreshToken: stored.refreshToken,
  })
  return true
}

async function parseProblem(res: Response): Promise<ProblemDetail | undefined> {
  const contentType = res.headers.get('content-type') ?? ''
  if (!contentType.includes('json')) return undefined
  try {
    return (await res.json()) as ProblemDetail
  } catch {
    return undefined
  }
}

async function refreshTokens(allowStoredRetry = true): Promise<TokenPair> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new ApiError('Session expired', 401)
  }
  const res = await fetch(`${API_BASE}${API_PATHS.auth.refresh}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!res.ok) {
    const problem = await parseProblem(res)
    if (allowStoredRetry) {
      const stored = loadStoredAuth()
      if (stored?.refreshToken && stored.refreshToken !== refreshToken) {
        setTokens({
          accessToken: stored.accessToken,
          refreshToken: stored.refreshToken,
        })
        return refreshTokens(false)
      }
    }
    setTokens(null)
    throw new ApiError(problem?.detail ?? 'Session expired', res.status, problem)
  }
  const data = (await res.json()) as AuthResponse
  const pair = { accessToken: data.accessToken, refreshToken: data.refreshToken }
  setTokens(pair)
  authRefreshListener?.(data)
  notifyTokenRefresh()
  const stored = localStorage.getItem(STORAGE_KEYS.auth)
  if (stored && data.user) {
    try {
      const parsed = JSON.parse(stored) as { accessToken: string; refreshToken: string; user: AuthResponse['user'] }
      localStorage.setItem(
        STORAGE_KEYS.auth,
        JSON.stringify({ ...parsed, accessToken: pair.accessToken, refreshToken: pair.refreshToken, user: data.user }),
      )
    } catch {
      /* ignore */
    }
  }
  return pair
}

export async function tryRefreshSession(): Promise<boolean> {
  if (!getRefreshToken() && !syncTokensFromStorage()) {
    return false
  }
  try {
    await refreshTokens()
    return true
  } catch {
    return false
  }
}

export function startProactiveTokenRefresh() {
  let timer: ReturnType<typeof setTimeout> | undefined

  const schedule = () => {
    if (timer) {
      clearTimeout(timer)
    }
    if (!getRefreshToken() || document.hidden) {
      return
    }
    timer = setTimeout(() => {
      void tryRefreshSession().finally(schedule)
    }, PROACTIVE_REFRESH_MS)
  }

  schedule()
  const onVisibility = () => schedule()
  document.addEventListener('visibilitychange', onVisibility)
  return () => {
    if (timer) {
      clearTimeout(timer)
    }
    document.removeEventListener('visibilitychange', onVisibility)
  }
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  retry = true,
): Promise<T> {
  const headers = new Headers(init.headers)
  if (
    !headers.has('Content-Type') &&
    init.body &&
    !(init.body instanceof FormData)
  ) {
    headers.set('Content-Type', 'application/json')
  }
  const access = getAccessToken()
  if (access) {
    headers.set('Authorization', `Bearer ${access}`)
  }

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })

  const shouldRefresh =
    retry &&
    (res.status === 401 || res.status === 403) &&
    !!access &&
    !!getRefreshToken()

  if (shouldRefresh) {
    refreshPromise ??= refreshTokens().finally(() => {
      refreshPromise = null
    })
    try {
      await refreshPromise
    } catch (e) {
      throw e
    }
    return apiFetch<T>(path, init, false)
  }

  if (res.status === 204) {
    return undefined as T
  }

  if (!res.ok) {
    const problem = await parseProblem(res)
    throw new ApiError(
      problem?.detail ?? problem?.title ?? `Request failed (${res.status})`,
      res.status,
      problem,
    )
  }

  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('json')) {
    return (await res.json()) as T
  }
  return undefined as T
}

export function apiUrl(path: string): string {
  return `${API_BASE}${path}`
}
