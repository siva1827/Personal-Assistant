import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createConsent, decideConsent, getAaStatus, listConsents, revokeConsent, syncConsent } from '../../api/bank'

export const useAaStatus = () => useQuery({ queryKey: ['aa', 'status'], queryFn: getAaStatus })

export const useConsents = (enabled: boolean) =>
  useQuery({ queryKey: ['aa', 'consents'], queryFn: listConsents, enabled })

function useRefresh() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ['aa'] })
}

export const useCreateConsent = () => {
  const refresh = useRefresh()
  return useMutation({ mutationFn: (historyDays?: number) => createConsent(historyDays), onSuccess: refresh })
}

export const useDecideConsent = () => {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: ({ id, approve }: { id: number; approve: boolean }) => decideConsent(id, approve),
    onSuccess: refresh,
  })
}

/** Imported bank transactions are expenses, so the spending screens must refresh as well. */
export const useSyncConsent = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: syncConsent,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['aa'] }),
        queryClient.invalidateQueries({ queryKey: ['expenses'] }),
        queryClient.invalidateQueries({ queryKey: ['budgets'] }),
        queryClient.invalidateQueries({ queryKey: ['notifications'] }),
      ]),
  })
}

export const useRevokeConsent = () => {
  const refresh = useRefresh()
  return useMutation({ mutationFn: revokeConsent, onSuccess: refresh })
}
