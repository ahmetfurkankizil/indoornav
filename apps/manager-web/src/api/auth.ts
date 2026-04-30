import { api } from './client'

export interface Manager { id: string; email: string; fullName: string; createdAt: string }
export interface AuthResponse { token: string; manager: Manager }

export const signup = (email: string, password: string, fullName: string) =>
  api.post<AuthResponse>('/auth/signup', { email, password, fullName }).then(r => r.data)

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/auth/login', { email, password }).then(r => r.data)

export const getMe = () =>
  api.get<Manager>('/auth/me').then(r => r.data)
