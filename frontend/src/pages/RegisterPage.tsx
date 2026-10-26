import { useState } from 'react'
import { IonButton, IonInput, IonSpinner, IonText } from '@ionic/react'
import { isAxiosError } from 'axios'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import { useAuth } from '../auth/useAuth'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    setError(null)
    try {
      await register(email, password, displayName)
      navigate('/home', { replace: true })
    } catch (e) {
      const status = isAxiosError(e) ? e.response?.status : undefined
      setError(status === 409 ? 'That email is already registered' : 'Could not create account. Check your details.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Create your account" subtitle="Pantry, recipes and spending in one place">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          void submit()
        }}
      >
        <IonInput
          label="Name"
          labelPlacement="floating"
          fill="outline"
          autocomplete="name"
          value={displayName}
          onIonInput={(e) => setDisplayName(String(e.detail.value ?? ''))}
        />
        <IonInput
          label="Email"
          labelPlacement="floating"
          fill="outline"
          type="email"
          autocomplete="email"
          style={{ marginTop: 14 }}
          value={email}
          onIonInput={(e) => setEmail(String(e.detail.value ?? ''))}
        />
        <IonInput
          label="Password"
          labelPlacement="floating"
          fill="outline"
          type="password"
          autocomplete="new-password"
          helperText="At least 8 characters"
          style={{ marginTop: 14 }}
          value={password}
          onIonInput={(e) => setPassword(String(e.detail.value ?? ''))}
        />
        {error && (
          <IonText color="danger">
            <p role="alert" style={{ margin: '10px 2px 0' }}>{error}</p>
          </IonText>
        )}
        <IonButton
          expand="block"
          type="submit"
          style={{ marginTop: 18 }}
          disabled={busy || !email || password.length < 8}
        >
          {busy ? <IonSpinner name="dots" /> : 'Create account'}
        </IonButton>
        <IonButton expand="block" fill="clear" routerLink="/login">
          Already have an account? Sign in
        </IonButton>
      </form>
    </AuthLayout>
  )
}
