import { useState } from 'react'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { heart, sparklesOutline, timeOutline } from 'ionicons/icons'
import EmptyState from '../components/EmptyState'
import ListSkeleton from '../components/ListSkeleton'
import { useFavorites, useHistory } from '../features/recipes/useRecipes'

type Tab = 'history' | 'favorites'

export default function RecipesPage() {
  const [tab, setTab] = useState<Tab>('history')
  const history = useHistory()
  const favorites = useFavorites()
  const query = tab === 'history' ? history : favorites
  const recipes = query.data?.content ?? []

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>Recipes</IonTitle>
          <IonButtons slot="end">
            <IonButton routerLink="/recipes/suggest" aria-label="Get recipe ideas">
              <IonIcon slot="start" icon={sparklesOutline} />
              Ideas
            </IonButton>
          </IonButtons>
        </IonToolbar>
        <IonToolbar>
          <IonSegment value={tab} onIonChange={(e) => setTab(e.detail.value as Tab)}>
            <IonSegmentButton value="history">
              <IonLabel>History</IonLabel>
            </IonSegmentButton>
            <IonSegmentButton value="favorites">
              <IonLabel>Favorites</IonLabel>
            </IonSegmentButton>
          </IonSegment>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        {query.isLoading && <ListSkeleton />}
        {query.isError && (
          <div className="card">
            <IonText color="danger">Could not load recipes.</IonText>
          </div>
        )}
        {query.isSuccess && recipes.length === 0 && tab === 'history' && (
          <EmptyState emoji="🍳" title="No recipes yet">
            Tap Ideas to get recipes from what's in your pantry. Everything you get is kept here.
          </EmptyState>
        )}
        {query.isSuccess && recipes.length === 0 && tab === 'favorites' && (
          <EmptyState emoji="💚" title="No favorites yet">
            Tap the heart on a recipe to keep it here.
          </EmptyState>
        )}
        {recipes.length > 0 && (
          <div className="row-list">
            {recipes.map((recipe) => (
              <IonItem key={recipe.id} routerLink={`/recipes/${recipe.id}`} lines="full" detail={false}>
                <span className="avatar">🍲</span>
                <IonLabel>
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
                {recipe.missingIngredients.length > 0 && (
                  <span slot="end" className="pill warn">
                    {recipe.missingIngredients.length} to buy
                  </span>
                )}
                {recipe.favorite && <IonIcon slot="end" icon={heart} color="danger" aria-label="Favorite" />}
              </IonItem>
            ))}
          </div>
        )}
      </IonContent>
    </IonPage>
  )
}
