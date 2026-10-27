import { beforeEach, describe, expect, it } from 'vitest'
import { buildDrafts, fingerprint, isBankSender, localIsoDate, rangeStartMs } from './drafts'
import { loadHandled, markHandled } from './handled'
import { sampleMessages, type RawSms } from './smsInbox'

const categories = [
  { id: 1, name: 'Food' },
  { id: 2, name: 'Transport' },
  { id: 3, name: 'Shopping' },
]

const sms = (id: string, address: string | null, body: string | null, date = 1_000): RawSms => ({
  id,
  address,
  body,
  date,
})

describe('isBankSender', () => {
  it('accepts named sender ids and rejects phone numbers', () => {
    expect(isBankSender('AX-HDFCBK')).toBe(true)
    expect(isBankSender('JD-ICICIB-S')).toBe(true)
    expect(isBankSender('+919800000000')).toBe(false)
    expect(isBankSender('9800000000')).toBe(false)
    expect(isBankSender(null)).toBe(false)
  })
})

describe('rangeStartMs', () => {
  const now = new Date(2026, 9, 27, 15, 30).getTime()

  it('starts at midnight today for one day', () => {
    expect(new Date(rangeStartMs(1, now))).toEqual(new Date(2026, 9, 27, 0, 0))
  })

  it('goes back whole days', () => {
    expect(new Date(rangeStartMs(3, now))).toEqual(new Date(2026, 9, 25, 0, 0))
    expect(new Date(rangeStartMs(7, now))).toEqual(new Date(2026, 9, 21, 0, 0))
  })
})

describe('fingerprint', () => {
  it('is stable for the same message and different for another', async () => {
    const a = sms('1', 'AX-BK', 'Rs 5 debited')
    expect(await fingerprint(a)).toBe(await fingerprint({ ...a, id: 'other-device-id' }))
    expect(await fingerprint(a)).not.toBe(await fingerprint(sms('1', 'AX-BK', 'Rs 6 debited')))
  })

  it('fits the 64 character limit of the API', async () => {
    expect((await fingerprint(sms('1', 'AX-BK', 'x'))).length).toBeLessThanOrEqual(64)
  })
})

describe('buildDrafts', () => {
  it('keeps only completed debits from bank senders, with a category guess', async () => {
    const now = new Date(2026, 9, 27, 12, 0).getTime()
    const drafts = await buildDrafts(sampleMessages(now), categories, new Set())

    expect(drafts.map((d) => d.merchant)).toEqual(['swiggy', 'AMAZON PAY INDIA', 'UBER INDIA'])
    expect(drafts.map((d) => d.amount)).toEqual([250, 1299.5, 60])
    expect(drafts.map((d) => d.categoryId)).toEqual([1, 3, 2])
    // credit, OTP and the message from a phone number are all left out
    expect(drafts).toHaveLength(3)
  })

  it('uses the message date when the text has none', async () => {
    const date = new Date(2026, 9, 27, 9, 0).getTime()
    const [draft] = await buildDrafts([sms('1', 'AX-BK', 'Rs.40 debited at CAFE', date)], categories, new Set())

    expect(draft.date).toBe(localIsoDate(date))
    expect(draft.date).toBe('2026-10-27')
  })

  it('prefers the date written in the message', async () => {
    const [draft] = await buildDrafts(
      [sms('1', 'AX-BK', 'Rs.40 debited from A/c XX1 on 25-10-26 at CAFE', new Date(2026, 9, 27).getTime())],
      categories,
      new Set(),
    )
    expect(draft.date).toBe('2026-10-25')
  })

  it('skips messages already handled and repeats of the same message', async () => {
    const message = sms('1', 'AX-BK', 'Rs.40 debited at CAFE', 5_000)
    const ref = await fingerprint(message)

    expect(await buildDrafts([message], categories, new Set([ref]))).toEqual([])
    expect(await buildDrafts([message, { ...message, id: '2' }], categories, new Set())).toHaveLength(1)
  })

  it('ignores messages without text', async () => {
    expect(await buildDrafts([sms('1', 'AX-BK', null)], categories, new Set())).toEqual([])
  })
})

describe('handled messages', () => {
  beforeEach(() => localStorage.clear())

  it('remembers what was handled', () => {
    expect(loadHandled().size).toBe(0)
    markHandled('abc')
    markHandled('def')
    expect([...loadHandled()]).toEqual(['abc', 'def'])
  })

  it('survives corrupted storage', () => {
    localStorage.setItem('handledSmsRefs', '{not json')
    expect(loadHandled().size).toBe(0)
  })

  it('keeps only the newest 500', () => {
    for (let i = 0; i < 505; i++) markHandled(`ref${i}`)
    const refs = loadHandled()
    expect(refs.size).toBe(500)
    expect(refs.has('ref504')).toBe(true)
    expect(refs.has('ref0')).toBe(false)
  })
})
