# File Dossier: PackageExporter.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/VecturAI/tools/preprocessor/PackageExporter.kt`
- **Type**: Kotlin Source (Export Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
The final output stage of the preprocessing tool. It transforms the human-readable `AuthoringConfig` into the optimized JSON formats required by the mobile application's runtime.

## Public Surface
- `export(...): ExportResult`: Generates the production bundle in the target directory.

## Important Logic
- **Data Transformation** (L44-142): Maps the `AuthoringConfig` models to optimized `Package*` DTOs (Data Transfer Objects). This includes generating QR payloads for markers and calculating centroid coordinates for rooms.
- **Asset Copying** (L144-148): Copies the source GLB file into the package as `preview.glb`, ensuring the 3D model is bundled with its navigation data.
- **Manifest Generation** (L151-163): Creates a `manifest.json` that acts as the entry point for the mobile app, listing all included files and their roles.

## Main Symbols
- `PackageManifest`, `PackageNavGraph`, `PackageRooms`, `PackageMarkers`, `PackageRouteRendering`: The exact serializable DTOs that define the binary contract between the preprocessor and the mobile clients.

## Used By
- `Pipeline.kt`: Step 4 of the production flow.

## Notes / Risks
- **Schema Versions**: Uses hardcoded schema versions (v1). Any breaking changes to the mobile app's loader must be reflected here.
- **Payload Generation**: Generates `VecturAI://` deep links for markers, which are used by the mobile app's QR scanner.
