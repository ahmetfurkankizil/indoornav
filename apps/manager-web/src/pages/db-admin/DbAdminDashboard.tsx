import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DbAdminLayout } from '../../components/Layout'
import { getStats } from '../../api/dbAdmin'

interface Stat { label: string; value: number; icon: string; color: string }

function StatCard({ stat }: { stat: Stat }) {
  return (
    <div className="bg-gray-800 border border-gray-700 rounded-lg p-5">
      <div className="flex items-center gap-3">
        <span className="text-3xl">{stat.icon}</span>
        <div>
          <p className="text-2xl font-bold text-white">{stat.value.toLocaleString()}</p>
          <p className="text-sm text-gray-400">{stat.label}</p>
        </div>
      </div>
    </div>
  )
}

export function DbAdminDashboard() {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['db-admin-stats'],
    queryFn: getStats,
    refetchInterval: 30_000,
  })

  const statCards: Stat[] = stats
    ? [
        { label: 'Managers',       value: stats.managerCount,    icon: '👤', color: 'blue' },
        { label: 'Buildings',      value: stats.buildingCount,   icon: '🏢', color: 'purple' },
        { label: 'Floors',         value: stats.floorCount,      icon: '🏗️', color: 'indigo' },
        { label: 'Nodes',          value: stats.nodeCount,       icon: '📍', color: 'yellow' },
        { label: 'Edges',          value: stats.edgeCount,       icon: '🔗', color: 'orange' },
        { label: 'Nav Packages',   value: stats.navPackageCount, icon: '📦', color: 'green' },
      ]
    : []

  return (
    <DbAdminLayout>
      <div className="max-w-5xl mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white">System Overview</h1>
          <p className="text-gray-400 text-sm mt-1">Live statistics — refreshes every 30 s</p>
        </div>

        {isLoading && (
          <div className="text-gray-400 py-12 text-center">Loading stats...</div>
        )}
        {error && (
          <div className="bg-red-900/40 border border-red-800 text-red-400 p-4 rounded-lg">
            Failed to load stats. Is the backend running?
          </div>
        )}

        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {statCards.map((s) => <StatCard key={s.label} stat={s} />)}
          </div>
        )}

        <div className="mt-10 bg-gray-800 border border-gray-700 rounded-lg p-5">
          <h2 className="text-lg font-semibold text-white mb-3">Quick Links</h2>
          <ul className="space-y-2 text-sm text-gray-300">
            <li>→ <a href="/db-admin/managers"     className="text-blue-400 hover:underline">Manage Managers</a></li>
            <li>→ <a href="/db-admin/buildings"    className="text-blue-400 hover:underline">Manage Buildings</a></li>
            <li>→ <a href="/db-admin/nav-packages" className="text-blue-400 hover:underline">Navigation Packages &amp; JSON Viewer</a></li>
          </ul>
        </div>

        <div className="mt-6 bg-yellow-900/30 border border-yellow-700 rounded-lg p-4 text-sm text-yellow-300">
          ⚠️ This panel has full system access. Actions here are irreversible. Use with caution.
        </div>
      </div>
    </DbAdminLayout>
  )
}
