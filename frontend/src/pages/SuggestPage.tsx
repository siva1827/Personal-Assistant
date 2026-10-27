import { useState } from 'react'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { isAxiosError } from 'axios'
import { heart, refreshOutline, sparklesOutline, timeOutline } from 'ionicons/icons'
import EmptyState from '../components/EmptyState'
import { describeSuggestError } from '../features/recipes/suggestErrors'
import { useSuggestRecipes, useThemes } from '../features/recipes/useRecipes'
import { useSelectedTheme } from '../features/recipes/useSelectedTheme'

export default function SuggestPage() {
  const themes = useThemes()
  const [themeId, selectTheme] = useSelectedTheme()
  const [note, setNote] = useState('')
  const [count, setCount] = useState(3)
  const suggest = useSuggestRecipes()

  const run = (fresh: boolean) =>
    suggest.mutate({
      themeId: themeId ?? undefined,
      customNote: note.trim() || undefined,
      count,
      fresh,
    })

  const problem = suggest.isError
    ? describeSuggestError(
        isAxiosError(suggest.error) ? suggest.error.response?.status : undefined,
        isAxiosError(suggest.error) ? (suggest.error.response?.data as { message?: string } | undefined)?.message : undefined,
      )
    : null
  const result = suggest.data

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/recipes" />
          </IonButtons>
          <IonTitle>Recipe ideas</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        <div className="card">
          <div className="section-title" style={{ margin: '0 0 8px' }}>
            Today's theme
          </div>
          <div className="cat-grid" role="radiogroup" aria-label="Day theme" style={{ marginTop: 0 }}>
            {themes.data?.map((theme) => (
              <button
                key={theme.id}
                type="button"
                role="radio"
                aria-checked={theme.id === themeId}
                className={`cat-chip${theme.id === themeId ? ' selected' : ''}`}
                onClick={() => selectTheme(theme.id === themeId ? null : theme.id)}
              >
                {theme.label}
              </button>
            ))}
          </div>

          <IonInput
            label="Anything else? (optional)"
            labelPlacement="stacked"
            fill="outline"
            placeholder="e.g. something light, I am tired"
            maxlength={200}
            value={note}
            onIonInput={(e) => setNote(String(e.detail.value ?? ''))}
          />

          <div className="section-title" style={{ margin: '16px 0 8px' }}>
            How many ideas?
          </div>
          <IonSegment value={count} onIonChange={(e) => setCount(Number(e.detail.value))}>
            {[2, 3, 4, 5].map((n) => (
              <IonSegmentButton key={n} value={n}>
                <IonLabel>{n}</IonLabel>
              </IonSegmentButton>
            ))}
          </IonSegment>

          <IonButton expand="block" style={{ marginTop: 16 }} disabled={suggest.isPending} onClick={() => run(false)}>
            {suggest.isPending ? (
              <IonSpinner name="dots" />
            ) : (
              <>
                <IonIcon slot="start" icon={sparklesOutline} />
                {result ? 'Suggest again' : 'Suggest recipes'}
              </>
            )}
          </IonButton>
          {suggest.isPending && (
            <p className="muted ion-text-center" role="status" style={{ marginBottom: 0 }}>
              Cooking up ideas from your pantry. This can take up to 20 seconds.
            </p>
          )}
        </div>

        {problem && (
          <div className="card">
            <IonText color="danger">
              <p role="alert" style={{ marginTop: 0 }}>
                {problem.message}
              </p>
            </IonText>
            {problem.action === 'pantry' && (
              <IonButton size="small" routerLink="/pantry">
                Go to my pantry
              </IonButton>
            )}
            {problem.action === 'retry' && (
              <IonButton size="small" fill="outline" onClick={() => run(true)}>
                Try again
              </IonButton>
            )}
          </div>
        )}

        {result && result.model === 'demo' && (
          <div className="card">
            <strong>Demo mode</strong>
            <p className="muted" style={{ margin: '4px 0 0' }}>
              These recipes are made up for testing. Add an AI API key on the server for real suggestions.
            </p>
          </div>
        )}

        {result && result.cached && (
          <div className="card">
            <p style={{ margin: '0 0 10px' }}>These are your recent suggestions for the same pantry and theme.</p>
            <IonButton size="small" fill="outline" disabled={suggest.isPending} onClick={() => run(true)}>
              <IonIcon slot="start" icon={refreshOutline} />
              Get fresh ideas
            </IonButton>
          </div>
        )}

        {result && result.filteredOut > 0 && (
          <div className="card">
            <p style={{ margin: 0 }}>
              {result.filteredOut === 1 ? 'One suggestion was' : `${result.filteredOut} suggestions were`} removed
              because {result.filteredOut === 1 ? 'it clashed' : 'they clashed'} with your allergies or diet.
            </p>
          </div>
        )}

        {result && result.recipes.length === 0 && (
          <EmptyState emoji="🥣" title="Nothing safe to suggest this time">
            Everything the AI came up with clashed with your allergies or diet. Try again for different ideas.
          </EmptyState>
        )}

        {result && result.recipes.length > 0 && (
          <>
            <div className="section-title">
              {result.recipes.length} {result.recipes.length === 1 ? 'idea' : 'ideas'} from your pantry
            </div>
            <div className="row-list">
              {result.recipes.map((recipe) => (
                <IonItem key={recipe.id} routerLink={`/recipes/${recipe.id}`} lines="full" detail={false}>
                  <span className="avatar">🍲</span>
                  <IonLabel className="ion-text-wrap">
                    <h3>{recipe.title}</h3>
                    <p>
                      {recipe.cookTimeMinutes && (
                        <>
                          <IonIcon icon={timeOutline} style={{ verticalAlign: '-2px' }} /> {recipe.cookTimeMinutes} min
                        </>
                      )}
                      {recipe.cookTimeMinutes && recipe.cuisine ? ' · ' : ''}
                      {recipe.cuisine}
                    </p>
                  </IonLabel>
                  <span slot="end" className={`pill${recipe.missingIngredients.length > 0 ? ' warn' : ''}`}>
                    {recipe.missingIngredients.length === 0 ? 'all in pantry' : `${recipe.missingIngredients.length} to buy`}
                  </span>
                  {recipe.favorite && <IonIcon slot="end" icon={heart} color="danger" aria-label="Favorite" />}
                </IonItem>
              ))}
            </div>
          </>
        )}
      </IonContent>
    </IonPage>
  )
}
