import { IonButton, IonContent, IonHeader, IonIcon, IonPage, IonTitle, IonToolbar } from '@ionic/react'
import { sparklesOutline } from 'ionicons/icons'
import { Link } from 'react-router-dom'
import { isExpiringSoon } from '../features/pantry/expiry'
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

  const name = profile.data?.displayName?.split(' ')[0]
  const expiring = pantry.data?.filter((p) => isExpiringSoon(p.expiryDate)).length ?? 0

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>Daily Assistant</IonTitle>
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
      </IonContent>
    </IonPage>
  )
}
