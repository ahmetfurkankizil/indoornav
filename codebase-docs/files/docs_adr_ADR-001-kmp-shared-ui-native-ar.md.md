# File Dossier: ADR-001-kmp-shared-ui-native-ar.md

## Metadata
- **Path**: `docs/adr/ADR-001-kmp-shared-ui-native-ar.md`
- **Type**: Markdown (Architecture Decision Record)
- **Feature**: `infrastructure`
- **Status**: Mapped

## Role
Documents the fundamental architectural decision of the VecturAI project: using Kotlin Multiplatform (KMP) for business logic, Compose Multiplatform for shared UI, and native shells for the platform-specific AR navigation experiences.

## Key Decisions
- **KMP**: Chosen for shared domain models, routing, and state management.
- **Compose Multiplatform**: Used for search, history, and preview screens.
- **Native AR**: ARCore (Android) and ARKit/RealityKit (iOS) used natively via a bridge pattern to avoid abstraction penalties in the sensitive AR layer.

## Context
- Small team (5 engineers) requiring high code reuse.
- Necessity for deep AR platform integration.
- Fast iteration for investor demos.

## Consequences
- ~80% code sharing for logic and UI.
- Single source of truth for navigation state.
- Maintenance of two separate AR implementations.
- API design complexity for the AR/Shared bridge.

## Related Features
- `infrastructure`: Defines the tech stack.
- `ar_visuals`: Context for why AR is platform-specific.

## Notes / Risks
- Matureness of Compose on iOS.
- Stability of the Bridge interface.
