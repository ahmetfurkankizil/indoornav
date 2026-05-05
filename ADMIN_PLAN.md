# VecturAI Web Admin Panel — Implementation Plan (Revised)

> Status: **DRAFT v2 — incorporating feedback 2026-04-30**
> Author: Claude Code

---

## 1. Goals & Scope

Replace the iOS-embedded admin tools with two distinct web panels:

1. **Manager Panel** — for building managers (client-facing): sign up, log in, upload floor maps, edit node/edge graphs, publish buildings.
2. **DB Admin Panel** — for system operators/developers: full database management, view all entities including raw navigation JSON.

iOS app retains only the visitor flow. All admin tools are removed from the mobile app.

---

## 2. Two-Panel Model

| | Manager Panel | DB Admin Panel |
|---|---|---|
| Users | Building managers (sign up themselves) | System operators (pre-configured credentials) |
| URL prefix | `/` (the main SPA) | `/db-admin` (separate protected section) |
| Auth | JWT — issued on signup/login | JWT — issued against env-var credentials only |
| Can see nav JSON | No | Yes |
| Can delete any manager | No | Yes |
| Scope | Own buildings only | All data |

---

## 3. Tech Stack Decisions

### 3.1 Frontend — Manager Panel

| Concern | Choice | Reason |
|---|---|---|
| Framework | **React 18 + TypeScript** | Rich client-side state for map editor |
| Build tool | **Vite** | Fast HMR |
| Styling | **Tailwind CSS** | Utility-first |
| 2D map editor | **Konva.js (`react-konva`)** | Canvas pan/zoom, node/edge interactions |
| GLB → 2D rendering | **Three.js (client-side)** | `GLTFLoader` + `OrthographicCamera` top-down render in browser; no server-side image pipeline |
| QR display | **qrcode.react** | Client-side QR rendering |
| HTTP client | **Axios** | Auth token interceptor |
| State | **React Query + Zustand** | Server state + editor state |
| Auth storage | **httpOnly cookie (JWT)** | XSS-safe |

### 3.2 Frontend — DB Admin Panel

Same React app, separate route namespace (`/db-admin/*`), separate auth context. No Konva/Three.js needed — plain table views + JSON viewer.

### 3.3 Backend

| Concern | Choice | Reason |
|---|---|---|
| Runtime | **Extend existing Ktor server** (`tools/admin-api/`) | Single JVM process, nav-preprocessor available |
| Database | **PostgreSQL** | Relational, JSONB support |
| ORM | **Exposed (JetBrains)** | Kotlin-native |
| Manager auth | **JWT (ktor-auth-jwt)** | Stateless |
| DB admin auth | **Separate JWT** with `role=superadmin` claim, credentials from env vars `DB_ADMIN_USER` / `DB_ADMIN_PASS` | No signup exposure |
| Password hashing | **BCrypt** | Standard |
| QR generation | **ZXing (JVM)** | Already in JVM ecosystem |
| File storage | **Local filesystem** | `uploads/` directory under server working dir |
| AI edge suggestion | **Anthropic Claude API (claude-haiku-4-5)** | Fast, cheap, vision-capable |

### 3.4 Mobile Changes

- Remove all `apps/iosApp/iosApp/admin/` files and admin gear icon
- Update QR scan to `VecturAI-building` contract
- Add nav package download + checksum-based disk cache
- Multi-floor Dijkstra

---

## 4. System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Browser — Single React SPA                                   │
│  ┌────────────────────────────┐  ┌──────────────────────────┐│
│  │  Manager Panel             │  │  DB Admin Panel           ││
│  │  /login  /signup           │  │  /db-admin/login          ││
│  │  /dashboard                │  │  /db-admin/managers       ││
│  │  /buildings/:id            │  │  /db-admin/buildings      ││
│  │  /editor/:floorId          │  │  /db-admin/nav-packages   ││
│  │  Three.js GLB → top-down   │  │  (raw JSON visible here)  ││
│  │  Konva canvas editor       │  │                           ││
│  └────────────┬───────────────┘  └────────────┬─────────────┘│
└───────────────┼──────────────────────────────┼───────────────┘
                │ JWT (manager)                 │ JWT (superadmin)
                │ /api/manager/*                │ /api/db-admin/*
┌───────────────▼───────────────────────────────▼───────────────┐
│  Admin API — Ktor (tools/admin-api/)                           │
│  ┌──────────────┐ ┌───────────────┐ ┌────────────────────┐   │
│  │ /auth/*      │ │ /api/manager/ │ │ /api/db-admin/     │   │
│  │ Manager auth │ │ buildings     │ │ Full CRUD + JSON    │   │
│  │              │ │ floors        │ │ viewer              │   │
│  │ /db-admin/   │ │ nodes / edges │ │                    │   │
│  │ auth/*       │ │ connections   │ │                    │   │
│  └──────────────┘ └───────────────┘ └────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ NavPackageGenerator  (nodes+edges → Dijkstra JSON → DB)  │ │
│  └──────────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Legacy: DraftJobRoutes + nav-preprocessor (kept, unused  │ │
│  │ by new flow but preserved for backward compatibility)    │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────┬──────────────────────────┬──────────┘
                          │                          │
              ┌───────────▼──────┐      ┌────────────▼─────────┐
              │   PostgreSQL      │      │  Local Filesystem     │
              │   (schema below)  │      │  uploads/             │
              └───────────────────┘      │  ├── glb/             │
                                         │  └── qr/              │
                                         └──────────────────────┘
                                                   │
              ┌────────────────────────────────────▼─────────────┐
              │  /mobile/buildings/{token}/nav-package            │
              │  (public endpoint — no manager auth required)     │
              └────────────────────────────────────┬─────────────┘
                                                   │
┌──────────────────────────────────────────────────▼─────────────┐
│  iOS App (visitor-only)                                          │
│  QR scan → download nav package → checksum cache → Dijkstra     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Database Schema

> All "admin" terminology replaced with "manager" in entity names.
> `navigation_packages.package_json` is NEVER returned by manager-facing API routes.

```sql
-- ─────────────────────────────────────────
-- MANAGERS  (building managers, self-service signup)
-- ─────────────────────────────────────────
CREATE TABLE managers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(320) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- BUILDINGS  (one QR code per building, covers all floors)
-- ─────────────────────────────────────────
CREATE TABLE buildings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id      UUID NOT NULL REFERENCES managers(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    address         TEXT,
    -- Opaque token embedded in QR payload; not the same as the UUID
    qr_token        VARCHAR(64) UNIQUE NOT NULL,
    -- Path to generated QR PNG in local filesystem
    qr_image_path   VARCHAR(512),
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',   -- draft | published
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- FLOORS  (one GLB per floor per building)
-- ─────────────────────────────────────────
CREATE TABLE floors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id     UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    floor_number    INTEGER NOT NULL,    -- -2,-1=basement  0=ground  1,2…=upper
    floor_name      VARCHAR(100) NOT NULL,   -- "Ground Floor", "Level 2"
    -- Relative path under uploads/glb/
    glb_file_path   VARCHAR(512) NOT NULL,
    -- GLB world-space bounding box, sent from browser Three.js renderer on first save
    -- Used server-side to convert canvas (0–1) ↔ world-space 3D coordinates
    bounds_min_x    FLOAT,
    bounds_max_x    FLOAT,
    bounds_min_z    FLOAT,
    bounds_max_z    FLOAT,
    floor_y         FLOAT,              -- Y elevation of this floor in world space
    -- Upload status only; no server-side image processing pipeline
    upload_status   VARCHAR(20) NOT NULL DEFAULT 'ready',   -- ready | replacing
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (building_id, floor_number)
);

-- ─────────────────────────────────────────
-- NODES  (room tags and waypoints placed on the 2D canvas)
-- ─────────────────────────────────────────
CREATE TABLE nodes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_id    UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    building_id UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    label       VARCHAR(255) NOT NULL,
    node_type   VARCHAR(30) NOT NULL DEFAULT 'room',
    -- room | corridor | entrance | elevator | stairs | exit | destination
    -- Normalized canvas position: 0.0 = left/top, 1.0 = right/bottom
    canvas_x    FLOAT NOT NULL,
    canvas_y    FLOAT NOT NULL,
    -- 3D world-space position, computed server-side from canvas + bounds
    world_x     FLOAT,
    world_y     FLOAT,    -- equals floor_y of the parent floor
    world_z     FLOAT,
    -- Optional extra properties (description, category, accessibility flags, etc.)
    metadata    JSONB NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- EDGES  (walkable paths between nodes on the same floor)
-- ─────────────────────────────────────────
CREATE TABLE edges (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_id         UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    building_id      UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    from_node_id     UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    to_node_id       UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    is_bidirectional BOOLEAN NOT NULL DEFAULT TRUE,
    -- Euclidean distance in metres, auto-computed from world positions on save
    distance         FLOAT,
    edge_type        VARCHAR(20) NOT NULL DEFAULT 'walk',   -- walk | stairs | elevator
    -- Optional intermediate control points (canvas-space 0–1) for curved AR paths
    -- Array of { "x": float, "y": float }
    waypoints        JSONB NOT NULL DEFAULT '[]',
    -- Track origin so manager can filter AI vs human edges
    created_by       VARCHAR(10) NOT NULL DEFAULT 'human',  -- human | ai
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT no_self_loop CHECK (from_node_id <> to_node_id)
);

-- ─────────────────────────────────────────
-- FLOOR CONNECTIONS  (stairs / elevators between floors)
-- ─────────────────────────────────────────
CREATE TABLE floor_connections (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id      UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    -- Both nodes must be of type 'elevator' or 'stairs'; enforced at application layer
    from_node_id     UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    to_node_id       UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    connection_type  VARCHAR(20) NOT NULL,    -- stairs | elevator | escalator | ramp
    is_bidirectional BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- NAVIGATION PACKAGES  (internal only)
-- NEVER returned by /api/manager/* routes.
-- Visible only via /api/db-admin/* and consumed by /mobile/* endpoint.
-- ─────────────────────────────────────────
CREATE TABLE navigation_packages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id   UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    version       INTEGER NOT NULL DEFAULT 1,
    -- SHA-256 of package_json; mobile uses this for checksum-based cache invalidation
    checksum      VARCHAR(64) NOT NULL,
    is_current    BOOLEAN NOT NULL DEFAULT TRUE,
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Full Dijkstra-ready payload. Column is excluded from all manager-facing queries.
    package_json  JSONB NOT NULL
);
-- Enforce single current package per building
CREATE UNIQUE INDEX uq_nav_package_current
    ON navigation_packages (building_id) WHERE is_current = TRUE;

-- ─────────────────────────────────────────
-- INDEXES
-- ─────────────────────────────────────────
CREATE INDEX idx_buildings_manager    ON buildings (manager_id);
CREATE INDEX idx_floors_building      ON floors (building_id);
CREATE INDEX idx_nodes_floor          ON nodes (floor_id);
CREATE INDEX idx_nodes_building       ON nodes (building_id);
CREATE INDEX idx_edges_floor          ON edges (floor_id);
CREATE INDEX idx_edges_from           ON edges (from_node_id);
CREATE INDEX idx_edges_to             ON edges (to_node_id);
CREATE INDEX idx_conn_building        ON floor_connections (building_id);
CREATE INDEX idx_navpkg_building      ON navigation_packages (building_id);
```

---

## 6. Backend API Design

### 6.1 Manager Auth  (`/auth/*` — no JWT required)

```
POST /auth/signup      { email, password, fullName }  → { manager, token }
POST /auth/login       { email, password }            → { manager, token }
POST /auth/logout
GET  /auth/me                                         → { manager }
```

### 6.2 DB Admin Auth  (`/db-admin/auth/*` — env-var credentials)

```
POST /db-admin/auth/login   { username, password }   → { token }  (role=superadmin)
POST /db-admin/auth/logout
```
Credentials checked against `DB_ADMIN_USER` / `DB_ADMIN_PASS` environment variables.
No signup — operator-provisioned only.

### 6.3 Manager — Buildings  (`/api/manager/buildings` — JWT required)

```
GET    /api/manager/buildings                → [Building]  (own buildings only)
POST   /api/manager/buildings                { name, description?, address? }  → Building
GET    /api/manager/buildings/{id}           → Building  (with floors list)
PUT    /api/manager/buildings/{id}           { name?, description?, address? }  → Building
DELETE /api/manager/buildings/{id}
GET    /api/manager/buildings/{id}/qr        → PNG binary stream
POST   /api/manager/buildings/{id}/publish   → triggers NavPackageGenerator, status → published
```

### 6.4 Manager — Floors  (`/api/manager/buildings/{bId}/floors`)

```
GET    /api/manager/buildings/{bId}/floors            → [Floor]
POST   /api/manager/buildings/{bId}/floors            multipart: { glbFile, floorNumber, floorName }
                                                      → Floor  (upload_status: ready)
PUT    /api/manager/buildings/{bId}/floors/{id}       multipart: { glbFile?, floorNumber?, floorName? }
                                                      → Floor  (replaces GLB file if provided)
DELETE /api/manager/buildings/{bId}/floors/{id}
GET    /api/manager/buildings/{bId}/floors/{id}/glb   → GLB binary stream
                                                      (Three.js loads this URL client-side)
PUT    /api/manager/buildings/{bId}/floors/{id}/bounds
                                                      { minX, maxX, minZ, maxZ, floorY }
                                                      → Floor  (sent from browser after Three.js
                                                        extracts bounding box from loaded GLB)
```

### 6.5 Manager — Nodes  (`/api/manager/floors/{fId}/nodes`)

```
GET    /api/manager/floors/{fId}/nodes             → [Node]
POST   /api/manager/floors/{fId}/nodes             { label, nodeType, canvasX, canvasY, metadata? }
                                                   → Node  (world coords computed server-side)
PUT    /api/manager/floors/{fId}/nodes/{id}        { label?, nodeType?, canvasX?, canvasY?, metadata? }
                                                   → Node
DELETE /api/manager/floors/{fId}/nodes/{id}
```

### 6.6 Manager — Edges  (`/api/manager/floors/{fId}/edges`)

```
GET    /api/manager/floors/{fId}/edges              → [Edge]
POST   /api/manager/floors/{fId}/edges              { fromNodeId, toNodeId, edgeType?,
                                                      waypoints?, isBidirectional? }  → Edge
PUT    /api/manager/floors/{fId}/edges/{id}         { waypoints?, edgeType?, isBidirectional? }  → Edge
DELETE /api/manager/floors/{fId}/edges/{id}

POST   /api/manager/floors/{fId}/edges/ai-suggest   { floorPlanImageBase64, nodes: [...] }
                                                    → [SuggestedEdge]
       Calls claude-haiku-4-5 Vision. Returns candidate edges with confidence scores.
       Manager accepts/rejects each individually. Accepted edges are saved via normal POST above.
```

### 6.7 Manager — Floor Connections  (`/api/manager/buildings/{bId}/connections`)

```
GET    /api/manager/buildings/{bId}/connections           → [FloorConnection]
POST   /api/manager/buildings/{bId}/connections           { fromNodeId, toNodeId,
                                                            connectionType, isBidirectional? }
                                                          → FloorConnection
DELETE /api/manager/buildings/{bId}/connections/{id}
```

### 6.8 Mobile API  (public, no manager auth)

```
GET  /mobile/buildings/{qrToken}/nav-package
     → { buildingId, buildingName, version, checksum, floors:[...], crossFloorConnections:[...] }
     Served from navigation_packages.package_json.
     package_json is the ONLY place this endpoint reads from; it is never assembled on the fly.
```

### 6.9 DB Admin API  (`/api/db-admin/*` — superadmin JWT required)

```
GET    /api/db-admin/managers                     → [Manager]  (all, paginated)
DELETE /api/db-admin/managers/{id}

GET    /api/db-admin/buildings                    → [Building]  (all managers, paginated)
DELETE /api/db-admin/buildings/{id}

GET    /api/db-admin/floors                       → [Floor]  (paginated)
DELETE /api/db-admin/floors/{id}

GET    /api/db-admin/nav-packages                 → [NavPackage]  (id, buildingId, version, checksum, generatedAt)
GET    /api/db-admin/nav-packages/{id}/json       → raw package_json  (ONLY accessible here)
DELETE /api/db-admin/nav-packages/{id}
POST   /api/db-admin/buildings/{id}/regenerate    → re-runs NavPackageGenerator for a building

GET    /api/db-admin/stats                        → { managerCount, buildingCount, floorCount, ... }
```

---

## 7. Navigation Package JSON (Mobile-Consumed)

```json
{
  "buildingId": "uuid",
  "buildingName": "Main Campus Block A",
  "version": 3,
  "checksum": "sha256hex",
  "floors": [
    {
      "floorId": "uuid",
      "floorNumber": 0,
      "floorName": "Ground Floor",
      "floorY": 0.0,
      "nodes": [
        { "id": "uuid", "label": "Reception", "type": "room",
          "position": { "x": 1.5, "y": 0.0, "z": -3.2 } }
      ],
      "edges": [
        { "id": "uuid", "from": "node-uuid", "to": "node-uuid",
          "distance": 4.1, "bidirectional": true, "type": "walk",
          "waypoints": [ { "x": 1.5, "y": 0.0, "z": -1.5 } ] }
      ]
    }
  ],
  "crossFloorConnections": [
    { "id": "uuid", "fromNodeId": "uuid", "toNodeId": "uuid",
      "type": "elevator", "bidirectional": true }
  ]
}
```

### QR Payload (updated v2 contract)

```json
{ "type": "VecturAI-building", "token": "abc123xyz", "v": 2 }
```

Mobile scans QR → extracts `token` → calls `/mobile/buildings/{token}/nav-package`.
Compares returned `checksum` against cached `checksum.txt`.
If different → download full package → overwrite cache.

---

## 8. GLB → 2D Map Rendering (Client-Side Three.js)

The floor plan is rendered entirely in the browser. No server-side image processing is used.

### Flow

```
Manager uploads GLB
        │
        ▼
Server stores file, returns /api/manager/.../floors/{id}/glb URL
        │
        ▼ (Map Editor page loads)
Three.js GLTFLoader fetches GLB URL
        │
        ▼
Scene loaded → traverse all meshes → Box3.setFromObject(scene)
→ extract: minX, maxX, minZ, maxZ, estimated floorY
        │
        ├──► PUT /api/manager/.../floors/{id}/bounds  (send extracted bounds to server)
        │    Server stores bounds; used later for canvas → world coordinate conversion
        │
        ▼
OrthographicCamera positioned above scene, looking straight down (-Y)
Camera frustum set to exactly [minX..maxX] × [minZ..maxZ]
        │
        ▼
renderer.render(scene, camera)
→ canvas.toDataURL('image/png') → used as Konva background image
        │
        ▼
Konva editor overlaid — nodes and edges drawn on top
```

### Coordinate Mapping

When a manager places a node at canvas position `(cx, cy)` (normalized 0–1):
```
worldX = bounds.minX + cx * (bounds.maxX - bounds.minX)
worldZ = bounds.minZ + cy * (bounds.maxZ - bounds.minZ)
worldY = floor.floorY
```
This mapping runs server-side when `POST /nodes` is called, filling `world_x/y/z` columns.

### Why client-side (not server-side)

- Eliminates JVM SVG→PNG pipeline dependency on the new flow
- Three.js WebGL renderer produces accurate, anti-aliased floor plan visuals from actual 3D geometry
- Bounds extraction is exact (Three.js `Box3` is authoritative for the loaded model)
- nav-preprocessor pipeline is preserved as-is for legacy compatibility; it is not invoked in the new flow

---

## 9. AI Edge Suggestion (claude-haiku-4-5 Vision)

### Flow

1. Manager clicks **"AI Suggest Edges"** in the map editor toolbar
2. Frontend captures the current Three.js-rendered floor plan as a base64 PNG (≤ 1024px, downscaled if needed)
3. Sends `POST /api/manager/floors/{fId}/edges/ai-suggest` with:
   - `floorPlanImageBase64` — the top-down floor plan PNG
   - `nodes` — array of `{ id, label, nodeType, canvasX, canvasY }`
4. Backend calls **claude-haiku-4-5** with image + structured prompt
5. Claude returns JSON array of suggested edges with confidence scores
6. Backend returns suggestions to frontend
7. Frontend renders suggested edges as **yellow dashed lines** overlaid on the canvas
8. Manager sees a panel: **"Accept All"** / **"Accept (n)"** / **"Reject All"**, plus per-edge accept/reject toggles
9. Accepted edges are saved to the DB via normal `POST /edges` calls with `created_by: "ai"`

### Prompt Template

```
You are an indoor navigation path planner. You are given:
1. A 2D top-down floor plan image of a building floor
2. A list of labeled nodes (rooms, corridors, entrances, etc.) with their
   pixel positions expressed as normalized values (0.0 = left/top, 1.0 = right/bottom)

Your task: suggest walkable edges connecting these nodes.

Rules:
- Edges must follow visible corridors and open floor space — never pass through walls
- Prefer paths a person would naturally walk (use corridors, not diagonal room-cuts)
- Add waypoints where the path must curve around an obstacle or wall corner
- Every 'room' node must be reachable from the 'entrance' node
- Do not connect nodes that are visually separated by a solid wall with no visible door

Respond ONLY with valid JSON, no explanation:
{
  "edges": [
    {
      "fromNodeId": "<id>",
      "toNodeId": "<id>",
      "waypoints": [ { "x": 0.0, "y": 0.0 } ],
      "confidence": 0.85
    }
  ]
}

Nodes:
{nodeListJson}
```

### Cost

- claude-haiku-4-5 input: ~$0.001 per call (image + ~500 tokens)
- Manager-triggered, not automatic: ~1–3 calls per floor map total

---

## 10. Manager Panel — Screen Breakdown

### 10.1 Auth
- **`/login`** — email + password
- **`/signup`** — email, password, full name; auto-login

### 10.2 Dashboard  `/dashboard`
- Building cards grid: name, floor count, status (draft/published), last updated
- Per-card actions: Edit, View QR, Delete
- "New Building" modal

### 10.3 Building Detail  `/buildings/:id`
- Editable: name, description, address
- Floors list (ordered by `floor_number`):
  - Each row: floor name, number, "Edit Map" button, "Replace GLB" button, delete
  - "Add Floor" — drag-and-drop GLB, floor number input, floor name input
- "Download QR Code" button → fetches `/api/manager/buildings/{id}/qr` → downloads PNG
- **"Publish"** button (disabled if any floor has 0 nodes):
  - Triggers nav package generation server-side
  - Shows spinner, then success/error toast
- Cross-floor connections panel:
  - Table of existing connections with delete
  - "Add Connection" — select floor A node + floor B node + connection type

### 10.4 Map Editor  `/buildings/:bId/floors/:fId/edit`

```
┌─────────────────────────────────────────────────────────────────┐
│ Toolbar                                                          │
│ [← Back]  Floor: Ground Floor    [Select▼][Add Node][Add Edge]  │
│                    [AI Suggest] [Undo][Redo]         [Save]      │
├────────────┬────────────────────────────────────────────────────┤
│ Sidebar    │  Canvas                                             │
│            │  ┌──────────────────────────────────────────────┐  │
│ (empty     │  │  Three.js top-down render (background)        │  │
│  when      │  │  ─────────────────────────────────────────   │  │
│  nothing   │  │  Edges: solid lines (human), dashed (AI)      │  │
│  selected) │  │  Nodes: colored circles + labels              │  │
│            │  │                                               │  │
│ — Node —   │  │  Suggested (AI) edges: yellow dashed          │  │
│ Label:     │  │  with Accept / Reject buttons on hover        │  │
│ [______]   │  │                                               │  │
│ Type: [▼]  │  │  Pan: drag on empty space                     │  │
│            │  │  Zoom: scroll wheel                           │  │
│ — Edge —   │  │                                               │  │
│ Type: [▼]  │  └──────────────────────────────────────────────┘  │
│ Bidir: [✓] │                                                     │
│            │  AI Suggestion Panel (slides in when suggestions    │
│ [Delete]   │  arrive): "14 edges suggested"                      │
│            │  [Accept All] [Accept Selected (3)] [Dismiss]       │
└────────────┴────────────────────────────────────────────────────┘
```

**Editing interactions:**
- **Select mode**: click node/edge → sidebar shows properties, inline edit
- **Add Node mode**: click empty space → node placed at cursor → sidebar opens for label/type
- **Add Edge mode**: click source node → click target node → edge drawn; click middle of edge to add waypoint
- **Delete**: `Delete` key or sidebar button
- **Undo/Redo**: Ctrl+Z / Ctrl+Y (command stack in Zustand)
- **Save**: persists all unsaved nodes/edges to backend in a single batch call; shows dirty indicator

**Node type color coding:**
- `room` → blue
- `corridor` → grey
- `entrance` → green
- `elevator` → purple
- `stairs` → orange
- `exit` → red

### 10.5 DB Admin Panel  `/db-admin/*`
- **`/db-admin/login`** — username + password (env-var credentials)
- **`/db-admin/dashboard`** — stats: manager count, building count, floor count, package count
- **`/db-admin/managers`** — paginated table, delete action
- **`/db-admin/buildings`** — paginated table with manager name, status, delete
- **`/db-admin/floors`** — paginated table, link to building
- **`/db-admin/nav-packages`** — list with version + checksum; "View JSON" opens raw JSON viewer; "Regenerate" button
- **JSON Viewer**: pretty-printed JSONB content with copy-to-clipboard; this is the ONLY place raw navigation JSON is visible

---

## 11. Mobile Integration Changes

### Removed from iOS
- `apps/iosApp/iosApp/admin/AdminAPIClient.swift`
- `apps/iosApp/iosApp/admin/AdminDraftJobsView.swift`
- `apps/iosApp/iosApp/admin/AdminDraftReviewView.swift`
- `apps/iosApp/iosApp/admin/AdminJobDetailView.swift`
- Admin gear icon + sheet from `ContentView.swift`

### Updated QR Flow

**Old payload** (v1): `{ "type": "VecturAI-entrance", "buildingId": "...", "entranceId": "...", "v": 1 }`

**New payload** (v2): `{ "type": "VecturAI-building", "token": "abc123xyz", "v": 2 }`

Mobile logic after QR scan:
1. Parse QR → read `type == "VecturAI-building"`, extract `token`
2. Check disk cache: `{cacheDir}/{token}/nav-package.json` + `{token}/checksum.txt`
3. Fetch `GET /mobile/buildings/{token}/nav-package`
4. Compare `response.checksum` with cached checksum
5. If match → use cached package. If different → save new package + update checksum file
6. Build in-memory nav graph → user selects destination → Dijkstra
7. AR navigation with waypoint-following arrow placement

### Multi-floor Dijkstra

- All nodes from all floors form one unified graph
- Cross-floor edge weights: `elevator = 20s equivalent`, `stairs = 30s equivalent` (tunable constants)
- Route result is a sequence of `(floorId, nodeId)` pairs
- When route crosses floors: AR shows a "Take elevator to Floor 2 →" instruction card; AR arrows resume after user transitions to next floor

### Checksum Cache Invalidation

- Cache is never TTL-expired; it only invalidates on `checksum` mismatch
- This means offline users continue navigating with their last-downloaded package indefinitely

---

## 12. File Structure (New / Changed)

```
VecturAI/
├── apps/
│   ├── manager-web/                         ← NEW: React SPA (manager + db-admin panels)
│   │   ├── src/
│   │   │   ├── api/                         ← Axios clients per resource
│   │   │   ├── components/                  ← Shared UI (buttons, modals, tables)
│   │   │   ├── pages/
│   │   │   │   ├── auth/                    ← Login, Signup
│   │   │   │   ├── dashboard/               ← Building list
│   │   │   │   ├── building/                ← Building detail + floor list
│   │   │   │   ├── editor/                  ← Map editor (Three.js + Konva)
│   │   │   │   └── db-admin/                ← DB admin panel pages
│   │   │   ├── stores/                      ← Zustand (editor state, auth)
│   │   │   ├── three/                       ← Three.js GLB loader + renderer utils
│   │   │   └── main.tsx
│   │   ├── package.json
│   │   └── vite.config.ts
│   └── iosApp/
│       └── iosApp/
│           ├── admin/                       ← DELETE entire directory
│           ├── ContentView.swift            ← Remove gear icon + AdminToolsSheet
│           └── QRScanView.swift             ← Update to parse v2 payload
├── tools/
│   └── admin-api/
│       └── src/main/kotlin/com/VecturAI/tools/admin/
│           ├── Application.kt               ← Add CORS for manager-web origin
│           │                                   Add PostgreSQL datasource config
│           ├── db/
│           │   ├── DatabaseFactory.kt       ← NEW: connection pool + schema init
│           │   └── tables/                  ← NEW: Exposed table objects
│           │       ├── Managers.kt
│           │       ├── Buildings.kt
│           │       ├── Floors.kt
│           │       ├── Nodes.kt
│           │       ├── Edges.kt
│           │       ├── FloorConnections.kt
│           │       └── NavigationPackages.kt
│           ├── routes/
│           │   ├── ManagerAuthRoutes.kt     ← NEW
│           │   ├── DbAdminAuthRoutes.kt     ← NEW
│           │   ├── BuildingRoutes.kt        ← NEW
│           │   ├── FloorRoutes.kt           ← NEW (serves GLB file, stores bounds)
│           │   ├── NodeRoutes.kt            ← NEW
│           │   ├── EdgeRoutes.kt            ← NEW (+ AI suggest endpoint)
│           │   ├── FloorConnectionRoutes.kt ← NEW
│           │   ├── MobileApiRoutes.kt       ← NEW: public /mobile/* endpoint
│           │   ├── DbAdminRoutes.kt         ← NEW: full CRUD + JSON viewer
│           │   └── DraftJobRoutes.kt        ← KEEP legacy, no changes
│           ├── service/
│           │   ├── ManagerAuthService.kt    ← NEW
│           │   ├── BuildingService.kt       ← NEW (QR token + ZXing QR gen)
│           │   ├── FloorService.kt          ← NEW (GLB file storage)
│           │   ├── NodeService.kt           ← NEW (canvas→world coord calc)
│           │   ├── EdgeService.kt           ← NEW (distance calc)
│           │   ├── AiEdgeSuggester.kt       ← NEW (Anthropic claude-haiku-4-5 call)
│           │   ├── NavPackageGenerator.kt   ← NEW (assemble + store JSONB)
│           │   └── … existing services      ← keep untouched
│           └── model/
│               └── … new data classes
├── uploads/                                 ← NEW: local filesystem storage root
│   ├── glb/                                 ← uploaded GLB files
│   └── qr/                                  ← generated QR PNG files
└── docs/
    └── adr/
        └── ADR-034-web-admin-panel.md       ← NEW
```

---

## 13. Implementation Phases

### Phase A — Backend: DB + Auth (est. 2–3 days)
- [ ] Add PostgreSQL + Exposed + BCrypt + ktor-auth-jwt to `admin-api/build.gradle.kts`
- [ ] `DatabaseFactory.kt` — HikariCP pool, schema auto-creation via Exposed DDL
- [ ] All seven Exposed table objects
- [ ] `ManagerAuthService` — signup, login, JWT issue (role=manager)
- [ ] `DbAdminAuthRoutes` — login against env vars, JWT issue (role=superadmin)
- [ ] `ManagerAuthRoutes`
- [ ] JWT middleware: two separate `authenticate` blocks (manager / superadmin)
- [ ] Unit tests for both auth paths

### Phase B — Backend: Buildings + Floors (est. 2 days)
- [ ] `BuildingService` — CRUD, QR token generation (nanoid-style), ZXing QR PNG gen
- [ ] `BuildingRoutes` — full CRUD + publish endpoint
- [ ] `FloorService` — GLB file save/delete, bounds storage
- [ ] `FloorRoutes` — upload, list, replace, GLB stream, bounds PUT
- [ ] `uploads/glb/` and `uploads/qr/` directory creation on startup

### Phase C — Backend: Nodes, Edges, AI, Nav Package (est. 2–3 days)
- [ ] `NodeService` — CRUD with canvas→world coordinate mapping
- [ ] `EdgeService` — CRUD, Euclidean distance auto-compute
- [ ] `FloorConnectionRoutes` — CRUD
- [ ] `AiEdgeSuggester` — Anthropic SDK call, prompt assembly, response parse
- [ ] `NavPackageGenerator` — query all floors/nodes/edges/connections → build JSON → store JSONB + SHA-256 checksum
- [ ] `MobileApiRoutes` — `/mobile/buildings/{token}/nav-package`
- [ ] `DbAdminRoutes` — full CRUD views + JSON viewer endpoint

### Phase D — Frontend: Auth + Dashboard (est. 2 days)
- [ ] Vite + React + Tailwind + React Router + React Query + Zustand project init in `apps/manager-web/`
- [ ] Axios instance with JWT interceptor
- [ ] Login / Signup pages + protected route wrapper
- [ ] Dashboard: building cards, create modal, delete confirm
- [ ] Error boundary + toast notifications

### Phase E — Frontend: Building Detail + Floor Upload (est. 2 days)
- [ ] Building detail page with inline edit
- [ ] Floor list with drag-and-drop GLB upload
- [ ] Upload progress indicator
- [ ] QR download button
- [ ] Floor deletion confirm dialog
- [ ] Publish button with loading state
- [ ] Cross-floor connections table + "Add Connection" form
- [ ] DB Admin panel pages (login, dashboard, tables, JSON viewer)

### Phase F — Frontend: Map Editor (est. 4–5 days)
- [ ] Three.js loader utility: `loadGlbTopDown(url)` → returns `{ canvas, bounds }`
  - `GLTFLoader` fetch, `OrthographicCamera` setup, `Box3` extraction
  - `renderer.render()` + `canvas.toDataURL()` output
  - Auto-sends bounds to backend `PUT .../bounds`
- [ ] Konva stage with floor plan PNG as background `Image` layer
- [ ] Pan + zoom (Konva `Stage` drag + wheel scale)
- [ ] **Select mode**: click nodes/edges → sidebar
- [ ] **Add Node mode**: click → place circle → sidebar label/type picker
- [ ] **Add Edge mode**: click node A → click node B → draw line
- [ ] Edge waypoint handles: draggable midpoint circles
- [ ] **Delete**: key + sidebar button
- [ ] Undo/Redo: command stack in Zustand (AddNode, DeleteNode, AddEdge, MoveNode, etc.)
- [ ] Dirty state indicator + auto-save prompt on navigate-away
- [ ] Save: batch PUT nodes + edges to backend
- [ ] **AI Suggest**:
  - Capture Three.js canvas as base64 PNG
  - `POST .../edges/ai-suggest`
  - Render yellow dashed overlay for suggested edges
  - Accept/Reject panel

### Phase G — Mobile Integration (est. 2 days)
- [ ] Delete `apps/iosApp/iosApp/admin/` directory
- [ ] Remove gear icon and `AdminToolsSheet` from `ContentView.swift`
- [ ] Update `QRPayload.swift` for v2 contract
- [ ] `NavigationPackageLoader.swift`: download from `/mobile/buildings/{token}/nav-package`, checksum cache
- [ ] Multi-floor graph builder + Dijkstra
- [ ] AR overlay: cross-floor instruction card ("Take elevator to Floor 2")

### Phase H — Integration Testing + ADR (est. 2 days)
- [ ] End-to-end: manager signup → create building → upload GLB → place nodes → AI suggest + accept → publish → iOS scan QR → navigate
- [ ] Error flows: upload failure, AI timeout, offline mobile, publish with no nodes
- [ ] `ADR-034-web-admin-panel.md`

---

## 14. Key Design Decisions

| Decision | Rationale |
|---|---|
| "Manager" not "admin" for building operators | Avoids conflation with system-level DB admin access |
| DB Admin panel uses env-var credentials, not signup | No public attack surface for superadmin account creation |
| `navigation_packages.package_json` excluded from all manager API queries | Routing internals stay opaque; managers trigger "Publish", they never see JSON |
| Client-side Three.js for 2D rendering | Accurate GL render from real geometry; eliminates server-side SVG/PNG pipeline; bounds extracted by Three.js are authoritative |
| Bounds sent from browser after GLB load | Server needs world coordinates for Dijkstra; browser is the only place GLB is actually parsed in the new flow |
| One QR per building, all floors in one package | Simpler UX; mobile downloads everything on first scan; no per-floor QR management overhead |
| Checksum-based cache invalidation (no TTL) | Offline users keep their last-good package; cache updates only when manager publishes a change |
| AI edges are opt-in suggestions, manager must accept | Walls are not always visually detectable with full confidence; human review prevents broken routes |
| Legacy DraftJobRoutes kept unchanged | No regression risk; backward-compatible with existing scripts and tests |

---

## 15. Out of Scope (Post-MVP)

- Real-time collaborative map editing (two managers same floor simultaneously)
- Indoor positioning / Bluetooth beacons
- Accessibility routing (wheelchair-only paths)
- Localization / multi-language labels
- Voice guidance in mobile AR
- Android visitor app parity
- Cloud deployment / CI-CD
- S3-compatible file storage migration
- Manager organization/team accounts (currently one manager = one account)
