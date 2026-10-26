import { daysUntil } from '../pantry/expiry'

export type DueTone = 'overdue' | 'today' | 'soon' | 'later'

export interface DueInfo {
  label: string
  tone: DueTone
}

function shortDate(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-US', { weekday: 'short', day: 'numeric', month: 'short' })
}

/** How a task's due date is shown: a label plus a tone that drives the colour. */
export function dueInfo(isoDate: string, today: Date = new Date()): DueInfo {
  const days = daysUntil(isoDate, today)
  if (days < 0) return { label: `Overdue · ${shortDate(isoDate)}`, tone: 'overdue' }
  if (days === 0) return { label: 'Today', tone: 'today' }
  if (days === 1) return { label: 'Tomorrow', tone: 'soon' }
  return { label: shortDate(isoDate), tone: 'later' }
}
