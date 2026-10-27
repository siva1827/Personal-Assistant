import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Preferences } from '@capacitor/preferences'
import { setAccessToken } from '../api/client'
import * as authApi from '../api/auth'
import { unregisterThisDevice } from '../features/push/push'
import type { AuthResponse } from '../api/types'
import { AuthContext, type AuthState } from './context'

const REFRESH_KEY = 'refreshToken'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setAuthenticated] = useState(false)
  const [ready, setReady] = useState(false)

  const store = useCallback(async (tokens: AuthResponse) => {
    setAccessToken(tokens.accessToken)
    await Preferences.set({ key: REFRESH_KEY, value: tokens.refreshToken })
    setAuthenticated(true)
  }, [])

  // Restore the session after a reload: the access token lives in memory only
  useEffect(() => {
    let cancelled = false
    const restore = async () => {
      try {
        const { value } = await Preferences.get({ key: REFRESH_KEY })
        if (value) {
          await store(await authApi.refresh(value))
        }
      } catch {
        await Preferences.remove({ key: REFRESH_KEY })
      } finally {
        if (!cancelled) setReady(true)
      }
    }
    void restore()
    return () => {
      cancelled = true
    }
  }, [store])

  const value = useMemo<AuthState>(
    () => ({
      isAuthenticated,
      ready,
      login: async (email, password) => store(await authApi.login(email, password)),
      register: async (email, password, name) => store(await authApi.register(email, password, name)),
      logout: async () => {
        // While still signed in: stop this phone receiving this account's push messages
        await unregisterThisDevice().catch(() => undefined)
        setAccessToken(null)
        await Preferences.remove({ key: REFRESH_KEY })
        setAuthenticated(false)
      },
    }),
    [isAuthenticated, ready, store],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
