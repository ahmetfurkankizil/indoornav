# File Dossier: validate-reviewed-package.sh

## Path
`scripts\validate-reviewed-package.sh`

## Type
Authored Source

## Role
Authored Source for the scripts component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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

red()    { printf '\033[0;31m%s
```

## Status
Mapped (Pass 3 Normalization)
