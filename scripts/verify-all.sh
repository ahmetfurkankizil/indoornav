#!/usr/bin/env bash
# ─────────────────────────────────────────────────
# VecturAI — Unified Local Verification
# Runs all automated checks in sequence.
# ─────────────────────────────────────────────────
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

run_step() {
    local name="$1"
    shift
    echo ""
    echo "═══════════════════════════════════════════"
    echo "  $name"
    echo "═══════════════════════════════════════════"
    if "$@"; then
        echo -e "${GREEN}✅ $name — PASSED${NC}"
        ((PASS++))
    else
        echo -e "${RED}❌ $name — FAILED${NC}"
        ((FAIL++))
    fi
}

# ── Step 1: Preprocessor Tests ──
run_step "Preprocessor Tests (~160)" ./gradlew :tools:nav-preprocessor:test

# ── Step 2: Android Debug Build ──
run_step "Android Debug Build" ./gradlew :apps:androidApp:assembleDebug

# ── Step 3: Demo Package Verification ──
run_step "Demo Package Verification" bash scripts/verify-demo-package.sh

# ── Step 4: iOS Local (optional) ──
if [[ "${SKIP_IOS:-0}" != "1" ]] && command -v xcodebuild &>/dev/null; then
    run_step "iOS Framework Build" ./gradlew :shared:core:linkDebugFrameworkIosArm64
else
    echo ""
    echo -e "${YELLOW}⏭  Skipping iOS (set SKIP_IOS=0 or install Xcode)${NC}"
fi

# ── Summary ──
echo ""
echo "═══════════════════════════════════════════"
echo "  RESULTS: ${GREEN}${PASS} passed${NC}, ${RED}${FAIL} failed${NC}"
echo "═══════════════════════════════════════════"

if [[ $FAIL -gt 0 ]]; then
    echo -e "${RED}VERIFICATION FAILED${NC}"
    exit 1
else
    echo -e "${GREEN}ALL CHECKS PASSED ✅${NC}"
fi
