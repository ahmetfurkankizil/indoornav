# File Dossier: regression-checks.sh

## Path
`scripts\regression-checks.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
#
# Minimal Regression Checks for VecturAI iOS MVP
#
# Fast, lightweight checks that can be run before any commit or build.
# Tests QR payload parsing, package loading logic, route existence, and
# marker asset/config naming consistency.
#
# Usage:
#   ./scripts/regression-checks.sh
#

set -euo pipefail

ERRORS=0

red()   { printf '\033[0;31m%s\033[0m\n' "$1"; }
green() { printf '\033[0;32m%s\033[0m\n' "$1"; }
blue()  { printf '\033[0;34m%s\033[0m\n' "$1"; }

pass() { green "  PASS: $1"; }
fail() { red   "  FAIL: $1"; ERRORS=$((ERRORS + 1)); }

echo ""
blue "=== VecturAI Regression Checks ==="
echo ""

PKG_DIR="sample/reviewed-house-package"

# --- 1. QR Payload Contract ---
blue "--- QR Payload Parsing ---"

python3 -c "
import json, sys

# Test 1: Valid payload parses correctly
valid = '{\"type\":\"vecturai-entrance\",\"buildingId\":\"19\",\"entranceId\":\"marker-main-entrance\",\"v\":1}'
p = json.loads(valid)
assert p['type'] == 'vecturai-entrance', 'type mismatc
```

## Status
Mapped (Pass 3 Normalization)
