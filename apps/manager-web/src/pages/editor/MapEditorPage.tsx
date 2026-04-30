import React, { useEffect, useRef, useCallback, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Stage, Layer, Image as KonvaImage, Circle, Line, Text } from 'react-konva'
import { useQuery } from '@tanstack/react-query'
import { useEditorStore, type EditorMode } from '../../stores/editorStore'
import { renderGlbTopDown } from '../../three/GlbRenderer'
import {
  listNodes, listEdges, createNode, updateNode, deleteNode,
  createEdge, updateEdge, deleteEdge, aiSuggestEdges,
  type Node, type Edge, type SuggestedEdge,
} from '../../api/mapEditor'
import { getBuilding, updateFloorBounds, getGlbUrl } from '../../api/buildings'

// ── Constants ──────────────────────────────────────────────────────────────────

const NODE_COLORS: Record<string, string> = {
  room:        '#3b82f6',
  corridor:    '#6b7280',
  entrance:    '#22c55e',
  elevator:    '#a855f7',
  stairs:         '#f97316',
  exit:           '#ef4444',
  'turning point': '#eab308',
}

const CANVAS_SIZE = 800

function norm2px(v: number) { return v * CANVAS_SIZE }

// ── Sidebar sub-components ─────────────────────────────────────────────────────

function NodeSidebar({
  node, onUpdate, onDelete,
}: { node: Node; onUpdate: (p: Partial<Node>) => void; onDelete: () => void }) {
  const [label, setLabel]       = useState(node.label)
  const [nodeType, setNodeType] = useState(node.nodeType)

  useEffect(() => { setLabel(node.label); setNodeType(node.nodeType) }, [node.id])

  return (
    <div className="p-4 space-y-3">
      <h3 className="font-semibold text-gray-600 text-xs uppercase tracking-wide">Selected Node</h3>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Label</label>
        <input
          className="w-full border rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-400"
          value={label}
          onChange={e => setLabel(e.target.value)}
          onBlur={() => onUpdate({ label })}
        />
      </div>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Type</label>
        <select
          className="w-full border rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-400"
          value={nodeType}
          onChange={e => { setNodeType(e.target.value); onUpdate({ nodeType: e.target.value }) }}
        >
          {Object.keys(NODE_COLORS).map(t => <option key={t}>{t}</option>)}
        </select>
      </div>
      <div className="text-xs text-gray-400">
        Canvas: ({node.canvasX.toFixed(3)}, {node.canvasY.toFixed(3)})
      </div>
      <button onClick={onDelete} className="w-full text-sm border border-red-200 text-red-600 py-1.5 rounded hover:bg-red-50">
        Delete node
      </button>
    </div>
  )
}

function EdgeSidebar({
  edge, onUpdate, onDelete,
}: { edge: Edge; onUpdate: (p: Partial<Edge>) => void; onDelete: () => void }) {
  return (
    <div className="p-4 space-y-3">
      <h3 className="font-semibold text-gray-600 text-xs uppercase tracking-wide">Selected Edge</h3>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Type</label>
        <select
          className="w-full border rounded px-2 py-1.5 text-sm focus:outline-none"
          value={edge.edgeType}
          onChange={e => onUpdate({ edgeType: e.target.value })}
        >
          {['walk', 'stairs', 'elevator'].map(t => <option key={t}>{t}</option>)}
        </select>
      </div>
      <div className="flex items-center gap-2">
        <input
          type="checkbox" id="bidir" checked={edge.isBidirectional}
          onChange={e => onUpdate({ isBidirectional: e.target.checked })}
        />
        <label htmlFor="bidir" className="text-sm text-gray-600">Bidirectional</label>
      </div>
      <p className="text-xs text-gray-400">
        Source: <span className={edge.createdBy === 'ai' ? 'text-yellow-600' : ''}>{edge.createdBy}</span>
      </p>
      <button onClick={onDelete} className="w-full text-sm border border-red-200 text-red-600 py-1.5 rounded hover:bg-red-50">
        Delete edge
      </button>
    </div>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────

export function MapEditorPage() {
  const { buildingId, floorId } = useParams<{ buildingId: string; floorId: string }>()
  const navigate                = useNavigate()
  const store                   = useEditorStore()

  const [floorImage, setFloorImage]   = useState<HTMLImageElement | null>(null)
  const [glbLoading, setGlbLoading]   = useState(false)
  const [glbError, setGlbError]       = useState('')
  const [toast, setToast]             = useState('')
  const [pendingFrom, setPendingFrom] = useState<string | null>(null)

  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(''), 2800) }

  // Warn on unload with dirty state
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (store.isDirty) { e.preventDefault(); e.returnValue = '' }
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [store.isDirty])

  // Building info
  const { data: building } = useQuery({
    queryKey: ['building', buildingId],
    queryFn: () => getBuilding(buildingId!),
    enabled: !!buildingId,
  })
  const currentFloor = building?.floors?.find(f => f.id === floorId)

  // Load nodes + edges
  const loadData = useCallback(async () => {
    if (!floorId) return
    const [nodes, edges] = await Promise.all([listNodes(floorId), listEdges(floorId)])
    store.setNodes(nodes)
    store.setEdges(edges)
    store.setDirty(false)
  }, [floorId])

  useEffect(() => { loadData() }, [loadData])

  // Render GLB
  useEffect(() => {
    if (!buildingId || !floorId) return
    setGlbLoading(true); setGlbError('')
    const glbPath = getGlbUrl(buildingId, floorId)
    const glbUrl  = glbPath.startsWith('http') ? glbPath : `${window.location.origin}${glbPath}`
    const token   = localStorage.getItem('manager_token') ?? undefined
    renderGlbTopDown(glbUrl, token)
      .then(({ imageDataUrl, bounds }) => {
        store.setFloorPlan(imageDataUrl, bounds)
        updateFloorBounds(buildingId, floorId, bounds).catch(() => {})
        const img = new window.Image()
        img.src = imageDataUrl
        img.onload = () => setFloorImage(img)
      })
      .catch(() => setGlbError('Could not render GLB. Check that the floor has a GLB uploaded.'))
      .finally(() => setGlbLoading(false))
  }, [buildingId, floorId])

  // Keyboard
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.key === 'Delete' || e.key === 'Backspace') && (e.target as HTMLElement).tagName !== 'INPUT') {
        if (store.selectedNodeId) handleDeleteNode(store.selectedNodeId)
        else if (store.selectedEdgeId) handleDeleteEdge(store.selectedEdgeId)
      }
      if (e.key === 'Escape') { store.setMode('select'); setPendingFrom(null) }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [store.selectedNodeId, store.selectedEdgeId])

  // ── Handlers ────────────────────────────────────────────────────────────────

  const handleStageClick = async (e: any) => {
    if (e.target !== e.target.getStage()) return  // clicked on a shape — let shape handle it
    const stage = e.target.getStage()
    const pos   = stage.getPointerPosition()
    if (!pos || !floorId) return
    const cx = pos.x / CANVAS_SIZE
    const cy = pos.y / CANVAS_SIZE

    if (store.mode === 'addNode') {
      try {
        const node = await createNode(floorId, { label: 'New Node', nodeType: 'room', canvasX: cx, canvasY: cy })
        store.addNode(node)
        store.setSelectedNode(node.id)
        store.setMode('select')
      } catch { showToast('Failed to create node') }
      return
    }

    if (store.mode === 'select') {
      store.setSelectedNode(null)
      store.setSelectedEdge(null)
    }
  }

  const handleNodeClick = async (nodeId: string) => {
    if (store.mode === 'addEdge') {
      if (!pendingFrom) {
        setPendingFrom(nodeId)
      } else if (pendingFrom !== nodeId && floorId) {
        try {
          const edge = await createEdge(floorId, { fromNodeId: pendingFrom, toNodeId: nodeId })
          store.addEdge(edge)
          showToast('Edge created')
        } catch { showToast('Failed to create edge') }
        setPendingFrom(null)
        store.setMode('select')
      }
      return
    }
    if (store.mode === 'select') {
      store.setSelectedNode(nodeId)
    }
  }

  const handleDeleteNode = async (nodeId: string) => {
    if (!floorId) return
    try { await deleteNode(floorId, nodeId); store.removeNode(nodeId); store.setSelectedNode(null) }
    catch { showToast('Delete failed') }
  }

  const handleDeleteEdge = async (edgeId: string) => {
    if (!floorId) return
    try { await deleteEdge(floorId, edgeId); store.removeEdge(edgeId); store.setSelectedEdge(null) }
    catch { showToast('Delete failed') }
  }

  const handleUpdateNode = async (nodeId: string, patch: Partial<Node>) => {
    if (!floorId) return
    try { const n = await updateNode(floorId, nodeId, patch); store.updateNodeInStore(n) }
    catch { showToast('Update failed') }
  }

  const handleUpdateEdge = async (edgeId: string, patch: Partial<Edge>) => {
    if (!floorId) return
    try { const e = await updateEdge(floorId, edgeId, patch); store.updateEdgeInStore(e) }
    catch { showToast('Update failed') }
  }

  const handleAiSuggest = async () => {
    if (!floorId || !store.floorPlanDataUrl) { showToast('Floor plan not loaded yet'); return }
    store.setAiLoading(true)
    try {
      const b64 = store.floorPlanDataUrl.split(',')[1]
      const result = await aiSuggestEdges(floorId, b64, store.nodes)
      store.setSuggestedEdges(result.edges)
      showToast(`${result.edges.length} edge${result.edges.length !== 1 ? 's' : ''} suggested`)
    } catch { showToast('AI suggestion failed') }
    finally { store.setAiLoading(false) }
  }

  const handleAcceptSuggestion = async (s: SuggestedEdge) => {
    if (!floorId) return
    try {
      const edge = await createEdge(floorId, { fromNodeId: s.fromNodeId, toNodeId: s.toNodeId, waypoints: s.waypoints, createdBy: 'ai' })
      store.addEdge(edge)
      store.setSuggestedEdges(store.suggestedEdges.filter(x => !(x.fromNodeId === s.fromNodeId && x.toNodeId === s.toNodeId)))
    } catch { showToast('Failed to accept edge') }
  }

  const handleAcceptAll = async () => {
    for (const s of [...store.suggestedEdges]) await handleAcceptSuggestion(s)
    store.clearSuggestions()
    showToast('All suggestions accepted')
  }

  const selectedNode = store.nodes.find(n => n.id === store.selectedNodeId) ?? null
  const selectedEdge = store.edges.find(e => e.id === store.selectedEdgeId) ?? null

  // ── Mode button ──────────────────────────────────────────────────────────────

  const ModeBtn = ({ mode, label, icon }: { mode: EditorMode; label: string; icon: string }) => (
    <button
      id={`mode-${mode}`}
      onClick={() => { store.setMode(mode); setPendingFrom(null) }}
      className={`flex items-center gap-1.5 px-3 py-1.5 rounded text-sm font-medium transition-colors ${
        store.mode === mode ? 'bg-blue-600 text-white shadow-sm' : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
      }`}
    >
      <span>{icon}</span>{label}
    </button>
  )

  // ── JSX ──────────────────────────────────────────────────────────────────────

  return (
    <div className="h-screen flex flex-col bg-gray-100 overflow-hidden">

      {/* Toolbar */}
      <div className="bg-white border-b border-gray-200 px-4 py-2 flex items-center gap-3 flex-shrink-0">
        <button id="editor-back" onClick={() => navigate(`/buildings/${buildingId}`)}
          className="text-sm text-gray-500 hover:text-gray-800">
          ← Back
        </button>
        <div className="w-px h-5 bg-gray-200" />
        <span className="text-sm font-medium text-gray-700 truncate max-w-xs">
          {building?.name ?? '…'} / {currentFloor?.floorName ?? floorId}
        </span>
        {store.isDirty && <span className="text-xs text-orange-500 font-semibold">● Unsaved changes</span>}
        <div className="ml-auto flex items-center gap-2">
          <ModeBtn mode="select"  label="Select"   icon="↖" />
          <ModeBtn mode="addNode" label="Add Node"  icon="⊕" />
          <ModeBtn mode="addEdge" label="Add Edge"  icon="⌇" />
          <button
            id="ai-suggest-btn"
            onClick={handleAiSuggest}
            disabled={store.isAiLoading || !store.floorPlanDataUrl}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded text-sm font-medium bg-amber-50 border border-amber-300 text-amber-800 hover:bg-amber-100 disabled:opacity-40"
          >
            {store.isAiLoading ? '⏳ Thinking…' : '✨ AI Suggest'}
          </button>
          <button onClick={loadData} className="px-3 py-1.5 text-sm border rounded text-gray-600 hover:bg-gray-50">↻ Reload</button>
        </div>
      </div>

      {/* Context hint bar */}
      {store.mode !== 'select' && (
        <div className="bg-blue-50 border-b border-blue-100 px-4 py-1.5 text-xs text-blue-700 flex-shrink-0">
          {store.mode === 'addNode' && '🖱 Click the floor plan to place a node — Esc to cancel'}
          {store.mode === 'addEdge' && (pendingFrom
            ? `🔗 Click a second node to connect (from: "${store.nodes.find(n => n.id === pendingFrom)?.label ?? '…'}")`
            : '🔗 Click the first node — Esc to cancel'
          )}
        </div>
      )}

      {/* AI suggestion bar */}
      {store.suggestedEdges.length > 0 && (
        <div className="bg-amber-50 border-b border-amber-200 px-4 py-2 flex items-center gap-4 text-sm flex-shrink-0">
          <span className="text-amber-800 font-medium">✨ {store.suggestedEdges.length} AI edges suggested (shown as dashed yellow)</span>
          <button onClick={handleAcceptAll} className="px-3 py-1 bg-amber-500 text-white rounded text-xs font-medium hover:bg-amber-600">Accept All</button>
          <span className="text-amber-700 text-xs">or click individual edges to accept below</span>
          <button onClick={() => store.clearSuggestions()} className="ml-auto px-3 py-1 border border-amber-400 text-amber-700 rounded text-xs hover:bg-amber-100">Dismiss</button>
        </div>
      )}

      {/* Body */}
      <div className="flex-1 flex overflow-hidden">

        {/* Canvas area */}
        <div className="flex-1 overflow-auto flex items-start justify-center p-4">
          <div
            className="relative shadow-xl rounded overflow-hidden border border-gray-300"
            style={{ width: CANVAS_SIZE, height: CANVAS_SIZE, background: '#e5e7eb' }}
          >
            {glbLoading && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-white/90 z-10">
                <div className="text-4xl mb-3 animate-spin">⚙️</div>
                <p className="text-gray-500 text-sm">Rendering floor plan…</p>
              </div>
            )}
            {glbError && !glbLoading && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-red-50 z-10 p-8 text-center">
                <div className="text-3xl mb-2">⚠️</div>
                <p className="text-red-600 text-sm">{glbError}</p>
              </div>
            )}

            <Stage width={CANVAS_SIZE} height={CANVAS_SIZE}
              onClick={handleStageClick}
              style={{ cursor: store.mode === 'addNode' ? 'crosshair' : 'default' }}
            >
              {/* Background */}
              <Layer>
                {floorImage && (
                  <KonvaImage
                    image={floorImage}
                    x={0}
                    y={0}
                    width={CANVAS_SIZE}
                    height={CANVAS_SIZE}
                    listening={false}
                  />
                )}
              </Layer>

              {/* Edges */}
              <Layer>
                {store.edges.map(edge => {
                  const from = store.nodes.find(n => n.id === edge.fromNodeId)
                  const to   = store.nodes.find(n => n.id === edge.toNodeId)
                  if (!from || !to) return null
                  const isSelected = edge.id === store.selectedEdgeId
                  return (
                    <Line key={edge.id}
                      points={[norm2px(from.canvasX), norm2px(from.canvasY), norm2px(to.canvasX), norm2px(to.canvasY)]}
                      stroke={isSelected ? '#2563eb' : edge.createdBy === 'ai' ? '#ca8a04' : '#1f2937'}
                      strokeWidth={isSelected ? 3 : 2}
                      dash={edge.createdBy === 'ai' ? [6, 3] : undefined}
                      onClick={(e) => { e.cancelBubble = true; store.setSelectedEdge(edge.id) }}
                      hitStrokeWidth={14}
                    />
                  )
                })}

                {/* AI suggestions */}
                {store.suggestedEdges.map((s, i) => {
                  const from = store.nodes.find(n => n.id === s.fromNodeId)
                  const to   = store.nodes.find(n => n.id === s.toNodeId)
                  if (!from || !to) return null
                  return (
                    <Line key={`sugg-${i}`}
                      points={[norm2px(from.canvasX), norm2px(from.canvasY), norm2px(to.canvasX), norm2px(to.canvasY)]}
                      stroke="#eab308" strokeWidth={2} dash={[9, 4]} opacity={0.85}
                      onClick={(e) => { e.cancelBubble = true; handleAcceptSuggestion(s) }}
                      hitStrokeWidth={14}
                    />
                  )
                })}
              </Layer>

              {/* Nodes */}
              <Layer>
                {store.nodes.map(node => {
                  const x         = norm2px(node.canvasX)
                  const y         = norm2px(node.canvasY)
                  const isSelected = node.id === store.selectedNodeId
                  const isPending  = node.id === pendingFrom
                  const color      = NODE_COLORS[node.nodeType] ?? '#6b7280'
                  return (
                    <React.Fragment key={node.id}>
                      <Circle
                        x={x} y={y}
                        radius={isSelected ? 11 : isPending ? 9 : 7}
                        fill={color}
                        stroke={isSelected ? '#fff' : isPending ? '#fbbf24' : 'rgba(0,0,0,0.25)'}
                        strokeWidth={isSelected ? 3 : isPending ? 3 : 1}
                        onClick={(e) => { e.cancelBubble = true; handleNodeClick(node.id) }}
                        style={{ cursor: 'pointer' }}
                      />
                      <Text
                        x={x + 12} y={y - 7}
                        text={node.label}
                        fontSize={11}
                        fill="#111827"
                        shadowColor="white"
                        shadowBlur={3}
                        shadowOffset={{ x: 0, y: 0 }}
                        listening={false}
                      />
                    </React.Fragment>
                  )
                })}
              </Layer>
            </Stage>
          </div>
        </div>

        {/* Sidebar */}
        <div className="w-52 bg-white border-l border-gray-200 flex flex-col overflow-y-auto flex-shrink-0">
          {selectedNode ? (
            <NodeSidebar
              node={selectedNode}
              onUpdate={(patch) => handleUpdateNode(selectedNode.id, patch)}
              onDelete={() => handleDeleteNode(selectedNode.id)}
            />
          ) : selectedEdge ? (
            <EdgeSidebar
              edge={selectedEdge}
              onUpdate={(patch) => handleUpdateEdge(selectedEdge.id, patch)}
              onDelete={() => handleDeleteEdge(selectedEdge.id)}
            />
          ) : (
            <div className="p-4">
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Legend</p>
              <div className="space-y-2">
                {Object.entries(NODE_COLORS).map(([t, c]) => (
                  <div key={t} className="flex items-center gap-2 text-xs text-gray-600">
                    <span className="w-3 h-3 rounded-full flex-shrink-0" style={{ background: c }} />
                    {t}
                  </div>
                ))}
              </div>
              <div className="mt-4 pt-4 border-t border-gray-100 text-xs text-gray-400 space-y-1">
                <p>Del — delete selected</p>
                <p>Esc — cancel mode</p>
                <p>Click yellow edge — accept AI</p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Status bar */}
      <div className="bg-white border-t border-gray-200 px-4 py-1 text-xs text-gray-500 flex gap-5 flex-shrink-0">
        <span>{store.nodes.length} nodes</span>
        <span>{store.edges.length} edges</span>
        {store.suggestedEdges.length > 0 && (
          <span className="text-amber-600 font-medium">{store.suggestedEdges.length} AI suggestions pending</span>
        )}
        <span className="ml-auto">{store.isDirty ? '● Unsaved' : '✓ Saved'}</span>
      </div>

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-gray-900 text-white text-sm px-4 py-2.5 rounded-lg shadow-lg z-50">
          {toast}
        </div>
      )}
    </div>
  )
}
