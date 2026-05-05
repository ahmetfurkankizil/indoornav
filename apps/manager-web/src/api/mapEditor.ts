import {
  lsListNodes, lsCreateNode, lsUpdateNode, lsDeleteNode, lsGetBuildingIdForFloor,
  lsListAreas, lsCreateArea, lsUpdateArea, lsDeleteArea
} from './localDb'

export interface Waypoint { x: number; y: number }

export interface Node {
  id: string; floorId: string; buildingId: string
  label: string; nodeType: string
  canvasX: number; canvasY: number
  worldX?: number; worldY?: number; worldZ?: number
  metadata: string; createdAt: string; updatedAt: string
}

export interface NavMeshArea {
  id: string; floorId: string; buildingId: string
  label: string; vertices: Waypoint[]
  createdAt: string; updatedAt: string
}

export interface SuggestedEdge {
  fromNodeId: string; toNodeId: string
  waypoints: Waypoint[]; confidence: number
}

export interface SuggestedNode {
  id: string; label: string; nodeType: string
  canvasX: number; canvasY: number
}

// ── Nodes ───────────────────────────────────────────────────────────────────────

export const listNodes = async (floorId: string): Promise<Node[]> =>
  lsListNodes(floorId)

export const createNode = async (floorId: string, data: {
  label: string; nodeType: string; canvasX: number; canvasY: number; metadata?: string
}): Promise<Node> => {
  const buildingId = lsGetBuildingIdForFloor(floorId) ?? ''
  return lsCreateNode(floorId, buildingId, data)
}

export const updateNode = async (floorId: string, nodeId: string, data: {
  label?: string; nodeType?: string; canvasX?: number; canvasY?: number; metadata?: string
}): Promise<Node> => lsUpdateNode(nodeId, data)

export const deleteNode = async (_floorId: string, nodeId: string): Promise<void> =>
  lsDeleteNode(nodeId)

// ── NavMesh Areas ───────────────────────────────────────────────────────────────

export const listNavMeshAreas = async (floorId: string): Promise<NavMeshArea[]> =>
  lsListAreas(floorId)

export const createNavMeshArea = async (floorId: string, data: Partial<NavMeshArea>): Promise<NavMeshArea> => {
  const buildingId = lsGetBuildingIdForFloor(floorId) ?? ''
  return lsCreateArea(floorId, buildingId, data)
}

export const updateNavMeshArea = async (floorId: string, areaId: string, data: Partial<NavMeshArea>): Promise<NavMeshArea> =>
  lsUpdateArea(areaId, data)

export const deleteNavMeshArea = async (_floorId: string, areaId: string): Promise<void> =>
  lsDeleteArea(areaId)
