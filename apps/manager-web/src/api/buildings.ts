import { api } from './client'
import type { Node } from './mapEditor'

export interface Floor {
  id: string; buildingId: string; floorNumber: number; floorName: string
  boundsMinX?: number; boundsMaxX?: number; boundsMinZ?: number; boundsMaxZ?: number
  floorY?: number; uploadStatus: string; mapFileType: string; createdAt: string; updatedAt: string
}

export interface Building {
  id: string; managerId: string; name: string; description?: string; address?: string
  widthMeters: number; qrToken: string; status: string; createdAt: string; updatedAt: string; floors?: Floor[]
}

export const listBuildings = () =>
  api.get<Building[]>('/api/manager/buildings').then(r => r.data)

export const createBuilding = (data: { name: string; description?: string; address?: string; widthMeters?: number }) =>
  api.post<Building>('/api/manager/buildings', data).then(r => r.data)

export const getBuilding = (id: string) =>
  api.get<Building>(`/api/manager/buildings/${id}`).then(r => r.data)

export const updateBuilding = (id: string, data: { name?: string; description?: string; address?: string; widthMeters?: number }) =>
  api.put<Building>(`/api/manager/buildings/${id}`, data).then(r => r.data)

export const deleteBuilding = (id: string) =>
  api.delete(`/api/manager/buildings/${id}`)

export const publishBuilding = (id: string) =>
  api.post(`/api/manager/buildings/${id}/publish`).then(r => r.data)

export const getQrUrl = (id: string) =>
  `/api/manager/buildings/${id}/qr`

export const listBuildingNodes = (id: string) =>
  api.get<Node[]>(`/api/manager/buildings/${id}/nodes`).then(r => r.data)

// ── Floors ────────────────────────────────────────────────────────────────────

export const listFloors = (buildingId: string) =>
  api.get<Floor[]>(`/api/manager/buildings/${buildingId}/floors`).then(r => r.data)

export const createFloor = (buildingId: string, form: FormData) =>
  api.post<Floor>(`/api/manager/buildings/${buildingId}/floors`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data)

export const replaceFloor = (buildingId: string, floorId: string, form: FormData) =>
  api.put<Floor>(`/api/manager/buildings/${buildingId}/floors/${floorId}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data)

export const deleteFloor = (buildingId: string, floorId: string) =>
  api.delete(`/api/manager/buildings/${buildingId}/floors/${floorId}`)

export const updateFloorBounds = (
  buildingId: string,
  floorId: string,
  bounds: { minX: number; maxX: number; minZ: number; maxZ: number; floorY: number }
) =>
  api.put<Floor>(`/api/manager/buildings/${buildingId}/floors/${floorId}/bounds`, bounds).then(r => r.data)

export const getGlbUrl = (buildingId: string, floorId: string) =>
  `/api/manager/buildings/${buildingId}/floors/${floorId}/glb`

export const getMapFileUrl = getGlbUrl

// ── Floor connections ─────────────────────────────────────────────────────────

export interface FloorConnection {
  id: string; buildingId: string; fromNodeId: string; toNodeId: string
  connectionType: string; isBidirectional: boolean; createdAt: string
}

export const listConnections = (buildingId: string) =>
  api.get<FloorConnection[]>(`/api/manager/buildings/${buildingId}/connections`).then(r => r.data)

export const createConnection = (
  buildingId: string,
  data: { fromNodeId: string; toNodeId: string; connectionType: string; isBidirectional?: boolean }
) =>
  api.post<FloorConnection>(`/api/manager/buildings/${buildingId}/connections`, data).then(r => r.data)

export const deleteConnection = (buildingId: string, connectionId: string) =>
  api.delete(`/api/manager/buildings/${buildingId}/connections/${connectionId}`)
