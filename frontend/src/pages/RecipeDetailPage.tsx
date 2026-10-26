import {
  IonBackButton,
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { heart, heartOutline, trashOutline } from 'ionicons/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { useDeleteRecipe, useRecipe, useToggleFavorite } from '../features/recipes/useRecipes'

export default function RecipeDetailPage() {
  const { id } = useParams()
  const recipeId = Number(id)
  const navigate = useNavigate()
  const recipe = useRecipe(recipeId)
  const toggle = useToggleFavorite()
  const remove = useDeleteRecipe()
  const data = recipe.data

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/recipes" />
          </IonButtons>
          <IonTitle>{data?.title ?? 'Recipe'}</IonTitle>
          {data && (
            <IonButtons slot="end">
              <IonButton
                aria-label={data.favorite ? 'Remove from favorites' : 'Save to favorites'}
                onClick={() => toggle.mutate({ id: data.id, favorite: data.favorite })}
              >
                <IonIcon slot="icon-only" icon={data.favorite ? heart : heartOutline} color="danger" />
              </IonButton>
              <IonButton
                aria-label="Delete recipe"
                color="danger"
                onClick={() => remove.mutate(data.id, { onSuccess: () => navigate('/recipes', { replace: true }) })}
              >
                <IonIcon slot="icon-only" icon={trashOutline} />
              </IonButton>
            </IonButtons>
          )}
        </IonToolbar>
      </IonHeader>
      <IonContent>
        {recipe.isError && (
          <IonText color="danger">
            <p className="ion-padding">Recipe not found.</p>
          </IonText>
        )}
        {data && (
          <>
            <div className="ion-padding">
              {data.description && <p>{data.description}</p>}
              <p>
                {[
                  data.cookTimeMinutes ? `${data.cookTimeMinutes} min` : null,
                  data.servings ? `serves ${data.servings}` : null,
                  data.cuisine,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
            </div>
            <IonList>
              <IonListHeader>
                <IonLabel>Ingredients</IonLabel>
              </IonListHeader>
              {data.ingredients.map((ing) => (
                <IonItem key={ing.name}>
                  <IonLabel>
                    {ing.name}
                    {(ing.quantity || ing.unit) && (
                      <p>
                        {ing.quantity ?? ''} {ing.unit ?? ''}
                      </p>
                    )}
                  </IonLabel>
                  {ing.in_pantry === false && (
                    <IonBadge slot="end" color="warning">
                      missing
                    </IonBadge>
                  )}
                </IonItem>
              ))}
              {data.missingIngredients.length > 0 && (
                <IonItem lines="none">
                  <IonLabel className="ion-text-wrap" color="medium">
                    To buy: {data.missingIngredients.join(', ')}
                  </IonLabel>
                </IonItem>
              )}
            </IonList>
            <IonList>
              <IonListHeader>
                <IonLabel>Steps</IonLabel>
              </IonListHeader>
              {data.steps.map((step, index) => (
                <IonItem key={index}>
                  <IonLabel className="ion-text-wrap">
                    {index + 1}. {step}
                  </IonLabel>
                </IonItem>
              ))}
            </IonList>
          </>
        )}
      </IonContent>
    </IonPage>
  )
}
