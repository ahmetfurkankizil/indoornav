export interface Manager { id: string; email: string; fullName: string; createdAt: string }
export interface AuthResponse { token: string; manager: Manager }

function makeManager(email: string, fullName: string): Manager {
  return { id: 'local-manager', email, fullName, createdAt: new Date().toISOString() }
}

export const login = async (email: string, _password: string): Promise<AuthResponse> => ({
  token: 'local-token',
  manager: makeManager(email, email.split('@')[0]),
})

export const signup = async (email: string, _password: string, fullName: string): Promise<AuthResponse> => ({
  token: 'local-token',
  manager: makeManager(email, fullName),
})

export const getMe = async (): Promise<Manager> => {
  try {
    const s = localStorage.getItem('manager_user')
    if (s) return JSON.parse(s)
  } catch { /* ignore */ }
  return makeManager('local@VecturAI.app', 'Manager')
}
