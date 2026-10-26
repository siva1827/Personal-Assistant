import { useState } from 'react'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import type { DietaryPreference, UserProfile } from '../api/types'
import { parseAllergies, useProfile, useUpdateProfile } from '../features/profile/useProfile'

const DIETS: { value: DietaryPreference; label: string }[] = [
  { value: 'ANY', label: 'No preference' },
  { value: 'VEG', label: 'Vegetarian' },
  { value: 'EGGETARIAN', label: 'Eggetarian' },
  { value: 'NON_VEG', label: 'Non-vegetarian' },
  { value: 'VEGAN', label: 'Vegan' },
]

function ProfileForm({ profile }: { profile: UserProfile }) {
  const update = useUpdateProfile()
  const [displayName, setDisplayName] = useState(profile.displayName ?? '')
  const [diet, setDiet] = useState<DietaryPreference>(profile.dietaryPreference)
  const [allergies, setAllergies] = useState(profile.allergies.join(', '))

  const save = () =>
    update.mutate({ displayName: displayName.trim(), dietaryPreference: diet, allergies: parseAllergies(allergies) })

  return (
    <>
      <p>{profile.email}</p>
      <IonInput label="Name" value={displayName} onIonInput={(e) => setDisplayName(String(e.detail.value ?? ''))} />
      <IonSelect label="Diet" value={diet} onIonChange={(e) => setDiet(e.detail.value as DietaryPreference)}>
        {DIETS.map((d) => (
          <IonSelectOption key={d.value} value={d.value}>
            {d.label}
          </IonSelectOption>
        ))}
      </IonSelect>
      <IonInput
        label="Allergies"
        labelPlacement="stacked"
        placeholder="peanut, shellfish"
        helperText="Comma separated. Recipes containing these are filtered out."
        value={allergies}
        onIonInput={(e) => setAllergies(String(e.detail.value ?? ''))}
      />
      <IonButton expand="block" onClick={save} disabled={update.isPending}>
        Save
      </IonButton>
      {update.isSuccess && <IonText color="success">Saved.</IonText>}
      {update.isError && <IonText color="danger">Could not save changes.</IonText>}
    </>
  )
}

export default function ProfilePage() {
  const profile = useProfile()

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/home" />
          </IonButtons>
          <IonTitle>Profile</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        {profile.isError && <IonText color="danger">Could not load your profile.</IonText>}
        {profile.data && <ProfileForm profile={profile.data} />}
      </IonContent>
    </IonPage>
  )
}
