#!/usr/bin/env bash
# generate-entrance-poster.sh — Single source of truth for the entrance poster.
#
# Generates a combined entrance poster that serves as BOTH:
#   1. The QR code scanned by AVCaptureSession
#   2. The AR reference image detected by ARKit
#
# The generated image is copied into the Xcode asset catalog so the printed
# poster and the bundled reference image are always identical.
#
# Requirements:
#   - Python 3 with 'qrcode' and 'Pillow' (pip3 install qrcode pillow)
#   OR
#   - qrencode CLI tool (brew install qrencode)
#
# Usage:
#   ./scripts/generate-entrance-poster.sh
#   ./scripts/generate-entrance-poster.sh --print-only   # just regenerate, don't copy to asset catalog

set -euo pipefail
cd "$(dirname "$0")/.."

REPO_ROOT="$(pwd)"

# --- Configuration (single source of truth) ---
BUILDING_ID="19"
ENTRANCE_ID="marker-main-entrance"
QR_VERSION=1
PHYSICAL_WIDTH_CM=21
PHYSICAL_WIDTH_M="0.21"
POSTER_PX=1050          # 1050px at 127 DPI ≈ 21cm print size
QR_QUIET_ZONE_MODULES=4 # standard QR quiet zone
BORDER_PX=60             # white border around QR for ARKit feature detection
ASSET_NAME="entrance_marker_main"

# Paths
POSTER_OUTPUT="${REPO_ROOT}/sample/entrance-poster"
POSTER_FILE="${POSTER_OUTPUT}/entrance_poster.png"
ASSET_CATALOG_DIR="${REPO_ROOT}/apps/iosApp/iosApp/Assets.xcassets/AR Resources.arresourcegroup/${ASSET_NAME}.arreferenceimage"
ASSET_IMAGE="${ASSET_CATALOG_DIR}/entrance_marker.png"

# QR payload
QR_PAYLOAD="{\"type\":\"VecturAI-entrance\",\"buildingId\":\"${BUILDING_ID}\",\"entranceId\":\"${ENTRANCE_ID}\",\"v\":${QR_VERSION}}"

echo "=== VecturAI Entrance Poster Generator ==="
echo ""
echo "QR Payload: ${QR_PAYLOAD}"
echo "Physical size: ${PHYSICAL_WIDTH_CM}cm × ${PHYSICAL_WIDTH_CM}cm"
echo "Pixel size: ${POSTER_PX}px × ${POSTER_PX}px"
echo "Asset name: ${ASSET_NAME}"
echo ""

mkdir -p "${POSTER_OUTPUT}"

# --- Generate poster ---
GENERATED=false

# Try Python first (better quality, more control)
if command -v python3 &>/dev/null; then
    if python3 -c "import qrcode; from PIL import Image" 2>/dev/null; then
        echo "[1/3] Generating poster with Python (qrcode + Pillow)..."
        python3 - "${QR_PAYLOAD}" "${POSTER_FILE}" "${POSTER_PX}" "${BORDER_PX}" <<'PYEOF'
import sys
import qrcode
from PIL import Image, ImageDraw, ImageFont

payload = sys.argv[1]
output_path = sys.argv[2]
target_size = int(sys.argv[3])
border = int(sys.argv[4])

# Generate QR code
qr = qrcode.QRCode(
    version=None,
    error_correction=qrcode.constants.ERROR_CORRECT_H,
    box_size=20,
    border=4,
)
qr.add_data(payload)
qr.make(fit=True)
qr_img = qr.make_image(fill_color="black", back_color="white").convert("RGB")

# Create poster canvas with white border
poster = Image.new("RGB", (target_size, target_size), "white")

# Scale QR to fit inside the border
qr_area = target_size - 2 * border
qr_scaled = qr_img.resize((qr_area, qr_area), Image.NEAREST)

# Paste centered
offset = border
poster.paste(qr_scaled, (offset, offset))

# Add thin black frame at the very edge (helps ARKit feature detection)
draw = ImageDraw.Draw(poster)
draw.rectangle([(0, 0), (target_size - 1, target_size - 1)], outline="black", width=3)

# Add small label at bottom
try:
    font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 18)
except:
    font = ImageFont.load_default()
label = "VecturAI Entrance"
bbox = draw.textbbox((0, 0), label, font=font)
tw = bbox[2] - bbox[0]
draw.text(((target_size - tw) // 2, target_size - border + 5), label, fill="black", font=font)

poster.save(output_path, "PNG", dpi=(127, 127))
print(f"  Poster saved: {output_path} ({target_size}x{target_size}px)")
PYEOF
        GENERATED=true
    else
        echo "  Python 3 found but missing 'qrcode' or 'Pillow'. Install: pip3 install qrcode pillow"
    fi
fi

# Fallback: qrencode CLI
# IMPORTANT: qrencode's -s/--size flag sets MODULE size (pixels per module),
# NOT total image size. A module size of 20 with ~57 modules = ~1140px total.
# Do NOT pass POSTER_PX as --size — that would create a ~60,000px image.
if [ "$GENERATED" = false ] && command -v qrencode &>/dev/null; then
    echo "[1/3] Generating poster with qrencode CLI..."
    qrencode -o "${POSTER_FILE}" -s 20 -m 4 -l H -t PNG "${QR_PAYLOAD}"
    echo "  Poster saved: ${POSTER_FILE}"
    # Resize to exact target if sips is available (macOS)
    if command -v sips &>/dev/null; then
        sips -z "${POSTER_PX}" "${POSTER_PX}" "${POSTER_FILE}" > /dev/null 2>&1
        echo "  Resized to ${POSTER_PX}x${POSTER_PX}px"
    fi
    GENERATED=true
fi

if [ "$GENERATED" = false ]; then
    echo ""
    echo "ERROR: No QR generation tool available."
    echo "Install one of:"
    echo "  pip3 install qrcode pillow"
    echo "  brew install qrencode"
    echo ""
    echo "Or manually create ${POSTER_FILE}:"
    echo "  - Generate a QR code from this payload: ${QR_PAYLOAD}"
    echo "  - Save as ${POSTER_PX}x${POSTER_PX}px PNG"
    echo "  - Include a white border of at least ${BORDER_PX}px"
    exit 1
fi

# --- Safety check: reject decompression-bomb-sized images ---
if command -v sips &>/dev/null; then
    ACTUAL_W=$(sips -g pixelWidth "${POSTER_FILE}" 2>/dev/null | awk '/pixelWidth/{print $2}')
    ACTUAL_H=$(sips -g pixelHeight "${POSTER_FILE}" 2>/dev/null | awk '/pixelHeight/{print $2}')
    MAX_PX=4000
    if [ "${ACTUAL_W:-0}" -gt "$MAX_PX" ] || [ "${ACTUAL_H:-0}" -gt "$MAX_PX" ]; then
        echo ""
        echo "ERROR: Generated image is ${ACTUAL_W}x${ACTUAL_H}px — exceeds ${MAX_PX}px safety limit."
        echo "This would cause Xcode/Preview to consume tens of GB of RAM."
        echo "Check the QR generation parameters (module size vs image size)."
        echo "Resizing to ${POSTER_PX}x${POSTER_PX}px as safety fallback..."
        sips -z "${POSTER_PX}" "${POSTER_PX}" "${POSTER_FILE}" > /dev/null 2>&1
        ACTUAL_W=$(sips -g pixelWidth "${POSTER_FILE}" 2>/dev/null | awk '/pixelWidth/{print $2}')
        echo "  Corrected to ${ACTUAL_W}x${ACTUAL_W}px"
    fi
fi

# --- Copy to asset catalog ---
if [ "${1:-}" = "--print-only" ]; then
    echo ""
    echo "[SKIP] --print-only: not copying to asset catalog."
else
    echo "[2/3] Copying to asset catalog..."
    mkdir -p "${ASSET_CATALOG_DIR}"
    cp "${POSTER_FILE}" "${ASSET_IMAGE}"
    echo "  Copied to: ${ASSET_IMAGE}"

    # Also copy to entrance_marker_display.imageset so the ARSessionManager
    # rebuild path uses the same image (physicalSize is 0 when loaded from catalog).
    DISPLAY_IMAGESET="${REPO_ROOT}/apps/iosApp/iosApp/Assets.xcassets/entrance_marker_display.imageset"
    mkdir -p "${DISPLAY_IMAGESET}"
    cp "${POSTER_FILE}" "${DISPLAY_IMAGESET}/entrance_marker.png"
    echo "  Copied to: ${DISPLAY_IMAGESET}/entrance_marker.png (display imageset sync)"

    # Write/update Contents.json for the reference image
    cat > "${ASSET_CATALOG_DIR}/Contents.json" <<JSONEOF
{
  "images" : [
    {
      "filename" : "entrance_marker.png",
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  },
  "properties" : {
    "physicalWidth" : ${PHYSICAL_WIDTH_M}
  }
}
JSONEOF
    echo "  Updated Contents.json (physicalWidth: ${PHYSICAL_WIDTH_M}m)"
fi

# --- Print instructions ---
echo "[3/3] Done."
echo ""
echo "=== Entrance Poster Contract ==="
echo "  Source image: ${POSTER_FILE}"
echo "  Asset catalog: ${ASSET_IMAGE}"
echo "  Asset name: ${ASSET_NAME}"
echo "  Physical size: ${PHYSICAL_WIDTH_CM}cm × ${PHYSICAL_WIDTH_CM}cm"
echo "  QR payload: ${QR_PAYLOAD}"
echo ""
echo "=== Next Steps ==="
echo "  1. Print ${POSTER_FILE} at exactly ${PHYSICAL_WIDTH_CM}cm × ${PHYSICAL_WIDTH_CM}cm on matte paper"
echo "  2. Place at eye level (~1.2m) at the building entrance"
echo "  3. Rebuild the iOS app in Xcode (the asset catalog was updated)"
echo "  4. Run: ./scripts/check-demo-readiness.sh"
echo "  5. Demo: scan the poster QR → AR will detect the same poster for alignment"
echo ""
echo "IMPORTANT: The printed poster and the bundled AR reference image must be"
echo "IDENTICAL. If you regenerate this poster, you must also rebuild the app."
