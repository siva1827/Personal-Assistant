import { describe, expect, it } from 'vitest'
import type { Expense } from '../../api/types'
import { dateLabel, groupByDate } from './group'

const today = new Date(2026, 9, 26) // Mon 26 Oct 2026

const expense = (id: number, spentOn: string, amount: number): Expense => ({
  id,
  categoryId: 1,
  categoryName: 'Food',
  categoryEmoji: null,
  amount,
  note: null,
  spentOn,
  source: 'MANUAL',
})

describe('dateLabel', () => {
  it('uses friendly names for today and yesterday', () => {
    expect(dateLabel('2026-10-26', today)).toBe('Today')
    expect(dateLabel('2026-10-25', today)).toBe('Yesterday')
  })

  it('uses a short date otherwise', () => {
    expect(dateLabel('2026-10-03', today)).toBe('Sat, Oct 3')
  })
})

describe('groupByDate', () => {
  it('groups by day, sums totals and keeps order', () => {
    const groups = groupByDate(
      [expense(1, '2026-10-26', 10), expense(2, '2026-10-26', 5.5), expense(3, '2026-10-03', 100)],
      today,
    )

    expect(groups.map((g) => g.date)).toEqual(['2026-10-26', '2026-10-03'])
    expect(groups[0].total).toBe(15.5)
    expect(groups[0].items).toHaveLength(2)
  })

  it('returns nothing for no expenses', () => {
    expect(groupByDate([], today)).toEqual([])
  })
})
