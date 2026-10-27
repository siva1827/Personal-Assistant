import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { useNavigate } from 'react-router-dom'
import type { AppNotification } from '../api/types'
import EmptyState from '../components/EmptyState'
import ListSkeleton from '../components/ListSkeleton'
import { notificationEmoji, timeAgo } from '../features/notifications/display'
import { useMarkAllRead, useMarkRead, useNotificationFeed } from '../features/notifications/useNotifications'

export default function NotificationsPage() {
  const feed = useNotificationFeed()
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const navigate = useNavigate()

  const open = (item: AppNotification) => {
    if (!item.read) markRead.mutate(item.id)
    if (item.link) navigate(item.link)
  }

  return (
    <IonPage>
      <IonHeader className="ion-no-border">
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/home" />
          </IonButtons>
          <IonTitle>Notifications</IonTitle>
          {feed.data && feed.data.unread > 0 && (
            <IonButtons slot="end">
              <IonButton onClick={() => markAll.mutate()} disabled={markAll.isPending}>
                Mark all read
              </IonButton>
            </IonButtons>
          )}
        </IonToolbar>
      </IonHeader>
      <IonContent className="app-content">
        {feed.isLoading && <ListSkeleton rows={4} />}
        {feed.isError && (
          <div className="card">
            <IonText color="danger">Could not load notifications.</IonText>
          </div>
        )}
        {feed.isSuccess && feed.data.items.length === 0 && (
          <EmptyState emoji="🔔" title="You're all caught up">
            Reminders for tasks, budgets and food that is about to expire will appear here.
          </EmptyState>
        )}
        {feed.data && feed.data.items.length > 0 && (
          <div className="row-list">
            {feed.data.items.map((item) => (
              <IonItem key={item.id} lines="full" button detail={false} onClick={() => open(item)}>
                <span className="avatar">{notificationEmoji(item.type)}</span>
                <IonLabel className="ion-text-wrap">
                  <h3 style={{ fontWeight: item.read ? 500 : 800 }}>{item.title}</h3>
                  <p>{item.body}</p>
                  <p>{timeAgo(item.createdAt)}</p>
                </IonLabel>
                {!item.read && <span slot="end" className="unread-dot" aria-label="Unread" />}
              </IonItem>
            ))}
          </div>
        )}
      </IonContent>
    </IonPage>
  )
}
