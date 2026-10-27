import { api } from './client'
import type { Recurrence, Task } from './types'

export const listTasks = (status: 'OPEN' | 'DONE') =>
  api.get<Task[]>('/tasks', { params: { status } }).then((r) => r.data)

export const createTask = (title: string, dueDate?: string, recurrence?: Recurrence) =>
  api.post<Task>('/tasks', { title, dueDate, recurrence }).then((r) => r.data)

export const updateTask = (
  id: number,
  changes: { title?: string; dueDate?: string; clearDueDate?: boolean; recurrence?: Recurrence; status?: 'OPEN' | 'DONE' },
) => api.patch<Task>(`/tasks/${id}`, changes).then((r) => r.data)

export const deleteTask = (id: number) => api.delete(`/tasks/${id}`)
