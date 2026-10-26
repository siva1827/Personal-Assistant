import { api } from './client'
import type { AuthResponse } from './types'

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data)

export const register = (email: string, password: string, displayName: string) =>
  api.post<AuthResponse>('/auth/register', { email, password, displayName }).then((r) => r.data)

export const refresh = (refreshToken: string) =>
  api.post<AuthResponse>('/auth/refresh', { refreshToken }).then((r) => r.data)
