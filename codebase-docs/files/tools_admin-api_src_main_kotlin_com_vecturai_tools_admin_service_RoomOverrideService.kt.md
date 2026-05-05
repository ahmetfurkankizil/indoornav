# File Dossier: RoomOverrideService.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/VecturAI/tools/admin/service/RoomOverrideService.kt`
- **Type**: Kotlin Source (Service)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Handles the persistence and validation of room metadata overrides. It ensures that corrections made by admin users are stored separately from the generated draft artifacts, preserving the "raw" output of the preprocessor while allowing for a non-destructive review workflow.

## Main Symbols
- `RoomOverrideService`: Core service class.
- `loadOverrides()`: Reads `room_overrides.json` for a specific job.
- `saveOverrides()`: Persists the override map to disk.
- `patchRoom()`: Merges a patch request into an existing override, validating the room's existence.

## Important Logic
- **Non-Destructive Design** (L10-14): Explicitly stated goal to never mutate generated draft artifacts. Overrides are stored in a sidecar file.
- **Validation** (L49-55):
    - Rejects overrides for `roomId`s that don't exist in the draft.
    - Ensures `displayName` is non-empty if provided.
- **Atomic Patching** (L57-68): Reads the current state, merges the patch using `copy()` logic, and writes the entire map back to disk.

## Uses
- `RoomOverrides`, `RoomOverride`, `RoomPatchRequest`: Internal API models.
- `kotlinx.serialization.json`: For persistence.

## Used By
- `DraftJobService`: Orchestrates higher-level job operations.
- `AsyncJobLifecycleTest.kt`, `RoomOverrideServiceTest.kt`: For testing override logic.

## Related Features
- `admin_orchestration`: Central to the manual correction phase of the admin tool.

## Notes / Risks
- **Concurrency**: Lacks explicit file locking. Multiple concurrent patches to the same job could lead to race conditions (last-write-wins).
- **Orphaned Overrides**: If the preprocessor is re-run and a room ID is removed, its override remains in `room_overrides.json` but is effectively ignored by the UI and exporter.
