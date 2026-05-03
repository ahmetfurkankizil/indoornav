#!/usr/bin/env bash
# ─────────────────────────────────────────────────
# Vectura AI — Demo Package Verification
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
    else
        echo -e "  ${RED}✗${NC} $label — NOT FOUND"
        ((ERRORS++))
    fi
}

echo "═══ Demo Package Verification ═══"
echo "Package: $PKG_DIR"
echo ""

# ── Authoring config ──
echo "Authoring Config:"
check_json "authoring_config.json" "$CONFIG"

if [[ -f "$CONFIG" ]]; then
    # Check required top-level keys
    for key in buildingId buildingName nodes edges rooms entranceMarkers; do
        if python3 -c "import json; d=json.load(open('$CONFIG')); assert '$key' in d" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} Key '$key' present"
        else
            echo -e "  ${RED}✗${NC} Key '$key' MISSING"
            ((ERRORS++))
        fi
    done

    # Count items
    NODES=$(python3 -c "import json; print(len(json.load(open('$CONFIG'))['nodes']))" 2>/dev/null || echo 0)
    EDGES=$(python3 -c "import json; print(len(json.load(open('$CONFIG'))['edges']))" 2>/dev/null || echo 0)
    ROOMS=$(python3 -c "import json; print(len(json.load(open('$CONFIG'))['rooms']))" 2>/dev/null || echo 0)
    MARKERS=$(python3 -c "import json; print(len(json.load(open('$CONFIG'))['entranceMarkers']))" 2>/dev/null || echo 0)
    CHECKPOINTS=$(python3 -c "import json; d=json.load(open('$CONFIG')); print(len(d.get('checkpointMarkers',[])))" 2>/dev/null || echo 0)
    echo "  Counts: $NODES nodes, $EDGES edges, $ROOMS rooms, $MARKERS entrance markers, $CHECKPOINTS checkpoints"
fi

echo ""

# ── Package files ──
echo "Package Output:"
if [[ -d "$PKG_DIR" ]]; then
    check_json "manifest.json" "$PKG_DIR/manifest.json"
    check_json "nav_graph.json" "$PKG_DIR/nav_graph.json"
    check_json "rooms.json" "$PKG_DIR/rooms.json"
    check_json "entrance_markers.json" "$PKG_DIR/entrance_markers.json"
    check_json "route_rendering.json" "$PKG_DIR/route_rendering.json"

    # Optional checkpoint markers
    if [[ -f "$PKG_DIR/checkpoint_markers.json" ]]; then
        check_json "checkpoint_markers.json" "$PKG_DIR/checkpoint_markers.json"
    else
        echo -e "  ${GREEN}✓${NC} checkpoint_markers.json — not present (optional, OK)"
    fi

    # Optional GLB
    if [[ -f "$PKG_DIR/preview.glb" ]]; then
        echo -e "  ${GREEN}✓${NC} preview.glb"
    else
        echo "  ⚠  preview.glb — not found (non-critical)"
    fi
else
    echo -e "  ${RED}✗${NC} Package directory not found: $PKG_DIR"
    echo "  Run: make preprocess"
    ((ERRORS++))
fi

echo ""

# ── Result ──
if [[ $ERRORS -gt 0 ]]; then
    echo -e "${RED}FAILED — $ERRORS error(s) found${NC}"
    exit 1
else
    echo -e "${GREEN}PASSED — Demo package is valid ✅${NC}"
fi
