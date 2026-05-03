import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Layout } from '../../components/Layout'
import { listBuildings, createBuilding, deleteBuilding, type Building } from '../../api/buildings'

export function DashboardPage() {
  const navigate      = useNavigate()
  const queryClient   = useQueryClient()
  const [showCreate, setShowCreate]   = useState(false)
  const [newName, setNewName]         = useState('')
  const [newDesc, setNewDesc]         = useState('')
  const [newAddr, setNewAddr]         = useState('')
  const [deleteId, setDeleteId]       = useState<string | null>(null)

  const { data: buildings = [], isLoading } = useQuery({
    queryKey: ['buildings'],
    queryFn: listBuildings,
  })

  const createMut = useMutation({
    mutationFn: () => createBuilding({ name: newName, description: newDesc || undefined, address: newAddr || undefined }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buildings'] })
      setShowCreate(false); setNewName(''); setNewDesc(''); setNewAddr('')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteBuilding(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['buildings'] }); setDeleteId(null) },
  })

  return (
    <Layout>
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold">Buildings</h1>
          <button
            onClick={() => setShowCreate(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 font-medium transition-colors"
          >
            + New Building
          </button>
        </div>

        {isLoading && <p className="text-gray-500">Loading...</p>}

        {buildings.length === 0 && !isLoading && (
          <div className="text-center py-16 text-gray-400">
            <p className="text-lg mb-2">No buildings yet</p>
            <p className="text-sm">Create your first building to get started.</p>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {buildings.map((b) => (
            <BuildingCard
              key={b.id}
              building={b}
              onOpen={() => navigate(`/buildings/${b.id}`)}
              onDelete={() => setDeleteId(b.id)}
            />
          ))}
        </div>
      </div>

      {/* Create modal */}
      {showCreate && (
        <Modal title="New Building" onClose={() => setShowCreate(false)}>
          <form
            onSubmit={(e) => { e.preventDefault(); createMut.mutate() }}
            className="space-y-4"
          >
            <div>
              <label className="block text-sm font-medium mb-1">Name *</label>
              <input
                required value={newName} onChange={e => setNewName(e.target.value)}
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g. Main Campus Block A"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Description</label>
              <textarea
                value={newDesc} onChange={e => setNewDesc(e.target.value)} rows={2}
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Address</label>
              <input
                value={newAddr} onChange={e => setNewAddr(e.target.value)}
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex gap-2 justify-end">
              <button type="button" onClick={() => setShowCreate(false)} className="px-4 py-2 border rounded hover:bg-gray-50">Cancel</button>
              <button type="submit" disabled={createMut.isPending || !newName.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
                {createMut.isPending ? 'Creating...' : 'Create'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Delete confirm */}
      {deleteId && (
        <Modal title="Delete Building" onClose={() => setDeleteId(null)}>
          <p className="text-gray-600 mb-4">This will permanently delete the building and all its floors, nodes, and edges.</p>
          <div className="flex gap-2 justify-end">
            <button onClick={() => setDeleteId(null)} className="px-4 py-2 border rounded hover:bg-gray-50">Cancel</button>
            <button onClick={() => deleteMut.mutate(deleteId)} disabled={deleteMut.isPending}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50">
              {deleteMut.isPending ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </Modal>
      )}
    </Layout>
  )
}

function BuildingCard({ building, onOpen, onDelete }: {
  building: Building; onOpen: () => void; onDelete: () => void
}) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between mb-2">
        <h3 className="font-semibold text-gray-900">{building.name}</h3>
        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
          building.status === 'published' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
        }`}>
          {building.status}
        </span>
      </div>
      {building.description && <p className="text-sm text-gray-500 mb-3 line-clamp-2">{building.description}</p>}
      <p className="text-xs text-gray-400 mb-4">
        {building.floors?.length ?? 0} floor{(building.floors?.length ?? 0) !== 1 ? 's' : ''}
      </p>
      <div className="flex gap-2">
        <button onClick={onOpen}
          className="flex-1 text-sm bg-blue-600 text-white py-1.5 rounded hover:bg-blue-700 transition-colors">
          Open
        </button>
        <button onClick={onDelete}
          className="text-sm px-3 py-1.5 border border-red-200 text-red-600 rounded hover:bg-red-50 transition-colors">
          Delete
        </button>
      </div>
    </div>
  )
}

function Modal({ title, onClose, children }: {
  title: string; onClose: () => void; children: React.ReactNode
}) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}
