import type { Building, Floor, FloorConnection } from './buildings'
import type { Node } from './mapEditor'

const LS_BUILDINGS = 'Vectura AI_buildings'
const LS_NODES = 'Vectura AI_nodes'
const LS_CONNS = 'Vectura AI_connections'

function uid() { return crypto.randomUUID() }
function now() { return new Date().toISOString() }
function load<T>(key: string): T[] { try { return JSON.parse(localStorage.getItem(key) ?? '[]') } catch { return [] } }
function save<T>(key: string, v: T[]) { localStorage.setItem(key, JSON.stringify(v)) }

// ── Buildings ───────────────────────────────────────────────────────────────────

export function lsListBuildings(): Building[] { return load<Building>(LS_BUILDINGS) }

export function lsGetBuilding(id: string): Building | null {
  return lsListBuildings().find(b => b.id === id) ?? null
}

export function lsCreateBuilding(data: { name: string; description?: string; address?: string; widthMeters?: number }): Building {
  const b: Building = {
    id: uid(), managerId: 'local-manager',
    name: data.name, description: data.description, address: data.address,
    widthMeters: data.widthMeters ?? 50,
    qrToken: uid(), status: 'draft',
    createdAt: now(), updatedAt: now(), floors: [],
  }
  save(LS_BUILDINGS, [...lsListBuildings(), b])
  return b
}

export function lsUpdateBuilding(id: string, data: Partial<Building>): Building {
  const all = lsListBuildings()
  const i = all.findIndex(b => b.id === id)
  if (i === -1) throw new Error('Building not found')
  all[i] = { ...all[i], ...data, updatedAt: now() }
  save(LS_BUILDINGS, all)
  return all[i]
}

export function lsDeleteBuilding(id: string): void {
  save(LS_BUILDINGS, lsListBuildings().filter(b => b.id !== id))
  save(LS_NODES, load<Node>(LS_NODES).filter(n => n.buildingId !== id))
  save(LS_CONNS, load<FloorConnection>(LS_CONNS).filter(c => c.buildingId !== id))
}

// ── Floors (nested inside Building.floors) ─────────────────────────────────────

export function lsCreateFloor(buildingId: string, data: { floorNumber: number; floorName: string; mapFileType?: string }): Floor {
  const floor: Floor = {
    id: uid(), buildingId,
    floorNumber: data.floorNumber, floorName: data.floorName,
    uploadStatus: data.mapFileType ? 'ready' : 'none',
    mapFileType: data.mapFileType ?? '',
    createdAt: now(), updatedAt: now(),
  }
  const all = lsListBuildings()
  const i = all.findIndex(b => b.id === buildingId)
  if (i === -1) throw new Error('Building not found')
  all[i] = { ...all[i], floors: [...(all[i].floors ?? []), floor], updatedAt: now() }
  save(LS_BUILDINGS, all)
  return floor
}

export function lsUpdateFloor(buildingId: string, floorId: string, data: Partial<Floor>): Floor {
  const all = lsListBuildings()
  const bi = all.findIndex(b => b.id === buildingId)
  if (bi === -1) throw new Error('Building not found')
  const floors = all[bi].floors ?? []
  const fi = floors.findIndex(f => f.id === floorId)
  if (fi === -1) throw new Error('Floor not found')
  floors[fi] = { ...floors[fi], ...data, updatedAt: now() }
  all[bi] = { ...all[bi], floors, updatedAt: now() }
  save(LS_BUILDINGS, all)
  return floors[fi]
}

export function lsDeleteFloor(buildingId: string, floorId: string): void {
  const all = lsListBuildings()
  const i = all.findIndex(b => b.id === buildingId)
  if (i === -1) return
  all[i] = { ...all[i], floors: (all[i].floors ?? []).filter(f => f.id !== floorId), updatedAt: now() }
  save(LS_BUILDINGS, all)
  save(LS_NODES, load<Node>(LS_NODES).filter(n => n.floorId !== floorId))
}

export function lsGetBuildingIdForFloor(floorId: string): string | null {
  for (const b of lsListBuildings()) {
    if ((b.floors ?? []).some(f => f.id === floorId)) return b.id
  }
  return null
}

// ── Nodes ───────────────────────────────────────────────────────────────────────

export function lsListNodes(floorId: string): Node[] {
  return load<Node>(LS_NODES).filter(n => n.floorId === floorId)
}

export function lsListBuildingNodes(buildingId: string): Node[] {
  return load<Node>(LS_NODES).filter(n => n.buildingId === buildingId)
}

export function lsCreateNode(
  floorId: string, buildingId: string,
  data: { label: string; nodeType: string; canvasX: number; canvasY: number; metadata?: string },
): Node {
  const node: Node = {
    id: uid(), floorId, buildingId,
    label: data.label, nodeType: data.nodeType,
    canvasX: data.canvasX, canvasY: data.canvasY,
    metadata: data.metadata ?? '{}',
    createdAt: now(), updatedAt: now(),
  }
  save(LS_NODES, [...load<Node>(LS_NODES), node])
  return node
}

export function lsUpdateNode(nodeId: string, data: Partial<Node>): Node {
  const all = load<Node>(LS_NODES)
  const i = all.findIndex(n => n.id === nodeId)
  if (i === -1) throw new Error('Node not found')
  all[i] = { ...all[i], ...data, updatedAt: now() }
  save(LS_NODES, all)
  return all[i]
}

export function lsDeleteNode(nodeId: string): void {
  save(LS_NODES, load<Node>(LS_NODES).filter(n => n.id !== nodeId))
}

// ── Floor connections ───────────────────────────────────────────────────────────

export function lsListConnections(buildingId: string): FloorConnection[] {
  return load<FloorConnection>(LS_CONNS).filter(c => c.buildingId === buildingId)
}

export function lsCreateConnection(buildingId: string, data: { fromNodeId: string; toNodeId: string; connectionType: string; isBidirectional?: boolean }): FloorConnection {
  const conn: FloorConnection = {
    id: uid(), buildingId,
    fromNodeId: data.fromNodeId, toNodeId: data.toNodeId,
    connectionType: data.connectionType,
    isBidirectional: data.isBidirectional ?? true,
    createdAt: now(),
  }
  save(LS_CONNS, [...load<FloorConnection>(LS_CONNS), conn])
  return conn
}

export function lsDeleteConnection(id: string): void {
  save(LS_CONNS, load<FloorConnection>(LS_CONNS).filter(c => c.id !== id))
}

// ── IndexedDB — map file blobs ──────────────────────────────────────────────────

const IDB_NAME = 'Vectura AI_files'
const IDB_STORE = 'map_files'

function openIdb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_NAME, 1)
    req.onupgradeneeded = () => {
      if (!req.result.objectStoreNames.contains(IDB_STORE)) {
        req.result.createObjectStore(IDB_STORE)
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

export async function saveMapFile(buildingId: string, floorId: string, file: File): Promise<void> {
  const db = await openIdb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, 'readwrite')
    const req = tx.objectStore(IDB_STORE).put({ blob: file, mimeType: file.type, name: file.name }, `${buildingId}_${floorId}`)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

export async function getMapFile(buildingId: string, floorId: string): Promise<{ blob: Blob; mimeType: string; name: string } | null> {
  const db = await openIdb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, 'readonly')
    const req = tx.objectStore(IDB_STORE).get(`${buildingId}_${floorId}`)
    req.onsuccess = () => resolve(req.result ?? null)
    req.onerror = () => reject(req.error)
  })
}
