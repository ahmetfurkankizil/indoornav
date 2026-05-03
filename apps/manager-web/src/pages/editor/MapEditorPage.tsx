import React, { useEffect, useRef, useCallback, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Stage, Layer, Image as KonvaImage, Circle, Line, Text, Rect } from 'react-konva'
import { useQuery } from '@tanstack/react-query'
import { useEditorStore, type EditorMode } from '../../stores/editorStore'
import { renderMapBlob } from '../../three/MapRenderer'
import {
  listNodes, createNode, updateNode, deleteNode,
  type Node, type NavMeshArea,
} from '../../api/mapEditor'
import { getBuilding, updateFloorBounds } from '../../api/buildings'
import { getMapFile } from '../../api/localDb'

// ── Constants ──────────────────────────────────────────────────────────────────

const NODE_COLORS: Record<string, string> = {
  room:     '#3b82f6',
  entrance: '#22c55e',
  elevator: '#a855f7',
  stairs:   '#f97316',
  exit:     '#ef4444',
}

const NODE_ICONS: Record<string, string> = {
  room:     '🚪',
  entrance: '🚶',
  elevator: '🛗',
  stairs:   '🪜',
  exit:     '🚪',
}

const CANVAS_SIZE        = 800
const NODE_RADIUS        = 8
const NODE_RADIUS_SEL    = 11
const CLOSE_SNAP_PX      = 15

function norm2px(v: number) { return v * CANVAS_SIZE }
function px2norm(v: number) { return v / CANVAS_SIZE }

// ── Node properties panel ──────────────────────────────────────────────────────

function NodePropertiesPanel({ node, onUpdate, onDelete }: {
  node: Node; onUpdate: (p: Partial<Node>) => void; onDelete: () => void
}) {
  const [label, setLabel]       = useState(node.label)
  const [nodeType, setNodeType] = useState(node.nodeType)
  useEffect(() => { setLabel(node.label); setNodeType(node.nodeType) }, [node.id])

  return (
    <div className="p-4 space-y-3 border-t border-gray-100">
      <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Node Properties</h3>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Label</label>
        <input
          className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          value={label}
          onChange={e => setLabel(e.target.value)}
          onBlur={() => label !== node.label && onUpdate({ label })}
          onKeyDown={e => e.key === 'Enter' && onUpdate({ label })}
        />
      </div>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Type</label>
        <select
          className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          value={nodeType}
          onChange={e => { setNodeType(e.target.value); onUpdate({ nodeType: e.target.value }) }}
        >
          {Object.keys(NODE_COLORS).map(t => (
            <option key={t} value={t}>{NODE_ICONS[t]} {t.charAt(0).toUpperCase() + t.slice(1)}</option>
          ))}
        </select>
      </div>
      <div className="text-xs text-gray-400 bg-gray-50 rounded-lg px-2.5 py-2">
        Position: ({node.canvasX.toFixed(3)}, {node.canvasY.toFixed(3)})
      </div>
      <button onClick={onDelete}
        className="w-full flex items-center justify-center gap-1.5 text-sm border border-red-200 text-red-600 py-1.5 rounded-lg hover:bg-red-50 transition-colors">
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        Delete node
      </button>
    </div>
  )
}

// ── Area properties panel ──────────────────────────────────────────────────────

function NavMeshAreaPropertiesPanel({ area, onUpdate, onDelete }: {
  area: NavMeshArea; onUpdate: (label: string) => void; onDelete: () => void
}) {
  const [label, setLabel] = useState(area.label)
  useEffect(() => { setLabel(area.label) }, [area.id])

  return (
    <div className="p-4 space-y-3 border-t border-gray-100">
      <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Area Properties</h3>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Label</label>
        <input
          className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          value={label}
          onChange={e => setLabel(e.target.value)}
          onBlur={() => label !== area.label && onUpdate(label)}
          onKeyDown={e => e.key === 'Enter' && onUpdate(label)}
        />
      </div>
      <div className="text-xs text-gray-400 bg-gray-50 rounded-lg px-2.5 py-2">
        {area.vertices.length} vertices
      </div>
      <button onClick={onDelete}
        className="w-full flex items-center justify-center gap-1.5 text-sm border border-red-200 text-red-600 py-1.5 rounded-lg hover:bg-red-50 transition-colors">
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        Delete area
      </button>
    </div>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────

export function MapEditorPage() {
  const { buildingId, floorId } = useParams<{ buildingId: string; floorId: string }>()
  const navigate = useNavigate()
  const store    = useEditorStore()

  const [floorImage, setFloorImage]     = useState<HTMLImageElement | null>(null)
  const [glbLoading, setGlbLoading]     = useState(false)
  const [glbError, setGlbError]         = useState('')
  const [toast, setToast]               = useState('')
  const [selectionBox, setSelectionBox] = useState<{ x: number; y: number; w: number; h: number } | null>(null)

  const rubberStartRef  = useRef<{ x: number; y: number } | null>(null)
  const isRubberRef     = useRef(false)
  const dragStartPositions = useRef<Record<string, { x: number; y: number }>>({})
  const dragOrigin         = useRef<{ x: number; y: number } | null>(null)
  const canvasContainerRef = useRef<HTMLDivElement>(null)
  const [aiGenerating, setAiGenerating] = useState(false)
  const [aiProgress, setAiProgress]     = useState(0)
  const aiTimerRef = useRef<number | null>(null)

  const AI_MESSAGES = [
    "Detecting room boundaries…",
    "Mapping navigation nodes…",
    "Calculating optimal routes…",
    "Creating walkable path…",
  ]
  const currentAiMessage = AI_MESSAGES[Math.min(AI_MESSAGES.length - 1, Math.floor((aiProgress / 100) * AI_MESSAGES.length))]

  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(''), 2800) }

  const selectedNodeId = store.selectedNodeIds[0] ?? null
  const selectedAreaId = store.selectedAreaIds[0] ?? null
  const selectedNode   = store.nodes.find(n => n.id === selectedNodeId) ?? null
  const selectedArea   = store.navMeshAreas.find(a => a.id === selectedAreaId) ?? null
  const multiSelected  = store.selectedNodeIds.length + store.selectedAreaIds.length > 1
  const anySelected    = store.selectedNodeIds.length > 0 || store.selectedAreaIds.length > 0

  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (store.isDirty) { e.preventDefault(); e.returnValue = '' }
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [store.isDirty])

  const { data: building } = useQuery({
    queryKey: ['building', buildingId],
    queryFn: () => getBuilding(buildingId!),
    enabled: !!buildingId,
  })
  const currentFloor = building?.floors?.find(f => f.id === floorId)

  const loadData = useCallback(async () => {
    if (!floorId) return
    const nodes = await listNodes(floorId)
    store.setNodes(nodes)
    store.setDirty(false)
  }, [floorId])

  useEffect(() => { loadData() }, [loadData])

  useEffect(() => {
    if (!buildingId || !floorId) return
    setGlbLoading(true); setGlbError('')
    getMapFile(buildingId, floorId)
      .then(async (fileData) => {
        if (!fileData) throw new Error('No map file uploaded for this floor.')
        const { imageDataUrl, bounds } = await renderMapBlob(fileData.blob, fileData.mimeType)
        store.setFloorPlan(imageDataUrl, bounds)
        updateFloorBounds(buildingId, floorId, bounds).catch(() => {})
        const img = new window.Image()
        img.src = imageDataUrl
        img.onload = () => setFloorImage(img)
      })
      .catch((err: any) => setGlbError(err?.message ?? 'Could not render floor plan.'))
      .finally(() => setGlbLoading(false))
  }, [buildingId, floorId])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement).tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

      if (e.key === 'Delete' || e.key === 'Backspace') {
        if (store.selectedNodeIds.length > 0 || store.selectedAreaIds.length > 0) {
          handleDeleteSelected()
        } else if (store.pendingAreaVertices.length > 0) {
          store.clearPendingVertices()
        }
      }
      if (e.key === 'Escape') {
        store.clearPendingVertices()
        store.setMode('select')
        store.clearSelection()
      }
      if ((e.key === 'a' || e.key === 'A') && (e.ctrlKey || e.metaKey)) {
        e.preventDefault()
        store.selectAllItems()
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [store.selectedNodeIds, store.selectedAreaIds, store.nodes, store.pendingAreaVertices])

  // ── Handlers ────────────────────────────────────────────────────────────────

  const handleDeleteSelected = async () => {
    if (!floorId) return
    const nodeIds = [...store.selectedNodeIds]
    const areaIds = [...store.selectedAreaIds]
    for (const id of nodeIds) {
      try { await deleteNode(floorId, id) } catch { /* continue */ }
    }
    areaIds.forEach(id => store.removeNavMeshArea(id))
    store.removeSelectedItems()
    const count = nodeIds.length + areaIds.length
    showToast(`Deleted ${count} item${count !== 1 ? 's' : ''}`)
  }

  const handleDeleteNode = async (nodeId: string) => {
    if (!floorId) return
    try { await deleteNode(floorId, nodeId); store.removeNode(nodeId) }
    catch { showToast('Delete failed') }
  }

  const handleDeleteArea = (areaId: string) => {
    store.removeNavMeshArea(areaId)
  }

  const handleUpdateNode = async (nodeId: string, patch: Partial<Node>) => {
    if (!floorId) return
    try { const n = await updateNode(floorId, nodeId, patch); store.updateNodeInStore(n) }
    catch { showToast('Update failed') }
  }

  const handleUpdateAreaLabel = (areaId: string, label: string) => {
    const area = store.navMeshAreas.find(a => a.id === areaId)
    if (area) store.updateNavMeshAreaInStore({ ...area, label })
  }

  const handleCloseArea = () => {
    if (store.pendingAreaVertices.length < 3) return
    const area: NavMeshArea = {
      id:         crypto.randomUUID(),
      floorId:    floorId ?? '',
      buildingId: '',
      label:      'Walkable Area',
      vertices:   store.pendingAreaVertices,
      createdAt:  new Date().toISOString(),
      updatedAt:  new Date().toISOString(),
    }
    store.addNavMeshArea(area)
    store.clearPendingVertices()
    store.toggleAreaSelection(area.id, false)
    showToast('Walkable area created')
  }

  const handleStartAiGeneration = () => {
    setAiGenerating(true)
    setAiProgress(0)
    let startTime = Date.now()
    const duration = 30000 // 30 seconds

    aiTimerRef.current = window.setInterval(() => {
      const elapsed = Date.now() - startTime
      const progress = Math.min(100, (elapsed / duration) * 100)
      setAiProgress(progress)

      if (elapsed >= duration) {
        stopAiGeneration()
        showToast('AI path generation complete')
      }
    }, 100)
  }

  const stopAiGeneration = () => {
    if (aiTimerRef.current) {
      window.clearInterval(aiTimerRef.current)
      aiTimerRef.current = null
    }
    setAiGenerating(false)
  }

  // ── Stage events ─────────────────────────────────────────────────────────────

  const handleStageMouseDown = (e: any) => {
    if (e.target !== e.target.getStage()) return
    if (store.mode !== 'select') return
    const pos = e.target.getStage().getPointerPosition()
    if (!pos) return
    isRubberRef.current   = true
    rubberStartRef.current = { x: pos.x, y: pos.y }
    setSelectionBox({ x: pos.x, y: pos.y, w: 0, h: 0 })
  }

  const handleStageMouseMove = (e: any) => {
    if (!isRubberRef.current || !rubberStartRef.current) return
    const pos = e.target.getStage().getPointerPosition()
    if (!pos) return
    const s = rubberStartRef.current
    setSelectionBox({
      x: Math.min(s.x, pos.x),
      y: Math.min(s.y, pos.y),
      w: Math.abs(pos.x - s.x),
      h: Math.abs(pos.y - s.y),
    })
  }

  const handleStageMouseUp = () => {
    if (!isRubberRef.current) return
    isRubberRef.current = false
    rubberStartRef.current = null
    if (selectionBox && (selectionBox.w > 5 || selectionBox.h > 5)) {
      store.selectItemsInRect(selectionBox, CANVAS_SIZE)
    } else {
      store.clearSelection()
    }
    setSelectionBox(null)
  }

  const handleStageClick = async (e: any) => {
    if (e.target !== e.target.getStage()) return
    const stage = e.target.getStage()
    const pos   = stage.getPointerPosition()
    if (!pos || !floorId) return

    if (store.mode === 'addNode') {
      try {
        const node = await createNode(floorId, {
          label:   store.selectedNodeType.charAt(0).toUpperCase() + store.selectedNodeType.slice(1),
          nodeType: store.selectedNodeType,
          canvasX: px2norm(pos.x),
          canvasY: px2norm(pos.y),
        })
        store.addNode(node)
        store.setSelectedNode(node.id)
        store.setMode('select')
      } catch { showToast('Failed to create node') }
      return
    }

    if (store.mode === 'drawArea') {
      // Snap to first vertex to close polygon
      if (store.pendingAreaVertices.length >= 3) {
        const first = store.pendingAreaVertices[0]
        const dx    = norm2px(first.x) - pos.x
        const dy    = norm2px(first.y) - pos.y
        if (Math.sqrt(dx * dx + dy * dy) <= CLOSE_SNAP_PX) {
          handleCloseArea()
          return
        }
      }
      store.addPendingVertex({ x: px2norm(pos.x), y: px2norm(pos.y) })
    }
  }

  // ── Node events ──────────────────────────────────────────────────────────────

  const handleNodeClick = (nodeId: string, e: any) => {
    e.cancelBubble = true
    if (store.mode === 'select') {
      store.toggleNodeSelection(nodeId, e.evt?.shiftKey ?? false)
    }
  }

  const handleNodeDragStart = (e: any, nodeId: string) => {
    const isInSel  = store.selectedNodeIds.includes(nodeId)
    const toTrack  = isInSel ? store.selectedNodeIds : [nodeId]
    const positions: Record<string, { x: number; y: number }> = {}
    for (const id of toTrack) {
      const n = store.nodes.find(n => n.id === id)
      if (n) positions[id] = { x: n.canvasX * CANVAS_SIZE, y: n.canvasY * CANVAS_SIZE }
    }
    dragStartPositions.current = positions
    dragOrigin.current = { x: e.target.x(), y: e.target.y() }
  }

  const handleNodeDragMove = (e: any, nodeId: string) => {
    const origin = dragOrigin.current
    if (!origin) return
    const dx = e.target.x() - origin.x
    const dy = e.target.y() - origin.y
    const isInSel  = store.selectedNodeIds.includes(nodeId)
    const toMove   = isInSel ? store.selectedNodeIds : [nodeId]
    for (const id of toMove) {
      const start = dragStartPositions.current[id]
      const n     = store.nodes.find(n => n.id === id)
      if (!start || !n) continue
      store.updateNodeInStore({
        ...n,
        canvasX: px2norm(Math.max(0, Math.min(CANVAS_SIZE, start.x + dx))),
        canvasY: px2norm(Math.max(0, Math.min(CANVAS_SIZE, start.y + dy))),
      })
    }
  }

  const handleNodeDragEnd = async (_e: any, nodeId: string) => {
    const isInSel = store.selectedNodeIds.includes(nodeId)
    const toSave  = isInSel ? store.selectedNodeIds : [nodeId]
    for (const id of toSave) {
      const n = store.nodes.find(n => n.id === id)
      if (n) await handleUpdateNode(id, { canvasX: n.canvasX, canvasY: n.canvasY })
    }
    dragOrigin.current = null
  }

  // ── Area drag ────────────────────────────────────────────────────────────────

  const handleAreaDragEnd = (e: any, area: NavMeshArea) => {
    const shape = e.target
    const dx    = px2norm(shape.x())
    const dy    = px2norm(shape.y())
    shape.position({ x: 0, y: 0 })
    const newVertices = area.vertices.map(v => ({
      x: Math.max(0, Math.min(1, v.x + dx)),
      y: Math.max(0, Math.min(1, v.y + dy)),
    }))
    store.updateNavMeshAreaInStore({ ...area, vertices: newVertices })
  }

  // ── HTML5 drag-and-drop from right panel ──────────────────────────────────────

  const handlePanelDragStart = (e: React.DragEvent, nodeType: string) => {
    e.dataTransfer.setData('nodeType', nodeType)
    e.dataTransfer.effectAllowed = 'copy'
  }

  const handleCanvasDrop = async (e: React.DragEvent) => {
    e.preventDefault()
    const nodeType = e.dataTransfer.getData('nodeType')
    if (!nodeType || !floorId || !canvasContainerRef.current) return
    const rect = canvasContainerRef.current.getBoundingClientRect()
    const cx   = px2norm(Math.max(0, Math.min(CANVAS_SIZE, e.clientX - rect.left)))
    const cy   = px2norm(Math.max(0, Math.min(CANVAS_SIZE, e.clientY - rect.top)))
    try {
      const node = await createNode(floorId, {
        label:   nodeType.charAt(0).toUpperCase() + nodeType.slice(1),
        nodeType,
        canvasX: cx,
        canvasY: cy,
      })
      store.addNode(node)
      store.setSelectedNode(node.id)
      showToast(`${nodeType} placed`)
    } catch { showToast('Failed to place node') }
  }

  // ── JSX ──────────────────────────────────────────────────────────────────────

  const ModeBtn = ({ mode, label, icon }: { mode: EditorMode; label: string; icon: React.ReactNode }) => (
    <button
      onClick={() => { store.setMode(mode); store.clearPendingVertices() }}
      className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
        store.mode === mode
          ? 'bg-blue-600 text-white shadow-sm'
          : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50 hover:border-gray-300'
      }`}
    >
      {icon} {label}
    </button>
  )

  const pendingCount = store.pendingAreaVertices.length
  const canCloseArea = pendingCount >= 3

  return (
    <div className="h-screen flex flex-col bg-gray-100 overflow-hidden">

      {/* Toolbar */}
      <div className="bg-white border-b border-gray-200 px-4 py-2.5 flex items-center gap-3 flex-shrink-0">
        <button onClick={() => navigate(`/buildings/${buildingId}`)}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back
        </button>
        <div className="w-px h-5 bg-gray-200" />
        <div className="min-w-0">
          <span className="text-sm font-semibold text-gray-800 truncate block">
            {building?.name ?? '…'} <span className="text-gray-400 font-normal">/ {currentFloor?.floorName ?? floorId}</span>
          </span>
        </div>
        {store.isDirty && (
          <span className="flex items-center gap-1 text-xs text-amber-600 font-medium bg-amber-50 px-2 py-0.5 rounded-full">
            <span className="w-1.5 h-1.5 bg-amber-500 rounded-full" /> Unsaved
          </span>
        )}

        <div className="ml-auto flex items-center gap-2">
          <ModeBtn mode="select"   label="Select"
            icon={<svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 15l-5 5M4 4l10 10M4 4h6M4 4v6" /></svg>} />
          <ModeBtn mode="addNode"  label="Add Node"
            icon={<svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>} />
          <ModeBtn mode="drawArea" label="Draw Area"
            icon={<svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" /></svg>} />

          <div className="w-px h-5 bg-gray-200" />

          <button onClick={loadData}
            className="p-1.5 rounded-lg text-gray-500 border border-gray-200 hover:bg-gray-50 hover:text-gray-700 transition-colors"
            title="Reload from server">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>

          <button
            onClick={handleStartAiGeneration}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-gradient-to-r from-purple-600 to-blue-600 text-white shadow-md hover:shadow-lg transition-all active:scale-95"
          >
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            AI Generate Path
          </button>
        </div>
      </div>

      {/* Context hint */}
      {store.mode !== 'select' && (
        <div className="bg-blue-600 px-4 py-1.5 text-xs text-white flex items-center gap-3 flex-shrink-0">
          <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {store.mode === 'addNode' && `Click the canvas to place a ${store.selectedNodeType} — or drag a type from the right panel — Esc to cancel`}
          {store.mode === 'drawArea' && (
            pendingCount === 0
              ? 'Click to add polygon vertices. Click the first vertex (green) to close — Esc to cancel'
              : `${pendingCount} vert${pendingCount !== 1 ? 'ices' : 'ex'} — click canvas to add more${canCloseArea ? ' · click first vertex or ' : ' · '}`
          )}
          {store.mode === 'drawArea' && canCloseArea && (
            <button
              onClick={handleCloseArea}
              className="ml-1 bg-white/20 hover:bg-white/30 px-2.5 py-0.5 rounded-full font-medium transition-colors"
            >
              Close & Save
            </button>
          )}
          {store.mode === 'drawArea' && pendingCount > 0 && (
            <button
              onClick={() => store.clearPendingVertices()}
              className="ml-auto bg-white/10 hover:bg-white/20 px-2.5 py-0.5 rounded-full transition-colors"
            >
              Discard
            </button>
          )}
        </div>
      )}

      {/* Body */}
      <div className="flex-1 flex overflow-hidden">

        {/* Canvas area */}
        <div className="flex-1 overflow-auto flex items-start justify-center p-6">
          <div
            ref={canvasContainerRef}
            className="relative shadow-2xl rounded-xl overflow-hidden border border-gray-300"
            style={{ width: CANVAS_SIZE, height: CANVAS_SIZE, background: '#e5e7eb', flexShrink: 0 }}
            onDragOver={e => e.preventDefault()}
            onDrop={handleCanvasDrop}
          >
            {glbLoading && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-white/95 z-10">
                <div className="w-10 h-10 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mb-3" />
                <p className="text-gray-500 text-sm font-medium">Rendering floor plan…</p>
              </div>
            )}
            {glbError && !glbLoading && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-red-50 z-10 p-8 text-center">
                <svg className="w-10 h-10 text-red-400 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
                </svg>
                <p className="text-red-600 text-sm font-medium mb-1">Floor plan unavailable</p>
                <p className="text-red-400 text-xs">{glbError}</p>
              </div>
            )}

            <Stage
              width={CANVAS_SIZE}
              height={CANVAS_SIZE}
              onClick={handleStageClick}
              onMouseDown={handleStageMouseDown}
              onMouseMove={handleStageMouseMove}
              onMouseUp={handleStageMouseUp}
              style={{
                cursor: store.mode === 'addNode' ? 'crosshair'
                      : store.mode === 'drawArea' ? 'crosshair'
                      : 'default',
              }}
            >
              {/* Background */}
              <Layer listening={false}>
                {floorImage && (
                  <KonvaImage image={floorImage} x={0} y={0} width={CANVAS_SIZE} height={CANVAS_SIZE} />
                )}
              </Layer>

              {/* NavMesh areas */}
              <Layer>
                {store.navMeshAreas.map(area => {
                  const pts   = area.vertices.flatMap(v => [norm2px(v.x), norm2px(v.y)])
                  const isSel = store.selectedAreaIds.includes(area.id)
                  return (
                    <React.Fragment key={area.id}>
                      <Line
                        points={pts}
                        closed={true}
                        fill={isSel ? 'rgba(59,130,246,0.25)' : 'rgba(59,130,246,0.12)'}
                        stroke={isSel ? '#2563eb' : '#3b82f6'}
                        strokeWidth={isSel ? 2.5 : 1.5}
                        dash={isSel ? undefined : [6, 3]}
                        shadowColor={isSel ? '#3b82f6' : undefined}
                        shadowBlur={isSel ? 6 : 0}
                        hitStrokeWidth={12}
                        draggable={store.mode === 'select'}
                        onClick={(e) => { e.cancelBubble = true; store.toggleAreaSelection(area.id, e.evt?.shiftKey ?? false) }}
                        onDragEnd={(e) => handleAreaDragEnd(e, area)}
                      />
                      {isSel && (
                        <Text
                          x={norm2px(area.vertices.reduce((s, v) => s + v.x, 0) / area.vertices.length)}
                          y={norm2px(area.vertices.reduce((s, v) => s + v.y, 0) / area.vertices.length) - 6}
                          text={area.label}
                          fontSize={10}
                          fill="#1d4ed8"
                          fontStyle="bold"
                          shadowColor="white" shadowBlur={3} shadowOffset={{ x: 0, y: 0 }}
                          listening={false}
                        />
                      )}
                    </React.Fragment>
                  )
                })}

                {/* Pending polygon being drawn */}
                {pendingCount >= 2 && (
                  <Line
                    points={store.pendingAreaVertices.flatMap(v => [norm2px(v.x), norm2px(v.y)])}
                    stroke="#3b82f6" strokeWidth={1.5} dash={[5, 3]} opacity={0.8}
                    listening={false}
                  />
                )}
                {pendingCount > 0 && store.pendingAreaVertices.map((v, i) => (
                  <Circle key={`pv-${i}`}
                    x={norm2px(v.x)} y={norm2px(v.y)}
                    radius={i === 0 && canCloseArea ? 7 : 4}
                    fill={i === 0 && canCloseArea ? '#22c55e' : '#3b82f6'}
                    stroke="white" strokeWidth={1.5}
                    listening={false}
                  />
                ))}
              </Layer>

              {/* Nodes */}
              <Layer>
                {store.nodes.map(node => {
                  const x      = norm2px(node.canvasX)
                  const y      = norm2px(node.canvasY)
                  const isSel  = store.selectedNodeIds.includes(node.id)
                  const color  = NODE_COLORS[node.nodeType] ?? '#6b7280'
                  const radius = isSel ? NODE_RADIUS_SEL : NODE_RADIUS

                  return (
                    <React.Fragment key={node.id}>
                      {isSel && (
                        <Circle x={x} y={y} radius={radius + 5}
                          fill="rgba(59,130,246,0.15)" stroke="#3b82f6"
                          strokeWidth={1.5} dash={[3, 2]} listening={false}
                        />
                      )}
                      <Circle
                        x={x} y={y} radius={radius}
                        fill={color}
                        stroke={isSel ? '#fff' : 'rgba(0,0,0,0.2)'}
                        strokeWidth={isSel ? 2 : 1}
                        shadowColor={isSel ? color : 'transparent'}
                        shadowBlur={isSel ? 8 : 0}
                        onClick={(e) => handleNodeClick(node.id, e)}
                        draggable={store.mode === 'select'}
                        onDragStart={(e) => handleNodeDragStart(e, node.id)}
                        onDragMove={(e) => handleNodeDragMove(e, node.id)}
                        onDragEnd={(e) => handleNodeDragEnd(e, node.id)}
                        name="node-circle"
                        id={node.id}
                      />
                      <Text
                        x={x + radius + 4} y={y - 6}
                        text={node.label} fontSize={11} fill="#111827"
                        shadowColor="white" shadowBlur={3} shadowOffset={{ x: 0, y: 0 }}
                        listening={false}
                      />
                    </React.Fragment>
                  )
                })}
              </Layer>

              {/* Rubber-band selection */}
              {selectionBox && (
                <Layer listening={false}>
                  <Rect
                    x={selectionBox.x} y={selectionBox.y}
                    width={selectionBox.w} height={selectionBox.h}
                    fill="rgba(59,130,246,0.08)"
                    stroke="#3b82f6" strokeWidth={1.5} dash={[4, 3]}
                  />
                </Layer>
              )}
            </Stage>
          </div>
        </div>

        {/* Right panel */}
        <div className="w-60 bg-white border-l border-gray-200 flex flex-col overflow-y-auto flex-shrink-0">

          {/* Node type palette */}
          <div className="p-4 border-b border-gray-100">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Node Types</p>
            <p className="text-xs text-gray-400 mb-3">Drag onto canvas or click to select type</p>
            <div className="space-y-1.5">
              {Object.entries(NODE_COLORS).map(([type, color]) => (
                <div
                  key={type}
                  draggable
                  onDragStart={e => handlePanelDragStart(e, type)}
                  onClick={() => { store.setSelectedNodeType(type); store.setMode('addNode') }}
                  className={`flex items-center gap-2.5 px-3 py-2 rounded-lg cursor-grab active:cursor-grabbing transition-all select-none ${
                    store.mode === 'addNode' && store.selectedNodeType === type
                      ? 'bg-blue-50 border border-blue-200 shadow-sm'
                      : 'border border-transparent hover:bg-gray-50 hover:border-gray-200'
                  }`}
                >
                  <span className="w-3 h-3 rounded-full flex-shrink-0" style={{ background: color }} />
                  <span className="text-sm text-gray-700 capitalize">{type}</span>
                  <svg className="w-3 h-3 text-gray-300 ml-auto flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8h16M4 16h16" />
                  </svg>
                </div>
              ))}
            </div>
          </div>

          {/* Navmesh area legend */}
          <div className="p-4 border-b border-gray-100">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Walkable Areas</p>
            <div className="flex items-center gap-2 text-xs text-gray-500">
              <span className="inline-block w-8 h-4 rounded border border-blue-400 border-dashed" style={{ background: 'rgba(59,130,246,0.12)' }} />
              NavMesh polygon
            </div>
            <p className="text-xs text-gray-400 mt-2">Use "Draw Area" to define walkable zones</p>
          </div>

          {/* Properties / selection info */}
          {multiSelected && (
            <div className="p-4 border-t border-gray-100">
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Selection</h3>
              <div className="bg-blue-50 border border-blue-100 rounded-lg p-3 mb-3 text-sm text-blue-800">
                {store.selectedNodeIds.length > 0 && <p>{store.selectedNodeIds.length} node{store.selectedNodeIds.length !== 1 ? 's' : ''}</p>}
                {store.selectedAreaIds.length > 0 && <p>{store.selectedAreaIds.length} area{store.selectedAreaIds.length !== 1 ? 's' : ''}</p>}
              </div>
              <button onClick={handleDeleteSelected}
                className="w-full flex items-center justify-center gap-1.5 text-sm border border-red-200 text-red-600 py-2 rounded-lg hover:bg-red-50 transition-colors">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                Delete selected
              </button>
            </div>
          )}

          {!multiSelected && selectedNode && (
            <NodePropertiesPanel
              node={selectedNode}
              onUpdate={patch => handleUpdateNode(selectedNode.id, patch)}
              onDelete={() => handleDeleteNode(selectedNode.id)}
            />
          )}

          {!multiSelected && selectedArea && !selectedNode && (
            <NavMeshAreaPropertiesPanel
              area={selectedArea}
              onUpdate={label => handleUpdateAreaLabel(selectedArea.id, label)}
              onDelete={() => handleDeleteArea(selectedArea.id)}
            />
          )}

          {/* Shortcuts */}
          {!anySelected && (
            <div className="p-4 border-t border-gray-100 mt-auto">
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Shortcuts</p>
              <div className="space-y-1.5 text-xs text-gray-400">
                <p><kbd className="bg-gray-100 px-1 py-0.5 rounded text-gray-500">Del</kbd> delete selected</p>
                <p><kbd className="bg-gray-100 px-1 py-0.5 rounded text-gray-500">Esc</kbd> cancel mode</p>
                <p><kbd className="bg-gray-100 px-1 py-0.5 rounded text-gray-500">Shift+click</kbd> multi-select</p>
                <p><kbd className="bg-gray-100 px-1 py-0.5 rounded text-gray-500">Drag canvas</kbd> rubber-band</p>
                <p><kbd className="bg-gray-100 px-1 py-0.5 rounded text-gray-500">Ctrl+A</kbd> select all nodes</p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Status bar */}
      <div className="bg-white border-t border-gray-200 px-4 py-1.5 text-xs text-gray-500 flex items-center gap-5 flex-shrink-0">
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-blue-400" /> {store.nodes.length} nodes
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full border border-blue-400 border-dashed" style={{ background: 'rgba(59,130,246,0.12)' }} />
          {store.navMeshAreas.length} areas
        </span>
        {anySelected && (
          <span className="text-blue-600 font-medium">
            {store.selectedNodeIds.length + store.selectedAreaIds.length} selected
          </span>
        )}
        <span className={`ml-auto flex items-center gap-1 font-medium ${store.isDirty ? 'text-amber-600' : 'text-green-600'}`}>
          {store.isDirty ? (
            <><span className="w-1.5 h-1.5 bg-amber-500 rounded-full" /> Unsaved</>
          ) : (
            <><span className="w-1.5 h-1.5 bg-green-500 rounded-full" /> Saved</>
          )}
        </span>
      </div>

      {/* Toast */}
      {/* Toast */}
      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-gray-900 text-white text-sm px-4 py-2.5 rounded-xl shadow-xl z-50 flex items-center gap-2">
          <span className="w-1.5 h-1.5 bg-green-400 rounded-full" />
          {toast}
        </div>
      )}

      {/* AI Generation Overlay */}
      {aiGenerating && (
        <div className="absolute inset-0 z-[100] flex items-center justify-center bg-gray-900/60 backdrop-blur-sm transition-opacity duration-300">
          <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-sm w-full mx-4 border border-gray-100 transform transition-transform duration-300 scale-100">
            <div className="text-center">
              <div className="relative w-20 h-20 mx-auto mb-6">
                <svg className="w-full h-full transform -rotate-90">
                  <circle
                    cx="40" cy="40" r="36"
                    stroke="currentColor" strokeWidth="8" fill="transparent"
                    className="text-gray-100"
                  />
                  <circle
                    cx="40" cy="40" r="36"
                    stroke="currentColor" strokeWidth="8" fill="transparent"
                    strokeDasharray={226}
                    strokeDashoffset={226 - (226 * aiProgress) / 100}
                    strokeLinecap="round"
                    className="text-blue-600 transition-all duration-300 ease-out"
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <svg className="w-8 h-8 text-blue-600 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>
              </div>
              
              <h3 className="text-xl font-bold text-gray-900 mb-2">AI Path Generation</h3>
              <div className="h-6 mb-8 overflow-hidden relative">
                <div 
                  className="transition-transform duration-700 ease-in-out"
                  style={{ transform: `translateY(-${Math.min(AI_MESSAGES.length - 1, Math.floor((aiProgress / 100) * AI_MESSAGES.length)) * 24}px)` }}
                >
                  {AI_MESSAGES.map((msg, i) => (
                    <p key={i} className="h-6 text-blue-600 text-sm font-medium flex items-center justify-center">
                      {msg}
                    </p>
                  ))}
                </div>
              </div>
              
              <div className="space-y-4">
                <div className="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
                  <div 
                    className="bg-blue-600 h-full transition-all duration-300 ease-out"
                    style={{ width: `${aiProgress}%` }}
                  />
                </div>
                
                <div className="h-4" /> {/* Spacer */}

                <button
                  onClick={stopAiGeneration}
                  className="w-full py-2.5 rounded-xl border border-gray-200 text-gray-600 text-sm font-semibold hover:bg-gray-50 transition-colors"
                >
                  Cancel Generation
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
