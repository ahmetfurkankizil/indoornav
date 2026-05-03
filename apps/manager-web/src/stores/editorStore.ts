import { create } from 'zustand'
import type { Node, NavMeshArea, SuggestedNode, Waypoint } from '../api/mapEditor'

export type EditorMode = 'select' | 'addNode' | 'drawArea'

export interface EditorState {
  mode: EditorMode
  selectedNodeType: string
  nodes: Node[]
  navMeshAreas: NavMeshArea[]
  suggestedNodes: SuggestedNode[]
  selectedNodeIds: string[]
  selectedAreaIds: string[]
  pendingAreaVertices: Waypoint[]
  isDirty: boolean
  isAiLoading: boolean
  floorPlanDataUrl: string | null
  bounds: { minX: number; maxX: number; minZ: number; maxZ: number; floorY: number } | null

  setMode: (mode: EditorMode) => void
  setSelectedNodeType: (type: string) => void
  setNodes: (nodes: Node[]) => void
  setNavMeshAreas: (areas: NavMeshArea[]) => void
  addNode: (node: Node) => void
  updateNodeInStore: (node: Node) => void
  removeNode: (nodeId: string) => void
  removeSelectedItems: () => void
  addNavMeshArea: (area: NavMeshArea) => void
  updateNavMeshAreaInStore: (area: NavMeshArea) => void
  removeNavMeshArea: (areaId: string) => void
  setSuggestedNodes: (nodes: SuggestedNode[]) => void
  clearSuggestions: () => void
  setSelectedNode: (id: string | null) => void
  setSelectedNodes: (ids: string[]) => void
  toggleNodeSelection: (id: string, additive: boolean) => void
  toggleAreaSelection: (id: string, additive: boolean) => void
  selectItemsInRect: (rect: { x: number; y: number; w: number; h: number }, canvasSize: number) => void
  clearSelection: () => void
  addPendingVertex: (v: Waypoint) => void
  clearPendingVertices: () => void
  setDirty: (dirty: boolean) => void
  setAiLoading: (loading: boolean) => void
  setFloorPlan: (dataUrl: string, bounds: EditorState['bounds']) => void
  selectAllItems: () => void
}

export const useEditorStore = create<EditorState>((set) => ({
  mode: 'select',
  selectedNodeType: 'room',
  nodes: [],
  navMeshAreas: [],
  suggestedNodes: [],
  selectedNodeIds: [],
  selectedAreaIds: [],
  pendingAreaVertices: [],
  isDirty: false,
  isAiLoading: false,
  floorPlanDataUrl: null,
  bounds: null,

  setMode: (mode) => set({ mode, selectedNodeIds: [], selectedAreaIds: [], pendingAreaVertices: [] }),
  setSelectedNodeType: (type) => set({ selectedNodeType: type }),
  setNodes: (nodes) => set({ nodes }),
  setNavMeshAreas: (navMeshAreas) => set({ navMeshAreas }),

  addNode: (node) => set((s) => ({ nodes: [...s.nodes, node], isDirty: true })),
  updateNodeInStore: (node) => set((s) => ({ nodes: s.nodes.map(n => n.id === node.id ? node : n) })),
  removeNode: (id) => set((s) => ({
    nodes: s.nodes.filter(n => n.id !== id),
    selectedNodeIds: s.selectedNodeIds.filter(nid => nid !== id),
    isDirty: true,
  })),
  removeSelectedItems: () => set((s) => {
    const nodeSet = new Set(s.selectedNodeIds)
    const areaSet = new Set(s.selectedAreaIds)
    const hasChanges = nodeSet.size > 0 || areaSet.size > 0
    return {
      nodes: s.nodes.filter(n => !nodeSet.has(n.id)),
      navMeshAreas: s.navMeshAreas.filter(a => !areaSet.has(a.id)),
      selectedNodeIds: [],
      selectedAreaIds: [],
      isDirty: hasChanges || s.isDirty,
    }
  }),

  addNavMeshArea: (area) => set((s) => ({ navMeshAreas: [...s.navMeshAreas, area], isDirty: true })),
  updateNavMeshAreaInStore: (area) => set((s) => ({
    navMeshAreas: s.navMeshAreas.map(a => a.id === area.id ? area : a),
  })),
  removeNavMeshArea: (id) => set((s) => ({
    navMeshAreas: s.navMeshAreas.filter(a => a.id !== id),
    selectedAreaIds: s.selectedAreaIds.filter(aid => aid !== id),
    isDirty: true,
  })),

  setSuggestedNodes: (nodes) => set({ suggestedNodes: nodes }),
  clearSuggestions: () => set({ suggestedNodes: [] }),

  setSelectedNode: (id) => set({ selectedNodeIds: id ? [id] : [], selectedAreaIds: [] }),
  setSelectedNodes: (ids) => set({ selectedNodeIds: ids, selectedAreaIds: [] }),

  toggleNodeSelection: (id, additive) => set((s) => {
    if (!additive) return { selectedNodeIds: [id], selectedAreaIds: [] }
    const exists = s.selectedNodeIds.includes(id)
    return { selectedNodeIds: exists ? s.selectedNodeIds.filter(nid => nid !== id) : [...s.selectedNodeIds, id] }
  }),
  toggleAreaSelection: (id, additive) => set((s) => {
    if (!additive) return { selectedAreaIds: [id], selectedNodeIds: [] }
    const exists = s.selectedAreaIds.includes(id)
    return { selectedAreaIds: exists ? s.selectedAreaIds.filter(aid => aid !== id) : [...s.selectedAreaIds, id] }
  }),

  selectItemsInRect: (rect, canvasSize) => set((s) => {
    const x1 = Math.min(rect.x, rect.x + rect.w)
    const y1 = Math.min(rect.y, rect.y + rect.h)
    const x2 = Math.max(rect.x, rect.x + rect.w)
    const y2 = Math.max(rect.y, rect.y + rect.h)
    
    const nodeIds = s.nodes
      .filter(n => {
        const nx = n.canvasX * canvasSize
        const ny = n.canvasY * canvasSize
        return nx >= x1 && nx <= x2 && ny >= y1 && ny <= y2
      })
      .map(n => n.id)

    const areaIds = s.navMeshAreas
      .filter(a => a.vertices.some(v => {
        const ax = v.x * canvasSize
        const ay = v.y * canvasSize
        return ax >= x1 && ax <= x2 && ay >= y1 && ay <= y2
      }))
      .map(a => a.id)

    return { selectedNodeIds: nodeIds, selectedAreaIds: areaIds }
  }),

  clearSelection: () => set({ selectedNodeIds: [], selectedAreaIds: [] }),
  addPendingVertex: (v) => set((s) => ({ pendingAreaVertices: [...s.pendingAreaVertices, v] })),
  clearPendingVertices: () => set({ pendingAreaVertices: [] }),
  setDirty: (dirty) => set({ isDirty: dirty }),
  setAiLoading: (loading) => set({ isAiLoading: loading }),
  setFloorPlan: (dataUrl, bounds) => set({ floorPlanDataUrl: dataUrl, bounds }),
  selectAllItems: () => set((s) => ({
    selectedNodeIds: s.nodes.map(n => n.id),
    selectedAreaIds: s.navMeshAreas.map(a => a.id),
  })),
}))
