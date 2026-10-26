import { IonButton, IonChip, IonContent, IonHeader, IonLabel, IonPage, IonTitle, IonToolbar } from '@ionic/react'
import { useAuth } from '../auth/useAuth'
import { useThemes } from '../features/recipes/useRecipes'

export default function HomePage() {
  const { logout } = useAuth()
  const themes = useThemes()

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Daily Assistant</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <h3>Today's theme</h3>
        <div>
          {themes.data?.map((theme) => (
            <IonChip key={theme.id}>
              <IonLabel>{theme.label}</IonLabel>
            </IonChip>
          ))}
        </div>
        <p>AI recipe suggestions based on your pantry and theme are coming soon.</p>

        <IonButton expand="block" routerLink="/pantry">
          My pantry
        </IonButton>
        <IonButton expand="block" routerLink="/recipes">
          Recipes and favorites
        </IonButton>
        <IonButton expand="block" fill="outline" routerLink="/profile">
          Profile
        </IonButton>
        <IonButton expand="block" fill="clear" onClick={logout}>
          Sign out
        </IonButton>
      </IonContent>
    </IonPage>
  )
}
