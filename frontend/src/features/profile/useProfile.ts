import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMe, updateMe } from '../../api/users'

const PROFILE_KEY = ['profile']

export const useProfile = () => useQuery({ queryKey: PROFILE_KEY, queryFn: getMe })

export const useUpdateProfile = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateMe,
    onSuccess: (profile) => queryClient.setQueryData(PROFILE_KEY, profile),
  })
}

/** Splits comma-separated input into trimmed, lowercase, de-duplicated names. */
export function parseAllergies(input: string): string[] {
  return [...new Set(input.split(',').map((a) => a.trim().toLowerCase()).filter(Boolean))]
}
