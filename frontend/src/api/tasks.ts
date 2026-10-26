import { api } from './client'
import type { Task } from './types'

export const listTasks = (status: 'OPEN' | 'DONE') =>
  api.get<Task[]>('/tasks', { params: { status } }).then((r) => r.data)

export const createTask = (title: string, dueDate?: string) =>
  api.post<Task>('/tasks', { title, dueDate }).then((r) => r.data)

export const updateTask = (
  id: number,
  changes: { title?: string; dueDate?: string; clearDueDate?: boolean; status?: 'OPEN' | 'DONE' },
) => api.patch<Task>(`/tasks/${id}`, changes).then((r) => r.data)

export const deleteTask = (id: number) => api.delete(`/tasks/${id}`)
