import { api } from './client'
import type { AddPantryItemRequest, Ingredient, PantryItem, UpdatePantryItemRequest } from './types'

export const searchIngredients = (query: string) =>
  api.get<Ingredient[]>('/ingredients', { params: { query } }).then((r) => r.data)

export const listPantry = () => api.get<PantryItem[]>('/pantry').then((r) => r.data)

export const addPantryItem = (request: AddPantryItemRequest) =>
  api.post<PantryItem>('/pantry', request).then((r) => r.data)

export const updatePantryItem = (id: number, request: UpdatePantryItemRequest) =>
  api.patch<PantryItem>(`/pantry/${id}`, request).then((r) => r.data)

export const deletePantryItem = (id: number) => api.delete(`/pantry/${id}`)
