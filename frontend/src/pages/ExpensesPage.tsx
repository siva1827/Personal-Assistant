import { useState } from 'react'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { add, chevronBack, chevronForward, mailUnreadOutline, walletOutline } from 'ionicons/icons'
import type { CategorySummary, Expense } from '../api/types'
import DailyChart from '../components/DailyChart'
import EmptyState from '../components/EmptyState'
import ExpenseFormModal from '../components/ExpenseFormModal'
import ListSkeleton from '../components/ListSkeleton'
import { groupByDate } from '../features/expenses/group'
import { budgetPercent, formatMoney } from '../features/expenses/money'
import { monthLabel, shiftMonth, toMonthKey } from '../features/expenses/month'
import { useExpenseList, useSummary } from '../features/expenses/useExpenses'

function CategoryRow({ row }: { row: CategorySummary }) {
  const barClass = row.status === 'OVER' ? 'danger' : row.status === 'NEAR' ? 'warn' : ''
  return (
    <IonItem lines="full" detail={false}>
      <span className="avatar">{row.emoji ?? '📦'}</span>
      <IonLabel>
        <h3>{row.name}</h3>
        {row.budget !== null ? (
          <>
            <div className={`bar ${barClass}`} style={{ margin: '6px 0 4px' }}>
              <span style={{ width: `${budgetPercent(row.total, row.budget)}%` }} />
            </div>
            <p>
              {row.total > row.budget
                ? `Over by ${formatMoney(row.total - row.budget)}`
                : row.total === row.budget
                  ? `Budget of ${formatMoney(row.budget)} reached`
                  : `${formatMoney(row.budget - row.total)} left of ${formatMoney(row.budget)}`}
            </p>
          </>
        ) : (
          <p>No budget</p>
        )}
      </IonLabel>
      <span slot="end" className="money">
        {formatMoney(row.total)}
      </span>
    </IonItem>
  )
}

export default function ExpensesPage() {
  const [month, setMonth] = useState(toMonthKey())
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Expense | null>(null)
  const summary = useSummary(month)
  const list = useExpenseList(month)

  const current = month === toMonthKey()
  const expenses = list.data?.content ?? []
  const loading = summary.isLoading || list.isLoading
  const empty = summary.isSuccess && list.isSuccess && expenses.length === 0

  const openNew = () => {
    setEditing(null)
    setFormOpen(true)
  }

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>Spending</IonTitle>
          <IonButtons slot="end">
            <IonButton routerLink="/expenses/sms" aria-label="Import from SMS">
              <IonIcon slot="start" icon={mailUnreadOutline} />
              SMS
            </IonButton>
            <IonButton routerLink="/budgets" aria-label="Budgets">
              <IonIcon slot="start" icon={walletOutline} />
              Budgets
            </IonButton>
          </IonButtons>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        <div className="month-switcher">
          <IonButton fill="clear" aria-label="Previous month" onClick={() => setMonth(shiftMonth(month, -1))}>
            <IonIcon slot="icon-only" icon={chevronBack} />
          </IonButton>
          <strong>{monthLabel(month)}</strong>
          <IonButton
            fill="clear"
            aria-label="Next month"
            disabled={current}
            onClick={() => setMonth(shiftMonth(month, 1))}
          >
            <IonIcon slot="icon-only" icon={chevronForward} />
          </IonButton>
        </div>

        {(summary.isError || list.isError) && (
          <div className="card">
            <IonText color="danger">Could not load your spending.</IonText>
          </div>
        )}
        {loading && <ListSkeleton rows={3} />}

        {summary.data && (
          <div className="card total-card">
            <div className="muted">Spent in {monthLabel(month).split(' ')[0]}</div>
            <div className="amount">{formatMoney(summary.data.total)}</div>
            {summary.data.byDay.length > 0 && <DailyChart month={month} byDay={summary.data.byDay} />}
          </div>
        )}

        {empty && (
          <EmptyState emoji="💸" title="No spending yet">
            {current ? 'Tap + to log your first expense. It takes about five seconds.' : 'Nothing logged this month.'}
          </EmptyState>
        )}

        {summary.data && summary.data.byCategory.length > 0 && (
          <>
            <div className="section-title">By category</div>
            <div className="row-list">
              {summary.data.byCategory.map((row) => (
                <CategoryRow key={row.categoryId} row={row} />
              ))}
            </div>
          </>
        )}

        {expenses.length > 0 && (
          <>
            <div className="section-title">Transactions</div>
            {groupByDate(expenses).map((group) => (
              <div key={group.date}>
                <div className="group-header">
                  <span>{group.label}</span>
                  <span className="money">{formatMoney(group.total)}</span>
                </div>
                <div className="row-list">
                  {group.items.map((expense) => (
                    <IonItem
                      key={expense.id}
                      lines="full"
                      button
                      detail={false}
                      onClick={() => {
                        setEditing(expense)
                        setFormOpen(true)
                      }}
                    >
                      <span className="avatar">{expense.categoryEmoji ?? '📦'}</span>
                      <IonLabel>
                        <h3>{expense.note ?? expense.categoryName}</h3>
                        {expense.note && <p>{expense.categoryName}</p>}
                      </IonLabel>
                      <span slot="end" className="money">
                        {formatMoney(expense.amount)}
                      </span>
                    </IonItem>
                  ))}
                </div>
              </div>
            ))}
          </>
        )}

        <IonFab slot="fixed" vertical="bottom" horizontal="end">
          <IonFabButton aria-label="Add expense" onClick={openNew}>
            <IonIcon icon={add} />
          </IonFabButton>
        </IonFab>

        <ExpenseFormModal isOpen={formOpen} expense={editing} onClose={() => setFormOpen(false)} />
      </IonContent>
    </IonPage>
  )
}
