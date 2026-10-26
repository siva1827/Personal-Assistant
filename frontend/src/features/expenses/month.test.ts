import { describe, expect, it } from 'vitest'
import { daysInMonth, monthLabel, shiftMonth, toMonthKey } from './month'

describe('month helpers', () => {
  it('formats a date as a month key', () => {
    expect(toMonthKey(new Date(2026, 0, 31))).toBe('2026-01')
    expect(toMonthKey(new Date(2026, 9, 5))).toBe('2026-10')
  })

  it('shifts across year boundaries', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2026-10', 0)).toBe('2026-10')
  })

  it('labels and sizes months', () => {
    expect(monthLabel('2026-10')).toBe('October 2026')
    expect(daysInMonth('2026-02')).toBe(28)
    expect(daysInMonth('2028-02')).toBe(29)
    expect(daysInMonth('2026-10')).toBe(31)
  })
})
