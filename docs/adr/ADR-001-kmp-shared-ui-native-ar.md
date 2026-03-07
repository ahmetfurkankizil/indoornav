# ADR-001: KMP + Shared Compose UI + Native AR Shells

**Status:** Accepted  
**Date:** 2026-03-08

## Context

We need a cross-platform mobile app targeting both Android and iOS. The app combines standard UI screens (search, preview, history, settings) with a live AR camera navigation experience.

Key constraints:
- Small team (5 engineers) — code sharing is critical
- AR features require deep platform integration (ARCore on Android, ARKit/RealityKit on iOS)
- Non-AR screens are standard UI that can be fully shared
- We need fast iteration for investor demos

## Decision

1. **Kotlin Multiplatform (KMP)** for all shared business logic (domain models, routing, repositories, state management)
2. **Compose Multiplatform** for all non-AR UI screens (Home, Search, Route Preview, History, Settings)
3. **Native AR shells** — platform-specific code for the live camera AR navigation experience:
   - Android: ARCore-based Activity
   - iOS: ARKit/RealityKit-based SwiftUI View
4. **Bridge pattern** — native AR shells observe the shared `NavigationState` via `StateFlow` and report events back

## Consequences

### Positive
- ~80% code sharing across platforms for business logic and UI
- Single source of truth for navigation state, observed by both Compose and native AR
- Each platform's AR SDK used natively (no abstraction penalty)
- Team can parallelize: Android AR specialist + iOS AR specialist + shared logic devs

### Negative
- Two separate AR implementations to maintain
- Compose Multiplatform for iOS is still maturing (acceptable for non-AR screens)
- Bridging between KMP shared state and native AR requires careful API design

### Risks
- Compose Multiplatform iOS performance may need optimization for smooth scrolling lists
- AR bridge interface must be stable to avoid breaking both platforms simultaneously
