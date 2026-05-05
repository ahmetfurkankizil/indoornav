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
valid = '{\"type\":\"VecturAI-entrance\",\"buildingId\":\"19\",\"entranceId\":\"marker-main-entrance\",\"v\":1}'
p = json.loads(valid)
assert p['type'] == 'VecturAI-entrance', 'type mismatch'
assert p['buildingId'] == '19', 'buildingId mismatch'
assert p['entranceId'] == 'marker-main-entrance', 'entranceId mismatch'
assert p['v'] == 1, 'version mismatch'
print('PASS: Valid payload parses correctly')

# Test 2: Missing type field is detectable
bad = '{\"buildingId\":\"19\",\"entranceId\":\"marker-main-entrance\",\"v\":1}'
p = json.loads(bad)
assert 'type' not in p, 'type should be missing'
print('PASS: Missing type field is detectable')

# Test 3: Non-JSON fails
try:
    json.loads('not-json')
    print('FAIL: Non-JSON should raise error')
except:
    print('PASS: Non-JSON correctly rejected')

# Test 4: Building ID matches manifest
manifest = json.load(open('$PKG_DIR/manifest.json'))
assert manifest['buildingId'] == '19', 'manifest buildingId unexpected'
print('PASS: Manifest buildingId matches QR contract (19 = Rectorate Building)')

# Test 5: Entrance ID exists in entrance_markers
markers = json.load(open('$PKG_DIR/entrance_markers.json'))
marker_ids = [m['id'] for m in markers['entranceMarkers']]
assert 'marker-main-entrance' in marker_ids, 'entrance marker not found'
print('PASS: Entrance marker-main-entrance exists in package')
" 2>&1 | while IFS= read -r line; do
    if [[ "$line" == PASS:* ]]; then
        pass "${line#PASS: }"
    elif [[ "$line" == FAIL:* ]]; then
        fail "${line#FAIL: }"
    fi
done

# --- 2. Package Loading ---
echo ""
blue "--- Package Loading ---"

python3 -c "
import json, os

pkg_dir = '$PKG_DIR'
required_files = ['manifest.json', 'rooms.json', 'nav_graph.json', 'entrance_markers.json', 'route_rendering.json']

# All files loadable as JSON
for f in required_files:
    path = os.path.join(pkg_dir, f)
    try:
        data = json.load(open(path))
        print(f'PASS: {f} loads as valid JSON')
    except Exception as e:
        print(f'FAIL: {f} failed to load: {e}')

# Manifest has expected structure
m = json.load(open(os.path.join(pkg_dir, 'manifest.json')))
for field in ['buildingId', 'buildingName', 'floorId', 'reviewStatus', 'packageVersion', 'files']:
    if field in m:
        print(f'PASS: manifest.{field} present')
    else:
        print(f'FAIL: manifest.{field} missing')

# Rooms file: all rooms reference valid nodes in the nav graph
rooms = json.load(open(os.path.join(pkg_dir, 'rooms.json')))
graph = json.load(open(os.path.join(pkg_dir, 'nav_graph.json')))
node_ids = {n['id'] for n in graph['nodes']}
for room in rooms['rooms']:
    dest = room['destinationNodeId']
    name = room['displayName']
    if dest in node_ids:
        print(f'PASS: Room \"{name}\" ({room[\"id\"]}) references valid node {dest}')
    else:
        print(f'FAIL: Room \"{name}\" ({room[\"id\"]}) references missing node {dest}')
" 2>&1 | while IFS= read -r line; do
    if [[ "$line" == PASS:* ]]; then
        pass "${line#PASS: }"
    elif [[ "$line" == FAIL:* ]]; then
        fail "${line#FAIL: }"
    fi
done

# --- 3. Route Existence ---
echo ""
blue "--- Route Existence (Entrance A to all rooms) ---"

python3 -c "
import json
from collections import deque

pkg_dir = '$PKG_DIR'
g = json.load(open(f'{pkg_dir}/nav_graph.json'))
r = json.load(open(f'{pkg_dir}/rooms.json'))
m = json.load(open(f'{pkg_dir}/entrance_markers.json'))

adj = {}
for n in g['nodes']: adj[n['id']] = []
for e in g['edges']:
    adj.setdefault(e['from'], []).append(e['to'])
    if e.get('bidirectional', False):
        adj.setdefault(e['to'], []).append(e['from'])

def shortest_path(start, target):
    visited = set()
    q = deque([(start, [start])])
    while q:
        cur, path = q.popleft()
        if cur == target: return path
        if cur in visited: continue
        visited.add(cur)
        for nb in adj.get(cur, []):
            q.append((nb, path + [nb]))
    return None

entrance = m['entranceMarkers'][0]['startNodeId']
for room in r['rooms']:
    dest = room['destinationNodeId']
    name = room['displayName']
    path = shortest_path(entrance, dest)
    if path and len(path) >= 2:
        print(f'PASS: Route to {name}: {\" -> \".join(path)} ({len(path)} nodes)')
    else:
        print(f'FAIL: No route from {entrance} to {dest} ({name})')
" 2>&1 | while IFS= read -r line; do
    if [[ "$line" == PASS:* ]]; then
        pass "${line#PASS: }"
    elif [[ "$line" == FAIL:* ]]; then
        fail "${line#FAIL: }"
    fi
done

# --- 4. Marker Asset/Config Consistency ---
echo ""
blue "--- Marker Asset Consistency ---"

ASSET_DIR="apps/iosApp/iosApp/Assets.xcassets/AR Resources.arresourcegroup"
MARKER_REF_NAME=$(python3 -c "import json; d=json.load(open('$PKG_DIR/entrance_markers.json')); print(d['entranceMarkers'][0]['referenceImageName'])" 2>/dev/null)

if [ -d "$ASSET_DIR/${MARKER_REF_NAME}.arreferenceimage" ]; then
    pass "AR asset directory matches referenceImageName: $MARKER_REF_NAME"
else
    fail "AR asset directory missing for referenceImageName: $MARKER_REF_NAME"
fi

if [ -f "$ASSET_DIR/${MARKER_REF_NAME}.arreferenceimage/Contents.json" ]; then
    pass "AR reference image Contents.json present"
else
    fail "AR reference image Contents.json missing"
fi

# Check iOS bundle has same entrance_markers.json
BUNDLE_DIR="apps/iosApp/iosApp/reviewed-package"
if [ -f "$BUNDLE_DIR/entrance_markers.json" ]; then
    BUNDLE_REF_NAME=$(python3 -c "import json; d=json.load(open('$BUNDLE_DIR/entrance_markers.json')); print(d['entranceMarkers'][0]['referenceImageName'])" 2>/dev/null)
    if [ "$MARKER_REF_NAME" = "$BUNDLE_REF_NAME" ]; then
        pass "Bundle referenceImageName matches source: $BUNDLE_REF_NAME"
    else
        fail "Bundle referenceImageName mismatch: source=$MARKER_REF_NAME, bundle=$BUNDLE_REF_NAME"
    fi
else
    fail "Bundle entrance_markers.json missing"
fi

# --- Summary ---
echo ""
blue "=== Regression Summary ==="
if [ "$ERRORS" -eq 0 ]; then
    green "All regression checks passed."
else
    red "$ERRORS check(s) failed."
fi

exit "$ERRORS"
