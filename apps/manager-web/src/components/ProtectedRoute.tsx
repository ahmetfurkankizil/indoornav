import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore, useDbAdminStore } from '../stores/authStore'

export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}

export function DbAdminRoute() {
  const isAuthenticated = useDbAdminStore((s) => s.isAuthenticated())
  return isAuthenticated ? <Outlet /> : <Navigate to="/db-admin/login" replace />
}
