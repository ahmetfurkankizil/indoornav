import { api } from './client'

export interface Waypoint { x: number; y: number }

export interface Node {
  id: string; floorId: string; buildingId: string
  label: string; nodeType: string
  canvasX: number; canvasY: number
  worldX?: number; worldY?: number; worldZ?: number
  metadata: string; createdAt: string; updatedAt: string
}

export interface Edge {
  id: string; floorId: string; buildingId: string
  fromNodeId: string; toNodeId: string
  isBidirectional: boolean; distance?: number
  edgeType: string; waypoints: Waypoint[]
  createdBy: string; createdAt: string; updatedAt: string
}

export interface SuggestedEdge {
  fromNodeId: string; toNodeId: string
  waypoints: Waypoint[]; confidence: number
}

export interface SuggestedNode {
  id: string; label: string; nodeType: string
  canvasX: number; canvasY: number
}

// ── Nodes ──────────────────────────────────────────────────────────────────────

export const listNodes = (floorId: string) =>
  api.get<Node[]>(`/api/manager/floors/${floorId}/nodes`).then(r => r.data)

export const createNode = (floorId: string, data: {
  label: string; nodeType: string; canvasX: number; canvasY: number; metadata?: string
}) =>
  api.post<Node>(`/api/manager/floors/${floorId}/nodes`, data).then(r => r.data)

export const updateNode = (floorId: string, nodeId: string, data: {
  label?: string; nodeType?: string; canvasX?: number; canvasY?: number; metadata?: string
}) =>
  api.put<Node>(`/api/manager/floors/${floorId}/nodes/${nodeId}`, data).then(r => r.data)

export const deleteNode = (floorId: string, nodeId: string) =>
  api.delete(`/api/manager/floors/${floorId}/nodes/${nodeId}`)

// ── Edges ──────────────────────────────────────────────────────────────────────

export const listEdges = (floorId: string) =>
  api.get<Edge[]>(`/api/manager/floors/${floorId}/edges`).then(r => r.data)

export const createEdge = (floorId: string, data: {
  fromNodeId: string; toNodeId: string
  edgeType?: string; isBidirectional?: boolean
  waypoints?: Waypoint[]; createdBy?: string
}) =>
  api.post<Edge>(`/api/manager/floors/${floorId}/edges`, data).then(r => r.data)

export const updateEdge = (floorId: string, edgeId: string, data: {
  edgeType?: string; isBidirectional?: boolean; waypoints?: Waypoint[]
}) =>
  api.put<Edge>(`/api/manager/floors/${floorId}/edges/${edgeId}`, data).then(r => r.data)

export const deleteEdge = (floorId: string, edgeId: string) =>
  api.delete(`/api/manager/floors/${floorId}/edges/${edgeId}`)

export const aiSuggestEdges = (floorId: string, floorPlanImageBase64: string, nodes: Node[]) =>
  api.post<{ nodes?: SuggestedNode[], edges: SuggestedEdge[] }>(`/api/manager/floors/${floorId}/edges/ai-suggest`, {
    floorPlanImageBase64,
    nodes: nodes.map(n => ({
      id: n.id, label: n.label, nodeType: n.nodeType,
      canvasX: n.canvasX, canvasY: n.canvasY,
    })),
  }).then(r => r.data)
