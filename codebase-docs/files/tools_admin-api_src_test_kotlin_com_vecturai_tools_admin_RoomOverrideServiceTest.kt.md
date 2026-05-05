# File Dossier: RoomOverrideServiceTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/VecturAI/tools/admin/RoomOverrideServiceTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Unit tests for the `RoomOverrideService`. It focuses on the logic of applying partial updates (patches) to room metadata, ensuring data persistence to `room_overrides.json`, and validating room identity and input constraints.

## Main Symbols
- `RoomOverrideServiceTest`: Test suite class.
- `patches displayName and persists`: Checks basic field update and disk serialization.
- `partial patch preserves existing fields`: Verifies the non-destructive merging logic of the service.
- `rejects unknown room id`: Validates that overrides can only be applied to rooms present in the draft.
- `multiple rooms patched independently`: Ensures no cross-talk between corrections for different rooms.

## Important Logic
- **Field Merging Logic** (L53-61, L78-85): Exhaustively tests how the service handles `null` vs `non-null` fields in patch requests, confirming that `null` in a request means "no change" while specific values update the store.
- **Persistence Verification** (L40-42): Reloads overrides from disk after a patch to ensure the JSON roundtrip is correct.
- **Validation Guardrails** (L64-75): Confirms that the service enforces business rules (non-empty names, valid IDs) before writing to the filesystem.

## Uses
- `RoomOverrideService`: The service under test.
- `RoomPatchRequest`: Input model for tests.

## Used By
- CI/CD: Automated unit testing of the override management layer.

## Related Features
- `admin_orchestration`: Ensures the integrity of the manual correction data store.

## Notes / Risks
- **IO Dependency**: Like other admin tests, this relies on real filesystem operations, which can be affected by environment-specific disk behavior.
