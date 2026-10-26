import { useState } from 'react'
import {
  IonAlert,
  IonButton,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonPage,
  IonSearchbar,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { addCircleOutline, trashOutline } from 'ionicons/icons'
import type { PantryItem } from '../api/types'
import { categoryEmoji } from '../components/categoryEmoji'
import EmptyState from '../components/EmptyState'
import ListSkeleton from '../components/ListSkeleton'
import { daysUntil, isExpiringSoon } from '../features/pantry/expiry'
import {
  useAddPantryItem,
  useDeletePantryItem,
  useIngredientSearch,
  usePantry,
  useUpdatePantryItem,
} from '../features/pantry/usePantry'

function expiryLabel(isoDate: string): string {
  const days = daysUntil(isoDate)
  if (days < 0) return 'expired'
  if (days === 0) return 'expires today'
  return `${days}d left`
}

export default function PantryPage() {
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<PantryItem | null>(null)
  const pantry = usePantry()
  const update = useUpdatePantryItem()
  const search = useIngredientSearch(query)
  const add = useAddPantryItem()
  const remove = useDeletePantryItem()

  const inPantry = new Set(pantry.data?.map((p) => p.ingredientId))
  const suggestions = (search.data ?? []).filter((i) => !inPantry.has(i.id))
  const searching = query.trim().length > 0

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonTitle>My pantry</IonTitle>
        </IonToolbar>
        <IonToolbar>
          <IonSearchbar
            placeholder="Search ingredients to add"
            value={query}
            debounce={250}
            onIonInput={(e) => setQuery(e.detail.value ?? '')}
          />
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        {searching && (
          <>
            <div className="section-title">Add to pantry</div>
            {search.isSuccess && suggestions.length === 0 && (
              <div className="card muted">No new ingredients match "{query.trim()}".</div>
            )}
            {suggestions.length > 0 && (
              <div className="row-list">
                {suggestions.map((ingredient) => (
                  <IonItem key={ingredient.id} lines="full" button detail={false}
                    onClick={() => add.mutate({ ingredientId: ingredient.id }, { onSuccess: () => setQuery('') })}>
                    <span className="avatar">{categoryEmoji(ingredient.category)}</span>
                    <IonLabel>
                      {ingredient.name}
                      {ingredient.category && <p>{ingredient.category}</p>}
                    </IonLabel>
                    <IonIcon slot="end" icon={addCircleOutline} color="primary" aria-label={`Add ${ingredient.name}`} />
                  </IonItem>
                ))}
              </div>
            )}
            {add.isError && <IonText color="danger"><p>Could not add that ingredient.</p></IonText>}
          </>
        )}

        <div className="section-title">In your pantry ({pantry.data?.length ?? 0})</div>
        {pantry.isLoading && <ListSkeleton />}
        {pantry.isError && <div className="card"><IonText color="danger">Could not load your pantry.</IonText></div>}
        {pantry.data?.length === 0 && (
          <EmptyState emoji="🧺" title="Your pantry is empty">
            Search above and tap an ingredient to add it. The more you add, the better your recipes.
          </EmptyState>
        )}
        {pantry.data && pantry.data.length > 0 && (
          <div className="row-list">
            {pantry.data.map((item) => (
              <IonItem key={item.id} lines="full" button detail={false} onClick={() => setEditing(item)}>
                <span className="avatar">{categoryEmoji(item.category)}</span>
                <IonLabel>
                  {item.ingredientName}
                  {(item.quantity !== null || item.unit) && (
                    <p>{[item.quantity, item.unit].filter((v) => v !== null && v !== '').join(' ')}</p>
                  )}
                </IonLabel>
                {item.expiryDate && (
                  <span slot="end" className={`pill${isExpiringSoon(item.expiryDate) ? ' warn' : ''}`}>
                    {expiryLabel(item.expiryDate)}
                  </span>
                )}
                <IonButton
                  slot="end"
                  fill="clear"
                  color="danger"
                  aria-label={`Remove ${item.ingredientName}`}
                  onClick={(e) => {
                    e.stopPropagation()
                    remove.mutate(item.id)
                  }}
                >
                  <IonIcon slot="icon-only" icon={trashOutline} />
                </IonButton>
              </IonItem>
            ))}
          </div>
        )}

        <IonAlert
          isOpen={editing !== null}
          header={editing ? `Edit ${editing.ingredientName}` : ''}
          onDidDismiss={() => setEditing(null)}
          inputs={[
            { name: 'quantity', type: 'number', placeholder: 'Quantity', value: editing?.quantity ?? '', min: 0 },
            { name: 'unit', type: 'text', placeholder: 'Unit (kg, pcs, ...)', value: editing?.unit ?? '' },
            { name: 'expiryDate', type: 'date', value: editing?.expiryDate ?? '' },
          ]}
          buttons={[
            { text: 'Cancel', role: 'cancel' },
            {
              text: 'Save',
              handler: (values: { quantity: string; unit: string; expiryDate: string }) => {
                if (!editing) return
                // Empty fields are left unchanged (the API treats missing as "no change")
                update.mutate({
                  id: editing.id,
                  quantity: values.quantity === '' ? undefined : Number(values.quantity),
                  unit: values.unit.trim() || undefined,
                  expiryDate: values.expiryDate || undefined,
                })
              },
            },
          ]}
        />
      </IonContent>
    </IonPage>
  )
}
