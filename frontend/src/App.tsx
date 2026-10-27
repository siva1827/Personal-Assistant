import { IonApp, IonRouterOutlet, IonSpinner, setupIonicReact } from '@ionic/react'
import { IonReactRouter } from '@ionic/react-router'
import { Navigate, Route } from 'react-router-dom'
import AppTabs from './AppTabs'
import { AuthProvider } from './auth/AuthContext'
import { useAuth } from './auth/useAuth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

setupIonicReact()

/**
 * Signed-out and signed-in users get separate routers, so the sign-in page is really removed
 * after logging in (and the app after logging out) instead of lingering underneath.
 */
function AppRoutes() {
  const { isAuthenticated, ready } = useAuth()

  if (!ready) {
    return <IonSpinner style={{ margin: '40vh auto 0', display: 'block' }} />
  }

  if (isAuthenticated) {
    return (
      <IonReactRouter key="signed-in">
        <IonRouterOutlet>
          <Route path="/*" element={<AppTabs />} />
        </IonRouterOutlet>
      </IonReactRouter>
    )
  }

  return (
    <IonReactRouter key="signed-out">
      <IonRouterOutlet>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/*" element={<Navigate to="/login" replace />} />
      </IonRouterOutlet>
    </IonReactRouter>
  )
}

export default function App() {
  return (
    <IonApp>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </IonApp>
  )
}
