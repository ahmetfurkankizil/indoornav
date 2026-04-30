import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DbAdminLayout } from '../../components/Layout'
import { listManagers, deleteManager } from '../../api/dbAdmin'

interface Manager {
  id: string
  email: string
  fullName: string
  createdAt: string
}

export function ManagersPage() {
  const queryClient = useQueryClient()
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [search, setSearch]     = useState('')

  const { data: managers = [], isLoading, error } = useQuery<Manager[]>({
    queryKey: ['db-admin-managers'],
    queryFn: listManagers,
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteManager(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['db-admin-managers'] })
      queryClient.invalidateQueries({ queryKey: ['db-admin-stats'] })
      setDeleteId(null)
    },
  })

  const filtered = managers.filter(
    (m) =>
      m.email.toLowerCase().includes(search.toLowerCase()) ||
      m.fullName.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <DbAdminLayout>
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white">Managers</h1>
            <p className="text-gray-400 text-sm mt-0.5">{managers.length} registered</p>
          </div>
          <input
            id="managers-search"
            type="text"
            placeholder="Search by email or name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="bg-gray-700 border border-gray-600 text-white text-sm rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 w-64"
          />
        </div>

        {isLoading && <p className="text-gray-400">Loading...</p>}
        {error && (
          <div className="bg-red-900/40 border border-red-800 text-red-400 p-4 rounded-lg">
            Failed to load managers.
          </div>
        )}

        {!isLoading && filtered.length === 0 && (
          <p className="text-gray-500 text-center py-12">No managers found.</p>
        )}

        {filtered.length > 0 && (
          <div className="bg-gray-800 border border-gray-700 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-700 text-gray-400 text-xs uppercase tracking-wide">
                  <th className="text-left px-5 py-3">Name</th>
                  <th className="text-left px-5 py-3">Email</th>
                  <th className="text-left px-5 py-3">Registered</th>
                  <th className="text-left px-5 py-3">ID</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {filtered.map((m) => (
                  <tr key={m.id} className="hover:bg-gray-750 transition-colors">
                    <td className="px-5 py-3 text-white font-medium">{m.fullName}</td>
                    <td className="px-5 py-3 text-gray-300">{m.email}</td>
                    <td className="px-5 py-3 text-gray-400 text-xs">
                      {new Date(m.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-5 py-3 text-gray-500 font-mono text-xs">{m.id.slice(0, 8)}…</td>
                    <td className="px-5 py-3 text-right">
                      <button
                        onClick={() => setDeleteId(m.id)}
                        className="text-red-400 hover:text-red-300 text-xs px-2 py-1 rounded border border-red-800 hover:border-red-600 transition-colors"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delete confirm modal */}
      {deleteId && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 border border-gray-700 rounded-lg w-full max-w-md shadow-2xl">
            <div className="px-6 py-4 border-b border-gray-700">
              <h2 className="text-lg font-semibold text-white">Delete Manager</h2>
            </div>
            <div className="p-6">
              <p className="text-gray-300 mb-2">
                This will permanently delete the manager account and all their buildings, floors,
                nodes, and edges.
              </p>
              <p className="text-red-400 text-sm font-medium mb-6">This action cannot be undone.</p>
              <div className="flex gap-3 justify-end">
                <button
                  onClick={() => setDeleteId(null)}
                  className="px-4 py-2 border border-gray-600 text-gray-300 rounded hover:bg-gray-700 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() => deleteMut.mutate(deleteId)}
                  disabled={deleteMut.isPending}
                  className="px-4 py-2 bg-red-700 hover:bg-red-600 text-white rounded disabled:opacity-50 transition-colors"
                >
                  {deleteMut.isPending ? 'Deleting...' : 'Delete Permanently'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </DbAdminLayout>
  )
}
