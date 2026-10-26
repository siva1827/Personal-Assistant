import { useState } from 'react'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonPage,
  IonText,
  IonTitle,
  IonToast,
  IonToolbar,
} from '@ionic/react'
import { cartOutline, heart, heartOutline, peopleOutline, timeOutline, trashOutline } from 'ionicons/icons'
import { useNavigate, useParams } from 'react-router-dom'
import ListSkeleton from '../components/ListSkeleton'
import { useAddMissingFromRecipe } from '../features/shopping/useShopping'
import { useDeleteRecipe, useRecipe, useToggleFavorite } from '../features/recipes/useRecipes'

export default function RecipeDetailPage() {
  const { id } = useParams()
  const recipeId = Number(id)
  const navigate = useNavigate()
  const recipe = useRecipe(recipeId)
  const toggle = useToggleFavorite()
  const remove = useDeleteRecipe()
  const addMissing = useAddMissingFromRecipe()
  const [toast, setToast] = useState(false)
  const data = recipe.data
  const added = addMissing.data

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
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
      <IonContent className="app-content">
        {recipe.isLoading && <ListSkeleton rows={5} />}
        {recipe.isError && (
          <div className="card">
            <IonText color="danger">Recipe not found.</IonText>
          </div>
        )}
        {data && (
          <>
            <div className="card">
              {data.description && <p style={{ marginTop: 0 }}>{data.description}</p>}
              <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap' }} className="muted">
                {data.cookTimeMinutes && (
                  <span>
                    <IonIcon icon={timeOutline} style={{ verticalAlign: '-2px' }} /> {data.cookTimeMinutes} min
                  </span>
                )}
                {data.servings && (
                  <span>
                    <IonIcon icon={peopleOutline} style={{ verticalAlign: '-2px' }} /> serves {data.servings}
                  </span>
                )}
                {data.cuisine && <span>{data.cuisine}</span>}
              </div>
            </div>

            {data.missingIngredients.length > 0 && (
              <div className="card">
                <strong>
                  {data.missingIngredients.length} missing {data.missingIngredients.length === 1 ? 'ingredient' : 'ingredients'}
                </strong>
                <p className="muted" style={{ margin: '4px 0 10px' }}>
                  {data.missingIngredients.join(', ')}
                </p>
                <IonButton
                  expand="block"
                  size="small"
                  disabled={addMissing.isPending}
                  onClick={() => addMissing.mutate(data.id, { onSuccess: () => setToast(true) })}
                >
                  <IonIcon slot="start" icon={cartOutline} />
                  Add to shopping list
                </IonButton>
                {addMissing.isError && (
                  <IonText color="danger">
                    <p role="alert" style={{ margin: '8px 0 0' }}>Could not update your shopping list.</p>
                  </IonText>
                )}
              </div>
            )}

            <div className="section-title">Ingredients</div>
            <div className="row-list">
              {data.ingredients.map((ing) => (
                <IonItem key={ing.name} lines="full">
                  <IonLabel>
                    {ing.name}
                    {(ing.quantity || ing.unit) && (
                      <p>{[ing.quantity, ing.unit].filter(Boolean).join(' ')}</p>
                    )}
                  </IonLabel>
                  {ing.in_pantry === false && (
                    <span slot="end" className="pill warn">
                      need to buy
                    </span>
                  )}
                  {ing.in_pantry === true && (
                    <span slot="end" className="pill">
                      in pantry
                    </span>
                  )}
                </IonItem>
              ))}
            </div>

            <div className="section-title">Steps</div>
            <div className="row-list">
              {data.steps.map((step, index) => (
                <IonItem key={index} lines="full">
                  <span className="avatar" style={{ fontSize: 16, fontWeight: 800 }}>
                    {index + 1}
                  </span>
                  <IonLabel className="ion-text-wrap">{step}</IonLabel>
                </IonItem>
              ))}
            </div>
          </>
        )}
        <IonToast
          isOpen={toast}
          duration={2500}
          position="top"
          message={
            added
              ? added.added.length === 0
                ? 'Everything is already on your shopping list'
                : `Added ${added.added.length} to your shopping list${added.skipped > 0 ? ` (${added.skipped} already there)` : ''}`
              : ''
          }
          onDidDismiss={() => setToast(false)}
        />
      </IonContent>
    </IonPage>
  )
}
