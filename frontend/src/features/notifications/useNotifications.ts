import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../../api/notifications'

const KEY = ['notifications']

/** Polled once a minute, so the bell badge stays current while the app is open. */
export const useNotificationFeed = () =>
  useQuery({ queryKey: KEY, queryFn: getNotifications, refetchInterval: 60_000 })

export const useMarkRead = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}

export const useMarkAllRead = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}
