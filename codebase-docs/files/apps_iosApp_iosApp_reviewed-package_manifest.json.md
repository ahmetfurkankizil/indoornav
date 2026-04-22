# File Dossier: manifest.json (reviewed-package)

## Metadata
- **Path**: `apps/iosApp/iosApp/reviewed-package/manifest.json`
- **Type**: JSON Configuration
- **Feature**: `navigation-data`
- **Status**: Mapped

## Purpose
The root index for a production-ready building package. It links related data files and provides top-level building identity and versioning information.

## Key Fields
- **buildingId / buildingName**: Official identifiers for the building.
- **packageVersion**: Schema version for the navigation data.
- **reviewStatus**: Must be `reviewed` for production use.
- **files**: A dictionary mapping logical data roles (rooms, navGraph, entranceMarkers) to their respective physical filenames in the bundle.

## Usage in App
The `BuildingPackageLoader` first reads this file to determine which auxiliary files to load. It ensures that the app is working with a consistent, validated set of navigation data.

## Technical Notes
- **Immutability**: Once a package is exported as "reviewed" from the admin tool, this manifest acts as the source of truth for the app bundle.
- **Platform Parity**: The structure of this file is mirrored in the Android implementation to ensure cross-platform consistency for shared building data.
