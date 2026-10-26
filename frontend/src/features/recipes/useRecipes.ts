import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addFavorite, deleteRecipe, getFavorites, getHistory, getRecipe, removeFavorite } from '../../api/recipes'
import { listThemes } from '../../api/themes'

export const useThemes = () => useQuery({ queryKey: ['themes'], queryFn: listThemes, staleTime: Infinity })

export const useHistory = () => useQuery({ queryKey: ['recipes', 'history'], queryFn: () => getHistory() })

export const useFavorites = () => useQuery({ queryKey: ['recipes', 'favorites'], queryFn: () => getFavorites() })

export const useRecipe = (id: number) => useQuery({ queryKey: ['recipes', id], queryFn: () => getRecipe(id) })

export const useToggleFavorite = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, favorite }: { id: number; favorite: boolean }) =>
      favorite ? removeFavorite(id) : addFavorite(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  })
}

export const useDeleteRecipe = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteRecipe,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  })
}
