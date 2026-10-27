import { IonIcon, IonLabel, IonRouterOutlet, IonTabBar, IonTabButton, IonTabs } from '@ionic/react'
import {
  cashOutline,
  checkmarkDoneOutline,
  homeOutline,
  nutritionOutline,
  restaurantOutline,
} from 'ionicons/icons'
import { Navigate, Route } from 'react-router-dom'
import { useDeviceAlerts } from './features/notifications/deviceAlerts'
import { useNotificationFeed } from './features/notifications/useNotifications'
import { usePushSync } from './features/push/push'
import BudgetsPage from './pages/BudgetsPage'
import NotificationsPage from './pages/NotificationsPage'
import ExpensesPage from './pages/ExpensesPage'
import HomePage from './pages/HomePage'
import PantryPage from './pages/PantryPage'
import ProfilePage from './pages/ProfilePage'
import RecipeDetailPage from './pages/RecipeDetailPage'
import RecipesPage from './pages/RecipesPage'
import SmsImportPage from './pages/SmsImportPage'
import TasksPage from './pages/TasksPage'

export default function AppTabs() {
  // Keeps the notification feed fresh app-wide and shows device alerts for new items
  const feed = useNotificationFeed()
  useDeviceAlerts(feed.data?.items)
  usePushSync()

  return (
    <IonTabs>
      <IonRouterOutlet>
        <Route path="/home" element={<HomePage />} />
        <Route path="/pantry" element={<PantryPage />} />
        <Route path="/recipes" element={<RecipesPage />} />
        <Route path="/recipes/:id" element={<RecipeDetailPage />} />
        <Route path="/expenses" element={<ExpensesPage />} />
        <Route path="/budgets" element={<BudgetsPage />} />
        <Route path="/expenses/sms" element={<SmsImportPage />} />
        <Route path="/tasks" element={<TasksPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        {/* After signing in or registering the URL is still /login or /register */}
        <Route path="/login" element={<Navigate to="/home" replace />} />
        <Route path="/register" element={<Navigate to="/home" replace />} />
        <Route path="/" element={<Navigate to="/home" replace />} />
      </IonRouterOutlet>
      <IonTabBar slot="bottom">
        <IonTabButton tab="home" href="/home">
          <IonIcon icon={homeOutline} />
          <IonLabel>Home</IonLabel>
        </IonTabButton>
        <IonTabButton tab="pantry" href="/pantry">
          <IonIcon icon={nutritionOutline} />
          <IonLabel>Pantry</IonLabel>
        </IonTabButton>
        <IonTabButton tab="recipes" href="/recipes">
          <IonIcon icon={restaurantOutline} />
          <IonLabel>Recipes</IonLabel>
        </IonTabButton>
        <IonTabButton tab="expenses" href="/expenses">
          <IonIcon icon={cashOutline} />
          <IonLabel>Spending</IonLabel>
        </IonTabButton>
        <IonTabButton tab="tasks" href="/tasks">
          <IonIcon icon={checkmarkDoneOutline} />
          <IonLabel>Tasks</IonLabel>
        </IonTabButton>
      </IonTabBar>
    </IonTabs>
  )
}
