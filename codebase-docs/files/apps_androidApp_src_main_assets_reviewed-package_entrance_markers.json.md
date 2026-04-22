# File Dossier: entrance_markers.json

## Path
`apps/androidApp/src/main/assets/reviewed-package/entrance_markers.json`

## Type
Bundled Runtime Data (Reviewed Package Entrance Markers)

## Role
Defines Android entrance marker metadata for QR validation and AR alignment.

## Key Fields
- `entranceMarkers[].id`
- `displayName`
- `startNodeId`
- `physicalWidthMeters`
- `physicalHeightMeters`
- `position`
- `forwardBasis`
- `rotationYDegrees`
- `referenceImageName`

## Used By
- `AndroidReviewedPackageLoader.kt`
- `AndroidNavigationFlowModel.kt`
- `AndroidArNavigationViewModel.kt`
- `UnifiedArSession.kt`

## Notes
- `referenceImageName` must match `assets/ar/<name>.png`.
