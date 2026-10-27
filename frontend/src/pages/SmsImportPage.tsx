import { useState } from 'react'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { isAxiosError } from 'axios'
import { lockClosedOutline, mailUnreadOutline } from 'ionicons/icons'
import EmptyState from '../components/EmptyState'
import { formatMoney } from '../features/expenses/money'
import { useCategories, useCreateExpense } from '../features/expenses/useExpenses'
import { buildDrafts, rangeStartMs, type SmsDraft } from '../features/sms/drafts'
import { loadHandled, markHandled } from '../features/sms/handled'
import { isRealInbox, loadSampleMessages, SmsInbox } from '../features/sms/smsInbox'

type Range = 1 | 3 | 7
const RANGES: { value: Range; label: string }[] = [
  { value: 1, label: 'Today' },
  { value: 3, label: '3 days' },
  { value: 7, label: '7 days' },
]

function describeError(error: unknown): string {
  const text = error instanceof Error ? error.message : typeof error === 'object' && error !== null && 'message' in error ? String((error as { message: unknown }).message) : ''
  if (/denied|PERMISSION/i.test(text)) {
    return 'Permission to read messages was denied. You can allow it in your phone settings under Apps, Daily Assistant, Permissions, then scan again.'
  }
  return 'Could not read your messages. Please try again.'
}

export default function SmsImportPage() {
  const categories = useCategories()
  const create = useCreateExpense()
  const [range, setRange] = useState<Range>(1)
  const [drafts, setDrafts] = useState<SmsDraft[] | null>(null)
  const [picked, setPicked] = useState<Record<string, number>>({})
  const [scanning, setScanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [added, setAdded] = useState(0)

  const cats = categories.data ?? []
  const fallbackCategory = cats.find((c) => c.name === 'Other')?.id ?? cats[0]?.id

  const categoryFor = (draft: SmsDraft): number | undefined => picked[draft.ref] ?? draft.categoryId ?? fallbackCategory

  const scan = async () => {
    setScanning(true)
    setError(null)
    setAdded(0)
    try {
      const { messages } = await SmsInbox.readInbox({ sinceMs: rangeStartMs(range), limit: 300 })
      setDrafts(await buildDrafts(messages, cats, loadHandled()))
      setPicked({})
    } catch (e) {
      setError(describeError(e))
      setDrafts(null)
    } finally {
      setScanning(false)
    }
  }

  const remove = (ref: string) => setDrafts((current) => current?.filter((d) => d.ref !== ref) ?? null)

  const skip = (draft: SmsDraft) => {
    markHandled(draft.ref)
    remove(draft.ref)
  }

  /** Returns true when the draft is dealt with (saved, or already on the server). */
  const save = async (draft: SmsDraft): Promise<boolean> => {
    const categoryId = categoryFor(draft)
    if (categoryId === undefined) return false
    try {
      await create.mutateAsync({
        categoryId,
        amount: draft.amount,
        note: draft.merchant ?? draft.sender,
        spentOn: draft.date,
        source: 'SMS',
        externalRef: draft.ref,
      })
      setAdded((n) => n + 1)
    } catch (e) {
      // 409 means this exact message was added before (another device, or storage was cleared)
      if (!(isAxiosError(e) && e.response?.status === 409)) return false
    }
    markHandled(draft.ref)
    remove(draft.ref)
    return true
  }

  const addAll = async () => {
    for (const draft of drafts ?? []) {
      if (!(await save(draft))) break
    }
  }

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/expenses" />
          </IonButtons>
          <IonTitle>Import from SMS</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        <div className="card">
          <p style={{ margin: '0 0 8px' }}>
            <IonIcon icon={lockClosedOutline} style={{ verticalAlign: '-2px' }} /> <strong>Private by design</strong>
          </p>
          <p className="muted" style={{ margin: 0 }}>
            Messages are read and understood on this device. Only the amount, shop and date of the ones you add are
            saved. Personal messages and OTPs are ignored, and nothing is added until you confirm.
          </p>
        </div>

        {!isRealInbox() && (
          <div className="card">
            <strong>Not on an Android phone</strong>
            <p className="muted" style={{ margin: '4px 0 10px' }}>
              A browser can't read your messages. You can try the screen with a few made-up sample messages.
            </p>
            <IonButton size="small" fill="outline" onClick={() => loadSampleMessages()}>
              Load sample messages
            </IonButton>
          </div>
        )}

        <IonSegment value={range} onIonChange={(e) => setRange(Number(e.detail.value) as Range)}>
          {RANGES.map((r) => (
            <IonSegmentButton key={r.value} value={r.value}>
              <IonLabel>{r.label}</IonLabel>
            </IonSegmentButton>
          ))}
        </IonSegment>
        <IonButton expand="block" style={{ marginTop: 14 }} onClick={() => void scan()} disabled={scanning || !categories.data}>
          {scanning ? <IonSpinner name="dots" /> : (
            <>
              <IonIcon slot="start" icon={mailUnreadOutline} />
              {drafts === null ? 'Scan messages' : 'Scan again'}
            </>
          )}
        </IonButton>

        {error && (
          <IonText color="danger">
            <p role="alert">{error}</p>
          </IonText>
        )}

        {added > 0 && (
          <IonText color="success">
            <p role="status">
              Added {added} {added === 1 ? 'expense' : 'expenses'}.
            </p>
          </IonText>
        )}

        {drafts !== null && drafts.length === 0 && (
          <EmptyState emoji="📭" title="Nothing new found">
            No new bank debit messages in this period. Messages you already added or skipped are not shown again.
          </EmptyState>
        )}

        {drafts !== null && drafts.length > 0 && (
          <>
            <div className="group-header">
              <span>
                {drafts.length} to review · {formatMoney(drafts.reduce((sum, d) => sum + d.amount, 0))}
              </span>
            </div>
            {drafts.map((draft) => (
              <div className="card" key={draft.ref}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                  <div>
                    <strong>{draft.merchant ?? 'Unknown merchant'}</strong>
                    <div className="muted" style={{ fontSize: 13 }}>
                      {draft.date} · {draft.sender}
                    </div>
                  </div>
                  <span className="money" style={{ fontSize: 18 }}>
                    {formatMoney(draft.amount)}
                  </span>
                </div>
                <IonSelect
                  aria-label="Category"
                  interface="action-sheet"
                  fill="outline"
                  style={{ marginTop: 10 }}
                  value={categoryFor(draft)}
                  onIonChange={(e) => setPicked((p) => ({ ...p, [draft.ref]: Number(e.detail.value) }))}
                >
                  {cats.map((c) => (
                    <IonSelectOption key={c.id} value={c.id}>
                      {c.emoji} {c.name}
                    </IonSelectOption>
                  ))}
                </IonSelect>
                <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                  <IonButton size="small" disabled={create.isPending} onClick={() => void save(draft)}>
                    Add
                  </IonButton>
                  <IonButton size="small" fill="clear" color="medium" onClick={() => skip(draft)}>
                    Skip
                  </IonButton>
                </div>
              </div>
            ))}
            {drafts.length > 1 && (
              <IonButton expand="block" fill="outline" disabled={create.isPending} onClick={() => void addAll()}>
                Add all {drafts.length}
              </IonButton>
            )}
            {create.isError && !isAxiosError(create.error) && (
              <IonText color="danger">
                <p role="alert">Something went wrong adding that expense.</p>
              </IonText>
            )}
          </>
        )}
      </IonContent>
    </IonPage>
  )
}
