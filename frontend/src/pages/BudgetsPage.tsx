import { useState } from 'react'
import {
  IonAlert,
  IonBackButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import type { Category } from '../api/types'
import ListSkeleton from '../components/ListSkeleton'
import { formatMoney } from '../features/expenses/money'
import { monthLabel, toMonthKey } from '../features/expenses/month'
import { useBudgets, useCategories, useClearBudget, useSetBudget } from '../features/expenses/useExpenses'

export default function BudgetsPage() {
  const month = toMonthKey()
  const categories = useCategories()
  const budgets = useBudgets(month)
  const setBudget = useSetBudget(month)
  const clearBudget = useClearBudget(month)
  const [editing, setEditing] = useState<Category | null>(null)

  const limits = new Map(budgets.data?.map((b) => [b.categoryId, b.limitAmount]))
  const currentLimit = editing ? limits.get(editing.id) : undefined

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/expenses" />
          </IonButtons>
          <IonTitle>Budgets</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        <p className="muted" style={{ margin: '4px 4px 12px' }}>
          Monthly limits for {monthLabel(month)}. Tap a category to set or change its limit.
        </p>
        {(categories.isLoading || budgets.isLoading) && <ListSkeleton rows={5} />}
        {(categories.isError || budgets.isError) && (
          <div className="card">
            <IonText color="danger">Could not load budgets.</IonText>
          </div>
        )}
        {categories.data && budgets.data && (
          <div className="row-list">
            {categories.data.map((category) => {
              const limit = limits.get(category.id)
              return (
                <IonItem key={category.id} lines="full" button detail={false} onClick={() => setEditing(category)}>
                  <span className="avatar">{category.emoji ?? '📦'}</span>
                  <IonLabel>{category.name}</IonLabel>
                  {limit !== undefined ? (
                    <span slot="end" className="money">
                      {formatMoney(limit)}
                    </span>
                  ) : (
                    <span slot="end" className="muted">
                      Set limit
                    </span>
                  )}
                </IonItem>
              )
            })}
          </div>
        )}

        <IonAlert
          isOpen={editing !== null}
          header={editing ? `${editing.name} budget` : ''}
          subHeader={`Monthly limit in ₹`}
          onDidDismiss={() => setEditing(null)}
          inputs={[{ name: 'limit', type: 'number', placeholder: 'e.g. 5000', min: 1, value: currentLimit ?? '' }]}
          buttons={[
            { text: 'Cancel', role: 'cancel' },
            ...(currentLimit !== undefined
              ? [
                  {
                    text: 'Remove',
                    role: 'destructive',
                    handler: () => {
                      if (editing) clearBudget.mutate(editing.id)
                    },
                  },
                ]
              : []),
            {
              text: 'Save',
              handler: (values: { limit: string }) => {
                const limitAmount = Number(values.limit)
                if (!editing || !(limitAmount > 0)) return false
                setBudget.mutate({ categoryId: editing.id, limitAmount })
              },
            },
          ]}
        />
      </IonContent>
    </IonPage>
  )
}
