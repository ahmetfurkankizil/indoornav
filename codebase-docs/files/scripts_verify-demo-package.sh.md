# File Dossier: verify-demo-package.sh

## Path
`scripts\verify-demo-package.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
# ─────────────────────────────────────────────────
# VecturAI — Demo Package Verification
# Validates the sample/demo package for integrity.
# ─────────────────────────────────────────────────
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG_DIR="$ROOT/sample/demo-building/package"
CONFIG="$ROOT/sample/demo-building/authoring_config.json"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

ERRORS=0

check_file() {
    local label="$1"
    local path="$2"
    if [[ -f "$path" ]]; then
        echo -e "  ${GREEN}✓${NC} $label"
    else
        echo -e "  ${RED}✗${NC} $label — NOT FOUND: $path"
        ((ERRORS++))
    fi
}

check_json() {
    local label="$1"
    local path="$2"
    if [[ -f "$path" ]]; then
        if python3 -c "import json; json.load(open('$path'))" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} $label — valid JSON"
        else
            echo -e "  ${RED}✗${NC} $label — INVALID JSON"
            ((ERRORS++))
        fi
  
```

## Status
Mapped (Pass 3 Normalization)
