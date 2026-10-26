import { createContext } from 'react'

export interface AuthState {
  isAuthenticated: boolean
  /** False until the stored refresh token (if any) has been tried on startup. */
  ready: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)
