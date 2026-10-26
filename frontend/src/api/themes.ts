import { api } from './client'
import type { DayTheme } from './types'

export const listThemes = () => api.get<DayTheme[]>('/themes').then((r) => r.data)
