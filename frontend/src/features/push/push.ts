import { useEffect } from 'react'
import { Capacitor } from '@capacitor/core'
import { PushNotifications } from '@capacitor/push-notifications'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { apiPlatform, type PushTestResult } from './testResult'

const ENABLED_KEY = 'pushEnabled'
const TOKEN_KEY = 'pushToken'
const REGISTRATION_TIMEOUT_MS = 20_000

/** Push needs the native app; a browser tab uses the Device alerts option instead. */
export const pushSupported = () => Capacitor.isNativePlatform()

/** Whether the user switched push on for this device. */
export const pushPreferred = () => localStorage.getItem(ENABLED_KEY) === 'on'

/** Registers with Firebase and resolves with this installation's device token. */
function requestToken(): Promise<string> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => finish(() => reject(new Error('Timed out waiting for Firebase'))), REGISTRATION_TIMEOUT_MS)
    const handles: { remove: () => Promise<void> }[] = []
    const finish = (settle: () => void) => {
      clearTimeout(timer)
      handles.forEach((h) => void h.remove())
      settle()
    }

    void (async () => {
      try {
        handles.push(
          await PushNotifications.addListener('registration', (token) => finish(() => resolve(token.value))),
          await PushNotifications.addListener('registrationError', (e) =>
            finish(() => reject(new Error(e.error || 'Firebase registration failed'))),
          ),
        )
        await PushNotifications.register()
      } catch (e) {
        finish(() => reject(e))
      }
    })()
  })
}

/** Gets a (possibly new) token and tells the server about it. */
async function registerThisDevice(): Promise<void> {
  const token = await requestToken()
  localStorage.setItem(TOKEN_KEY, token)
  await api.post('/devices', { token, platform: apiPlatform(Capacitor.getPlatform()) })
}

/** Asks Android for permission, then registers. Returns false if the user said no. */
export async function enablePush(): Promise<boolean> {
  const permission = await PushNotifications.requestPermissions()
  if (permission.receive !== 'granted') return false
  await registerThisDevice()
  localStorage.setItem(ENABLED_KEY, 'on')
  return true
}

/** Forgets this device on the server. Used on sign-out and when push is switched off. */
export async function unregisterThisDevice(): Promise<void> {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return
  await api.post('/devices/unregister', { token })
  localStorage.removeItem(TOKEN_KEY)
}

export async function disablePush(): Promise<void> {
  localStorage.removeItem(ENABLED_KEY)
  try {
    await unregisterThisDevice()
  } finally {
    await PushNotifications.unregister().catch(() => undefined)
  }
}

export const sendTestPush = () => api.post<PushTestResult>('/devices/test').then((r) => r.data)

/**
 * While signed in on a phone where push is on: keeps the token fresh (Firebase can change it),
 * refreshes the notification list when a message arrives in the foreground, and opens the right
 * screen when one is tapped.
 */
export function usePushSync() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  useEffect(() => {
    if (!pushSupported() || !pushPreferred()) return
    let cancelled = false
    const handles: { remove: () => Promise<void> }[] = []

    void (async () => {
      try {
        handles.push(
          await PushNotifications.addListener('pushNotificationReceived', () => {
            void queryClient.invalidateQueries({ queryKey: ['notifications'] })
          }),
          await PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
            const link = action.notification.data?.link
            if (typeof link === 'string' && link.startsWith('/')) navigate(link)
            void queryClient.invalidateQueries({ queryKey: ['notifications'] })
          }),
        )
        if (!cancelled) await registerThisDevice()
      } catch {
        // Not fatal: the in-app notification list still works without push
      }
    })()

    return () => {
      cancelled = true
      handles.forEach((h) => void h.remove())
    }
  }, [queryClient, navigate])
}
