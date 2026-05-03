import type { Node } from './mapEditor'
import {
  lsListBuildings, lsGetBuilding, lsCreateBuilding, lsUpdateBuilding, lsDeleteBuilding,
  lsCreateFloor, lsUpdateFloor, lsDeleteFloor,
  lsListBuildingNodes, lsListConnections, lsCreateConnection, lsDeleteConnection,
  saveMapFile,
} from './localDb'

export interface Floor {
  id: string; buildingId: string; floorNumber: number; floorName: string
  boundsMinX?: number; boundsMaxX?: number; boundsMinZ?: number; boundsMaxZ?: number
  floorY?: number; uploadStatus: string; mapFileType: string; createdAt: string; updatedAt: string
}

export interface Building {
  id: string; managerId: string; name: string; description?: string; address?: string
  widthMeters: number; qrToken: string; status: string; createdAt: string; updatedAt: string; floors?: Floor[]
}

export interface FloorConnection {
  id: string; buildingId: string; fromNodeId: string; toNodeId: string
  connectionType: string; isBidirectional: boolean; createdAt: string
}

export const listBuildings = async (): Promise<Building[]> => lsListBuildings()

export const createBuilding = async (data: { name: string; description?: string; address?: string; widthMeters?: number }): Promise<Building> =>
  lsCreateBuilding(data)

export const getBuilding = async (id: string): Promise<Building> => {
  const b = lsGetBuilding(id)
  if (!b) throw new Error('Building not found')
  return b
}

export const updateBuilding = async (id: string, data: { name?: string; description?: string; address?: string; widthMeters?: number }): Promise<Building> =>
  lsUpdateBuilding(id, data)

export const deleteBuilding = async (id: string): Promise<void> => lsDeleteBuilding(id)

export const publishBuilding = async (id: string): Promise<Building> =>
  lsUpdateBuilding(id, { status: 'published' })

export const getQrUrl = (_id: string) => ''

export const listBuildingNodes = async (id: string): Promise<Node[]> => lsListBuildingNodes(id)

// ── Floors ─────────────────────────────────────────────────────────────────────

export const listFloors = async (buildingId: string): Promise<Floor[]> =>
  (lsGetBuilding(buildingId)?.floors ?? [])

export const createFloor = async (buildingId: string, form: FormData): Promise<Floor> => {
  const file        = form.get('mapFile') as File | null
  const floorNumber = Number(form.get('floorNumber') ?? 0)
  const floorName   = String(form.get('floorName') ?? 'Floor')
  const floor = lsCreateFloor(buildingId, {
    floorNumber, floorName,
    mapFileType: file?.type ?? '',
  })
  if (file) await saveMapFile(buildingId, floor.id, file)
  return floor
}

export const replaceFloor = async (buildingId: string, floorId: string, form: FormData): Promise<Floor> => {
  const file = form.get('mapFile') as File | null
  const floor = lsUpdateFloor(buildingId, floorId, {
    uploadStatus: file ? 'ready' : 'none',
    mapFileType:  file?.type ?? '',
  })
  if (file) await saveMapFile(buildingId, floorId, file)
  return floor
}

export const deleteFloor = async (buildingId: string, floorId: string): Promise<void> =>
  lsDeleteFloor(buildingId, floorId)

export const updateFloorBounds = async (
  buildingId: string, floorId: string,
  bounds: { minX: number; maxX: number; minZ: number; maxZ: number; floorY: number },
): Promise<Floor> =>
  lsUpdateFloor(buildingId, floorId, {
    boundsMinX: bounds.minX, boundsMaxX: bounds.maxX,
    boundsMinZ: bounds.minZ, boundsMaxZ: bounds.maxZ,
    floorY: bounds.floorY,
  })

export const getGlbUrl    = (_buildingId: string, _floorId: string) => ''
export const getMapFileUrl = getGlbUrl

// ── Floor connections ───────────────────────────────────────────────────────────

export const listConnections = async (buildingId: string): Promise<FloorConnection[]> =>
  lsListConnections(buildingId)

export const createConnection = async (
  buildingId: string,
  data: { fromNodeId: string; toNodeId: string; connectionType: string; isBidirectional?: boolean },
): Promise<FloorConnection> => lsCreateConnection(buildingId, data)

export const deleteConnection = async (_buildingId: string, connectionId: string): Promise<void> =>
  lsDeleteConnection(connectionId)
