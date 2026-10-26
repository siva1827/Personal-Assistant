import { api } from './client'
import type { UpdateProfileRequest, UserProfile } from './types'

export const getMe = () => api.get<UserProfile>('/users/me').then((r) => r.data)

export const updateMe = (request: UpdateProfileRequest) =>
  api.patch<UserProfile>('/users/me', request).then((r) => r.data)
