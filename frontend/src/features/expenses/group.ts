import type { Expense } from '../../api/types'
import { daysUntil } from '../pantry/expiry'

export interface ExpenseGroup {
  date: string
  label: string
  total: number
  items: Expense[]
}

export function dateLabel(isoDate: string, today: Date = new Date()): string {
  const diff = daysUntil(isoDate, today)
  if (diff === 0) return 'Today'
  if (diff === -1) return 'Yesterday'
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-US', { weekday: 'short', day: 'numeric', month: 'short' })
}

/** Groups expenses by day, keeping the incoming (newest first) order. */
export function groupByDate(expenses: Expense[], today: Date = new Date()): ExpenseGroup[] {
  const groups = new Map<string, ExpenseGroup>()
  for (const expense of expenses) {
    let group = groups.get(expense.spentOn)
    if (!group) {
      group = { date: expense.spentOn, label: dateLabel(expense.spentOn, today), total: 0, items: [] }
      groups.set(expense.spentOn, group)
    }
    group.items.push(expense)
    group.total += expense.amount
  }
  return [...groups.values()]
}
