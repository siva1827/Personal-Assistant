import { useState } from 'react'
import {
  IonButton,
  IonCheckbox,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { addOutline, trashOutline } from 'ionicons/icons'
import { isAxiosError } from 'axios'
import type { Recurrence } from '../api/types'
import EmptyState from '../components/EmptyState'
import ListSkeleton from '../components/ListSkeleton'
import { formatMoney } from '../features/expenses/money'
import {
  useAddShoppingItem,
  useClearPurchased,
  useDeleteShoppingItem,
  useShopping,
  useUpdateShoppingItem,
} from '../features/shopping/useShopping'
import { estimatedTotal } from '../features/shopping/totals'
import { dueInfo } from '../features/tasks/dueLabel'
import { useCreateTask, useDeleteTask, useDoneTasks, useOpenTasks, useUpdateTask } from '../features/tasks/useTasks'

const REPEAT_OPTIONS: { value: Recurrence; label: string }[] = [
  { value: 'NONE', label: 'Does not repeat' },
  { value: 'DAILY', label: 'Every day' },
  { value: 'WEEKLY', label: 'Every week' },
  { value: 'MONTHLY', label: 'Every month' },
]

function TodoList() {
  const open = useOpenTasks()
  const [showDone, setShowDone] = useState(false)
  const done = useDoneTasks(showDone)
  const create = useCreateTask()
  const update = useUpdateTask()
  const remove = useDeleteTask()
  const [title, setTitle] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [recurrence, setRecurrence] = useState<Recurrence>('NONE')

  const add = () => {
    const text = title.trim()
    if (!text) return
    create.mutate(
      { title: text, dueDate: dueDate || undefined, recurrence: dueDate ? recurrence : undefined },
      {
        onSuccess: () => {
          setTitle('')
          setDueDate('')
          setRecurrence('NONE')
        },
      },
    )
  }

  return (
    <>
      <form
        className="card"
        onSubmit={(e) => {
          e.preventDefault()
          add()
        }}
      >
        <IonInput
          label="Add a task"
          placeholder="e.g. Call the plumber"
          labelPlacement="stacked"
          fill="outline"
          maxlength={200}
          value={title}
          onIonInput={(e) => setTitle(String(e.detail.value ?? ''))}
        />
        <div style={{ display: 'flex', gap: 10, marginTop: 12, alignItems: 'center' }}>
          <IonInput
            label="Due (optional)"
            labelPlacement="stacked"
            fill="outline"
            type="date"
            value={dueDate}
            onIonInput={(e) => {
              const value = String(e.detail.value ?? '')
              setDueDate(value)
              if (!value) setRecurrence('NONE')
            }}
          />
          <IonButton type="submit" disabled={!title.trim() || create.isPending}>
            <IonIcon slot="start" icon={addOutline} />
            Add
          </IonButton>
        </div>
        <IonSelect
          label="Repeat"
          labelPlacement="stacked"
          fill="outline"
          style={{ marginTop: 12 }}
          disabled={!dueDate}
          value={recurrence}
          placeholder={dueDate ? undefined : 'Pick a due date to repeat'}
          onIonChange={(e) => setRecurrence(e.detail.value as Recurrence)}
        >
          {REPEAT_OPTIONS.map((o) => (
            <IonSelectOption key={o.value} value={o.value}>
              {o.label}
            </IonSelectOption>
          ))}
        </IonSelect>
        {create.isError && (
          <IonText color="danger">
            <p role="alert">Could not add the task.</p>
          </IonText>
        )}
      </form>

      {open.isLoading && <ListSkeleton rows={3} />}
      {open.isError && (
        <div className="card">
          <IonText color="danger">Could not load your tasks.</IonText>
        </div>
      )}
      {open.isSuccess && open.data.length === 0 && (
        <EmptyState emoji="✅" title="Nothing to do">
          Add a task above, or enjoy the free time.
        </EmptyState>
      )}
      {open.data && open.data.length > 0 && (
        <div className="row-list">
          {open.data.map((task) => {
            const due = task.dueDate ? dueInfo(task.dueDate) : null
            return (
              <IonItem key={task.id} lines="full" detail={false}>
                <IonCheckbox
                  slot="start"
                  aria-label={`Mark ${task.title} as done`}
                  onIonChange={() => update.mutate({ id: task.id, status: 'DONE' })}
                />
                <IonLabel className="ion-text-wrap">
                  {task.title}
                  {task.recurrence !== 'NONE' && <p>↻ Repeats {task.recurrence.toLowerCase()}</p>}
                </IonLabel>
                {due && (
                  <span
                    slot="end"
                    className={`pill${due.tone === 'overdue' ? ' danger' : due.tone === 'today' ? ' warn' : ''}`}
                  >
                    {due.label}
                  </span>
                )}
                <IonButton
                  slot="end"
                  fill="clear"
                  color="danger"
                  aria-label={`Delete ${task.title}`}
                  onClick={() => remove.mutate(task.id)}
                >
                  <IonIcon slot="icon-only" icon={trashOutline} />
                </IonButton>
              </IonItem>
            )
          })}
        </div>
      )}

      <IonButton fill="clear" size="small" onClick={() => setShowDone(!showDone)}>
        {showDone ? 'Hide completed' : 'Show completed'}
      </IonButton>
      {showDone && done.data && done.data.length === 0 && <p className="muted">No completed tasks yet.</p>}
      {showDone && done.data && done.data.length > 0 && (
        <div className="row-list">
          {done.data.map((task) => (
            <IonItem key={task.id} lines="full" detail={false}>
              <IonCheckbox
                slot="start"
                checked
                aria-label={`Mark ${task.title} as not done`}
                onIonChange={() => update.mutate({ id: task.id, status: 'OPEN' })}
              />
              <IonLabel className="muted" style={{ textDecoration: 'line-through' }}>
                {task.title}
              </IonLabel>
            </IonItem>
          ))}
        </div>
      )}
    </>
  )
}

function ShoppingList() {
  const list = useShopping()
  const add = useAddShoppingItem()
  const update = useUpdateShoppingItem()
  const remove = useDeleteShoppingItem()
  const clear = useClearPurchased()
  const [name, setName] = useState('')
  const [cost, setCost] = useState('')

  const items = list.data ?? []
  const bought = items.filter((i) => i.purchased).length
  const toBuy = items.length - bought
  const duplicate = add.isError && isAxiosError(add.error) && add.error.response?.status === 409

  const submit = () => {
    const text = name.trim()
    if (!text) return
    const estimatedCost = cost.trim() === '' ? undefined : Number(cost)
    add.mutate(
      { name: text, estimatedCost },
      {
        onSuccess: () => {
          setName('')
          setCost('')
        },
      },
    )
  }

  return (
    <>
      <form
        className="card"
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
      >
        <IonInput
          label="Add an item"
          placeholder="e.g. Tomatoes"
          labelPlacement="stacked"
          fill="outline"
          maxlength={100}
          value={name}
          onIonInput={(e) => setName(String(e.detail.value ?? ''))}
        />
        <div style={{ display: 'flex', gap: 10, marginTop: 12, alignItems: 'center' }}>
          <IonInput
            label="Estimated cost ₹ (optional)"
            labelPlacement="stacked"
            fill="outline"
            type="number"
            inputmode="decimal"
            step="0.01"
            min={0}
            value={cost}
            onIonInput={(e) => setCost(String(e.detail.value ?? ''))}
          />
          <IonButton type="submit" disabled={!name.trim() || add.isPending}>
            <IonIcon slot="start" icon={addOutline} />
            Add
          </IonButton>
        </div>
        {add.isError && (
          <IonText color="danger">
            <p role="alert">{duplicate ? 'That item is already on your list.' : 'Could not add the item.'}</p>
          </IonText>
        )}
      </form>

      {list.isLoading && <ListSkeleton rows={3} />}
      {list.isError && (
        <div className="card">
          <IonText color="danger">Could not load your shopping list.</IonText>
        </div>
      )}
      {list.isSuccess && items.length === 0 && (
        <EmptyState emoji="🛒" title="Your list is empty">
          Add items above, or open a recipe and add what's missing.
        </EmptyState>
      )}
      {items.length > 0 && (
        <>
          <div className="group-header">
            <span>
              {toBuy} to buy{bought > 0 ? ` · ${bought} bought` : ''}
            </span>
            <span className="money">{estimatedTotal(items) > 0 ? `≈ ${formatMoney(estimatedTotal(items))}` : ''}</span>
          </div>
          <div className="row-list">
            {items.map((item) => (
              <IonItem key={item.id} lines="full" detail={false}>
                <IonCheckbox
                  slot="start"
                  checked={item.purchased}
                  aria-label={`${item.name} bought`}
                  onIonChange={(e) => update.mutate({ id: item.id, purchased: e.detail.checked })}
                />
                <IonLabel className={item.purchased ? 'muted' : undefined}
                  style={item.purchased ? { textDecoration: 'line-through' } : undefined}>
                  {item.name}
                </IonLabel>
                {item.estimatedCost !== null && (
                  <span slot="end" className="money">
                    {formatMoney(item.estimatedCost)}
                  </span>
                )}
                <IonButton
                  slot="end"
                  fill="clear"
                  color="danger"
                  aria-label={`Remove ${item.name}`}
                  onClick={() => remove.mutate(item.id)}
                >
                  <IonIcon slot="icon-only" icon={trashOutline} />
                </IonButton>
              </IonItem>
            ))}
          </div>
          {bought > 0 && (
            <IonButton fill="outline" expand="block" onClick={() => clear.mutate()} disabled={clear.isPending}>
              Clear {bought} bought {bought === 1 ? 'item' : 'items'}
            </IonButton>
          )}
        </>
      )}
    </>
  )
}

type Tab = 'todo' | 'shopping'

export default function TasksPage() {
  const [tab, setTab] = useState<Tab>('todo')

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>Tasks</IonTitle>
        </IonToolbar>
        <IonToolbar>
          <IonSegment value={tab} onIonChange={(e) => setTab(e.detail.value as Tab)}>
            <IonSegmentButton value="todo">
              <IonLabel>To-do</IonLabel>
            </IonSegmentButton>
            <IonSegmentButton value="shopping">
              <IonLabel>Shopping list</IonLabel>
            </IonSegmentButton>
          </IonSegment>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">{tab === 'todo' ? <TodoList /> : <ShoppingList />}</IonContent>
    </IonPage>
  )
}
