const CURRENCY = 'INR'

/** Whole amounts show without decimals; others show two. */
export function formatMoney(amount: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: CURRENCY,
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

/** Share of the budget used, capped at 100 for progress bars. */
export function budgetPercent(spent: number, budget: number): number {
  if (budget <= 0) return 0
  return Math.min(100, Math.round((spent / budget) * 100))
}
