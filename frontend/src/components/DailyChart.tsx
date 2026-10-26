import type { DaySummary } from '../api/types'
import { daysInMonth } from '../features/expenses/month'
import { formatMoney } from '../features/expenses/money'

interface Props {
  month: string
  byDay: DaySummary[]
}

/** One bar per day of the month, scaled to the busiest day. */
export default function DailyChart({ month, byDay }: Props) {
  const totals = new Map(byDay.map((d) => [Number(d.date.slice(8, 10)), d.total]))
  const days = daysInMonth(month)
  const max = Math.max(...byDay.map((d) => d.total), 1)

  return (
    <div>
      <div className="daily-chart" role="img" aria-label="Spending per day this month">
        {Array.from({ length: days }, (_, i) => {
          const day = i + 1
          const total = totals.get(day) ?? 0
          return (
            <div key={day} className={`col${total > 0 ? ' has' : ''}`} title={`${day}: ${formatMoney(total)}`}>
              <span style={{ height: `${total > 0 ? Math.max(6, (total / max) * 100) : 0}%` }} />
            </div>
          )
        })}
      </div>
      <div className="daily-chart-axis">
        <span>1</span>
        <span>{Math.ceil(days / 2)}</span>
        <span>{days}</span>
      </div>
    </div>
  )
}
