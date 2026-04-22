# Feature Documentation: Admin Tools (iOS)

## Overview
The Admin Tools on iOS provide a developer-only suite for managing the 3D scanning and navigation authoring pipeline. These tools bridge the gap between physical space capture (using Polycam) and production-ready navigation packages.

## Architecture
The Admin Tools are isolated from the visitor navigation flow and are accessed via a hidden or developer-gated entry point. They communicate exclusively with a local Admin API backend.

- **AdminDraftJobsView**: The hub for uploading .glb scans and monitoring draft generation.
- **AdminJobDetailView**: Detailed status and artifact listing for a specific draft job.
- **AdminDraftReviewView**: The primary inspection and refinement tool for auto-generated drafts.
- **AdminAPIClient**: The networking layer for the Admin API.

## Key Workflows

### 1. Scan Upload & Job Creation
1. The admin selects a `.glb` file exported from Polycam using the native iOS file picker.
2. `AdminAPIClient` uploads the file to the `/drafts/upload` endpoint.
3. A "Draft Job" is created on the backend, and the app begins polling for status updates.

### 2. Draft Review & Refinement
1. Once a job succeeds, the admin reviews the auto-generated 2D maps (Occupancy and Graph) rendered as SVGs.
2. The admin inspects the "Room Candidates" list.
3. For each room, the admin can provide a production-ready display name, category, and description via the `RoomEditSheet`.
4. These refinements are sent to the backend as overrides.

### 3. Production Export
1. After all room labels and categories are verified, the admin triggers "Export Reviewed Package".
2. The backend generates the finalized production dataset (`manifest.json`, `nav_graph.json`, etc.).
3. These files are then incorporated into the app's `reviewed-package` directory for the next release.

## Implementation Details
- **Networking**: Uses standard `URLSession` with `multipart/form-data` for uploads.
- **SVG Rendering**: Offloaded to `WKWebView` to simplify the mobile client's rendering requirements.
- **Local IP Configuration**: Currently requires manual IP configuration in `AdminAPIClient.swift` to connect to a development Mac on the same network.

## Related Files
- [AdminAPIClient.swift](file:///c:/Users/emirh/Desktop/bitirme/vecturai/codebase-docs/files/apps_iosApp_iosApp_admin_AdminAPIClient.swift.md)
- [AdminDraftJobsView.swift](file:///c:/Users/emirh/Desktop/bitirme/vecturai/codebase-docs/files/apps_iosApp_iosApp_admin_AdminDraftJobsView.swift.md)
- [AdminDraftReviewView.swift](file:///c:/Users/emirh/Desktop/bitirme/vecturai/codebase-docs/files/apps_iosApp_iosApp_admin_AdminDraftReviewView.swift.md)
- [AdminJobDetailView.swift](file:///c:/Users/emirh/Desktop/bitirme/vecturai/codebase-docs/files/apps_iosApp_iosApp_admin_AdminJobDetailView.swift.md)
