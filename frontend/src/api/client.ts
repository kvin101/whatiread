import type { AuthResponse, ProblemDetail } from './types'
import { API_PATHS } from './paths'
import { STORAGE_KEYS } from '../lib/constants'

const API_BASE = import.meta.env.VITE_API_URL ?? ''

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

export function setAuthRefreshListener(listener: AuthRefreshListener | null) {
  authRefreshListener = listener
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

async function parseProblem(res: Response): Promise<ProblemDetail | undefined> {
  const contentType = res.headers.get('content-type') ?? ''
  if (!contentType.includes('json')) return undefined
  try {
    return (await res.json()) as ProblemDetail
  } catch {
    return undefined
  }
}

async function refreshTokens(): Promise<TokenPair> {
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
    setTokens(null)
    throw new ApiError(problem?.detail ?? 'Session expired', res.status, problem)
  }
  const data = (await res.json()) as AuthResponse
  const pair = { accessToken: data.accessToken, refreshToken: data.refreshToken }
  setTokens(pair)
  authRefreshListener?.(data)
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

  if (res.status === 401 && retry && getRefreshToken()) {
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
