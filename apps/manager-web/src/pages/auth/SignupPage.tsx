import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'

// Signup is not meaningful in offline mode — redirect to dashboard directly.
export function SignupPage() {
  const navigate        = useNavigate()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())

  useEffect(() => {
    navigate(isAuthenticated ? '/dashboard' : '/login', { replace: true })
  }, [])

  return null
}
