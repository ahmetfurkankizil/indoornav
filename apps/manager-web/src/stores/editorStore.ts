import { create } from 'zustand'
import type { Node, Edge, SuggestedEdge, SuggestedNode } from '../api/mapEditor'

export type EditorMode = 'select' | 'addNode' | 'addEdge'

export interface EditorState {
  mode: EditorMode
  nodes: Node[]
  edges: Edge[]
  suggestedNodes: SuggestedNode[]
  suggestedEdges: SuggestedEdge[]
  selectedNodeId: string | null
  selectedEdgeId: string | null
  pendingEdgeFromId: string | null  // first node selected in addEdge mode
  isDirty: boolean
  isAiLoading: boolean
  floorPlanDataUrl: string | null
  bounds: { minX: number; maxX: number; minZ: number; maxZ: number; floorY: number } | null

  setMode: (mode: EditorMode) => void
  setNodes: (nodes: Node[]) => void
  setEdges: (edges: Edge[]) => void
  addNode: (node: Node) => void
  updateNodeInStore: (node: Node) => void
  removeNode: (nodeId: string) => void
  addEdge: (edge: Edge) => void
  updateEdgeInStore: (edge: Edge) => void
  removeEdge: (edgeId: string) => void
  setSuggestions: (nodes: SuggestedNode[], edges: SuggestedEdge[]) => void
  setSuggestedEdges: (edges: SuggestedEdge[]) => void
  clearSuggestions: () => void
  setSelectedNode: (id: string | null) => void
  setSelectedEdge: (id: string | null) => void
  setPendingEdgeFrom: (id: string | null) => void
  setDirty: (dirty: boolean) => void
  setAiLoading: (loading: boolean) => void
  setFloorPlan: (dataUrl: string, bounds: EditorState['bounds']) => void
}

export const useEditorStore = create<EditorState>((set) => ({
  mode: 'select',
  nodes: [],
  edges: [],
  suggestedNodes: [],
  suggestedEdges: [],
  selectedNodeId: null,
  selectedEdgeId: null,
  pendingEdgeFromId: null,
  isDirty: false,
  isAiLoading: false,
  floorPlanDataUrl: null,
  bounds: null,

  setMode: (mode) => set({ mode, selectedNodeId: null, selectedEdgeId: null, pendingEdgeFromId: null }),
  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),
  addNode: (node) => set((s) => ({ nodes: [...s.nodes, node], isDirty: true })),
  updateNodeInStore: (node) => set((s) => ({ nodes: s.nodes.map(n => n.id === node.id ? node : n) })),
  removeNode: (id) => set((s) => ({
    nodes: s.nodes.filter(n => n.id !== id),
    edges: s.edges.filter(e => e.fromNodeId !== id && e.toNodeId !== id),
    isDirty: true,
  })),
  addEdge: (edge) => set((s) => ({ edges: [...s.edges, edge], isDirty: true })),
  updateEdgeInStore: (edge) => set((s) => ({ edges: s.edges.map(e => e.id === edge.id ? edge : e) })),
  removeEdge: (id) => set((s) => ({ edges: s.edges.filter(e => e.id !== id), isDirty: true })),
  setSuggestions: (nodes, edges) => set({ suggestedNodes: nodes, suggestedEdges: edges }),
  setSuggestedEdges: (edges) => set({ suggestedEdges: edges }),
  clearSuggestions: () => set({ suggestedNodes: [], suggestedEdges: [] }),
  setSelectedNode: (id) => set({ selectedNodeId: id, selectedEdgeId: null }),
  setSelectedEdge: (id) => set({ selectedEdgeId: id, selectedNodeId: null }),
  setPendingEdgeFrom: (id) => set({ pendingEdgeFromId: id }),
  setDirty: (dirty) => set({ isDirty: dirty }),
  setAiLoading: (loading) => set({ isAiLoading: loading }),
  setFloorPlan: (dataUrl, bounds) => set({ floorPlanDataUrl: dataUrl, bounds }),
}))
