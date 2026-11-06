import { describe, expect, it } from 'vitest'
import { daysLeft, describeSync, statusInfo } from './describe'

const result = (imported: number, skipped: number, creditsIgnored: number) => ({
  imported,
  skipped,
  creditsIgnored,
  accounts: [],
})

describe('describeSync', () => {
  it('reports what was added and what was left out', () => {
    expect(describeSync(result(89, 0, 3))).toBe('Added 89 expenses to your spending, 3 incoming payments ignored.')
    expect(describeSync(result(1, 2, 1))).toBe(
      'Added 1 expense to your spending, 2 already there, 1 incoming payment ignored.',
    )
  })

  it('says so when there is nothing new', () => {
    expect(describeSync(result(0, 89, 3))).toBe('Nothing new. 89 transactions were already in your spending.')
    expect(describeSync(result(0, 1, 0))).toBe('Nothing new. 1 transaction was already in your spending.')
    expect(describeSync(result(0, 0, 0))).toBe('No spending found in this period.')
  })
})

describe('statusInfo', () => {
  it('has a readable label and colour for every status', () => {
    expect(statusInfo('ACTIVE')).toEqual({ label: 'Connected', tone: 'ok' })
    expect(statusInfo('PENDING').tone).toBe('warn')
    expect(statusInfo('REVOKED').label).toBe('Stopped')
    expect(statusInfo('REJECTED').tone).toBe('danger')
    expect(statusInfo('EXPIRED').label).toBe('Expired')
  })
})

describe('daysLeft', () => {
  const now = new Date('2026-11-06T00:00:00Z')

  it('counts whole days, rounding up', () => {
    expect(daysLeft('2026-12-06T00:00:00Z', now)).toBe(30)
    expect(daysLeft('2026-11-06T06:00:00Z', now)).toBe(1)
  })

  it('never goes negative and copes with no date', () => {
    expect(daysLeft('2026-11-01T00:00:00Z', now)).toBe(0)
    expect(daysLeft(null, now)).toBeNull()
  })
})
