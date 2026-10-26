import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { Preferences } from '@capacitor/preferences'
import { setAccessToken } from '../api/client'
import * as authApi from '../api/auth'
import type { AuthResponse } from '../api/types'

interface AuthState {
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setAuthenticated] = useState(false)

  const store = useCallback(async (tokens: AuthResponse) => {
    setAccessToken(tokens.accessToken)
    await Preferences.set({ key: 'refreshToken', value: tokens.refreshToken })
    setAuthenticated(true)
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      isAuthenticated,
      login: async (email, password) => store(await authApi.login(email, password)),
      register: async (email, password, name) => store(await authApi.register(email, password, name)),
      logout: async () => {
        setAccessToken(null)
        await Preferences.remove({ key: 'refreshToken' })
        setAuthenticated(false)
      },
    }),
    [isAuthenticated, store],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
