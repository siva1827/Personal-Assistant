import { useState } from 'react'
import { IonButton, IonInput, IonSpinner, IonText } from '@ionic/react'
import AuthLayout from '../components/AuthLayout'
import { useAuth } from '../auth/useAuth'

export default function LoginPage() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
    } catch {
      setError('Invalid email or password')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to your daily assistant">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          void submit()
        }}
      >
        <IonInput
          label="Email"
          labelPlacement="stacked"
          fill="outline"
          type="email"
          autocomplete="email"
          value={email}
          onIonInput={(e) => setEmail(String(e.detail.value ?? ''))}
        />
        <IonInput
          label="Password"
          labelPlacement="stacked"
          fill="outline"
          type="password"
          autocomplete="current-password"
          style={{ marginTop: 14 }}
          value={password}
          onIonInput={(e) => setPassword(String(e.detail.value ?? ''))}
        />
        {error && (
          <IonText color="danger">
            <p role="alert" style={{ margin: '10px 2px 0' }}>{error}</p>
          </IonText>
        )}
        <IonButton expand="block" type="submit" style={{ marginTop: 18 }} disabled={busy || !email || !password}>
          {busy ? <IonSpinner name="dots" /> : 'Sign in'}
        </IonButton>
        <IonButton expand="block" fill="clear" routerLink="/register">
          New here? Create an account
        </IonButton>
      </form>
    </AuthLayout>
  )
}
