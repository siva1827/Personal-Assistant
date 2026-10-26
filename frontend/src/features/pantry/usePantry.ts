import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addPantryItem, deletePantryItem, listPantry, searchIngredients, updatePantryItem } from '../../api/pantry'
import type { UpdatePantryItemRequest } from '../../api/types'

const PANTRY_KEY = ['pantry']

export const usePantry = () => useQuery({ queryKey: PANTRY_KEY, queryFn: listPantry })

export const useIngredientSearch = (query: string) =>
  useQuery({
    queryKey: ['ingredients', query],
    queryFn: () => searchIngredients(query),
    enabled: query.trim().length > 0,
  })

export const useAddPantryItem = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: addPantryItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: PANTRY_KEY }),
  })
}

export const useUpdatePantryItem = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...request }: { id: number } & UpdatePantryItemRequest) => updatePantryItem(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: PANTRY_KEY }),
  })
}

export const useDeletePantryItem = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deletePantryItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: PANTRY_KEY }),
  })
}
