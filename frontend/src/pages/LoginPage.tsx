import { useState } from 'react'
import { IonButton, IonContent, IonHeader, IonInput, IonPage, IonText, IonTitle, IonToolbar } from '@ionic/react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = async () => {
    try {
      await login(email, password)
      navigate('/home', { replace: true })
    } catch {
      setError('Invalid email or password')
    }
  }

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Sign in</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <IonInput label="Email" type="email" value={email} onIonInput={(e) => setEmail(String(e.detail.value ?? ''))} />
        <IonInput label="Password" type="password" value={password} onIonInput={(e) => setPassword(String(e.detail.value ?? ''))} />
        {error && <IonText color="danger">{error}</IonText>}
        <IonButton expand="block" onClick={submit}>Sign in</IonButton>
        <IonButton expand="block" fill="clear" routerLink="/register">Create account</IonButton>
      </IonContent>
    </IonPage>
  )
}
