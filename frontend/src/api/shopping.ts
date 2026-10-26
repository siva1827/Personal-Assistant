import { api } from './client'
import type { AddedFromRecipe, ShoppingItem } from './types'

export const listShopping = () => api.get<ShoppingItem[]>('/shopping').then((r) => r.data)

export const addShoppingItem = (name: string, estimatedCost?: number) =>
  api.post<ShoppingItem>('/shopping', { name, estimatedCost }).then((r) => r.data)

export const updateShoppingItem = (id: number, changes: { purchased?: boolean; estimatedCost?: number; clearCost?: boolean }) =>
  api.patch<ShoppingItem>(`/shopping/${id}`, changes).then((r) => r.data)

export const deleteShoppingItem = (id: number) => api.delete(`/shopping/${id}`)

export const clearPurchased = () => api.delete<{ removed: number }>('/shopping/purchased').then((r) => r.data)

export const addMissingFromRecipe = (recipeId: number) =>
  api.post<AddedFromRecipe>(`/shopping/from-recipe/${recipeId}`).then((r) => r.data)
