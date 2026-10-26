import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  addMissingFromRecipe,
  addShoppingItem,
  clearPurchased,
  deleteShoppingItem,
  listShopping,
  updateShoppingItem,
} from '../../api/shopping'

const KEY = ['shopping']

export const useShopping = () => useQuery({ queryKey: KEY, queryFn: listShopping })

function useInvalidateShopping() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: KEY })
}

export const useAddShoppingItem = () => {
  const invalidate = useInvalidateShopping()
  return useMutation({
    mutationFn: ({ name, estimatedCost }: { name: string; estimatedCost?: number }) =>
      addShoppingItem(name, estimatedCost),
    onSuccess: invalidate,
  })
}

export const useUpdateShoppingItem = () => {
  const invalidate = useInvalidateShopping()
  return useMutation({
    mutationFn: ({ id, ...changes }: { id: number } & Parameters<typeof updateShoppingItem>[1]) =>
      updateShoppingItem(id, changes),
    onSuccess: invalidate,
  })
}

export const useDeleteShoppingItem = () => {
  const invalidate = useInvalidateShopping()
  return useMutation({ mutationFn: deleteShoppingItem, onSuccess: invalidate })
}

export const useClearPurchased = () => {
  const invalidate = useInvalidateShopping()
  return useMutation({ mutationFn: clearPurchased, onSuccess: invalidate })
}

export const useAddMissingFromRecipe = () => {
  const invalidate = useInvalidateShopping()
  return useMutation({ mutationFn: addMissingFromRecipe, onSuccess: invalidate })
}
