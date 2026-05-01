# VecturAI Web Admin — Running Guide

## Overview

The system has **two servers** that must both be running:

| Server | Command | Default URL |
|---|---|---|
| **Admin API** (Kotlin/Ktor backend) | `./gradlew :tools:admin-api:run` | `http://localhost:8080` |
| **Manager Web** (React frontend) | `npm run dev` (in `apps/manager-web/`) | `http://localhost:5173` |

Both panels live in the same React SPA at `localhost:5173`. All API calls are proxied by Vite to `localhost:8080` automatically — **no CORS issues in dev**.

---

## Step 1 — PostgreSQL

You need a running PostgreSQL instance. The defaults are:

| Setting | Default | Env var to override |
|---|---|---|
| Host | `localhost` | `POSTGRES_HOST` |
| Port | `5432` | `POSTGRES_PORT` |
| Database | `vecturai` | `POSTGRES_DB` |
| User | `vecturai` | `POSTGRES_USER` |
| Password | `vecturai` | `POSTGRES_PASSWORD` |

### Quick setup (one-time)

```sql
-- Run as postgres superuser
CREATE USER vecturai WITH PASSWORD 'vecturai';
CREATE DATABASE vecturai OWNER vecturai;
```

> **Note**: The Ktor server auto-creates all tables on first startup via Exposed `SchemaUtils.createMissingTablesAndColumns`. No migrations to run manually.

### Using Docker instead

**For PowerShell:**
```powershell
docker run -d `
  --name vecturai-pg `
  -e POSTGRES_USER=vecturai `
  -e POSTGRES_PASSWORD=vecturai `
  -e POSTGRES_DB=vecturai `
  -p 5432:5432 `
  postgres:16
```

**For Command Prompt (cmd):**
```cmd
docker run -d ^
  --name vecturai-pg ^
  -e POSTGRES_USER=vecturai ^
  -e POSTGRES_PASSWORD=vecturai ^
  -e POSTGRES_DB=vecturai ^
  -p 5432:5432 ^
  postgres:16
```

---

### Step 2 — Start the Backend (Admin API)

From the **project root** (`c:\Users\holym\vecturai`):

1. **Configure Environment**: A `.env` file has been created in `tools/admin-api/.env`. Open it and set your `DB_ADMIN_PASS` and other settings.
2. **Run Server**:

**On Windows (PowerShell):**
```powershell
.\gradlew.bat :tools:admin-api:run
```

**On Windows (Command Prompt):**
```cmd
gradlew :tools:admin-api:run
```

**On Linux/macOS:**
```bash
./gradlew :tools:admin-api:run
```

### The `.env` file

The backend now automatically loads configuration from `tools/admin-api/.env`.

```ini
# tools/admin-api/.env
DB_ADMIN_USER=sysadmin
DB_ADMIN_PASS=supersecret    # MUST SET THIS
JWT_SECRET=change-me
POSTGRES_PASSWORD=vecturai
...
```

> [!IMPORTANT]
> The `.env` file is the preferred way to manage local configuration. Values in your actual system environment will still take precedence if they exist.

### Expected startup output

```
╔══════════════════════════════════════════════╗
║   VecturAI Admin API                         ║
╚══════════════════════════════════════════════╝
  Port:        8080
  Uploads dir: uploads
  Jobs dir:    build/admin-draft-jobs  (legacy)
```

---

## Step 3 — Start the Frontend (Manager Web)

In a **separate terminal**, from `apps/manager-web/`:

```bash
cd apps/manager-web
npm install        # first time only
npm run dev
```

On Windows (if PowerShell blocks scripts):
```cmd
cd apps\manager-web
cmd /c "npm run dev"
```

You should see:
```
  VITE v6.x  ready in Xms

  ➜  Local:   http://localhost:5173/
```

---

## Step 4 — Using the Panels

### 4.1 Manager Panel — `http://localhost:5173`

For **building managers** who set up indoor navigation for their buildings.

#### Sign up (first time)

1. Go to `http://localhost:5173/signup`
2. Enter your **full name**, **email**, and **password** (min 8 chars)
3. You are automatically logged in and redirected to the Dashboard

#### Login (returning)

1. Go to `http://localhost:5173/login`
2. Enter email + password → redirected to `/dashboard`

#### Workflow: Create a Building & Map It

```
/signup or /login
        ↓
/dashboard  →  "+ New Building" → enter name / description / address
        ↓
Click "Open" on the building card
        ↓
/buildings/:id  →  "Add Floor" → drag & drop .glb file, set floor number
        ↓
Click "Edit Map" next to the floor
        ↓
/buildings/:bId/floors/:fId/edit  (Map Editor)
        ↓
Toolbar modes:
  [Select]   → click nodes/edges to edit properties in sidebar
  [Add Node] → click floor plan to place a node, set label/type
  [Add Edge] → click node A, click node B → edge created
  [AI Suggest] → AI analyses the floor plan and draws suggested edges (yellow dashes)
                 click "Accept All" or click individual yellow edge to accept
        ↓
Back to /buildings/:id  →  "Publish" button → generates nav package for iOS
        ↓
"Download QR Code" → print the QR and attach to building entrance
```

#### Key pages

| URL | Purpose |
|---|---|
| `/dashboard` | All your buildings (cards grid) |
| `/buildings/:id` | Building detail — floors, QR download, publish, cross-floor connections |
| `/buildings/:bId/floors/:fId/edit` | **Map Editor** — place nodes & edges on 3D floor plan |

#### Map Editor controls

| Action | How |
|---|---|
| Place node | Switch to **Add Node** mode → click floor plan |
| Draw edge | Switch to **Add Edge** mode → click node A → click node B |
| Edit node label/type | Click node (Select mode) → sidebar |
| Delete selected | `Delete` or `Backspace` key |
| Cancel mode | `Esc` |
| Accept AI edge | Click yellow dashed line |
| Accept all AI edges | "Accept All" in the yellow bar |

---

### 4.2 DB Admin Panel — `http://localhost:5173/db-admin/login`

For **system operators** with full database access.

> [!WARNING]
> The DB Admin panel has **destructive delete** access to all data. Use with caution.

#### Login

1. Go to `http://localhost:5173/db-admin/login`
2. Enter the username/password set via `DB_ADMIN_USER` / `DB_ADMIN_PASS` env vars
   - Default username: `sysadmin`
   - Default password: *(empty — you must set `DB_ADMIN_PASS` for login to work)*

#### Pages

| URL | Purpose |
|---|---|
| `/db-admin` | **Dashboard** — live system stats (managers, buildings, floors, nodes, edges, packages) |
| `/db-admin/managers` | List all managers — searchable, delete with confirmation |
| `/db-admin/buildings` | List all buildings — searchable, delete, **Regenerate nav package** |
| `/db-admin/nav-packages` | All nav packages — **View JSON** (raw navigation data), delete |

#### The JSON Viewer

The `/db-admin/nav-packages` page is the **only place** raw `navigation_packages.package_json` is exposed. Click **View JSON** on any package row to open a modal showing the full Dijkstra-ready nav graph. Use the **Copy** button to copy it to clipboard.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Backend fails to start — `Connection refused to localhost:5432` | PostgreSQL is not running. Start it first. |
| DB Admin login always fails with "Invalid credentials" | Set `DB_ADMIN_PASS` env var to a non-empty value before starting the backend. |
| Map Editor shows "Could not render GLB" | The floor was uploaded but the GLB file may be missing. Try re-uploading via **Replace GLB** on the Building Detail page. |
| `npm run dev` blocked — "running scripts is disabled" | Use `cmd /c "npm run dev"` instead of running in PowerShell directly. |
| Frontend shows 401 / 403 on all API calls | Your JWT token expired. Sign out and sign back in. |
| AI Suggest does nothing | `ANTHROPIC_API_KEY` is not set, or the key has no credits. The endpoint returns an empty list gracefully. |

---

## Quick Reference — All URLs

| URL | Who | What |
|---|---|---|
| `http://localhost:5173/signup` | Manager | Create account |
| `http://localhost:5173/login` | Manager | Sign in |
| `http://localhost:5173/dashboard` | Manager | Building list |
| `http://localhost:5173/buildings/:id` | Manager | Building detail + floors |
| `http://localhost:5173/buildings/:bId/floors/:fId/edit` | Manager | Map Editor |
| `http://localhost:5173/db-admin/login` | Operator | DB Admin sign in |
| `http://localhost:5173/db-admin` | Operator | System stats |
| `http://localhost:5173/db-admin/managers` | Operator | All managers |
| `http://localhost:5173/db-admin/buildings` | Operator | All buildings |
| `http://localhost:5173/db-admin/nav-packages` | Operator | Nav packages + JSON viewer |
| `http://localhost:8080/mobile/buildings/:token/nav-package` | iOS app | Public nav package endpoint |
