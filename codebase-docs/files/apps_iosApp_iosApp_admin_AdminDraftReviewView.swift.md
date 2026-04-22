# File Dossier: AdminDraftReviewView.swift

## Metadata
- **Path**: `apps/iosApp/iosApp/admin/AdminDraftReviewView.swift`
- **Type**: Swift Source (SwiftUI View)
- **Feature**: `admin-tools`, `ios-app`
- **Status**: Mapped

## Purpose
Enables detailed inspection of a processed draft. Admins can view 2D maps, check graph connectivity, edit room metadata, and ultimately export the "reviewed" package for production use.

## Key Components
- **AdminDraftReviewView**: Multi-section list showing building details, counts, geometry stats, and room candidates.
- **AdminDraftReviewViewModel**: Handles loading the draft summary, patching room details, and triggering the final export.
- **SVGPreviewCell**: Uses a `WKWebView` to render the auto-generated 2D occupancy and graph maps.
- **RoomEditSheet**: A form for overriding the auto-generated room names, categories, and descriptions.

## Logic Flow
1. **Load**: View fetches the `DraftSummary` from the backend.
2. **Inspect**: User reviews the `occupancy_debug.svg` and `draft_graph_debug.svg` artifacts.
3. **Refine**: User taps a room to open the edit sheet and provides production-ready labels.
4. **Export**: User triggers "Export Reviewed Package", which prompts the backend to generate the final 5-file production set.

## Dependencies
- `AdminAPIClient.swift`: For fetching summaries and patching rooms.
- `WebKit`: For rendering SVG artifacts.

## Technical Notes
- **Preview System**: SVG rendering is offloaded to `WKWebView` to avoid heavy rasterization or third-party dependencies in the admin tool.
- **Persistence**: Edits to rooms are sent immediately to the backend as "overrides" via the `patchRoom` endpoint.
