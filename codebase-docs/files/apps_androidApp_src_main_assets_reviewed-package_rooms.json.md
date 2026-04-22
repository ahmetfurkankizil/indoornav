# File Dossier: rooms.json

## Path
`apps/androidApp/src/main/assets/reviewed-package/rooms.json`

## Type
Bundled Runtime Data (Reviewed Package Rooms)

## Role
Defines Android destination rooms, display names, categories, descriptions, and destination graph node ids.

## Key Fields
- `rooms[].id`
- `rooms[].displayName`
- `rooms[].destinationNodeId`
- `rooms[].category`
- `rooms[].description`

## Used By
- `AndroidReviewedPackageLoader.kt`
- `AndroidNavigationFlowModel.kt`
- `AndroidNavigationApp.kt`

## Notes
- Powers destination grouping/search and route selection.
