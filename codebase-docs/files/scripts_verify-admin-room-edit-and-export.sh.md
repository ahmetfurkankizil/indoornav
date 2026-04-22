# File Dossier: verify-admin-room-edit-and-export.sh

## Path
`scripts\verify-admin-room-edit-and-export.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
# verify-admin-room-edit-and-export.sh
# End-to-end verification for Admin Epic Phase 3:
#   1. Upload a sample GLB and wait for the job to succeed
#   2. PATCH a room and verify the summary reflects the override
#   3. Export the reviewed package and verify all 5 files are present
#   4. Verify rooms.json in the exported package contains the edited values
#
# Usage:
#   ./scripts/verify-admin-room-edit-and-export.sh [--start-server]
#
# Options:
#   --start-server   Build and start the admin API before running checks (background).

set -euo pipefail

BASE_URL="${ADMIN_API_BASE_URL:-http://localhost:8080}"
SAMPLE_GLB="sample/demo-building/polycam_export.glb"
MAX_POLL_SECONDS=120
POLL_INTERVAL=3

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}  PASS${NC}  $1"; }
fail() { echo -e "${RED}  FAIL${NC}  $1"; exit 1; }
info() { echo -e "${YELLOW}  INFO${NC}  $1"; }

# ── Optional server start ────────────────────────────────
```

## Status
Mapped (Pass 3 Normalization)
