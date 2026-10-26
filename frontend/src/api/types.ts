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

export interface DayTheme {
  id: number
  code: string
  label: string
}
