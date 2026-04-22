# File Dossier: manifest.schema.json

## Metadata
- **Path**: `docs/contracts/manifest.schema.json`
- **Type**: JSON Schema
- **Feature**: `navigation_data_format`
- **Status**: Mapped

## Role
Defines the structure and validation rules for the building manifest file. This manifest is the entry point for mobile applications to discover and load the navigation package for a specific building.

## Key Properties
- `buildingId`: Unique identifier for the building.
- `version`: Monotonically increasing version for cache invalidation.
- `files`: Map of checksums for `nav_graph`, `rooms`, and `entrance_markers`.
- `preprocessorVersion`: Identifies the tool version that generated the package.

## Constraints
- Required fields: `buildingId`, `name`, `version`, `floor`, `files`.
- Minimum version: 1.
- Mandatory files: `nav_graph`, `rooms`, `entrance_markers`.

## Uses
- `PackageExporter.kt`: Generates manifest files according to this schema.
- `validate-reviewed-package.sh`: Uses this schema for automated package validation.
- Mobile Apps: Use this schema (implied) to parse building metadata.

## Related Features
- `navigation_data_format`: This is a core component of the format definition.

## Notes / Risks
- **Version Compatibility**: Changing the schema requires synchronized updates across the preprocessor, the admin export tool, and both mobile applications.
