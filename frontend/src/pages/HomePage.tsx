import { IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonPage, IonTitle, IonToolbar } from '@ionic/react'
import { personCircleOutline, sparklesOutline } from 'ionicons/icons'
import { Link } from 'react-router-dom'
import { formatMoney } from '../features/expenses/money'
import { toMonthKey } from '../features/expenses/month'
import { useSummary } from '../features/expenses/useExpenses'
import { daysUntil, isExpiringSoon } from '../features/pantry/expiry'
import { useOpenTasks } from '../features/tasks/useTasks'
import { usePantry } from '../features/pantry/usePantry'
import { useProfile } from '../features/profile/useProfile'
import { useFavorites, useThemes } from '../features/recipes/useRecipes'
import { useSelectedTheme } from '../features/recipes/useSelectedTheme'

function greeting(hour = new Date().getHours()) {
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function HomePage() {
  const profile = useProfile()
  const themes = useThemes()
  const pantry = usePantry()
  const favorites = useFavorites()
  const [themeId, selectTheme] = useSelectedTheme()

  const summary = useSummary(toMonthKey())
  const overBudget = summary.data?.byCategory.filter((c) => c.status === 'OVER').length ?? 0
  const nearBudget = summary.data?.byCategory.filter((c) => c.status === 'NEAR').length ?? 0

  const tasks = useOpenTasks()
  const overdueTasks = tasks.data?.filter((t) => t.dueDate && daysUntil(t.dueDate) < 0).length ?? 0
  const dueToday = tasks.data?.filter((t) => t.dueDate && daysUntil(t.dueDate) === 0).length ?? 0

  const name = profile.data?.displayName?.split(' ')[0]
  const expiring = pantry.data?.filter((p) => isExpiringSoon(p.expiryDate)).length ?? 0

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>Daily Assistant</IonTitle>
          <IonButtons slot="end">
            <IonButton routerLink="/profile" aria-label="Profile and settings">
              <IonIcon slot="icon-only" icon={personCircleOutline} />
            </IonButton>
          </IonButtons>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        <section className="hero">
          <h1>
            {greeting()}
            {name ? `, ${name}` : ''}
          </h1>
          <p>What kind of day is it? Pick a theme and we'll cook around it.</p>
          <div className="chip-row" role="listbox" aria-label="Day theme">
            {themes.data?.map((theme) => (
              <button
                key={theme.id}
                type="button"
                role="option"
                aria-selected={theme.id === themeId}
                className={`theme-chip${theme.id === themeId ? ' selected' : ''}`}
                onClick={() => selectTheme(theme.id === themeId ? null : theme.id)}
              >
                {theme.label}
              </button>
            ))}
          </div>
          <IonButton expand="block" color="light" style={{ marginTop: 16 }} disabled>
            <IonIcon slot="start" icon={sparklesOutline} />
            Suggestions coming soon
          </IonButton>
        </section>

        <div className="stat-grid">
          <Link className="stat" to="/pantry">
            <div className="value">{pantry.data?.length ?? '–'}</div>
            <div className="label">In pantry</div>
          </Link>
          <Link className="stat" to="/pantry">
            <div className="value" style={expiring > 0 ? { color: 'var(--ion-color-warning-shade)' } : undefined}>
              {pantry.data ? expiring : '–'}
            </div>
            <div className="label">Expiring soon</div>
          </Link>
          <Link className="stat" to="/recipes">
            <div className="value">{favorites.data?.totalElements ?? '–'}</div>
            <div className="label">Favorites</div>
          </Link>
        </div>

        <div className="section-title">To do</div>
        <Link className="card" to="/tasks" style={{ display: 'block', color: 'inherit', textDecoration: 'none' }}>
          {tasks.data ? (
            tasks.data.length === 0 ? (
              <span className="muted">Nothing on your list. Nice.</span>
            ) : (
              <>
                <strong>
                  {tasks.data.length} open {tasks.data.length === 1 ? 'task' : 'tasks'}
                </strong>
                {overdueTasks > 0 && (
                  <span className="pill danger" style={{ marginLeft: 8 }}>
                    {overdueTasks} overdue
                  </span>
                )}
                {overdueTasks === 0 && dueToday > 0 && (
                  <span className="pill warn" style={{ marginLeft: 8 }}>
                    {dueToday} due today
                  </span>
                )}
              </>
            )
          ) : (
            <span className="muted">Loading…</span>
          )}
        </Link>

        <div className="section-title">This month</div>
        <Link className="card" to="/expenses" style={{ display: 'block', color: 'inherit', textDecoration: 'none' }}>
          <div className="muted">Spent so far</div>
          <div className="amount" style={{ fontSize: 28, fontWeight: 800 }}>
            {summary.data ? formatMoney(summary.data.total) : '–'}
          </div>
          {overBudget > 0 && (
            <span className="pill danger" style={{ marginTop: 8 }}>
              {overBudget} {overBudget === 1 ? 'category' : 'categories'} over budget
            </span>
          )}
          {overBudget === 0 && nearBudget > 0 && (
            <span className="pill warn" style={{ marginTop: 8 }}>
              {nearBudget} near the limit
            </span>
          )}
        </Link>
      </IonContent>
    </IonPage>
  )
}
