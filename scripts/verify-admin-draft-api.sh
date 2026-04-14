#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────
# verify-admin-draft-api.sh
# End-to-end verification of the admin draft ingestion pipeline.
#
# Usage:
#   ./scripts/verify-admin-draft-api.sh [--start-server]
#
# Without --start-server, assumes the admin API is already running
# on localhost:8080 (or ADMIN_API_URL env var).
#
# With --start-server, starts the server in the background,
# runs the test, then stops it.
# ─────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
API_URL="${ADMIN_API_URL:-http://localhost:8080}"
SAMPLE_GLB="$REPO_ROOT/sample/demo-building/package/12_9_2025.glb"
SERVER_PID=""
START_SERVER=false

for arg in "$@"; do
    case "$arg" in
        --start-server) START_SERVER=true ;;
    esac
done

cleanup() {
    if [ -n "$SERVER_PID" ]; then
        echo "  Stopping server (PID $SERVER_PID)..."
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

passed=0
failed=0

check() {
    local desc="$1"
    shift
    if "$@" >/dev/null 2>&1; then
        echo "  ✓ $desc"
        ((passed++))
    else
        echo "  ✗ $desc"
        ((failed++))
    fi
}

check_output() {
    local desc="$1"
    local expected="$2"
    local actual="$3"
    if echo "$actual" | grep -q "$expected"; then
        echo "  ✓ $desc"
        ((passed++))
    else
        echo "  ✗ $desc (expected '$expected' in output)"
        echo "    actual: $actual"
        ((failed++))
    fi
}

echo "╔══════════════════════════════════════════════╗"
echo "║   Admin Draft API Verification               ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

# ── Pre-checks ──────────────────────────────────────────
echo "── Pre-checks ──"
check "Sample GLB exists" test -f "$SAMPLE_GLB"
check "curl available" command -v curl
check "jq available" command -v jq

if [ ! -f "$SAMPLE_GLB" ]; then
    echo "FATAL: Sample GLB not found at $SAMPLE_GLB"
    exit 1
fi

# ── Start server if requested ───────────────────────────
if [ "$START_SERVER" = true ]; then
    echo ""
    echo "── Starting admin API server ──"
    cd "$REPO_ROOT"
    ./gradlew :tools:admin-api:run &
    SERVER_PID=$!
    echo "  Server starting (PID $SERVER_PID)..."

    # Wait for server to become ready
    for i in $(seq 1 30); do
        if curl -s "$API_URL/admin/draft-jobs" >/dev/null 2>&1; then
            echo "  ✓ Server ready"
            break
        fi
        if [ "$i" -eq 30 ]; then
            echo "  ✗ Server failed to start within 30 seconds"
            exit 1
        fi
        sleep 1
    done
fi

echo ""
echo "── API: List jobs (empty) ──"
JOBS=$(curl -sf "$API_URL/admin/draft-jobs")
check_output "GET /admin/draft-jobs returns array" "\[" "$JOBS"

echo ""
echo "── API: Upload GLB (async — must return before pipeline completes) ──"
# --max-time 10 proves the upload response is not blocked by the pipeline.
# A synchronous pipeline on a real scan takes 30–120 s; if it timed out here
# the backend regressed to synchronous processing.
UPLOAD_RESPONSE=$(curl -sf --max-time 10 -X POST \
    -F "file=@$SAMPLE_GLB" \
    "$API_URL/admin/draft-jobs" || echo "UPLOAD_TIMEOUT")

if [ "$UPLOAD_RESPONSE" = "UPLOAD_TIMEOUT" ]; then
    echo "  ✗ Upload response timed out after 10s — pipeline may be blocking the HTTP response"
    ((failed++))
    exit 1
fi

echo "  Response: $UPLOAD_RESPONSE"
JOB_ID=$(echo "$UPLOAD_RESPONSE" | jq -r '.id')
JOB_STATUS=$(echo "$UPLOAD_RESPONSE" | jq -r '.status')
check_output "Upload returns job id" "." "$JOB_ID"
check_output "Upload returns queued status immediately (async)" "queued" "$JOB_STATUS"

echo ""
echo "── API: Poll job status ──"
FINAL_STATUS=""
for i in $(seq 1 60); do
    JOB_DETAIL=$(curl -sf "$API_URL/admin/draft-jobs/$JOB_ID")
    CURRENT_STATUS=$(echo "$JOB_DETAIL" | jq -r '.status')
    if [ "$CURRENT_STATUS" = "succeeded" ] || [ "$CURRENT_STATUS" = "failed" ]; then
        FINAL_STATUS="$CURRENT_STATUS"
        break
    fi
    echo "  ... status: $CURRENT_STATUS (attempt $i/60)"
    sleep 2
done

if [ "$FINAL_STATUS" = "succeeded" ]; then
    echo "  ✓ Job completed with status: succeeded"
    ((passed++))
elif [ "$FINAL_STATUS" = "failed" ]; then
    ERROR_MSG=$(echo "$JOB_DETAIL" | jq -r '.errorMessage // "unknown"')
    echo "  ✗ Job failed: $ERROR_MSG"
    ((failed++))
else
    echo "  ✗ Job did not complete within timeout (last status: $CURRENT_STATUS)"
    ((failed++))
fi

echo ""
echo "── API: Get job detail ──"
JOB_DETAIL=$(curl -sf "$API_URL/admin/draft-jobs/$JOB_ID")
check_output "Job detail contains id" "$JOB_ID" "$JOB_DETAIL"
check_output "Job detail contains filename" "12_9_2025.glb" "$JOB_DETAIL"

echo ""
echo "── API: Get artifacts ──"
ARTIFACTS=$(curl -sf "$API_URL/admin/draft-jobs/$JOB_ID/artifacts")
echo "  Artifacts: $ARTIFACTS"
if [ "$FINAL_STATUS" = "succeeded" ]; then
    check_output "Artifacts include authoring_config" "authoring_config.generated.json" "$ARTIFACTS"
    check_output "Artifacts include geometry_stats" "geometry_stats.json" "$ARTIFACTS"
    check_output "Artifacts include generation_metadata" "generation_metadata.json" "$ARTIFACTS"
fi

echo ""
echo "── API: Reject non-GLB ──"
REJECT_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -F "file=@$SCRIPT_DIR/verify-admin-draft-api.sh;filename=bad.obj" \
    "$API_URL/admin/draft-jobs")
if [ "$REJECT_RESPONSE" = "400" ]; then
    echo "  ✓ Non-GLB upload rejected with 400"
    ((passed++))
else
    echo "  ✗ Non-GLB upload returned $REJECT_RESPONSE (expected 400)"
    ((failed++))
fi

echo ""
echo "── API: List jobs (non-empty) ──"
JOBS=$(curl -sf "$API_URL/admin/draft-jobs")
JOB_COUNT=$(echo "$JOBS" | jq 'length')
if [ "$JOB_COUNT" -ge 1 ]; then
    echo "  ✓ Jobs list has $JOB_COUNT job(s)"
    ((passed++))
else
    echo "  ✗ Jobs list is empty after upload"
    ((failed++))
fi

echo ""
echo "══════════════════════════════════════════════"
echo "Results: $passed passed, $failed failed"
echo "══════════════════════════════════════════════"

if [ "$failed" -gt 0 ]; then
    exit 1
fi
echo "All checks passed."
