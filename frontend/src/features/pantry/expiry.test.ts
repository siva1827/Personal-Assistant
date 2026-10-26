import { describe, expect, it } from 'vitest'
import { daysUntil, isExpiringSoon } from './expiry'

const today = new Date(2026, 9, 26) // 26 Oct 2026

describe('expiry helpers', () => {
  it('counts whole days until a date', () => {
    expect(daysUntil('2026-10-26', today)).toBe(0)
    expect(daysUntil('2026-10-29', today)).toBe(3)
    expect(daysUntil('2026-10-25', today)).toBe(-1)
  })

  it('flags items inside the window, including expired ones', () => {
    expect(isExpiringSoon('2026-10-29', 3, today)).toBe(true)
    expect(isExpiringSoon('2026-10-30', 3, today)).toBe(false)
    expect(isExpiringSoon('2026-10-01', 3, today)).toBe(true)
  })

  it('ignores items without an expiry date', () => {
    expect(isExpiringSoon(null, 3, today)).toBe(false)
  })
})
