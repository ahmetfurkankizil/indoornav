# File Dossier: AdminDraftJobsView.swift

## Metadata
- **Path**: `apps/iosApp/iosApp/admin/AdminDraftJobsView.swift`
- **Type**: Swift Source (SwiftUI View)
- **Feature**: `admin-tools`, `ios-app`
- **Status**: Mapped

## Purpose
Provides the primary administrative interface for uploading raw 3D scans (.glb files) to the backend and monitoring the status of draft generation jobs.

## Key Components
- **AdminDraftJobsView**: The main UI container using a standard SwiftUI `List` with sections for "Upload GLB" and "Draft Jobs".
- **AdminDraftJobsViewModel**: Manages the state for file selection, upload progress, and the list of jobs.
- **AdminJobRow**: A subview that displays a single job's status, filename, and creation date with appropriate iconography.

## Logic Flow
1. **Selection**: User triggers the `fileImporter` to select a GLB file.
2. **Upload**: The viewmodel uses `AdminAPIClient` to send the file to the local backend.
3. **Polling**: Once a job is created, the viewmodel enters a 3-second polling loop to update statuses (`queued` -> `processing` -> `succeeded`/`failed`).
4. **Navigation**: Tapping a successful job navigates to `AdminJobDetailView`.

## Dependencies
- `AdminAPIClient.swift`: For network interactions.
- `AdminJobDetailView.swift`: Navigation destination.
- `UniformTypeIdentifiers`: To restrict file picker to `.glb` files.

## Technical Notes
- **Security Scoping**: Uses `startAccessingSecurityScopedResource()` to read files selected from outside the app sandbox (e.g., from Files app).
- **Environment**: Explicitly designed for local development; requires the backend to be reachable via the network configuration in `AdminAPIClient`.
