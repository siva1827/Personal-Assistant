import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createTask, deleteTask, listTasks, updateTask } from '../../api/tasks'
import type { Recurrence } from '../../api/types'

export const useOpenTasks = () => useQuery({ queryKey: ['tasks', 'OPEN'], queryFn: () => listTasks('OPEN') })

export const useDoneTasks = (enabled: boolean) =>
  useQuery({ queryKey: ['tasks', 'DONE'], queryFn: () => listTasks('DONE'), enabled })

function useInvalidateTasks() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ['tasks'] })
}

export const useCreateTask = () => {
  const invalidate = useInvalidateTasks()
  return useMutation({
    mutationFn: ({ title, dueDate, recurrence }: { title: string; dueDate?: string; recurrence?: Recurrence }) =>
      createTask(title, dueDate, recurrence),
    onSuccess: invalidate,
  })
}

export const useUpdateTask = () => {
  const invalidate = useInvalidateTasks()
  return useMutation({
    mutationFn: ({ id, ...changes }: { id: number } & Parameters<typeof updateTask>[1]) => updateTask(id, changes),
    onSuccess: invalidate,
  })
}

export const useDeleteTask = () => {
  const invalidate = useInvalidateTasks()
  return useMutation({ mutationFn: deleteTask, onSuccess: invalidate })
}
