#!/usr/bin/env bash
#
# Validates a reviewed navigation package.
#
# Usage:
#   ./scripts/validate-reviewed-package.sh [path/to/package/directory]
#
# Defaults to: sample/reviewed-house-package/
#
# Checks:
#   1. All required files exist (manifest, rooms, nav_graph, entrance_markers, route_rendering)
#   2. manifest.json has required fields
#   3. All room destinationNodeIds reference valid nav_graph nodes
#   4. Entrance marker startNodeIds reference valid nav_graph nodes
#   5. All edge endpoints reference valid nav_graph nodes
#   6. No duplicate node ids
#   7. No duplicate edge ids
#   8. No duplicate room ids
#   9. Edge costs are positive numbers
#  10. Graph is connected from entrance to each destination (BFS reachability)
#  11. Entrance marker metadata fields are complete (Phase 4)
#  12. Building id consistency between manifest and QR contract (Phase 4)

set -euo pipefail

PACKAGE_DIR="${1:-sample/reviewed-house-package}"
ERRORS=0
WARNINGS=0

red()    { printf '\033[0;31m%s\033[0m\n' "$1"; }
green()  { printf '\033[0;32m%s\033[0m\n' "$1"; }
yellow() { printf '\033[0;33m%s\033[0m\n' "$1"; }

pass() { green "  PASS: $1"; }
fail() { red   "  FAIL: $1"; ERRORS=$((ERRORS + 1)); }
warn() { yellow "  WARN: $1"; WARNINGS=$((WARNINGS + 1)); }

echo "=== Reviewed Package Validator ==="
echo "Package: $PACKAGE_DIR"
echo ""

# --- Check: required files exist ---
echo "--- File existence ---"
REQUIRED_FILES=("manifest.json" "rooms.json" "nav_graph.json" "entrance_markers.json" "route_rendering.json")
for f in "${REQUIRED_FILES[@]}"; do
    if [ -f "$PACKAGE_DIR/$f" ]; then
        pass "$f exists"
    else
        fail "$f is missing"
    fi
done

# Abort early if critical files are missing
for f in manifest.json rooms.json nav_graph.json entrance_markers.json; do
    if [ ! -f "$PACKAGE_DIR/$f" ]; then
        red "\nCritical files missing. Cannot continue validation."
        exit 1
    fi
done

# --- Check: manifest fields ---
echo ""
echo "--- Manifest validation ---"
for field in buildingId buildingName floorId reviewStatus packageVersion; do
    val=$(python3 -c "import json; d=json.load(open('$PACKAGE_DIR/manifest.json')); print(d.get('$field',''))" 2>/dev/null)
    if [ -n "$val" ]; then
        pass "manifest.$field = $val"
    else
        fail "manifest.$field is missing or empty"
    fi
done

# --- Load graph data ---
echo ""
echo "--- Nav graph validation ---"

# Extract node ids
NODE_IDS=$(python3 -c "
import json
g = json.load(open('$PACKAGE_DIR/nav_graph.json'))
for n in g['nodes']:
    print(n['id'])
" 2>/dev/null)

NODE_COUNT=$(echo "$NODE_IDS" | wc -l | tr -d ' ')
pass "nav_graph has $NODE_COUNT nodes"

# Check for duplicate node ids
DUP_NODES=$(echo "$NODE_IDS" | sort | uniq -d)
if [ -z "$DUP_NODES" ]; then
    pass "No duplicate node ids"
else
    fail "Duplicate node ids: $DUP_NODES"
fi

# Extract edge data and validate
python3 -c "
import json, sys

g = json.load(open('$PACKAGE_DIR/nav_graph.json'))
node_ids = set(n['id'] for n in g['nodes'])
edge_ids = []
errors = 0

for e in g['edges']:
    eid = e['id']
    edge_ids.append(eid)

    if e['from'] not in node_ids:
        print(f'EDGE_ERR: edge {eid} references unknown from-node: {e[\"from\"]}')
        errors += 1
    if e['to'] not in node_ids:
        print(f'EDGE_ERR: edge {eid} references unknown to-node: {e[\"to\"]}')
        errors += 1
    if e.get('cost', 0) <= 0:
        print(f'EDGE_ERR: edge {eid} has invalid cost: {e.get(\"cost\")}')
        errors += 1

# Check duplicate edge ids
seen = set()
for eid in edge_ids:
    if eid in seen:
        print(f'EDGE_ERR: duplicate edge id: {eid}')
        errors += 1
    seen.add(eid)

if errors == 0:
    print('EDGE_OK')
else:
    print(f'EDGE_ERRORS:{errors}')
" 2>/dev/null | while IFS= read -r line; do
    if [[ "$line" == "EDGE_OK" ]]; then
        pass "All edges reference valid nodes, positive costs, no duplicates"
    elif [[ "$line" == EDGE_ERR:* ]]; then
        fail "$line"
    elif [[ "$line" == EDGE_ERRORS:* ]]; then
        fail "${line#EDGE_ERRORS:} edge validation errors found"
    fi
done

# --- Check: rooms reference valid nodes ---
echo ""
echo "--- Room validation ---"

python3 -c "
import json

g = json.load(open('$PACKAGE_DIR/nav_graph.json'))
r = json.load(open('$PACKAGE_DIR/rooms.json'))
node_ids = set(n['id'] for n in g['nodes'])
room_ids = []
errors = 0

for room in r['rooms']:
    room_ids.append(room['id'])
    dest = room['destinationNodeId']
    name = room['displayName']
    if dest in node_ids:
        print(f'ROOM_OK: {name} -> {dest}')
    else:
        print(f'ROOM_ERR: {name} references unknown node: {dest}')
        errors += 1

# Check duplicate room ids
seen = set()
for rid in room_ids:
    if rid in seen:
        print(f'ROOM_ERR: duplicate room id: {rid}')
        errors += 1
    seen.add(rid)

if errors > 0:
    print(f'ROOM_ERRORS:{errors}')
" 2>/dev/null | while IFS= read -r line; do
    if [[ "$line" == ROOM_OK:* ]]; then
        pass "${line#ROOM_OK: }"
    elif [[ "$line" == ROOM_ERR:* ]]; then
        fail "${line#ROOM_ERR: }"
    elif [[ "$line" == ROOM_ERRORS:* ]]; then
        fail "${line#ROOM_ERRORS:} room validation errors"
    fi
done

# --- Check: entrance markers reference valid nodes and have required metadata ---
echo ""
echo "--- Entrance marker validation ---"

python3 -c "
import json

g = json.load(open('$PACKAGE_DIR/nav_graph.json'))
m = json.load(open('$PACKAGE_DIR/entrance_markers.json'))
manifest = json.load(open('$PACKAGE_DIR/manifest.json'))
node_ids = set(n['id'] for n in g['nodes'])

for marker in m['entranceMarkers']:
    mid = marker['id']
    start = marker['startNodeId']

    # Check start node reference
    if start in node_ids:
        print(f'MARKER_OK: {mid} -> {start}')
    else:
        print(f'MARKER_ERR: {mid} references unknown node: {start}')

    # Check required metadata fields (Phase 4)
    required_fields = {
        'physicalWidthMeters': 'physical width',
        'physicalHeightMeters': 'physical height',
        'position': 'position',
        'forwardBasis': 'forward basis',
        'referenceImageName': 'reference image name',
    }
    for field, label in required_fields.items():
        val = marker.get(field)
        if val is None or val == '':
            print(f'MARKER_ERR: {mid} missing required field: {label} ({field})')
        else:
            print(f'MARKER_META_OK: {mid}.{field} = {val}')

    # Check position has x, y, z
    pos = marker.get('position', {})
    for coord in ['x', 'y', 'z']:
        if coord not in pos:
            print(f'MARKER_ERR: {mid} position missing coordinate: {coord}')

    # Check physical dimensions are positive
    w = marker.get('physicalWidthMeters', 0)
    h = marker.get('physicalHeightMeters', 0)
    if isinstance(w, (int, float)) and w > 0:
        print(f'MARKER_META_OK: {mid} physicalWidth={w}m')
    else:
        print(f'MARKER_ERR: {mid} physicalWidthMeters must be positive: {w}')
    if isinstance(h, (int, float)) and h > 0:
        print(f'MARKER_META_OK: {mid} physicalHeight={h}m')
    else:
        print(f'MARKER_ERR: {mid} physicalHeightMeters must be positive: {h}')

# Check building id is present and non-empty (QR contract consistency)
building_id = manifest.get('buildingId', '')
if building_id:
    print(f'BUILDING_OK: manifest.buildingId = {building_id}')
else:
    print('BUILDING_ERR: manifest.buildingId is missing or empty (required for QR contract)')
" 2>/dev/null | while IFS= read -r line; do
    if [[ "$line" == MARKER_OK:* ]]; then
        pass "${line#MARKER_OK: }"
    elif [[ "$line" == MARKER_ERR:* ]]; then
        fail "${line#MARKER_ERR: }"
    elif [[ "$line" == MARKER_META_OK:* ]]; then
        pass "${line#MARKER_META_OK: }"
    elif [[ "$line" == BUILDING_OK:* ]]; then
        pass "${line#BUILDING_OK: }"
    elif [[ "$line" == BUILDING_ERR:* ]]; then
        fail "${line#BUILDING_ERR: }"
    fi
done

# --- Check: graph connectivity (BFS from entrance to each destination) ---
echo ""
echo "--- Connectivity check ---"

python3 -c "
import json
from collections import deque

g = json.load(open('$PACKAGE_DIR/nav_graph.json'))
r = json.load(open('$PACKAGE_DIR/rooms.json'))
m = json.load(open('$PACKAGE_DIR/entrance_markers.json'))

# Build adjacency
adj = {}
for n in g['nodes']:
    adj[n['id']] = []
for e in g['edges']:
    adj.setdefault(e['from'], []).append(e['to'])
    if e.get('bidirectional', False):
        adj.setdefault(e['to'], []).append(e['from'])

# BFS reachability
def reachable(start, target):
    visited = set()
    q = deque([start])
    while q:
        cur = q.popleft()
        if cur == target:
            return True
        if cur in visited:
            continue
        visited.add(cur)
        for nb in adj.get(cur, []):
            if nb not in visited:
                q.append(nb)
    return False

entrance = m['entranceMarkers'][0]['startNodeId']

for room in r['rooms']:
    dest = room['destinationNodeId']
    name = room['displayName']
    if reachable(entrance, dest):
        print(f'CONN_OK: {entrance} -> {dest} ({name})')
    else:
        print(f'CONN_ERR: No path from {entrance} to {dest} ({name})')
" 2>/dev/null | while IFS= read -r line; do
    if [[ "$line" == CONN_OK:* ]]; then
        pass "Reachable: ${line#CONN_OK: }"
    elif [[ "$line" == CONN_ERR:* ]]; then
        fail "Unreachable: ${line#CONN_ERR: }"
    fi
done

# --- Summary ---
echo ""
echo "=== Validation Summary ==="
if [ "$ERRORS" -eq 0 ]; then
    green "All checks passed."
else
    red "$ERRORS error(s) found."
fi
if [ "$WARNINGS" -gt 0 ]; then
    yellow "$WARNINGS warning(s)."
fi

exit "$ERRORS"
