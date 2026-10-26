import { useState } from 'react'
import {
  IonBackButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { heart } from 'ionicons/icons'
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
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/home" />
          </IonButtons>
          <IonTitle>Recipes</IonTitle>
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
      <IonContent>
        {query.isError && (
          <IonText color="danger">
            <p className="ion-padding">Could not load recipes.</p>
          </IonText>
        )}
        {query.isSuccess && recipes.length === 0 && (
          <IonText color="medium">
            <p className="ion-padding">
              {tab === 'history' ? 'No recipes yet. AI suggestions are coming soon.' : 'No favorites yet.'}
            </p>
          </IonText>
        )}
        <IonList>
          {recipes.map((recipe) => (
            <IonItem key={recipe.id} routerLink={`/recipes/${recipe.id}`} detail>
              <IonLabel>
                {recipe.title}
                <p>
                  {[recipe.cookTimeMinutes ? `${recipe.cookTimeMinutes} min` : null, recipe.cuisine]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
              </IonLabel>
              {recipe.favorite && <IonIcon slot="end" icon={heart} color="danger" aria-label="Favorite" />}
            </IonItem>
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  )
}
