import { useState } from 'react'
import { IonButton, IonContent, IonHeader, IonInput, IonPage, IonText, IonTitle, IonToolbar } from '@ionic/react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = async () => {
    try {
      await register(email, password, displayName)
      navigate('/home', { replace: true })
    } catch {
      setError('Could not create account')
    }
  }

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Create account</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <IonInput label="Name" value={displayName} onIonInput={(e) => setDisplayName(String(e.detail.value ?? ''))} />
        <IonInput label="Email" type="email" value={email} onIonInput={(e) => setEmail(String(e.detail.value ?? ''))} />
        <IonInput label="Password" type="password" value={password} onIonInput={(e) => setPassword(String(e.detail.value ?? ''))} />
        {error && <IonText color="danger">{error}</IonText>}
        <IonButton expand="block" onClick={submit}>Register</IonButton>
      </IonContent>
    </IonPage>
  )
}
