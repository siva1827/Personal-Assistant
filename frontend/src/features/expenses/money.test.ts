import { describe, expect, it } from 'vitest'
import { budgetPercent, formatMoney } from './money'

describe('formatMoney', () => {
  it('drops decimals for whole amounts and keeps two otherwise', () => {
    expect(formatMoney(250)).toBe('₹250')
    expect(formatMoney(40.5)).toBe('₹40.50')
  })

  it('uses Indian digit grouping', () => {
    expect(formatMoney(1234567)).toBe('₹12,34,567')
  })
})

describe('budgetPercent', () => {
  it('rounds and caps at 100', () => {
    expect(budgetPercent(50, 200)).toBe(25)
    expect(budgetPercent(250, 200)).toBe(100)
  })

  it('is zero without a usable budget', () => {
    expect(budgetPercent(10, 0)).toBe(0)
  })
})
