# File Dossier: authoring_config.generated.json

## Metadata
- **Path**: `apps/iosApp/iosApp/authoring_config.generated.json`
- **Type**: JSON Configuration
- **Feature**: `admin-tools`, `navigation-data`
- **Status**: Mapped

## Purpose
The raw, auto-generated output from the backend navigation preprocessor. It contains the full spatial graph (nodes and edges) and candidate room locations before any human review or refinement.

## Key Structures
- **entranceMarkers**: List of detected/suggested entry points. Initially contains placeholders that require manual position verification.
- **nodes**: A dense list of spatial coordinates (x, y, z) representing the navigable space, categorized by type (junction, room_entry).
- **edges**: The connectivity graph, defining which nodes are linked and the travel cost (distance) between them.
- **asset**: Metadata about the source GLB file and the tool used for the scan (e.g., Polycam).

## Usage in App
This file is used by the `AdminDraftReviewView` to populate the draft inspection UI. It serves as the base layer that admins "patch" with human-friendly labels and metadata before exporting the final production package.

## Technical Notes
- **Coordinates**: Uses a right-handed coordinate system consistent with ARKit/RealityKit.
- **Generation**: This file is typically updated by the `AdminAPIClient` when a new GLB scan is processed by the backend tools.
