import { IonApp, IonRouterOutlet, setupIonicReact } from '@ionic/react'
import { IonReactRouter } from '@ionic/react-router'
import { Navigate, Route } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './auth/RequireAuth'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import PantryPage from './pages/PantryPage'
import ProfilePage from './pages/ProfilePage'
import RecipeDetailPage from './pages/RecipeDetailPage'
import RecipesPage from './pages/RecipesPage'
import RegisterPage from './pages/RegisterPage'

setupIonicReact()

export default function App() {
  return (
    <IonApp>
      <AuthProvider>
        <IonReactRouter>
          <IonRouterOutlet>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/home" element={<RequireAuth><HomePage /></RequireAuth>} />
            <Route path="/pantry" element={<RequireAuth><PantryPage /></RequireAuth>} />
            <Route path="/recipes" element={<RequireAuth><RecipesPage /></RequireAuth>} />
            <Route path="/recipes/:id" element={<RequireAuth><RecipeDetailPage /></RequireAuth>} />
            <Route path="/profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />
            <Route path="/" element={<Navigate to="/home" replace />} />
          </IonRouterOutlet>
        </IonReactRouter>
      </AuthProvider>
    </IonApp>
  )
}
