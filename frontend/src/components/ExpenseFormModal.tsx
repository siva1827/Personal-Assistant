import { useState } from 'react'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonModal,
  IonText,
  IonTextarea,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { chatbubbleEllipsesOutline, trashOutline } from 'ionicons/icons'
import type { Category, Expense } from '../api/types'
import {
  useCategories,
  useCreateExpense,
  useDeleteExpense,
  useUpdateExpense,
} from '../features/expenses/useExpenses'
import { guessCategoryId, parseBankSms } from '../features/expenses/smsParser'

const LAST_CATEGORY_KEY = 'lastExpenseCategoryId'

function todayIso(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

interface FormProps {
  expense: Expense | null
  categories: Category[]
  onDone: () => void
}

function ExpenseForm({ expense, categories, onDone }: FormProps) {
  const create = useCreateExpense()
  const update = useUpdateExpense()
  const remove = useDeleteExpense()

  const remembered = Number(localStorage.getItem(LAST_CATEGORY_KEY)) || null
  const [amount, setAmount] = useState(expense ? String(expense.amount) : '')
  const [categoryId, setCategoryId] = useState<number | null>(expense?.categoryId ?? remembered)
  const [note, setNote] = useState(expense?.note ?? '')
  const [spentOn, setSpentOn] = useState(expense?.spentOn ?? todayIso())

  const value = Number(amount)
  const valid = value > 0 && categoryId !== null && /^\d+(\.\d{1,2})?$/.test(amount)
  const busy = create.isPending || update.isPending || remove.isPending
  const failed = create.isError || update.isError || remove.isError

  const save = () => {
    if (!valid || categoryId === null) return
    const request = { categoryId, amount: value, note: note.trim(), spentOn }
    localStorage.setItem(LAST_CATEGORY_KEY, String(categoryId))
    if (expense) update.mutate({ id: expense.id, ...request }, { onSuccess: onDone })
    else create.mutate(request, { onSuccess: onDone })
  }

  const [smsOpen, setSmsOpen] = useState(false)
  const [smsText, setSmsText] = useState('')
  const [smsNote, setSmsNote] = useState<{ ok: boolean; text: string } | null>(null)

  const fillFromSms = () => {
    const parsed = parseBankSms(smsText)
    if (!parsed) {
      setSmsNote({ ok: false, text: "That doesn't look like a bank transaction message." })
      return
    }
    if (parsed.direction === 'CREDIT') {
      setSmsNote({ ok: false, text: `This looks like ₹${parsed.amount} received, not an expense.` })
      return
    }
    setAmount(String(parsed.amount))
    if (parsed.merchant) setNote(parsed.merchant)
    if (parsed.date) setSpentOn(parsed.date)
    const guessed = guessCategoryId(parsed.merchant, categories)
    if (guessed !== null) setCategoryId(guessed)
    setSmsNote({
      ok: true,
      text: guessed !== null ? 'Filled in. Check the details and save.' : 'Filled in. Pick a category and save.',
    })
  }

  return (
    <>
      {!expense && (
        <div style={{ marginBottom: 8 }}>
          <IonButton fill="clear" size="small" onClick={() => setSmsOpen(!smsOpen)}>
            <IonIcon slot="start" icon={chatbubbleEllipsesOutline} />
            {smsOpen ? 'Hide bank SMS' : 'Paste a bank SMS'}
          </IonButton>
          {smsOpen && (
            <div className="card" style={{ marginTop: 6 }}>
              <IonTextarea
                label="Bank SMS"
                labelPlacement="floating"
                fill="outline"
                rows={3}
                placeholder="Rs.250.00 debited from A/c XX1234 to VPA swiggy@upi on 26-10-26"
                value={smsText}
                onIonInput={(e) => {
                  setSmsText(String(e.detail.value ?? ''))
                  setSmsNote(null)
                }}
              />
              <IonButton expand="block" size="small" style={{ marginTop: 10 }} disabled={!smsText.trim()} onClick={fillFromSms}>
                Fill from SMS
              </IonButton>
              {smsNote && (
                <IonText color={smsNote.ok ? 'success' : 'danger'}>
                  <p role="status" style={{ margin: '8px 2px 0' }}>{smsNote.text}</p>
                </IonText>
              )}
            </div>
          )}
        </div>
      )}
      <IonInput
        className="amount-input"
        type="number"
        inputmode="decimal"
        placeholder="0"
        label="Amount (₹)"
        labelPlacement="stacked"
        autofocus
        value={amount}
        onIonInput={(e) => setAmount(String(e.detail.value ?? ''))}
      />

      <div className="section-title" style={{ marginTop: 14 }}>
        Category
      </div>
      <div className="cat-grid" role="radiogroup" aria-label="Category">
        {categories.map((c) => (
          <button
            key={c.id}
            type="button"
            role="radio"
            aria-checked={c.id === categoryId}
            className={`cat-chip${c.id === categoryId ? ' selected' : ''}`}
            onClick={() => setCategoryId(c.id)}
          >
            {c.emoji} {c.name}
          </button>
        ))}
      </div>

      <IonInput
        label="Note (optional)"
        labelPlacement="floating"
        fill="outline"
        maxlength={200}
        value={note}
        onIonInput={(e) => setNote(String(e.detail.value ?? ''))}
      />
      <IonInput
        label="Date"
        labelPlacement="stacked"
        fill="outline"
        type="date"
        style={{ marginTop: 14 }}
        value={spentOn}
        onIonInput={(e) => setSpentOn(String(e.detail.value ?? ''))}
      />

      {failed && (
        <IonText color="danger">
          <p role="alert">Something went wrong. Please try again.</p>
        </IonText>
      )}

      <IonButton expand="block" style={{ marginTop: 18 }} onClick={save} disabled={!valid || busy}>
        {expense ? 'Save changes' : 'Add expense'}
      </IonButton>
      {expense && (
        <IonButton
          expand="block"
          fill="clear"
          color="danger"
          disabled={busy}
          onClick={() => remove.mutate(expense.id, { onSuccess: onDone })}
        >
          <IonIcon slot="start" icon={trashOutline} />
          Delete
        </IonButton>
      )}
    </>
  )
}

interface Props {
  isOpen: boolean
  expense: Expense | null
  onClose: () => void
}

/** Bottom sheet for adding a new expense, or editing an existing one. */
export default function ExpenseFormModal({ isOpen, expense, onClose }: Props) {
  const categories = useCategories()

  return (
    <IonModal isOpen={isOpen} onDidDismiss={onClose} initialBreakpoint={0.9} breakpoints={[0, 0.9]}>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>{expense ? 'Edit expense' : 'New expense'}</IonTitle>
          <IonButtons slot="end">
            <IonButton onClick={onClose}>Close</IonButton>
          </IonButtons>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        {categories.data && (
          <ExpenseForm key={expense?.id ?? 'new'} expense={expense} categories={categories.data} onDone={onClose} />
        )}
        {categories.isError && <IonText color="danger">Could not load categories.</IonText>}
      </IonContent>
    </IonModal>
  )
}
