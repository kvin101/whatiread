import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { authApi } from '../api/auth'
import { setAuthRefreshListener, setTokens, getRefreshToken } from '../api/client'
import type { AuthResponse, User } from '../api/types'
import { loadStoredAuth, saveStoredAuth } from './storage'

type AuthContextValue = {
  user: User | null
  isLoading: boolean
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (data: {
    email: string
    username: string
    password: string
    firstName: string
    lastName?: string
    phoneNumber?: string
  }) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
  completeAuth: (data: AuthResponse) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function applyAuth(data: AuthResponse, user: User) {
  setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken })
  saveStoredAuth({
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    user,
  })
}

async function syncUserFromApi(data: AuthResponse): Promise<User> {
  applyAuth(data, data.user)
  const fresh = await authApi.me()
  const stored = loadStoredAuth()
  if (stored) {
    saveStoredAuth({ ...stored, user: fresh })
  }
  return fresh
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    setAuthRefreshListener((data) => {
      setUser(data.user)
      const stored = loadStoredAuth()
      if (stored) {
        saveStoredAuth({ ...stored, user: data.user })
      }
    })
    return () => setAuthRefreshListener(null)
  }, [])

  useEffect(() => {
    const stored = loadStoredAuth()
    if (stored) {
      setTokens({
        accessToken: stored.accessToken,
        refreshToken: stored.refreshToken,
      })
      setUser(stored.user)
      authApi
        .me()
        .then((fresh) => {
          setUser(fresh)
          saveStoredAuth({ ...stored, user: fresh })
        })
        .catch(() => {
          saveStoredAuth(null)
          setTokens(null)
          setUser(null)
        })
        .finally(() => setIsLoading(false))
    } else {
      setIsLoading(false)
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const data = await authApi.login({ email, password })
    setUser(await syncUserFromApi(data))
  }, [])

  const register = useCallback(
    async (data: {
      email: string
      username: string
      password: string
      firstName: string
      lastName?: string
      phoneNumber?: string
    }) => {
      const res = await authApi.register(data)
      setUser(await syncUserFromApi(res))
    },
    [],
  )

  const logout = useCallback(async () => {
    const refresh = getRefreshToken()
    if (refresh) {
      try {
        await authApi.logout(refresh)
      } catch {
        /* ignore */
      }
    }
    setTokens(null)
    saveStoredAuth(null)
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    const fresh = await authApi.me()
    setUser(fresh)
    const stored = loadStoredAuth()
    if (stored) {
      saveStoredAuth({ ...stored, user: fresh })
    }
  }, [])

  const completeAuth = useCallback(async (data: AuthResponse) => {
    setUser(await syncUserFromApi(data))
  }, [])

  const value = useMemo(
    () => ({
      user,
      isLoading,
      isAuthenticated: !!user,
      login,
      register,
      logout,
      refreshUser,
      completeAuth,
    }),
    [user, isLoading, login, register, logout, refreshUser, completeAuth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
