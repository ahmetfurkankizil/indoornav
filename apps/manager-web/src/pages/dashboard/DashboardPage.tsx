import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Layout } from '../../components/Layout'
import { listBuildings, createBuilding, deleteBuilding, type Building } from '../../api/buildings'

type FilterTab = 'all' | 'published' | 'draft'

export function DashboardPage() {
  const navigate      = useNavigate()
  const queryClient   = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [newName, setNewName]       = useState('')
  const [newDesc, setNewDesc]       = useState('')
  const [newAddr, setNewAddr]       = useState('')
  const [deleteId, setDeleteId]     = useState<string | null>(null)
  const [search, setSearch]         = useState('')
  const [filter, setFilter]         = useState<FilterTab>('all')

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

  const stats = useMemo(() => ({
    total:     buildings.length,
    published: buildings.filter(b => b.status === 'published').length,
    draft:     buildings.filter(b => b.status === 'draft').length,
    floors:    buildings.reduce((sum, b) => sum + (b.floors?.length ?? 0), 0),
  }), [buildings])

  const filtered = useMemo(() =>
    buildings
      .filter(b => filter === 'all' || b.status === filter)
      .filter(b => !search ||
        b.name.toLowerCase().includes(search.toLowerCase()) ||
        (b.address ?? '').toLowerCase().includes(search.toLowerCase())
      ),
    [buildings, filter, search]
  )

  return (
    <Layout>
      <div className="max-w-6xl mx-auto space-y-6">

        {/* Page header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Buildings</h1>
            <p className="text-sm text-gray-500 mt-0.5">Manage your indoor navigation buildings and floor maps</p>
          </div>
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 font-medium transition-colors shadow-sm text-sm"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            New Building
          </button>
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard icon="🏢" label="Total Buildings" value={stats.total}     color="blue" />
          <StatCard icon="✅" label="Published"       value={stats.published} color="green" />
          <StatCard icon="📝" label="Draft"           value={stats.draft}     color="amber" />
          <StatCard icon="📐" label="Total Floors"    value={stats.floors}    color="purple" />
        </div>

        {/* Filters + search */}
        <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
          <div className="flex bg-gray-100 rounded-lg p-1 gap-1 flex-shrink-0">
            {(['all', 'published', 'draft'] as FilterTab[]).map(tab => (
              <button
                key={tab}
                onClick={() => setFilter(tab)}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-all capitalize ${
                  filter === tab
                    ? 'bg-white shadow text-gray-900'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                {tab}
                {tab !== 'all' && (
                  <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
                    tab === 'published' ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                  }`}>
                    {tab === 'published' ? stats.published : stats.draft}
                  </span>
                )}
              </button>
            ))}
          </div>

          <div className="relative flex-1 max-w-sm">
            <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
            </svg>
            <input
              type="text"
              placeholder="Search buildings..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            {search && (
              <button onClick={() => setSearch('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-lg leading-none">
                ×
              </button>
            )}
          </div>
        </div>

        {/* Loading state */}
        {isLoading && (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mb-3" />
            <p className="text-sm text-gray-500">Loading buildings…</p>
          </div>
        )}

        {/* Empty state */}
        {!isLoading && filtered.length === 0 && (
          <div className="text-center py-20 bg-white rounded-xl border border-dashed border-gray-200">
            <div className="text-5xl mb-4">{search ? '🔍' : '🏗️'}</div>
            <h3 className="text-lg font-semibold text-gray-700 mb-1">
              {search ? `No results for "${search}"` : 'No buildings yet'}
            </h3>
            <p className="text-sm text-gray-400 mb-6 max-w-sm mx-auto">
              {search
                ? 'Try a different name or clear the search filter.'
                : 'Create your first building to start mapping floors and navigation paths.'}
            </p>
            {!search && (
              <button
                onClick={() => setShowCreate(true)}
                className="px-5 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm font-medium transition-colors"
              >
                Create your first building
              </button>
            )}
          </div>
        )}

        {/* Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(b => (
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
          <form onSubmit={e => { e.preventDefault(); createMut.mutate() }} className="space-y-4">
            <FormField label="Building Name" required>
              <input
                required value={newName} onChange={e => setNewName(e.target.value)}
                placeholder="e.g. Main Campus Block A"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </FormField>
            <FormField label="Description">
              <textarea
                value={newDesc} onChange={e => setNewDesc(e.target.value)} rows={2}
                placeholder="Brief description of this building…"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
            </FormField>
            <FormField label="Address">
              <input
                value={newAddr} onChange={e => setNewAddr(e.target.value)}
                placeholder="e.g. 123 Main St, City"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </FormField>
            <div className="flex gap-2 justify-end pt-2 border-t border-gray-100">
              <button type="button" onClick={() => setShowCreate(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                Cancel
              </button>
              <button type="submit" disabled={createMut.isPending || !newName.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors">
                {createMut.isPending ? 'Creating…' : 'Create Building'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Delete confirm */}
      {deleteId && (
        <Modal title="Delete Building" onClose={() => setDeleteId(null)}>
          <div className="flex gap-3 mb-5">
            <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg className="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
              </svg>
            </div>
            <div>
              <p className="font-semibold text-gray-900">Delete this building?</p>
              <p className="text-sm text-gray-500 mt-0.5">All floors, nodes, and edges will be permanently removed. This cannot be undone.</p>
            </div>
          </div>
          <div className="flex gap-2 justify-end">
            <button onClick={() => setDeleteId(null)}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button onClick={() => deleteMut.mutate(deleteId!)} disabled={deleteMut.isPending}
              className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors">
              {deleteMut.isPending ? 'Deleting…' : 'Delete Building'}
            </button>
          </div>
        </Modal>
      )}
    </Layout>
  )
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function StatCard({
  icon, label, value, color,
}: { icon: string; label: string; value: number; color: 'blue' | 'green' | 'amber' | 'purple' }) {
  const palette = {
    blue:   'bg-blue-50 border-blue-100 text-blue-700',
    green:  'bg-green-50 border-green-100 text-green-700',
    amber:  'bg-amber-50 border-amber-100 text-amber-700',
    purple: 'bg-purple-50 border-purple-100 text-purple-700',
  }
  return (
    <div className={`${palette[color]} border rounded-xl p-4 flex items-center gap-3`}>
      <span className="text-2xl leading-none">{icon}</span>
      <div>
        <p className="text-2xl font-bold leading-none">{value}</p>
        <p className="text-xs text-gray-500 mt-0.5">{label}</p>
      </div>
    </div>
  )
}

function BuildingCard({ building, onOpen, onDelete }: {
  building: Building; onOpen: () => void; onDelete: () => void
}) {
  const floorCount  = building.floors?.length ?? 0
  const isPublished = building.status === 'published'

  return (
    <div className="bg-white border border-gray-200 rounded-xl overflow-hidden hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 group">
      {/* Status band */}
      <div className={`h-1.5 ${isPublished
        ? 'bg-gradient-to-r from-green-400 to-emerald-500'
        : 'bg-gradient-to-r from-amber-400 to-yellow-400'
      }`} />

      <div className="p-5">
        {/* Header row */}
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2 min-w-0">
            <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg className="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
            <h3 className="font-semibold text-gray-900 truncate group-hover:text-blue-600 transition-colors">
              {building.name}
            </h3>
          </div>
          <span className={`text-xs px-2 py-1 rounded-full font-medium flex-shrink-0 ml-2 ${
            isPublished ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
          }`}>
            {isPublished ? '● Live' : '○ Draft'}
          </span>
        </div>

        {/* Description */}
        {building.description && (
          <p className="text-sm text-gray-500 mb-3 line-clamp-2 leading-relaxed">{building.description}</p>
        )}

        {/* Address */}
        {building.address && (
          <p className="text-xs text-gray-400 mb-3 flex items-center gap-1.5 truncate">
            <svg className="w-3 h-3 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {building.address}
          </p>
        )}

        {/* Meta */}
        <div className="flex items-center gap-4 text-xs text-gray-400 pb-3 mb-3 border-b border-gray-50">
          <span className="flex items-center gap-1">
            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2 2v0" />
            </svg>
            {floorCount} floor{floorCount !== 1 ? 's' : ''}
          </span>
          {building.createdAt && (
            <span className="flex items-center gap-1">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              {new Date(building.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
            </span>
          )}
        </div>

        {/* Actions */}
        <div className="flex gap-2">
          <button onClick={onOpen}
            className="flex-1 flex items-center justify-center gap-1.5 text-sm bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition-colors font-medium">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Open Editor
          </button>
          <button onClick={onDelete} title="Delete building"
            className="px-3 py-2 border border-gray-200 text-gray-400 rounded-lg hover:border-red-200 hover:text-red-500 hover:bg-red-50 transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  )
}

function FormField({ label, required, children }: {
  label: string; required?: boolean; children: React.ReactNode
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
    </div>
  )
}

function Modal({ title, onClose, children }: {
  title: string; onClose: () => void; children: React.ReactNode
}) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
          <button onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors text-xl leading-none">
            ×
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}
