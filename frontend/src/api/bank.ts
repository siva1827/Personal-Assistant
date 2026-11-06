import { api } from './client'
import type { AaStatus, BankConsent, BankSyncResult } from './types'

export const getAaStatus = () => api.get<AaStatus>('/aa/status').then((r) => r.data)

export const listConsents = () => api.get<BankConsent[]>('/aa/consents').then((r) => r.data)

export const createConsent = (historyDays?: number) =>
  api.post<BankConsent>('/aa/consents', { historyDays }).then((r) => r.data)

/** Demo provider only: stands in for approving or rejecting inside the user's AA app */
export const decideConsent = (id: number, approve: boolean) =>
  api.post<BankConsent>(`/aa/consents/${id}/${approve ? 'mock-approve' : 'mock-reject'}`).then((r) => r.data)

export const syncConsent = (id: number) => api.post<BankSyncResult>(`/aa/consents/${id}/sync`).then((r) => r.data)

export const revokeConsent = (id: number) => api.delete(`/aa/consents/${id}`)
