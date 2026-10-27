import type { NotificationType } from '../../api/types'

const EMOJI: Record<NotificationType, string> = {
  TASK_DUE: '⏰',
  TASK_OVERDUE: '❗',
  BUDGET_NEAR: '⚠️',
  BUDGET_OVER: '🚨',
  PANTRY_EXPIRING: '🥬',
}

export const notificationEmoji = (type: NotificationType): string => EMOJI[type]

/** "just now", "5 min ago", "3 h ago", "yesterday", then a short date. */
export function timeAgo(iso: string, now: Date = new Date()): string {
  const then = new Date(iso)
  const minutes = Math.floor((now.getTime() - then.getTime()) / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  if (hours < 48) return 'yesterday'
  return then.toLocaleDateString('en-US', { day: 'numeric', month: 'short' })
}
