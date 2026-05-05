import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore, useDbAdminStore } from '../stores/authStore'

interface Props { children: React.ReactNode }

export function Layout({ children }: Props) {
  const manager   = useAuthStore((s) => s.manager)
  const clearAuth = useAuthStore((s) => s.clearAuth)
  const navigate  = useNavigate()

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
        <Link to="/dashboard" className="text-xl font-bold text-blue-600">Vectura AI</Link>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{manager?.email}</span>
          <button
            onClick={() => { clearAuth(); navigate('/login') }}
            className="text-sm text-gray-500 hover:text-gray-900 transition-colors"
          >
            Sign out
          </button>
        </div>
      </header>
      <main className="flex-1 p-6">{children}</main>
    </div>
  )
}

export function DbAdminLayout({ children }: Props) {
  const clearToken = useDbAdminStore((s) => s.clearToken)
  const navigate   = useNavigate()

  return (
    <div className="min-h-screen flex flex-col bg-gray-900 text-gray-100">
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <span className="text-lg font-bold text-red-400">Vectura AI DB Admin</span>
          <nav className="flex gap-4 text-sm">
            <Link to="/db-admin"              className="hover:text-white">Dashboard</Link>
            <Link to="/db-admin/managers"     className="hover:text-white">Managers</Link>
            <Link to="/db-admin/buildings"    className="hover:text-white">Buildings</Link>
            <Link to="/db-admin/nav-packages" className="hover:text-white">Nav Packages</Link>
          </nav>
        </div>
        <button
          onClick={() => { clearToken(); navigate('/db-admin/login') }}
          className="text-sm text-gray-400 hover:text-white"
        >
          Sign out
        </button>
      </header>
      <main className="flex-1 p-6">{children}</main>
    </div>
  )
}
