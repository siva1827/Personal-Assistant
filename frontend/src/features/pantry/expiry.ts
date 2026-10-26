const DAY_MS = 24 * 60 * 60 * 1000

/** Whole days from `today` until the ISO date (yyyy-MM-dd); negative once expired. */
export function daysUntil(isoDate: string, today: Date = new Date()): number {
  const [y, m, d] = isoDate.split('-').map(Number)
  const target = Date.UTC(y, m - 1, d)
  const start = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate())
  return Math.round((target - start) / DAY_MS)
}

export function isExpiringSoon(isoDate: string | null, withinDays = 3, today: Date = new Date()): boolean {
  return isoDate !== null && daysUntil(isoDate, today) <= withinDays
}
