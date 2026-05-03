# File Dossier: NavigationSessionCoordinatorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/NavigationSessionCoordinatorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `navigation_session_management`
- **Status**: Mapped

## Role
Tests the orchestration of a navigation session's lifecycle. It ensures that sessions are correctly started, tracked (via progress updates), ended (either manually or automatically upon arrival), and archived into the visit history.

## Public Surface
- `NavigationSessionCoordinatorTest`: Test class.

## Main Symbols
- `TestCoordinator`: Mock implementation of the navigation session orchestrator.
- `updateProgress()`: Simulates progress updates from the navigation engine.
- `endSession()` / `cancelSession()`: Trigger lifecycle state transitions.

## Important Logic
- **Lifecycle State Transitions** (L67-88): Verifies that starting a session creates an active state, and ending/cancelling it correctly updates the status and moves the session data to a "summary" state.
- **Auto-Completion Trigger** (L91-105): Confirms that reaching 95% progress automatically ends the session with the appropriate completion code based on whether the mode is "REAL_SCAN" or "SIMULATED_SCAN".
- **History Integration** (L108-124): Ensures that every completed or cancelled session is automatically recorded in the history list, maintaining a "most recent first" order.
- **State Reset** (L127-133): Validates that starting a new session clears the summary of the previous one, preventing stale UI state.

## Related Features
- `navigation_session_management`: This is the primary feature under test.
- `visit_history`: Validates the automatic recording of sessions.

## Notes / Risks
- **Redundant Logic**: As with other tests in this batch, it uses a local `TestCoordinator` mock. This ensures that the high-level lifecycle protocol is validated independently of the complex threading and Ktor logic in the production coordinator.
