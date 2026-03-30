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
for arg in "$@"; do
  [[ "$arg" == "--start-server" ]] && START_SERVER=true
done

# ── Server lifecycle ──────────────────────────────────────
start_server() {
  info "Starting admin API server…"
  cd "$REPO_ROOT"
  ./gradlew :tools:admin-api:run > /tmp/admin-api.log 2>&1 &
  SERVER_PID=$!
  info "Waiting for server to become ready (PID $SERVER_PID)…"
  for i in $(seq 1 30); do
    if curl -sf "$BASE_URL/admin/draft-jobs" > /dev/null 2>&1; then
      ok "Server is up"
      return
    fi
    sleep 1
  done
  fail "Server did not start within 30s"
  cat /tmp/admin-api.log
  exit 1
}

stop_server() {
  if [[ -n "$SERVER_PID" ]]; then
    info "Stopping server (PID $SERVER_PID)…"
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}
trap stop_server EXIT

# ── Helpers ───────────────────────────────────────────────
require_jq() {
  if ! command -v jq &> /dev/null; then
    echo "Error: jq is required. Install via: brew install jq" >&2
    exit 1
  fi
}

assert_status() {
  local actual="$1" expected="$2" label="$3"
  if [[ "$actual" == "$expected" ]]; then
    ok "$label (HTTP $actual)"
  else
    fail "$label — expected $expected, got $actual"
  fi
}

assert_nonempty() {
  local value="$1" label="$2"
  if [[ -n "$value" && "$value" != "null" && "$value" != "[]" ]]; then
    ok "$label: $value"
  else
    fail "$label is empty or null"
  fi
}

assert_true() {
  local value="$1" label="$2"
  if [[ "$value" == "true" ]]; then
    ok "$label"
  else
    fail "$label — expected true, got $value"
  fi
}

assert_gt() {
  local value="$1" threshold="$2" label="$3"
  if [[ "$value" =~ ^[0-9]+$ ]] && (( value > threshold )); then
    ok "$label: $value"
  else
    fail "$label — expected > $threshold, got $value"
  fi
}

# ── Phase 1 prerequisite check ────────────────────────────
check_phase1() {
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Phase 1 smoke check"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/admin/draft-jobs")
  assert_status "$HTTP" "200" "GET /admin/draft-jobs reachable"
}

# ── Upload sample GLB and wait for completion ─────────────
upload_and_wait() {
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Uploading sample GLB"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  if [[ ! -f "$SAMPLE_GLB" ]]; then
    fail "Sample GLB not found at: $SAMPLE_GLB"
    exit 1
  fi
  ok "Sample GLB found: $(basename "$SAMPLE_GLB")"

  UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST "$BASE_URL/admin/draft-jobs" \
    -F "file=@$SAMPLE_GLB" 2>&1)

  HTTP=$(echo "$UPLOAD_RESPONSE" | tail -1)
  BODY=$(echo "$UPLOAD_RESPONSE" | head -n -1)

  assert_status "$HTTP" "201" "POST /admin/draft-jobs (upload)"

  JOB_ID=$(echo "$BODY" | jq -r '.id // empty')
  assert_nonempty "$JOB_ID" "Job ID assigned"

  info "Job ID: $JOB_ID"
  echo "$JOB_ID"
}

# ── Poll for job completion ───────────────────────────────
wait_for_success() {
  local job_id="$1"
  echo ""
  info "Polling job $job_id for completion…"

  for i in $(seq 1 60); do
    STATUS=$(curl -s "$BASE_URL/admin/draft-jobs/$job_id" | jq -r '.status // empty')
    case "$STATUS" in
      succeeded)
        ok "Job reached 'succeeded' (attempt $i)"
        return 0
        ;;
      failed)
        ERROR=$(curl -s "$BASE_URL/admin/draft-jobs/$job_id" | jq -r '.errorMessage // "unknown"')
        fail "Job failed: $ERROR"
        return 1
        ;;
      queued|processing)
        echo -n "."
        sleep 3
        ;;
      *)
        fail "Unexpected status: $STATUS"
        return 1
        ;;
    esac
  done

  echo ""
  fail "Job did not complete within 180s"
  return 1
}

# ── Summary endpoint tests ────────────────────────────────
verify_summary() {
  local job_id="$1"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Summary endpoint"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  SUMMARY_RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/admin/draft-jobs/$job_id/summary")
  HTTP=$(echo "$SUMMARY_RESP" | tail -1)
  SUMMARY=$(echo "$SUMMARY_RESP" | head -n -1)

  assert_status "$HTTP" "200" "GET /admin/draft-jobs/$job_id/summary"

  # Basic shape
  assert_nonempty "$(echo "$SUMMARY" | jq -r '.jobId // empty')" "jobId present"
  assert_nonempty "$(echo "$SUMMARY" | jq -r '.buildingId // empty')" "buildingId present"

  # Counts
  NODE_COUNT=$(echo "$SUMMARY" | jq -r '.counts.nodes // 0')
  ROOM_COUNT=$(echo "$SUMMARY" | jq -r '.counts.rooms // 0')
  EDGE_COUNT=$(echo "$SUMMARY" | jq -r '.counts.edges // 0')
  assert_gt "$NODE_COUNT" 0 "Node count > 0"
  assert_gt "$ROOM_COUNT" 0 "Room count > 0"
  assert_gt "$EDGE_COUNT" 0 "Edge count > 0"

  # Rooms list
  ROOMS_LEN=$(echo "$SUMMARY" | jq '.rooms | length')
  assert_gt "$ROOMS_LEN" 0 "Rooms array non-empty"

  FIRST_ROOM_NAME=$(echo "$SUMMARY" | jq -r '.rooms[0].displayName // empty')
  assert_nonempty "$FIRST_ROOM_NAME" "First room has displayName"

  # Artifact availability
  HAS_OCCUPANCY=$(echo "$SUMMARY" | jq -r '.artifactAvailability.hasOccupancyPreview // false')
  HAS_GRAPH=$(echo "$SUMMARY" | jq -r '.artifactAvailability.hasGraphPreview // false')
  assert_true "$HAS_OCCUPANCY" "artifactAvailability.hasOccupancyPreview"
  assert_true "$HAS_GRAPH" "artifactAvailability.hasGraphPreview"

  # Generation metadata
  CONFIDENCE=$(echo "$SUMMARY" | jq -r '.generationMetadata.confidence // empty')
  assert_nonempty "$CONFIDENCE" "generationMetadata.confidence present"

  # Geometry stats
  VERTICES=$(echo "$SUMMARY" | jq -r '.geometryStats.totalVertices // 0')
  assert_gt "$VERTICES" 0 "geometryStats.totalVertices > 0"

  info "Room candidates:"
  echo "$SUMMARY" | jq -r '.rooms[] | "    • \(.displayName) [\(.category // "?")] → \(.destinationNodeId // "?")"'
}

# ── Artifact content tests ────────────────────────────────
verify_artifact_content() {
  local job_id="$1"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Artifact content endpoints"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  for ARTIFACT in "occupancy_debug.svg" "draft_graph_debug.svg"; do
    CONTENT_RESP=$(curl -s -w "\n%{http_code}" \
      "$BASE_URL/admin/draft-jobs/$job_id/artifacts/$ARTIFACT/content")
    HTTP=$(echo "$CONTENT_RESP" | tail -1)
    BODY=$(echo "$CONTENT_RESP" | head -n -1)

    assert_status "$HTTP" "200" "GET artifacts/$ARTIFACT/content"

    if echo "$BODY" | grep -q "<svg"; then
      ok "$ARTIFACT contains <svg>"
    else
      fail "$ARTIFACT does not contain <svg> tag"
    fi
  done

  # Path traversal rejection
  TRAVERSAL_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/admin/draft-jobs/$job_id/artifacts/..%2Fjob.json/content")
  if [[ "$TRAVERSAL_HTTP" == "404" || "$TRAVERSAL_HTTP" == "400" ]]; then
    ok "Path traversal rejected (HTTP $TRAVERSAL_HTTP)"
  else
    fail "Path traversal not rejected — got HTTP $TRAVERSAL_HTTP"
  fi

  # Unknown artifact
  UNKNOWN_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/admin/draft-jobs/$job_id/artifacts/not_a_real_file.svg/content")
  assert_status "$UNKNOWN_HTTP" "404" "Unknown artifact returns 404"
}

# ── 404 guard for nonexistent job ─────────────────────────
verify_not_found() {
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Error handling"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/admin/draft-jobs/does-not-exist/summary")
  assert_status "$HTTP" "404" "Summary for nonexistent job returns 404"
}

# ── Main ──────────────────────────────────────────────────
require_jq

if [[ "$START_SERVER" == "true" ]]; then
  start_server
else
  info "Targeting $BASE_URL (use --start-server to auto-start)"
  if ! curl -sf "$BASE_URL/admin/draft-jobs" > /dev/null 2>&1; then
    echo ""
    echo "Error: Admin API not reachable at $BASE_URL"
    echo "Start it with: ./gradlew :tools:admin-api:run"
    echo "Or run this script with: ./scripts/verify-admin-draft-review.sh --start-server"
    exit 1
  fi
fi

check_phase1
JOB_ID=$(upload_and_wait)
wait_for_success "$JOB_ID"
verify_summary "$JOB_ID"
verify_artifact_content "$JOB_ID"
verify_not_found

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if (( FAIL == 0 )); then
  echo -e "${GREEN}All checks passed: $PASS passed, 0 failed${NC}"
  exit 0
else
  echo -e "${RED}$FAIL check(s) failed, $PASS passed${NC}"
  exit 1
fi
