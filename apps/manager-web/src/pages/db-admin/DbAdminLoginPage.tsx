import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { dbAdminLogin } from '../../api/dbAdmin'
import { useDbAdminStore } from '../../stores/authStore'

export function DbAdminLoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)
  const setToken  = useDbAdminStore((s) => s.setToken)
  const navigate  = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await dbAdminLogin(username, password)
      setToken(data.token)
      navigate('/db-admin')
    } catch (err: any) {
      setError(err.response?.data?.error || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900">
      <div className="bg-gray-800 border border-gray-700 p-8 rounded-lg shadow-2xl w-full max-w-md">
        <div className="mb-8 text-center">
          <span className="inline-block text-3xl mb-3">🔐</span>
          <h1 className="text-2xl font-bold text-white">DB Admin Access</h1>
          <p className="text-gray-400 text-sm mt-1">System operators only</p>
        </div>

        {error && (
          <div className="mb-4 text-sm text-red-400 bg-red-900/40 border border-red-800 p-3 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Username</label>
            <input
              id="db-admin-username"
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-red-500"
              placeholder="sysadmin"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Password</label>
            <input
              id="db-admin-password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-red-700 hover:bg-red-600 text-white py-2 rounded-md disabled:opacity-50 transition-colors font-medium mt-2"
          >
            {loading ? 'Authenticating...' : 'Sign in'}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-gray-500">
          Credentials are set via <code className="bg-gray-700 px-1 rounded">DB_ADMIN_USER</code> /{' '}
          <code className="bg-gray-700 px-1 rounded">DB_ADMIN_PASS</code> env vars
        </p>
      </div>
    </div>
  )
}
