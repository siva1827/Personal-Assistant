export interface AuthResponse {
  accessToken: string
  refreshToken: string
}

export interface UserProfile {
  id: number
  email: string
  displayName: string | null
  dietaryPreference: 'ANY' | 'VEG' | 'NON_VEG' | 'VEGAN' | 'EGGETARIAN'
}
