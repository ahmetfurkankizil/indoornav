import { useState, useRef, useEffect } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Layout } from '../../components/Layout'
import {
  getBuilding, updateBuilding, createFloor, deleteFloor, publishBuilding,
  listConnections, createConnection, deleteConnection,
  replaceFloor, listBuildingNodes,
  type Floor, type FloorConnection,
} from '../../api/buildings'
import { listNodes, type Node } from '../../api/mapEditor'

export function BuildingDetailPage() {
  const { id: buildingId } = useParams<{ id: string }>()
  const navigate      = useNavigate()
  const queryClient   = useQueryClient()
  const fileRef       = useRef<HTMLInputElement>(null)

  const [showAddFloor, setShowAddFloor] = useState(false)
  const [floorName, setFloorName]       = useState('')
  const [floorNumber, setFloorNumber]   = useState(0)
  const [mapFile, setMapFile]           = useState<File | null>(null)
  const [replaceFloorId, setReplaceFloorId] = useState<string | null>(null)
  const [replaceFile, setReplaceFile]   = useState<File | null>(null)
  const [showQr, setShowQr]             = useState(false)
  const [connFrom, setConnFrom]         = useState('')
  const [connTo, setConnTo]             = useState('')
  const [connType, setConnType]         = useState('elevator')
  const [errorPopup, setErrorPopup]     = useState<string | null>(null)

  const { data: building, isLoading } = useQuery({
    queryKey: ['building', buildingId],
    queryFn: () => getBuilding(buildingId!),
    enabled: !!buildingId,
  })

  const { data: connections = [] } = useQuery({
    queryKey: ['connections', buildingId],
    queryFn: () => listConnections(buildingId!),
    enabled: !!buildingId,
  })
  
  const { data: nodes = [] } = useQuery<Node[]>({
    queryKey: ['buildingNodes', buildingId],
    queryFn: () => listBuildingNodes(buildingId!),
    enabled: !!buildingId,
  })

  // Auto-detect connection type
  useEffect(() => {
    const fromNode = nodes.find(n => n.id === connFrom)
    const toNode = nodes.find(n => n.id === connTo)
    if (fromNode && toNode && fromNode.nodeType === toNode.nodeType) {
      setConnType(fromNode.nodeType)
    }
  }, [connFrom, connTo, nodes])

  const addFloorMut = useMutation({
    mutationFn: () => {
      const form = new FormData()
      form.append('floorNumber', String(floorNumber))
      form.append('floorName', floorName)
      form.append('mapFile', mapFile!)
      return createFloor(buildingId!, form)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['building', buildingId] })
      setShowAddFloor(false); setFloorName(''); setMapFile(null)
    },
  })

  const replaceFloorMut = useMutation({
    mutationFn: () => {
      const form = new FormData()
      form.append('mapFile', replaceFile!)
      return replaceFloor(buildingId!, replaceFloorId!, form)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['building', buildingId] })
      setReplaceFloorId(null); setReplaceFile(null)
    },
  })

  const deleteFloorMut = useMutation({
    mutationFn: (floorId: string) => deleteFloor(buildingId!, floorId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['building', buildingId] }),
  })

  const publishMut = useMutation({
    mutationFn: () => publishBuilding(buildingId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['building', buildingId] }),
  })

  const updateBuildingMut = useMutation({
    mutationFn: (data: { widthMeters: number }) => updateBuilding(buildingId!, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['building', buildingId] }),
  })

  const addConnMut = useMutation({
    mutationFn: () => {
      const fromNode = nodes.find(n => n.id === connFrom)
      const toNode = nodes.find(n => n.id === connTo)
      
      if (!fromNode || !toNode) throw new Error("Nodes not found")
      
      if (connFrom === connTo) {
        setErrorPopup("You cannot connect a node to itself. Please select two different nodes, typically on different floors.")
        return Promise.reject("Same node")
      }
      
      if (fromNode.nodeType !== toNode.nodeType) {
        setErrorPopup(`You cannot connect different types of nodes: "${fromNode.label}" is a ${fromNode.nodeType}, while "${toNode.label}" is a ${toNode.nodeType}.`)
        return Promise.reject("Type mismatch")
      }
      
      if (connType !== fromNode.nodeType) {
        setErrorPopup(`Selected connection type (${connType}) does not match the nodes' type (${fromNode.nodeType}). Please select the correct type.`)
        return Promise.reject("Type mismatch with connection")
      }

      return createConnection(buildingId!, { fromNodeId: connFrom, toNodeId: connTo, connectionType: connType })
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['connections', buildingId] }); setConnFrom(''); setConnTo('') },
  })

  const delConnMut = useMutation({
    mutationFn: (id: string) => deleteConnection(buildingId!, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['connections', buildingId] }),
  })

  if (isLoading) return <Layout><p className="text-gray-500">Loading...</p></Layout>
  if (!building) return <Layout><p className="text-red-500">Building not found.</p></Layout>

  const floors = building.floors ?? []

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* Breadcrumb */}
        <nav className="text-sm text-gray-500 mb-4">
          <Link to="/dashboard" className="hover:text-blue-600">Buildings</Link>
          <span className="mx-2">/</span>
          <span>{building.name}</span>
        </nav>

        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold">{building.name}</h1>
            {building.description && <p className="text-gray-500 mt-1">{building.description}</p>}
          </div>
          <div className="flex items-center gap-2">
            <span className={`text-sm px-3 py-1 rounded-full font-medium ${
              building.status === 'published' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
            }`}>{building.status}</span>
            <button type="button" onClick={() => setShowQr(true)}
              className="text-sm px-3 py-2 border rounded-lg hover:bg-gray-50">
              View QR
            </button>
            <button
              onClick={() => publishMut.mutate()}
              disabled={publishMut.isPending || floors.length === 0}
              className="text-sm px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {publishMut.isPending ? 'Publishing...' : 'Publish'}
            </button>
          </div>
        </div>

        {/* Building Settings */}
        <section className="mb-8 bg-blue-50 border border-blue-100 rounded-xl p-6">
          <h2 className="text-lg font-bold text-blue-900 mb-4 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
            Building Scale & Calibration
          </h2>
          <div className="flex items-end gap-4">
            <div className="flex-1">
              <label className="block text-sm font-semibold text-blue-800 mb-1.5">Real World Width (Meters)</label>
              <div className="relative">
                <input 
                  type="number" 
                  step="0.1"
                  defaultValue={building.widthMeters}
                  onBlur={(e) => {
                    const val = parseFloat(e.target.value)
                    if (val > 0 && val !== building.widthMeters) {
                      updateBuildingMut.mutate({ widthMeters: val })
                    }
                  }}
                  className="w-full pl-3 pr-10 py-2.5 bg-white border border-blue-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all"
                  placeholder="e.g. 45.0"
                />
                <div className="absolute right-3 top-1/2 -translate-y-1/2 text-blue-400 font-medium text-sm">m</div>
              </div>
              <p className="mt-2 text-xs text-blue-600/70 italic">
                Measure the full width of your floor plan in the real world. This ensures AR arrows align perfectly with corridors.
              </p>
            </div>
            <div className="pb-8">
              {updateBuildingMut.isPending && <span className="text-xs text-blue-500 animate-pulse font-medium">Saving...</span>}
            </div>
          </div>
        </section>

        {/* Floors */}
        <section className="mb-8">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-lg font-semibold">Floors</h2>
            <button onClick={() => setShowAddFloor(true)}
              className="text-sm px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700">
              + Add Floor
            </button>
          </div>

          {floors.length === 0 && (
            <p className="text-gray-400 text-sm py-4">No floors yet. Upload a map file (GLB, DXF, SVG, or PNG) to get started.</p>
          )}

          <div className="space-y-2">
            {floors.sort((a, b) => a.floorNumber - b.floorNumber).map((floor) => (
              <FloorRow
                key={floor.id}
                floor={floor}
                onEdit={() => navigate(`/buildings/${buildingId}/floors/${floor.id}/edit`)}
                onReplace={() => setReplaceFloorId(floor.id)}
                onDelete={() => deleteFloorMut.mutate(floor.id)}
              />
            ))}
          </div>
        </section>

        {/* Cross-floor connections */}
        <section>
          <h2 className="text-lg font-semibold mb-3">Cross-floor Connections</h2>
          <div className="bg-white border rounded-lg p-4 mb-3">
            <div className="flex gap-4 flex-wrap items-end">
              <div className="flex-1 min-w-[200px]">
                <label className="block text-xs font-medium mb-1 text-gray-600">From Node</label>
                <select 
                  value={connFrom} 
                  onChange={e => setConnFrom(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none bg-white" 
                >
                  <option value="">Select From Node...</option>
                  {nodes.filter(n => n.nodeType === 'elevator' || n.nodeType === 'stairs').sort((a, b) => {
                    const fA = building.floors?.find(f => f.id === a.floorId)
                    const fB = building.floors?.find(f => f.id === b.floorId)
                    if (fA?.floorNumber !== fB?.floorNumber) return (fA?.floorNumber ?? 0) - (fB?.floorNumber ?? 0)
                    return (a.label || "").localeCompare(b.label || "")
                  }).map(n => {
                    const floor = building.floors?.find(f => f.id === n.floorId)
                    return (
                      <option key={n.id} value={n.id}>
                        {n.label || 'Unnamed Node'} ({floor?.floorName || 'Unknown Floor'})
                      </option>
                    )
                  })}
                </select>
              </div>
              <div className="flex-1 min-w-[200px]">
                <label className="block text-xs font-medium mb-1 text-gray-600">To Node</label>
                <select 
                  value={connTo} 
                  onChange={e => setConnTo(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none bg-white" 
                >
                  <option value="">Select To Node...</option>
                  {nodes.filter(n => n.nodeType === 'elevator' || n.nodeType === 'stairs').sort((a, b) => {
                    const fA = building.floors?.find(f => f.id === a.floorId)
                    const fB = building.floors?.find(f => f.id === b.floorId)
                    if (fA?.floorNumber !== fB?.floorNumber) return (fA?.floorNumber ?? 0) - (fB?.floorNumber ?? 0)
                    return (a.label || "").localeCompare(b.label || "")
                  }).map(n => {
                    const floor = building.floors?.find(f => f.id === n.floorId)
                    return (
                      <option key={n.id} value={n.id}>
                        {n.label || 'Unnamed Node'} ({floor?.floorName || 'Unknown Floor'})
                      </option>
                    )
                  })}
                </select>
              </div>
              
              <div>
                <label className="block text-xs font-medium mb-1 text-gray-600">Type</label>
                <select value={connType} onChange={e => setConnType(e.target.value)}
                  className="border rounded-lg px-3 py-2 text-sm bg-white focus:ring-2 focus:ring-blue-500 outline-none">
                  <option value="elevator">Elevator</option>
                  <option value="stairs">Stairs</option>
                </select>
              </div>
              <button onClick={() => addConnMut.mutate()}
                disabled={!connFrom || !connTo || addConnMut.isPending}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors">
                Add Connection
              </button>
            </div>
          </div>

          {connections.length > 0 && (
            <div className="grid gap-2">
              {connections.map((c) => {
                const fromNode = nodes.find(n => n.id === c.fromNodeId)
                const toNode = nodes.find(n => n.id === c.toNodeId)
                const fromFloor = building.floors?.find(f => f.id === fromNode?.floorId)
                const toFloor = building.floors?.find(f => f.id === toNode?.floorId)

                return (
                  <div key={c.id} className="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-5 py-3 text-sm shadow-sm hover:shadow-md transition-shadow">
                    <div className="flex-1 grid grid-cols-[1fr,auto,1fr] items-center gap-4">
                      <div className="flex flex-col">
                        <span className="font-semibold text-gray-900">{fromNode?.label || 'Unknown'}</span>
                        <span className="text-[10px] uppercase tracking-wider text-gray-400 font-bold">{fromFloor?.floorName || 'Unknown Floor'}</span>
                      </div>
                      
                      <div className="flex flex-col items-center">
                        <svg className="w-5 h-5 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                        <span className="text-[10px] font-bold text-blue-500 bg-blue-50 px-1.5 py-0.5 rounded mt-1">{c.connectionType}</span>
                      </div>

                      <div className="flex flex-col text-right">
                        <span className="font-semibold text-gray-900">{toNode?.label || 'Unknown'}</span>
                        <span className="text-[10px] uppercase tracking-wider text-gray-400 font-bold">{toFloor?.floorName || 'Unknown Floor'}</span>
                      </div>
                    </div>

                    <button onClick={() => delConnMut.mutate(c.id)}
                      className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                      title="Remove connection">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                    </button>
                  </div>
                )
              })}
            </div>
          )}
        </section>
      </div>

      {/* Add floor modal */}
      {showAddFloor && (
        <Modal title="Add Floor" onClose={() => setShowAddFloor(false)}>
          <form onSubmit={e => { e.preventDefault(); addFloorMut.mutate() }} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Floor Number</label>
              <input type="number" required value={floorNumber} onChange={e => setFloorNumber(Number(e.target.value))}
                className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
              <p className="text-xs text-gray-400 mt-1">Use 0 for ground, 1 for first floor, -1 for basement.</p>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Floor Name</label>
              <input required value={floorName} onChange={e => setFloorName(e.target.value)}
                placeholder="e.g. Ground Floor" className="w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Map File</label>
              <input type="file" accept=".glb,.dxf,.svg,.png" required onChange={e => setMapFile(e.target.files?.[0] ?? null)}
                className="w-full text-sm" />
              <p className="text-xs text-gray-400 mt-1">Supported formats: GLB (3D scan), DXF, SVG, PNG</p>
            </div>
            <div className="flex gap-2 justify-end">
              <button type="button" onClick={() => setShowAddFloor(false)} className="px-4 py-2 border rounded hover:bg-gray-50">Cancel</button>
              <button type="submit" disabled={addFloorMut.isPending || !mapFile}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
                {addFloorMut.isPending ? 'Uploading...' : 'Upload'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Replace map modal */}
      {replaceFloorId && (
        <Modal title="Replace Map File" onClose={() => setReplaceFloorId(null)}>
          <p className="text-sm text-gray-600 mb-4">Uploading a new map file will reset the floor bounds. You will need to re-open the map editor to re-extract them.</p>
          <input type="file" accept=".glb,.dxf,.svg,.png" onChange={e => setReplaceFile(e.target.files?.[0] ?? null)} className="w-full text-sm mb-2" />
          <p className="text-xs text-gray-400 mb-4">Supported formats: GLB (3D scan), DXF, SVG, PNG</p>
          <div className="flex gap-2 justify-end">
            <button onClick={() => setReplaceFloorId(null)} className="px-4 py-2 border rounded hover:bg-gray-50">Cancel</button>
            <button onClick={() => replaceFloorMut.mutate()} disabled={!replaceFile || replaceFloorMut.isPending}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
              {replaceFloorMut.isPending ? 'Uploading...' : 'Replace'}
            </button>
          </div>
        </Modal>
      )}

      {/* QR modal */}
      {showQr && (
        <QrModal buildingId={buildingId!} onClose={() => setShowQr(false)} />
      )}

      {/* Error Popup */}
      {errorPopup && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[100] p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-sm w-full p-6 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center gap-3 text-red-600 mb-4">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
              <h3 className="text-lg font-bold">Invalid Connection</h3>
            </div>
            <p className="text-gray-600 text-sm mb-6 leading-relaxed">
              {errorPopup}
            </p>
            <button
              onClick={() => setErrorPopup(null)}
              className="w-full bg-red-600 text-white py-2.5 rounded-lg font-medium hover:bg-red-700 transition-colors shadow-sm"
            >
              Understood
            </button>
          </div>
        </div>
      )}
    </Layout>
  )
}

function QrModal({ buildingId, onClose }: { buildingId: string; onClose: () => void }) {
  return (
    <Modal title="Building QR Code" onClose={onClose}>
      <div className="flex flex-col items-center gap-4 py-4">
        <div className="w-48 h-48 border-2 border-dashed border-gray-200 rounded-xl flex items-center justify-center text-gray-400 text-sm text-center p-4">
          QR code generation requires the backend server
        </div>
        <p className="text-xs text-gray-400">Building ID: {buildingId}</p>
      </div>
    </Modal>
  )
}

function FloorRow({ floor, onEdit, onReplace, onDelete }: {
  floor: Floor; onEdit: () => void; onReplace: () => void; onDelete: () => void
}) {
  return (
    <div className="flex items-center justify-between bg-white border rounded-lg px-4 py-3">
      <div>
        <span className="font-medium">{floor.floorName}</span>
        <span className="ml-2 text-sm text-gray-400">Floor {floor.floorNumber}</span>
        {!floor.boundsMinX && (
          <span className="ml-2 text-xs text-orange-500 bg-orange-50 px-1.5 py-0.5 rounded">
            Open editor to initialize bounds
          </span>
        )}
      </div>
      <div className="flex gap-2">
        <button onClick={onEdit}
          className="text-sm px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700">
          Edit Map
        </button>
        <button onClick={onReplace}
          className="text-sm px-3 py-1.5 border rounded hover:bg-gray-50">
          Replace Map
        </button>
        <button onClick={onDelete}
          className="text-sm px-3 py-1.5 border border-red-200 text-red-600 rounded hover:bg-red-50">
          Delete
        </button>
      </div>
    </div>
  )
}

function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}
