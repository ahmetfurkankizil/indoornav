#!/usr/bin/env bash
#
# Demo Readiness Check
#
# Validates that the VecturAI iOS MVP is ready for a demo.
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
blue "=== VecturAI Demo Readiness Check ==="
echo ""

# --- 1. Reviewed Package Validation ---
blue "--- 1. Reviewed Package ---"
if [ -f "scripts/validate-reviewed-package.sh" ]; then
    if bash scripts/validate-reviewed-package.sh sample/reviewed-house-package/ > /dev/null 2>&1; then
        pass "Reviewed package validates successfully"
    else
        fail "Reviewed package validation failed — run: ./scripts/validate-reviewed-package.sh sample/reviewed-house-package/"
    fi
else
    fail "Validator script not found: scripts/validate-reviewed-package.sh"
fi

# --- 2. iOS Bundle Package Consistency ---
echo ""
blue "--- 2. iOS Bundle Package ---"
BUNDLE_DIR="apps/iosApp/iosApp/reviewed-package"
SOURCE_DIR="sample/reviewed-house-package"

for f in manifest.json rooms.json nav_graph.json entrance_markers.json route_rendering.json; do
    if [ ! -f "$BUNDLE_DIR/$f" ]; then
        fail "Missing from iOS bundle: $BUNDLE_DIR/$f"
    elif [ ! -f "$SOURCE_DIR/$f" ]; then
        fail "Missing from source: $SOURCE_DIR/$f"
    else
        if diff -q "$SOURCE_DIR/$f" "$BUNDLE_DIR/$f" > /dev/null 2>&1; then
            pass "$f matches between source and bundle"
        else
            warn "$f differs between source and iOS bundle — run: cp $SOURCE_DIR/*.json $BUNDLE_DIR/"
        fi
    fi
done

# --- 3. AR Resources Asset Catalog ---
echo ""
blue "--- 3. AR Reference Image Asset ---"
AR_ASSETS="apps/iosApp/iosApp/Assets.xcassets/AR Resources.arresourcegroup"
MARKER_ASSET="$AR_ASSETS/entrance_marker_main.arreferenceimage"

if [ -d "$AR_ASSETS" ]; then
    pass "AR Resources group exists"
else
    fail "AR Resources group missing: $AR_ASSETS"
fi

if [ -d "$MARKER_ASSET" ]; then
    pass "entrance_marker_main reference image exists"

    # Check the image file exists
    if [ -f "$MARKER_ASSET/entrance_marker.png" ]; then
        pass "Marker image file present"

        # Check image is not a trivially small placeholder
        IMG_SIZE=$(wc -c < "$MARKER_ASSET/entrance_marker.png" | tr -d ' ')
        if [ "$IMG_SIZE" -lt 1000 ]; then
            warn "Marker image is very small (${IMG_SIZE} bytes) — may be a placeholder. Run: ./scripts/generate-entrance-poster.sh"
        else
            pass "Marker image file size: ${IMG_SIZE} bytes"
        fi

        # CRITICAL: Check image pixel dimensions are not absurdly large
        # An oversized image (e.g. 64000x64000) will cause Xcode to consume
        # tens of GB of RAM and effectively crash the machine.
        if command -v sips &>/dev/null; then
            IMG_W=$(sips -g pixelWidth "$MARKER_ASSET/entrance_marker.png" 2>/dev/null | awk '/pixelWidth/{print $2}')
            IMG_H=$(sips -g pixelHeight "$MARKER_ASSET/entrance_marker.png" 2>/dev/null | awk '/pixelHeight/{print $2}')
            if [ "${IMG_W:-0}" -gt 4000 ] || [ "${IMG_H:-0}" -gt 4000 ]; then
                fail "Marker image is ${IMG_W}x${IMG_H}px — WAY TOO LARGE. This will crash Xcode. Max ~1050x1050. Run: ./scripts/generate-entrance-poster.sh"
            else
                pass "Marker image dimensions: ${IMG_W}x${IMG_H}px"
            fi
        fi
    else
        fail "Marker image file missing: $MARKER_ASSET/entrance_marker.png — run: ./scripts/generate-entrance-poster.sh"
    fi

    # Check Contents.json references correct physical width
    if [ -f "$MARKER_ASSET/Contents.json" ]; then
        ASSET_WIDTH=$(python3 -c "import json; d=json.load(open('$MARKER_ASSET/Contents.json')); print(d.get('properties',{}).get('physicalWidth',''))" 2>/dev/null)
        PKG_WIDTH=$(python3 -c "import json; d=json.load(open('$SOURCE_DIR/entrance_markers.json')); print(d['entranceMarkers'][0]['physicalWidthMeters'])" 2>/dev/null)
        if [ "$ASSET_WIDTH" = "$PKG_WIDTH" ]; then
            pass "Physical width matches: ${ASSET_WIDTH}m"
        else
            warn "Physical width mismatch: asset=$ASSET_WIDTH, package=$PKG_WIDTH"
        fi
    fi
else
    fail "entrance_marker_main reference image missing: $MARKER_ASSET — run: ./scripts/generate-entrance-poster.sh"
fi

# --- 4. Reference Image Name Consistency ---
echo ""
blue "--- 4. Naming Consistency ---"
PKG_REF_NAME=$(python3 -c "import json; d=json.load(open('$SOURCE_DIR/entrance_markers.json')); print(d['entranceMarkers'][0]['referenceImageName'])" 2>/dev/null)
ASSET_DIR_NAME=$(basename "$MARKER_ASSET" .arreferenceimage 2>/dev/null || echo "")

if [ "$PKG_REF_NAME" = "$ASSET_DIR_NAME" ]; then
    pass "referenceImageName '$PKG_REF_NAME' matches asset directory name"
else
    fail "referenceImageName mismatch: package='$PKG_REF_NAME', asset dir='$ASSET_DIR_NAME'"
fi

# --- 4b. Poster Source Consistency ---
POSTER_SOURCE="sample/entrance-poster/entrance_poster.png"
if [ -f "$POSTER_SOURCE" ]; then
    if [ -f "$MARKER_ASSET/entrance_marker.png" ]; then
        if diff -q "$POSTER_SOURCE" "$MARKER_ASSET/entrance_marker.png" > /dev/null 2>&1; then
            pass "Poster source matches AR asset catalog image"
        else
            fail "Poster source differs from AR asset! Run: ./scripts/generate-entrance-poster.sh (regenerate + copy)"
        fi
    fi
else
    warn "Poster source not found at $POSTER_SOURCE — run: ./scripts/generate-entrance-poster.sh"
fi

# --- 5. QR Contract Consistency ---
echo ""
blue "--- 5. QR Contract ---"
BUILDING_ID=$(python3 -c "import json; d=json.load(open('$SOURCE_DIR/manifest.json')); print(d['buildingId'])" 2>/dev/null)
ENTRANCE_ID=$(python3 -c "import json; d=json.load(open('$SOURCE_DIR/entrance_markers.json')); print(d['entranceMarkers'][0]['id'])" 2>/dev/null)

if [ -n "$BUILDING_ID" ]; then
    pass "QR buildingId: $BUILDING_ID"
else
    fail "No buildingId in manifest"
fi

if [ -n "$ENTRANCE_ID" ]; then
    pass "QR entranceId: $ENTRANCE_ID"
else
    fail "No entrance marker id"
fi

echo ""
echo "  Expected QR payload (embedded in the entrance poster):"
echo "  {\"type\":\"vecturai-entrance\",\"buildingId\":\"$BUILDING_ID\",\"entranceId\":\"$ENTRANCE_ID\",\"v\":1}"
echo ""
echo "  NOTE: The entrance poster is a single artifact containing this QR code."
echo "  It also serves as the AR reference image. Generate with: ./scripts/generate-entrance-poster.sh"

# --- 6. Route Connectivity ---
echo ""
blue "--- 6. Route Connectivity ---"
python3 -c "
import json
from collections import deque

g = json.load(open('$SOURCE_DIR/nav_graph.json'))
r = json.load(open('$SOURCE_DIR/rooms.json'))
m = json.load(open('$SOURCE_DIR/entrance_markers.json'))

adj = {}
for n in g['nodes']: adj[n['id']] = []
for e in g['edges']:
    adj.setdefault(e['from'], []).append(e['to'])
    if e.get('bidirectional', False):
        adj.setdefault(e['to'], []).append(e['from'])

def reachable(start, target):
    visited = set()
    q = deque([start])
    while q:
        cur = q.popleft()
        if cur == target: return True
        if cur in visited: continue
        visited.add(cur)
        for nb in adj.get(cur, []): q.append(nb)
    return False

entrance = m['entranceMarkers'][0]['startNodeId']
for room in r['rooms']:
    dest = room['destinationNodeId']
    name = room['displayName']
    if reachable(entrance, dest):
        print(f'ROUTE_OK: {name} ({entrance} -> {dest})')
    else:
        print(f'ROUTE_ERR: No path to {name} ({entrance} -> {dest})')
" 2>/dev/null | while IFS= read -r line; do
    if [[ "$line" == ROUTE_OK:* ]]; then
        pass "${line#ROUTE_OK: }"
    elif [[ "$line" == ROUTE_ERR:* ]]; then
        fail "${line#ROUTE_ERR: }"
    fi
done

# --- 7. Build Check ---
echo ""
blue "--- 7. Xcode Project ---"
if [ -f "apps/iosApp/iosApp.xcodeproj/project.pbxproj" ]; then
    pass "Xcode project file exists"
else
    fail "Xcode project file missing"
fi

# Check Info.plist has camera permission
if grep -q "NSCameraUsageDescription" "apps/iosApp/iosApp/Info.plist" 2>/dev/null; then
    pass "Camera usage description present in Info.plist"
else
    fail "NSCameraUsageDescription missing from Info.plist"
fi

# --- Summary ---
echo ""
echo ""
blue "=== Demo Readiness Summary ==="
if [ "$ERRORS" -eq 0 ]; then
    green "ALL CHECKS PASSED — ready for demo."
else
    red "$ERRORS error(s) found — fix before demo."
fi
if [ "$WARNINGS" -gt 0 ]; then
    yellow "$WARNINGS warning(s) — review before demo."
fi
echo ""

exit "$ERRORS"
