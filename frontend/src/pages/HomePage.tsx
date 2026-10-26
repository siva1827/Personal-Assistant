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
        <IonButton expand="block" routerLink="/pantry">
          My pantry
        </IonButton>
        <p>Day themes and AI recipe suggestions arrive next.</p>
        <IonButton fill="outline" onClick={logout}>
          Sign out
        </IonButton>
      </IonContent>
    </IonPage>
  )
}
