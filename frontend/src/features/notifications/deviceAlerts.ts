import { useEffect } from 'react'
import type { AppNotification } from '../../api/types'

const ENABLED_KEY = 'deviceAlerts'
const LAST_KEY = 'lastAlertedNotificationId'

export const alertsSupported = () => typeof Notification !== 'undefined'

export const alertsEnabled = () =>
  alertsSupported() && Notification.permission === 'granted' && localStorage.getItem(ENABLED_KEY) === 'on'

/**
 * Turns device alerts on or off. Turning on asks the browser for permission, and returns false
 * if the user said no. The newest existing notification is marked as already announced so
 * switching on does not flood the screen with old ones.
 */
export async function setAlertsEnabled(on: boolean, currentItems: AppNotification[] = []): Promise<boolean> {
  if (!on) {
    localStorage.removeItem(ENABLED_KEY)
    return true
  }
  if (!alertsSupported()) return false
  const permission = Notification.permission === 'granted' ? 'granted' : await Notification.requestPermission()
  if (permission !== 'granted') return false
  localStorage.setItem(ENABLED_KEY, 'on')
  localStorage.setItem(LAST_KEY, String(Math.max(0, ...currentItems.map((n) => n.id))))
  return true
}

/** Ids not yet announced on this device, oldest first. */
export function unannounced(items: AppNotification[], lastId: number): AppNotification[] {
  return items.filter((n) => !n.read && n.id > lastId).sort((a, b) => a.id - b.id)
}

/** Shows a system notification for each new unread one. Only works while the app is open. */
export function useDeviceAlerts(items: AppNotification[] | undefined) {
  useEffect(() => {
    if (!items || !alertsEnabled()) return
    const fresh = unannounced(items, Number(localStorage.getItem(LAST_KEY) ?? 0))
    if (fresh.length === 0) return
    localStorage.setItem(LAST_KEY, String(fresh[fresh.length - 1].id))
    fresh.slice(-3).forEach((n) => new Notification(n.title, { body: n.body, tag: `daily-assistant-${n.id}` }))
  }, [items])
}
