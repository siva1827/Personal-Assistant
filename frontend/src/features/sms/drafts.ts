import { guessCategoryId, parseBankSms } from '../expenses/smsParser'
import type { RawSms } from './smsInbox'

/** An expense found in a message, waiting for the user to confirm it. */
export interface SmsDraft {
  /** Fingerprint of the message; the server uses it to refuse duplicates. */
  ref: string
  sender: string
  amount: number
  merchant: string | null
  /** yyyy-MM-dd */
  date: string
  categoryId: number | null
}

/**
 * Banks send from named sender ids such as "AX-HDFCBK"; friends and family send from phone
 * numbers. Only the former are looked at, so personal messages are never even parsed.
 */
export function isBankSender(address: string | null): boolean {
  return address !== null && /[A-Za-z]/.test(address)
}

export function localIsoDate(epochMs: number): string {
  const d = new Date(epochMs)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Midnight at the start of the day, (days - 1) days ago. days = 1 means "today". */
export function rangeStartMs(days: number, now: number = Date.now()): number {
  const d = new Date(now)
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - (days - 1))
  return d.getTime()
}

/** A short, stable id for a message: the same SMS always gives the same value. */
export async function fingerprint(sms: RawSms): Promise<string> {
  const text = `${sms.address ?? ''}|${sms.date}|${sms.body ?? ''}`
  const bytes = new TextEncoder().encode(text)
  if (globalThis.crypto?.subtle) {
    const digest = await globalThis.crypto.subtle.digest('SHA-256', bytes)
    return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0'))
      .join('')
      .slice(0, 40)
  }
  // Very old webviews without SubtleCrypto: two FNV-1a passes keep collisions unlikely
  let h1 = 0x811c9dc5
  let h2 = 0x01000193
  for (const byte of bytes) {
    h1 = Math.imul(h1 ^ byte, 0x01000193) >>> 0
    h2 = Math.imul(h2 ^ byte, 0x811c9dc5) >>> 0
  }
  return `f${h1.toString(16).padStart(8, '0')}${h2.toString(16).padStart(8, '0')}`
}

/**
 * Turns raw messages into drafts: bank senders only, completed debits only, nothing already
 * handled, each message at most once. Newest first.
 */
export async function buildDrafts(
  messages: RawSms[],
  categories: { id: number; name: string }[],
  handled: ReadonlySet<string>,
): Promise<SmsDraft[]> {
  const seen = new Set<string>()
  const drafts: SmsDraft[] = []
  for (const sms of [...messages].sort((a, b) => b.date - a.date)) {
    if (!isBankSender(sms.address) || !sms.body) continue
    const parsed = parseBankSms(sms.body)
    if (!parsed || parsed.direction !== 'DEBIT') continue
    const ref = await fingerprint(sms)
    if (handled.has(ref) || seen.has(ref)) continue
    seen.add(ref)
    drafts.push({
      ref,
      sender: sms.address as string,
      amount: parsed.amount,
      merchant: parsed.merchant,
      date: parsed.date ?? localIsoDate(sms.date),
      categoryId: guessCategoryId(parsed.merchant, categories),
    })
  }
  return drafts
}
