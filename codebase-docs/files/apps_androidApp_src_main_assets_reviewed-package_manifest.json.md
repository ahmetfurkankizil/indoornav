# File Dossier: manifest.json

## Path
`apps/androidApp/src/main/assets/reviewed-package/manifest.json`

## Type
Bundled Runtime Data (Reviewed Package Manifest)

## Role
Android copy of the reviewed package manifest. Declares package/building identity and the four package file names consumed by `AndroidReviewedPackageLoader`.

## Key Fields
- `packageVersion`
- `buildingId`
- `buildingName`
- `floorId`
- `reviewStatus`
- `files.rooms`
- `files.navGraph`
- `files.entranceMarkers`
- `files.routeRendering`

## Used By
- `AndroidReviewedPackageLoader.kt`
- `QRPayload.kt` validation through loaded config

## Notes
- Must stay in sync with the iOS reviewed package for parity demos.
