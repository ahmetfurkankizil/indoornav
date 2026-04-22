# File Dossier: generate-entrance-poster.sh

## Path
`scripts\generate-entrance-poster.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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
QR_QUIET_ZONE_MO
```

## Status
Mapped (Pass 3 Normalization)
