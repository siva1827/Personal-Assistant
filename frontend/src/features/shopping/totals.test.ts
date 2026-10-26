import { describe, expect, it } from 'vitest'
import type { ShoppingItem } from '../../api/types'
import { estimatedTotal } from './totals'

const item = (id: number, estimatedCost: number | null, purchased = false): ShoppingItem => ({
  id,
  name: `item ${id}`,
  estimatedCost,
  purchased,
  sourceRecipeId: null,
})

describe('estimatedTotal', () => {
  it('adds estimates of items still to buy, avoiding float drift', () => {
    expect(estimatedTotal([item(1, 0.1), item(2, 0.2)])).toBe(0.3)
  })

  it('ignores bought items and items without an estimate', () => {
    expect(estimatedTotal([item(1, 60), item(2, 40, true), item(3, null)])).toBe(60)
  })

  it('is zero for an empty list', () => {
    expect(estimatedTotal([])).toBe(0)
  })
})
