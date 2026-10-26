/** Month keys are "yyyy-MM", matching the API's month parameter. */

export function toMonthKey(date: Date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function parts(key: string): [number, number] {
  const [y, m] = key.split('-').map(Number)
  return [y, m]
}

export function shiftMonth(key: string, delta: number): string {
  const [y, m] = parts(key)
  return toMonthKey(new Date(y, m - 1 + delta, 1))
}

export function monthLabel(key: string): string {
  const [y, m] = parts(key)
  return new Date(y, m - 1, 1).toLocaleString('en-US', { month: 'long', year: 'numeric' })
}

export function daysInMonth(key: string): number {
  const [y, m] = parts(key)
  return new Date(y, m, 0).getDate()
}
