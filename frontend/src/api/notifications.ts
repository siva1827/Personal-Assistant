import { api } from './client'
import type { NotificationFeed } from './types'

export const getNotifications = () => api.get<NotificationFeed>('/notifications').then((r) => r.data)

export const markNotificationRead = (id: number) => api.post(`/notifications/${id}/read`)

export const markAllNotificationsRead = () =>
  api.post<{ updated: number }>('/notifications/read-all').then((r) => r.data)
