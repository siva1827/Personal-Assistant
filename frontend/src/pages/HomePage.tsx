import { IonButton, IonContent, IonHeader, IonPage, IonTitle, IonToolbar } from '@ionic/react'
import { useAuth } from '../auth/AuthContext'

export default function HomePage() {
  const { logout } = useAuth()
  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Daily Assistant</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <p>Pantry, themes and recipe suggestions arrive in Phase 1.</p>
        <IonButton onClick={logout}>Sign out</IonButton>
      </IonContent>
    </IonPage>
  )
}
