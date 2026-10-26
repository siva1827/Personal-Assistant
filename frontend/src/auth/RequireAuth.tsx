import type { ReactElement } from 'react'
import { IonSpinner } from '@ionic/react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

export default function RequireAuth({ children }: { children: ReactElement }) {
  const { isAuthenticated, ready } = useAuth()
  if (!ready) return <IonSpinner />
  return isAuthenticated ? children : <Navigate to="/login" replace />
}
