import { useState } from 'react'
import {
  IonAlert,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonSpinner,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { isAxiosError } from 'axios'
import { lockClosedOutline, shieldCheckmarkOutline } from 'ionicons/icons'
import EmptyState from '../components/EmptyState'
import ListSkeleton from '../components/ListSkeleton'
import { daysLeft, describeSync, statusInfo } from '../features/bank/describe'
import {
  useAaStatus,
  useConsents,
  useCreateConsent,
  useDecideConsent,
  useRevokeConsent,
  useSyncConsent,
} from '../features/bank/useBank'
import { formatMoney } from '../features/expenses/money'
import type { BankConsent } from '../api/types'

const apiMessage = (error: unknown): string | undefined =>
  isAxiosError(error) ? (error.response?.data as { message?: string } | undefined)?.message : undefined

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' })
}

export default function BankPage() {
  const status = useAaStatus()
  const configured = status.data?.configured === true
  const mock = status.data?.mock === true
  const consents = useConsents(configured)
  const create = useCreateConsent()
  const decide = useDecideConsent()
  const sync = useSyncConsent()
  const revoke = useRevokeConsent()
  const [confirmStop, setConfirmStop] = useState(false)

  const latest: BankConsent | undefined = consents.data?.[0]
  const active = latest?.status === 'ACTIVE'
  const pending = latest?.status === 'PENDING'
  const canLink = configured && consents.isSuccess && !active && !pending
  const failure = apiMessage(create.error) ?? apiMessage(decide.error) ?? apiMessage(sync.error) ?? apiMessage(revoke.error)
  const busy = create.isPending || decide.isPending || sync.isPending || revoke.isPending

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/expenses" />
          </IonButtons>
          <IonTitle>Bank connect</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        {status.isLoading && <ListSkeleton rows={3} />}
        {status.isError && (
          <div className="card">
            <IonText color="danger">Could not reach the server.</IonText>
          </div>
        )}

        {status.data && !configured && (
          <EmptyState emoji="🏦" title="Bank connect is not switched on">
            It needs an Account Aggregator provider on the server. To try the whole flow with invented data, start
            the API with AA_PROVIDER=mock. See docs/BANK_CONNECT.md.
          </EmptyState>
        )}

        {configured && mock && (
          <div className="card">
            <strong>Demo mode</strong>
            <p className="muted" style={{ margin: '4px 0 0' }}>
              This is a pretend bank. The accounts and transactions are invented, and nothing here connects to a
              real bank.
            </p>
          </div>
        )}

        {configured && (
          <div className="card">
            <p style={{ margin: '0 0 8px' }}>
              <IonIcon icon={shieldCheckmarkOutline} style={{ verticalAlign: '-2px' }} />{' '}
              <strong>You stay in control</strong>
            </p>
            <p className="muted" style={{ margin: 0 }}>
              An Account Aggregator is India's consent-based way to share bank data. You approve exactly what is
              shared and for how long, only money that went out is added to your spending, and you can stop sharing
              at any time. Expenses already added stay.
            </p>
          </div>
        )}

        {consents.isLoading && configured && <ListSkeleton rows={2} />}

        {/* Waiting for approval */}
        {pending && latest && (
          <div className="card">
            <div className="section-title" style={{ margin: '0 0 8px' }}>
              {mock ? 'Demo Account Aggregator' : 'Approve in your Account Aggregator app'}
            </div>
            <p style={{ marginTop: 0 }}>
              <strong>Daily Assistant</strong> asks to see your account balances and transactions.
            </p>
            <ul className="muted" style={{ paddingLeft: 18, margin: '0 0 12px' }}>
              <li>Purpose: {latest.purpose}</li>
              <li>
                Transactions from {formatDate(latest.dataFrom)}
              </li>
              <li>Access ends 30 days after you approve, or sooner if you stop it</li>
            </ul>
            {mock ? (
              <div style={{ display: 'flex', gap: 8 }}>
                <IonButton disabled={busy} onClick={() => decide.mutate({ id: latest.id, approve: true })}>
                  <IonIcon slot="start" icon={lockClosedOutline} />
                  Approve
                </IonButton>
                <IonButton fill="outline" color="medium" disabled={busy} onClick={() => decide.mutate({ id: latest.id, approve: false })}>
                  Reject
                </IonButton>
              </div>
            ) : (
              <p className="muted" style={{ margin: 0 }}>
                Open your Account Aggregator app to approve, then come back and pull down to refresh.
              </p>
            )}
            <IonButton fill="clear" size="small" color="danger" disabled={busy} onClick={() => setConfirmStop(true)}>
              Cancel this request
            </IonButton>
          </div>
        )}

        {/* Connected */}
        {active && latest && (
          <>
            <div className="section-title">Linked accounts</div>
            {latest.accounts.length === 0 && (
              <div className="card muted">Accounts appear here after your first sync.</div>
            )}
            {latest.accounts.map((account) => (
              <div className="card" key={account.id} style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <strong>{account.bankName}</strong>
                  <div className="muted" style={{ fontSize: 13 }}>
                    {account.accountType.toLowerCase()} · {account.maskedNumber}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div className="money" style={{ fontSize: 18 }}>
                    {account.balance === null ? '-' : formatMoney(account.balance)}
                  </div>
                  <div className="muted" style={{ fontSize: 12 }}>
                    balance
                  </div>
                </div>
              </div>
            ))}

            <div className="card">
              <span className={`pill${statusInfo(latest.status).tone === 'ok' ? '' : ' warn'}`}>
                {statusInfo(latest.status).label}
              </span>
              <p className="muted" style={{ margin: '8px 0 12px' }}>
                {latest.lastSyncedAt ? `Last synced ${formatDate(latest.lastSyncedAt)}. ` : 'Not synced yet. '}
                Access ends in {daysLeft(latest.expiresAt) ?? '?'} days.
              </p>
              <IonButton expand="block" disabled={busy} onClick={() => sync.mutate(latest.id)}>
                {sync.isPending ? <IonSpinner name="dots" /> : latest.lastSyncedAt ? 'Sync now' : 'Fetch my transactions'}
              </IonButton>
              {sync.isSuccess && (
                <IonText color="success">
                  <p role="status" style={{ margin: '10px 0 0' }}>
                    {describeSync(sync.data)}
                  </p>
                </IonText>
              )}
              <IonButton fill="clear" color="danger" expand="block" disabled={busy} onClick={() => setConfirmStop(true)}>
                Stop sharing
              </IonButton>
            </div>
          </>
        )}

        {/* No connection, or the last one ended */}
        {canLink && (
          <div className="card">
            {latest && (
              <p style={{ marginTop: 0 }}>
                Your last connection is{' '}
                <span className={`pill ${statusInfo(latest.status).tone === 'danger' ? 'danger' : 'warn'}`}>
                  {statusInfo(latest.status).label}
                </span>
                .
              </p>
            )}
            <p className={latest ? 'muted' : undefined} style={{ marginTop: 0 }}>
              {latest ? 'Link again to keep your spending up to date.' : 'Link a bank account to bring in your spending automatically.'}
            </p>
            <IonButton expand="block" disabled={busy} onClick={() => create.mutate(90)}>
              {create.isPending ? <IonSpinner name="dots" /> : 'Link a bank account'}
            </IonButton>
            <p className="muted" style={{ fontSize: 13, marginBottom: 0 }}>
              Covers the last 90 days of transactions.
            </p>
          </div>
        )}

        {consents.isError && (
          <div className="card">
            <IonText color="danger">Could not load your bank connections.</IonText>
          </div>
        )}
        {failure && (
          <IonText color="danger">
            <p role="alert" className="ion-padding-horizontal">
              {failure}
            </p>
          </IonText>
        )}

        <IonAlert
          isOpen={confirmStop}
          header={pending ? 'Cancel this request?' : 'Stop sharing?'}
          message={
            pending
              ? 'Nothing has been shared yet.'
              : 'No more transactions will be fetched. Expenses already added to your spending stay.'
          }
          onDidDismiss={() => setConfirmStop(false)}
          buttons={[
            { text: 'Keep it', role: 'cancel' },
            {
              text: pending ? 'Cancel request' : 'Stop sharing',
              role: 'destructive',
              handler: () => {
                if (latest) revoke.mutate(latest.id)
              },
            },
          ]}
        />
      </IonContent>
    </IonPage>
  )
}
