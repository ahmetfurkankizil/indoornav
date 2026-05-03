import { create } from 'zustand'
import type { Manager } from '../api/auth'


interface AuthState {
  manager: Manager | null
  token: string | null
  setAuth: (manager: Manager, token: string) => void
  clearAuth: () => void
  isAuthenticated: () => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  manager: (() => {
    try {
      const s = localStorage.getItem('manager_user')
      return s ? JSON.parse(s) : null
    } catch { return null }
  })(),
  token: localStorage.getItem('manager_token'),

  setAuth: (manager, token) => {
    localStorage.setItem('manager_token', token)
    localStorage.setItem('manager_user', JSON.stringify(manager))
    set({ manager, token })
  },

  clearAuth: () => {
    localStorage.removeItem('manager_token')
    localStorage.removeItem('manager_user')
    set({ manager: null, token: null })
  },

  isAuthenticated: () => !!get().token,
}))

interface DbAdminState {
  token: string | null
  setToken: (token: string) => void
  clearToken: () => void
  isAuthenticated: () => boolean
}

export const useDbAdminStore = create<DbAdminState>((set, get) => ({
  token: localStorage.getItem('dbadmin_token'),

  setToken: (token) => {
    localStorage.setItem('dbadmin_token', token)
    set({ token })
  },

  clearToken: () => {
    localStorage.removeItem('dbadmin_token')
    set({ token: null })
  },

  isAuthenticated: () => !!get().token,
}))
