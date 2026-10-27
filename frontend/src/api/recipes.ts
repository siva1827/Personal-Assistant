import { api } from './client'
import type { Page, Recipe, SuggestRequest, SuggestionResponse } from './types'

export const getHistory = (page = 0, size = 20) =>
  api.get<Page<Recipe>>('/recipes/history', { params: { page, size } }).then((r) => r.data)

export const getRecipe = (id: number) => api.get<Recipe>(`/recipes/${id}`).then((r) => r.data)

export const deleteRecipe = (id: number) => api.delete(`/recipes/${id}`)

export const getFavorites = (page = 0, size = 20) =>
  api.get<Page<Recipe>>('/favorites', { params: { page, size } }).then((r) => r.data)

export const addFavorite = (id: number) => api.put(`/favorites/${id}`)

export const removeFavorite = (id: number) => api.delete(`/favorites/${id}`)

export const suggestRecipes = (request: SuggestRequest) =>
  api.post<SuggestionResponse>('/recipes/suggest', request).then((r) => r.data)
