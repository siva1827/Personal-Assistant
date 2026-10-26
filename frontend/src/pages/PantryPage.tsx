import { useState } from 'react'
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
  IonSearchbar,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { addOutline, trashOutline } from 'ionicons/icons'
import { isExpiringSoon } from '../features/pantry/expiry'
import { useAddPantryItem, useDeletePantryItem, useIngredientSearch, usePantry } from '../features/pantry/usePantry'

export default function PantryPage() {
  const [query, setQuery] = useState('')
  const pantry = usePantry()
  const search = useIngredientSearch(query)
  const add = useAddPantryItem()
  const remove = useDeletePantryItem()

  const inPantry = new Set(pantry.data?.map((p) => p.ingredientId))
  const suggestions = (search.data ?? []).filter((i) => !inPantry.has(i.id))

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/home" />
          </IonButtons>
          <IonTitle>My pantry</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonSearchbar
          placeholder="Search ingredients to add"
          value={query}
          debounce={250}
          onIonInput={(e) => setQuery(e.detail.value ?? '')}
        />

        {suggestions.length > 0 && (
          <IonList>
            {suggestions.map((ingredient) => (
              <IonItem key={ingredient.id}>
                <IonLabel>
                  {ingredient.name}
                  {ingredient.category && <p>{ingredient.category}</p>}
                </IonLabel>
                <IonButton
                  slot="end"
                  fill="clear"
                  aria-label={`Add ${ingredient.name}`}
                  onClick={() => add.mutate({ ingredientId: ingredient.id }, { onSuccess: () => setQuery('') })}
                >
                  <IonIcon slot="icon-only" icon={addOutline} />
                </IonButton>
              </IonItem>
            ))}
          </IonList>
        )}

        {add.isError && (
          <IonText color="danger">
            <p className="ion-padding-horizontal">Could not add that ingredient.</p>
          </IonText>
        )}

        <IonList>
          <IonListHeader>
            <IonLabel>In your pantry ({pantry.data?.length ?? 0})</IonLabel>
          </IonListHeader>
          {pantry.isError && (
            <IonItem>
              <IonText color="danger">Could not load your pantry.</IonText>
            </IonItem>
          )}
          {pantry.data?.length === 0 && (
            <IonItem>
              <IonLabel color="medium">Nothing here yet. Search above to add ingredients.</IonLabel>
            </IonItem>
          )}
          {pantry.data?.map((item) => (
            <IonItem key={item.id}>
              <IonLabel>
                {item.ingredientName}
                {(item.quantity !== null || item.unit) && (
                  <p>
                    {item.quantity ?? ''} {item.unit ?? ''}
                  </p>
                )}
              </IonLabel>
              {isExpiringSoon(item.expiryDate) && (
                <IonBadge color="warning" slot="end">
                  expiring {item.expiryDate}
                </IonBadge>
              )}
              <IonButton
                slot="end"
                fill="clear"
                color="danger"
                aria-label={`Remove ${item.ingredientName}`}
                onClick={() => remove.mutate(item.id)}
              >
                <IonIcon slot="icon-only" icon={trashOutline} />
              </IonButton>
            </IonItem>
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  )
}
