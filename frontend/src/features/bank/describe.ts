import type { BankSyncResult, ConsentStatus } from '../../api/types'

export interface StatusInfo {
  label: string
  /** Matches the pill styles: plain green, warning amber, danger red */
  tone: 'ok' | 'warn' | 'danger'
}

export function statusInfo(status: ConsentStatus): StatusInfo {
  switch (status) {
    case 'ACTIVE':
      return { label: 'Connected', tone: 'ok' }
    case 'PENDING':
      return { label: 'Waiting for approval', tone: 'warn' }
    case 'REJECTED':
      return { label: 'Rejected', tone: 'danger' }
    case 'REVOKED':
      return { label: 'Stopped', tone: 'danger' }
    case 'EXPIRED':
      return { label: 'Expired', tone: 'warn' }
  }
}

const plural = (n: number, one: string, many: string) => `${n} ${n === 1 ? one : many}`

/** One sentence about what a sync did, including the case where there was nothing new. */
export function describeSync(result: BankSyncResult): string {
  if (result.imported === 0) {
    return result.skipped > 0
      ? `Nothing new. ${plural(result.skipped, 'transaction was', 'transactions were')} already in your spending.`
      : 'No spending found in this period.'
  }
  const parts = [`Added ${plural(result.imported, 'expense', 'expenses')} to your spending`]
  if (result.skipped > 0) parts.push(`${result.skipped} already there`)
  if (result.creditsIgnored > 0) {
    parts.push(`${plural(result.creditsIgnored, 'incoming payment', 'incoming payments')} ignored`)
  }
  return `${parts.join(', ')}.`
}

/** Whole days from now until the instant, never negative. */
export function daysLeft(isoInstant: string | null, now: Date = new Date()): number | null {
  if (!isoInstant) return null
  return Math.max(0, Math.ceil((new Date(isoInstant).getTime() - now.getTime()) / 86_400_000))
}
