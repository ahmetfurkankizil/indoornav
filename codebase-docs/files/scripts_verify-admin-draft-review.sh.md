# File Dossier: verify-admin-draft-review.sh

## Path
`scripts\verify-admin-draft-review.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
# verify-admin-draft-review.sh
# Verifies the Admin Draft Review (Phase 2) API end-to-end using the sample GLB.
#
# Usage:
#   ./scripts/verify-admin-draft-review.sh              # target a running server on localhost:8080
#   ./scripts/verify-admin-draft-review.sh --start-server   # start server, run tests, stop server
#
# Prerequisites: curl, jq

set -euo pipefail

BASE_URL="${ADMIN_API_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SAMPLE_GLB="$REPO_ROOT/sample/demo-building/package/12_9_2025.glb"
SERVER_PID=""
START_SERVER=false
PASS=0
FAIL=0

# ── Colours ───────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
ok()   { echo -e "${GREEN}  ✓ $*${NC}"; ((PASS++)); }
fail() { echo -e "${RED}  ✗ $*${NC}"; ((FAIL++)); }
info() { echo -e "${YELLOW}  → $*${NC}"; }

# ── Argument parsing ──────────────────────────────────────
```

## Status
Mapped (Pass 3 Normalization)
