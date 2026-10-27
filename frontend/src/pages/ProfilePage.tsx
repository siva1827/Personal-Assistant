import { useState } from 'react'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonText,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/react'
import type { DietaryPreference, UserProfile } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { alertsEnabled, alertsSupported, setAlertsEnabled } from '../features/notifications/deviceAlerts'
import { useNotificationFeed } from '../features/notifications/useNotifications'
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
    <div className="card">
      <p className="muted" style={{ marginTop: 0 }}>
        {profile.email}
      </p>
      <IonInput
        label="Name"
        labelPlacement="stacked"
        fill="outline"
        value={displayName}
        onIonInput={(e) => setDisplayName(String(e.detail.value ?? ''))}
      />
      <IonSelect
        label="Diet"
        labelPlacement="stacked"
        fill="outline"
        style={{ marginTop: 14 }}
        value={diet}
        onIonChange={(e) => setDiet(e.detail.value as DietaryPreference)}
      >
        {DIETS.map((d) => (
          <IonSelectOption key={d.value} value={d.value}>
            {d.label}
          </IonSelectOption>
        ))}
      </IonSelect>
      <IonInput
        label="Allergies"
        labelPlacement="stacked"
        fill="outline"
        placeholder="peanut, shellfish"
        helperText="Comma separated. Recipes containing these are filtered out."
        style={{ marginTop: 14 }}
        value={allergies}
        onIonInput={(e) => setAllergies(String(e.detail.value ?? ''))}
      />
      <IonButton expand="block" style={{ marginTop: 16 }} onClick={save} disabled={update.isPending}>
        Save changes
      </IonButton>
      {update.isSuccess && (
        <IonText color="success">
          <p role="status" className="ion-text-center">Saved</p>
        </IonText>
      )}
      {update.isError && (
        <IonText color="danger">
          <p role="alert" className="ion-text-center">Could not save changes</p>
        </IonText>
      )}
    </div>
  )
}

function DeviceAlertsCard() {
  const feed = useNotificationFeed()
  const [on, setOn] = useState(alertsEnabled())
  const [denied, setDenied] = useState(false)

  if (!alertsSupported()) {
    return (
      <div className="card">
        <strong>Device alerts</strong>
        <p className="muted" style={{ marginBottom: 0 }}>
          This browser does not support system notifications. Reminders still appear under the bell.
        </p>
      </div>
    )
  }

  const toggle = async (next: boolean) => {
    const ok = await setAlertsEnabled(next, feed.data?.items)
    setDenied(next && !ok)
    setOn(next && ok)
  }

  return (
    <div className="card">
      <IonItem lines="none" style={{ '--padding-start': '0px', '--inner-padding-end': '0px' }}>
        <IonLabel className="ion-text-wrap">
          <h3>Device alerts</h3>
          <p>Show a system notification for new reminders while the app is open.</p>
        </IonLabel>
        <IonToggle
          slot="end"
          aria-label="Device alerts"
          checked={on}
          onIonChange={(e) => void toggle(e.detail.checked)}
        />
      </IonItem>
      {denied && (
        <IonText color="danger">
          <p role="alert" style={{ margin: '8px 0 0' }}>
            Notifications are blocked for this site. Allow them in your browser settings, then try again.
          </p>
        </IonText>
      )}
    </div>
  )
}

export default function ProfilePage() {
  const profile = useProfile()
  const { logout } = useAuth()

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/home" />
          </IonButtons>
          <IonTitle>Profile</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        {profile.isError && (
          <div className="card">
            <IonText color="danger">Could not load your profile.</IonText>
          </div>
        )}
        {profile.data && <ProfileForm profile={profile.data} />}
        <DeviceAlertsCard />
        <IonButton expand="block" fill="outline" color="danger" onClick={logout}>
          Sign out
        </IonButton>
      </IonContent>
    </IonPage>
  )
}
