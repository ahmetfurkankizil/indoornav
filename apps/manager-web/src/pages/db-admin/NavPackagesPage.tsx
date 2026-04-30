import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DbAdminLayout } from '../../components/Layout'
import { listNavPackages, getNavPackageJson, deleteNavPackage } from '../../api/dbAdmin'

interface NavPackage {
  id: string
  buildingId: string
  buildingName: string
  version: number
  checksum: string
  isCurrent: boolean
  generatedAt: string
}

export function NavPackagesPage() {
  const queryClient = useQueryClient()
  const [viewingId, setViewingId]         = useState<string | null>(null)
  const [viewingJson, setViewingJson]     = useState<string | null>(null)
  const [jsonLoading, setJsonLoading]     = useState(false)
  const [deleteId, setDeleteId]           = useState<string | null>(null)
  const [copied, setCopied]               = useState(false)

  const { data: packages = [], isLoading, error } = useQuery<NavPackage[]>({
    queryKey: ['db-admin-nav-packages'],
    queryFn: listNavPackages,
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteNavPackage(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['db-admin-nav-packages'] })
      queryClient.invalidateQueries({ queryKey: ['db-admin-stats'] })
      setDeleteId(null)
    },
  })

  const handleViewJson = async (id: string) => {
    setViewingId(id)
    setViewingJson(null)
    setJsonLoading(true)
    try {
      const data = await getNavPackageJson(id)
      setViewingJson(JSON.stringify(data, null, 2))
    } catch {
      setViewingJson('Error loading JSON.')
    } finally {
      setJsonLoading(false)
    }
  }

  const handleCopy = () => {
    if (!viewingJson) return
    navigator.clipboard.writeText(viewingJson).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  return (
    <DbAdminLayout>
      <div className="max-w-6xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-white">Navigation Packages</h1>
          <p className="text-gray-400 text-sm mt-0.5">
            Raw nav JSON is only accessible here. Click <span className="text-blue-400">View JSON</span> to inspect.
          </p>
        </div>

        {isLoading && <p className="text-gray-400">Loading...</p>}
        {error && (
          <div className="bg-red-900/40 border border-red-800 text-red-400 p-4 rounded-lg">
            Failed to load nav packages.
          </div>
        )}

        {!isLoading && packages.length === 0 && (
          <p className="text-gray-500 text-center py-12">
            No nav packages yet. Publish a building to generate one.
          </p>
        )}

        {packages.length > 0 && (
          <div className="bg-gray-800 border border-gray-700 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-700 text-gray-400 text-xs uppercase tracking-wide">
                  <th className="text-left px-5 py-3">Building</th>
                  <th className="text-left px-5 py-3">Version</th>
                  <th className="text-left px-5 py-3">Current</th>
                  <th className="text-left px-5 py-3">Checksum</th>
                  <th className="text-left px-5 py-3">Generated</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {packages.map((pkg) => (
                  <tr key={pkg.id} className="hover:bg-gray-750 transition-colors">
                    <td className="px-5 py-3 text-white font-medium">{pkg.buildingName}</td>
                    <td className="px-5 py-3 text-gray-300">v{pkg.version}</td>
                    <td className="px-5 py-3">
                      {pkg.isCurrent ? (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-green-900/50 text-green-400 border border-green-800">
                          current
                        </span>
                      ) : (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-gray-700 text-gray-500 border border-gray-600">
                          superseded
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500" title={pkg.checksum}>
                      {pkg.checksum.slice(0, 16)}…
                    </td>
                    <td className="px-5 py-3 text-gray-400 text-xs">
                      {new Date(pkg.generatedAt).toLocaleString()}
                    </td>
                    <td className="px-5 py-3 text-right">
                      <div className="flex gap-2 justify-end">
                        <button
                          onClick={() => handleViewJson(pkg.id)}
                          className="text-blue-400 hover:text-blue-300 text-xs px-2 py-1 rounded border border-blue-800 hover:border-blue-600 transition-colors"
                        >
                          View JSON
                        </button>
                        <button
                          onClick={() => setDeleteId(pkg.id)}
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

      {/* JSON Viewer modal */}
      {viewingId && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-900 border border-gray-700 rounded-lg w-full max-w-4xl max-h-[85vh] flex flex-col shadow-2xl">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-700">
              <h2 className="text-lg font-semibold text-white">
                Navigation Package JSON
                <span className="ml-2 font-mono text-xs text-gray-500">{viewingId.slice(0, 8)}…</span>
              </h2>
              <div className="flex items-center gap-3">
                {viewingJson && (
                  <button
                    onClick={handleCopy}
                    className="text-xs px-3 py-1 bg-gray-700 hover:bg-gray-600 text-gray-300 rounded border border-gray-600 transition-colors"
                  >
                    {copied ? '✅ Copied!' : '📋 Copy'}
                  </button>
                )}
                <button
                  onClick={() => { setViewingId(null); setViewingJson(null) }}
                  className="text-gray-400 hover:text-white text-xl leading-none"
                >
                  ×
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-auto p-4">
              {jsonLoading && (
                <p className="text-gray-400 text-center py-8">Loading JSON...</p>
              )}
              {viewingJson && !jsonLoading && (
                <pre className="text-xs text-green-300 font-mono whitespace-pre-wrap break-words leading-relaxed">
                  {viewingJson}
                </pre>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Delete confirm */}
      {deleteId && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 border border-gray-700 rounded-lg w-full max-w-md shadow-2xl">
            <div className="px-6 py-4 border-b border-gray-700">
              <h2 className="text-lg font-semibold text-white">Delete Nav Package</h2>
            </div>
            <div className="p-6">
              <p className="text-gray-300 mb-6">
                This nav package record will be deleted from the database. Mobile users who
                have cached it will continue to use their local copy until the building is
                re-published and they re-scan the QR code.
              </p>
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
                  {deleteMut.isPending ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </DbAdminLayout>
  )
}
