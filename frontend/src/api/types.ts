export interface AuthResponse {
  accessToken: string
  refreshToken: string
}

export interface UserProfile {
  id: number
  email: string
  displayName: string | null
  dietaryPreference: 'ANY' | 'VEG' | 'NON_VEG' | 'VEGAN' | 'EGGETARIAN'
  allergies: string[]
}

export interface Ingredient {
  id: number
  name: string
  category: string | null
  staple: boolean
}

export interface PantryItem {
  id: number
  ingredientId: number
  ingredientName: string
  category: string | null
  quantity: number | null
  unit: string | null
  expiryDate: string | null
  addedAt: string
}

export interface AddPantryItemRequest {
  ingredientId: number
  quantity?: number
  unit?: string
  expiryDate?: string
}

export interface UpdatePantryItemRequest {
  quantity?: number
  unit?: string
  expiryDate?: string
}

export interface DayTheme {
  id: number
  code: string
  label: string
}

export type DietaryPreference = UserProfile['dietaryPreference']

export interface UpdateProfileRequest {
  displayName?: string
  dietaryPreference?: DietaryPreference
  allergies?: string[]
}

export interface RecipeIngredient {
  name: string
  quantity?: number | string | null
  unit?: string | null
  in_pantry?: boolean
}

export interface Recipe {
  id: number
  title: string
  description: string | null
  cookTimeMinutes: number | null
  servings: number | null
  cuisine: string | null
  dietaryType: DietaryPreference
  ingredients: RecipeIngredient[]
  missingIngredients: string[]
  steps: string[]
  favorite: boolean
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
