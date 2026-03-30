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

# ── Optional server start ────────────────────────────────────────────────────
if [[ "${1:-}" == "--start-server" ]]; then
    info "Building and starting admin API server…"
    ./gradlew :tools:admin-api:installDist --quiet
    tools/admin-api/build/install/admin-api/bin/admin-api &
    SERVER_PID=$!
    trap "kill $SERVER_PID 2>/dev/null || true" EXIT
    sleep 4
fi

# ── Prerequisite checks ──────────────────────────────────────────────────────
echo ""
echo "=== Admin Room Edit + Export Verification ==="
echo "  API: $BASE_URL"
echo ""

if [[ ! -f "$SAMPLE_GLB" ]]; then
    fail "Sample GLB not found at $SAMPLE_GLB — run the nav-preprocessor first"
fi

command -v curl  >/dev/null 2>&1 || fail "curl is required"
command -v jq    >/dev/null 2>&1 || fail "jq is required"

# ── Health check ─────────────────────────────────────────────────────────────
echo "--- 1. Server health ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/admin/draft-jobs")
if [[ "$STATUS" == "200" ]]; then
    pass "Admin API is reachable"
else
    fail "Admin API returned HTTP $STATUS — is the server running? (use --start-server)"
fi

# ── Upload GLB ───────────────────────────────────────────────────────────────
echo ""
echo "--- 2. Upload sample GLB ---"
UPLOAD_RESP=$(curl -s -X POST "$BASE_URL/admin/draft-jobs" \
    -F "file=@$SAMPLE_GLB")
JOB_ID=$(echo "$UPLOAD_RESP" | jq -r '.id // empty')
[[ -n "$JOB_ID" ]] || fail "Upload failed: $UPLOAD_RESP"
pass "Job created: $JOB_ID"

# ── Poll until succeeded ─────────────────────────────────────────────────────
echo ""
echo "--- 3. Wait for job to succeed (up to ${MAX_POLL_SECONDS}s) ---"
ELAPSED=0
while true; do
    JOB_STATUS=$(curl -s "$BASE_URL/admin/draft-jobs/$JOB_ID" | jq -r '.status // empty')
    if [[ "$JOB_STATUS" == "succeeded" ]]; then
        pass "Job $JOB_ID succeeded"
        break
    elif [[ "$JOB_STATUS" == "failed" ]]; then
        ERR=$(curl -s "$BASE_URL/admin/draft-jobs/$JOB_ID" | jq -r '.errorMessage // "unknown"')
        fail "Job $JOB_ID failed: $ERR"
    fi
    if [[ $ELAPSED -ge $MAX_POLL_SECONDS ]]; then
        fail "Job $JOB_ID did not succeed within ${MAX_POLL_SECONDS}s (status: $JOB_STATUS)"
    fi
    sleep $POLL_INTERVAL
    ELAPSED=$((ELAPSED + POLL_INTERVAL))
done

# ── Read room ids from summary ───────────────────────────────────────────────
echo ""
echo "--- 4. Read draft summary ---"
SUMMARY=$(curl -s "$BASE_URL/admin/draft-jobs/$JOB_ID/summary")
ROOM_COUNT=$(echo "$SUMMARY" | jq '.rooms | length')
[[ "$ROOM_COUNT" -gt 0 ]] || fail "Summary has no rooms"
ROOM_ID=$(echo "$SUMMARY" | jq -r '.rooms[0].id')
ORIGINAL_NAME=$(echo "$SUMMARY" | jq -r '.rooms[0].displayName')
pass "Summary has $ROOM_COUNT room(s); first room: $ROOM_ID (\"$ORIGINAL_NAME\")"

# ── PATCH room ───────────────────────────────────────────────────────────────
echo ""
echo "--- 5. PATCH room display name and category ---"
PATCH_NAME="Verified Kitchen $(date +%s)"
PATCH_RESP=$(curl -s -X PATCH "$BASE_URL/admin/draft-jobs/$JOB_ID/rooms/$ROOM_ID" \
    -H "Content-Type: application/json" \
    -d "{\"displayName\":\"$PATCH_NAME\",\"category\":\"kitchen\"}")
PATCH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PATCH "$BASE_URL/admin/draft-jobs/$JOB_ID/rooms/$ROOM_ID" \
    -H "Content-Type: application/json" \
    -d "{\"displayName\":\"$PATCH_NAME\",\"category\":\"kitchen\"}")

if [[ "$PATCH_STATUS" == "200" ]]; then
    pass "PATCH returned 200"
else
    fail "PATCH returned HTTP $PATCH_STATUS: $PATCH_RESP"
fi

# ── Verify summary reflects override ────────────────────────────────────────
echo ""
echo "--- 6. Verify summary reflects the override ---"
UPDATED_SUMMARY=$(curl -s "$BASE_URL/admin/draft-jobs/$JOB_ID/summary")
UPDATED_NAME=$(echo "$UPDATED_SUMMARY" | jq -r --arg id "$ROOM_ID" '.rooms[] | select(.id == $id) | .displayName')
UPDATED_CAT=$(echo  "$UPDATED_SUMMARY" | jq -r --arg id "$ROOM_ID" '.rooms[] | select(.id == $id) | .category')

if [[ "$UPDATED_NAME" == "$PATCH_NAME" ]]; then
    pass "Summary displayName updated to \"$UPDATED_NAME\""
else
    fail "Summary displayName is \"$UPDATED_NAME\", expected \"$PATCH_NAME\""
fi

if [[ "$UPDATED_CAT" == "kitchen" ]]; then
    pass "Summary category updated to \"$UPDATED_CAT\""
else
    fail "Summary category is \"$UPDATED_CAT\", expected \"kitchen\""
fi

# ── Export reviewed package ──────────────────────────────────────────────────
echo ""
echo "--- 7. Export reviewed package ---"
EXPORT_RESP=$(curl -s -X POST "$BASE_URL/admin/draft-jobs/$JOB_ID/export-reviewed-package")
EXPORT_STATUS_VAL=$(echo "$EXPORT_RESP" | jq -r '.status // empty')
EXPORT_FILES=$(echo "$EXPORT_RESP" | jq -r '.files[]' 2>/dev/null | tr '\n' ' ')

if [[ "$EXPORT_STATUS_VAL" == "succeeded" ]]; then
    pass "Export succeeded; files: $EXPORT_FILES"
else
    fail "Export failed: $EXPORT_RESP"
fi

# ── Verify all 5 required files present ─────────────────────────────────────
echo ""
echo "--- 8. Verify all 5 required package files ---"
for FNAME in manifest.json rooms.json nav_graph.json entrance_markers.json route_rendering.json; do
    PRESENT=$(echo "$EXPORT_RESP" | jq -r --arg f "$FNAME" '.files[] | select(. == $f)' | wc -l | tr -d ' ')
    if [[ "$PRESENT" -gt 0 ]]; then
        pass "$FNAME present in export"
    else
        fail "$FNAME missing from export result"
    fi
done

# ── Verify GET reviewed-package lists files ──────────────────────────────────
echo ""
echo "--- 9. GET reviewed-package file listing ---"
PKG_LIST=$(curl -s "$BASE_URL/admin/draft-jobs/$JOB_ID/reviewed-package")
PKG_COUNT=$(echo "$PKG_LIST" | jq 'length')
[[ "$PKG_COUNT" -ge 5 ]] || fail "reviewed-package listing has $PKG_COUNT files, expected >=5"
pass "reviewed-package lists $PKG_COUNT files"

# ── Verify rooms.json contains edited values ─────────────────────────────────
echo ""
echo "--- 10. Verify rooms.json contains edited display name ---"
ROOMS_JSON=$(curl -s "$BASE_URL/admin/draft-jobs/$JOB_ID/reviewed-package/rooms.json/content")
if echo "$ROOMS_JSON" | grep -q "$PATCH_NAME"; then
    pass "rooms.json contains \"$PATCH_NAME\""
else
    fail "rooms.json does not contain \"$PATCH_NAME\"; content: $ROOMS_JSON"
fi
if echo "$ROOMS_JSON" | grep -q '"kitchen"'; then
    pass "rooms.json contains category \"kitchen\""
else
    fail "rooms.json does not contain category \"kitchen\""
fi

# ── Path traversal rejection ─────────────────────────────────────────────────
echo ""
echo "--- 11. Path traversal rejection ---"
TRAV_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/admin/draft-jobs/$JOB_ID/reviewed-package/..%2Fjob.json/content")
if [[ "$TRAV_STATUS" == "404" || "$TRAV_STATUS" == "400" ]]; then
    pass "Path traversal blocked (HTTP $TRAV_STATUS)"
else
    fail "Path traversal not blocked (HTTP $TRAV_STATUS)"
fi

# ── Done ─────────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}All checks passed — Admin Phase 3 (room edit + export) verified.${NC}"
echo ""
