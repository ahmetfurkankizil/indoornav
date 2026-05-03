import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DbAdminLayout } from '../../components/Layout'
import { listAllBuildings, deleteAnyBuilding, regenerateNavPackage } from '../../api/dbAdmin'

interface Building {
  id: string
  managerId: string
  name: string
  description?: string
  address?: string
  status: string
  qrToken: string
  createdAt: string
  updatedAt: string
}

export function BuildingsAdminPage() {
  const queryClient  = useQueryClient()
  const [deleteId, setDeleteId]       = useState<string | null>(null)
  const [regenId, setRegenId]         = useState<string | null>(null)
  const [regenSuccess, setRegenSuccess] = useState<string | null>(null)
  const [search, setSearch]           = useState('')

  const { data: buildings = [], isLoading, error } = useQuery<Building[]>({
    queryKey: ['db-admin-buildings'],
    queryFn: listAllBuildings,
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteAnyBuilding(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['db-admin-buildings'] })
      queryClient.invalidateQueries({ queryKey: ['db-admin-stats'] })
      setDeleteId(null)
    },
  })

  const regenMut = useMutation({
    mutationFn: (id: string) => regenerateNavPackage(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['db-admin-nav-packages'] })
      setRegenId(null)
      setRegenSuccess(id)
      setTimeout(() => setRegenSuccess(null), 3000)
    },
  })

  const filtered = buildings.filter(
    (b) =>
      b.name.toLowerCase().includes(search.toLowerCase()) ||
      b.qrToken.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <DbAdminLayout>
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white">Buildings</h1>
            <p className="text-gray-400 text-sm mt-0.5">{buildings.length} total</p>
          </div>
          <input
            id="buildings-search"
            type="text"
            placeholder="Search by name or QR token..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="bg-gray-700 border border-gray-600 text-white text-sm rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 w-72"
          />
        </div>

        {regenSuccess && (
          <div className="mb-4 bg-green-900/40 border border-green-700 text-green-400 p-3 rounded-lg text-sm">
            ✅ Nav package regenerated successfully for building {regenSuccess.slice(0, 8)}…
          </div>
        )}

        {isLoading && <p className="text-gray-400">Loading...</p>}
        {error && (
          <div className="bg-red-900/40 border border-red-800 text-red-400 p-4 rounded-lg">
            Failed to load buildings.
          </div>
        )}

        {!isLoading && filtered.length === 0 && (
          <p className="text-gray-500 text-center py-12">No buildings found.</p>
        )}

        {filtered.length > 0 && (
          <div className="bg-gray-800 border border-gray-700 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-700 text-gray-400 text-xs uppercase tracking-wide">
                  <th className="text-left px-5 py-3">Name</th>
                  <th className="text-left px-5 py-3">Status</th>
                  <th className="text-left px-5 py-3">QR Token</th>
                  <th className="text-left px-5 py-3">Manager ID</th>
                  <th className="text-left px-5 py-3">Updated</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {filtered.map((b) => (
                  <tr key={b.id} className="hover:bg-gray-750 transition-colors">
                    <td className="px-5 py-3 text-white font-medium">
                      <div>{b.name}</div>
                      {b.address && <div className="text-gray-500 text-xs">{b.address}</div>}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        b.status === 'published'
                          ? 'bg-green-900/50 text-green-400 border border-green-800'
                          : 'bg-yellow-900/50 text-yellow-400 border border-yellow-800'
                      }`}>
                        {b.status}
                      </span>
                    </td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-400">{b.qrToken}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500">{b.managerId.slice(0, 8)}…</td>
                    <td className="px-5 py-3 text-gray-400 text-xs">
                      {new Date(b.updatedAt).toLocaleDateString()}
                    </td>
                    <td className="px-5 py-3 text-right">
                      <div className="flex gap-2 justify-end">
                        <button
                          onClick={() => setRegenId(b.id)}
                          className="text-blue-400 hover:text-blue-300 text-xs px-2 py-1 rounded border border-blue-800 hover:border-blue-600 transition-colors"
                        >
                          Regen
                        </button>
                        <button
                          onClick={() => setDeleteId(b.id)}
                          className="text-red-400 hover:text-red-300 text-xs px-2 py-1 rounded border border-red-800 hover:border-red-600 transition-colors"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delete confirm */}
      {deleteId && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 border border-gray-700 rounded-lg w-full max-w-md shadow-2xl">
            <div className="px-6 py-4 border-b border-gray-700">
              <h2 className="text-lg font-semibold text-white">Delete Building</h2>
            </div>
            <div className="p-6">
              <p className="text-gray-300 mb-2">
                This will permanently delete the building including all floors, nodes, edges, and nav packages.
              </p>
              <p className="text-red-400 text-sm font-medium mb-6">This action cannot be undone.</p>
              <div className="flex gap-3 justify-end">
                <button onClick={() => setDeleteId(null)} className="px-4 py-2 border border-gray-600 text-gray-300 rounded hover:bg-gray-700 transition-colors">
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

      {/* Regenerate confirm */}
      {regenId && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 border border-gray-700 rounded-lg w-full max-w-md shadow-2xl">
            <div className="px-6 py-4 border-b border-gray-700">
              <h2 className="text-lg font-semibold text-white">Regenerate Nav Package</h2>
            </div>
            <div className="p-6">
              <p className="text-gray-300 mb-6">
                This will re-run the nav package generator for this building and mark the new package as current.
                The old package will be superseded.
              </p>
              <div className="flex gap-3 justify-end">
                <button onClick={() => setRegenId(null)} className="px-4 py-2 border border-gray-600 text-gray-300 rounded hover:bg-gray-700 transition-colors">
                  Cancel
                </button>
                <button
                  onClick={() => regenMut.mutate(regenId)}
                  disabled={regenMut.isPending}
                  className="px-4 py-2 bg-blue-700 hover:bg-blue-600 text-white rounded disabled:opacity-50 transition-colors"
                >
                  {regenMut.isPending ? 'Regenerating...' : 'Regenerate'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </DbAdminLayout>
  )
}
