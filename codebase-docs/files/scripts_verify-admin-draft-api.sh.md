# File Dossier: verify-admin-draft-api.sh

## Path
`scripts\verify-admin-draft-api.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────
# verify-admin-draft-api.sh
# End-to-end verification of the admin draft ingestion pipeline.
#
# Usage:
#   ./scripts/verify-admin-draft-api.sh [--start-server]
#
# Without --start-server, assumes the admin API is already running
# on localhost:8080 (or ADMIN_API_URL env var).
#
# With --start-server, starts the server in the background,
# runs the test, then stops it.
# ─────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
API_URL="${ADMIN_API_URL:-http://localhost:8080}"
SAMPLE_GLB="$REPO_ROOT/sample/demo-building/package/12_9_2025.glb"
SERVER_PID=""
START_SERVER=false

for arg in "$@"; do
    case "$arg" in
        --start-server) START_SERVER=true ;;
    esac
done

cleanup() {
    if [ -n "$SERVER_PID" ]; then
        echo "  Stopping server (PID $SERVER_PID)..."
        kill "$SERVER_PID" 2>/de
```

## Status
Mapped (Pass 3 Normalization)
