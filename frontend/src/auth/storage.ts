import type { User } from '../api/types'
import { STORAGE_KEYS } from '../lib/constants'

// Tokens in localStorage are readable by XSS; CSP mitigates but HttpOnly cookies need a larger auth refactor.
const STORAGE_KEY = STORAGE_KEYS.auth

export type StoredAuth = {
  accessToken: string
  refreshToken: string
  user: User
}

export function loadStoredAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as StoredAuth
  } catch {
    return null
  }
}

export function saveStoredAuth(auth: StoredAuth | null) {
  if (!auth) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
}
