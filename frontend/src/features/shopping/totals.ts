import type { ShoppingItem } from '../../api/types'

/** Sum of the estimates on items still to buy; items without an estimate count as zero. */
export function estimatedTotal(items: ShoppingItem[]): number {
  const cents = items
    .filter((i) => !i.purchased && i.estimatedCost !== null)
    .reduce((sum, i) => sum + Math.round((i.estimatedCost ?? 0) * 100), 0)
  return cents / 100
}
