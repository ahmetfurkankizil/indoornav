# File Dossier: check-demo-readiness.sh

## Path
`scripts\check-demo-readiness.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
#!/usr/bin/env bash
#
# Demo Readiness Check
#
# Validates that the Vectura AI iOS MVP is ready for a demo.
# Runs all checks needed before presenting to investors.
#
# Usage:
#   ./scripts/check-demo-readiness.sh
#

set -euo pipefail

ERRORS=0
WARNINGS=0

red()    { printf '\033[0;31m%s\033[0m\n' "$1"; }
green()  { printf '\033[0;32m%s\033[0m\n' "$1"; }
yellow() { printf '\033[0;33m%s\033[0m\n' "$1"; }
blue()   { printf '\033[0;34m%s\033[0m\n' "$1"; }

pass() { green "  PASS: $1"; }
fail() { red   "  FAIL: $1"; ERRORS=$((ERRORS + 1)); }
warn() { yellow "  WARN: $1"; WARNINGS=$((WARNINGS + 1)); }

echo ""
blue "=== Vectura AI Demo Readiness Check ==="
echo ""

# --- 1. Reviewed Package Validation ---
blue "--- 1. Reviewed Package ---"
if [ -f "scripts/validate-reviewed-package.sh" ]; then
    if bash scripts/validate-reviewed-package.sh sample/reviewed-house-package/ > /dev/null 2>&1; then
        pass "Reviewed package validates successfully"
    else
        fail "Reviewed package valida
```

## Status
Mapped (Pass 3 Normalization)
