# ADR-033: Client-Facing Polish — AR Overlay, Haptics, Microcopy

**Status:** Accepted
**Date:** 2026-04-13
**Deciders:** VecturAI team
**Relates to:** ADR-025 (iOS truth-path navigation flow), ADR-027 (rolling lookahead AR guidance), ADR-012 (demo-first UX)

## Context

The visitor-facing flow works end-to-end (Phases 1–6), but the screens use technical wording ("Detecting entrance poster...", "Follow the arrows"), the AR overlay shows basic progress without proactive turn guidance, and there is no haptic feedback at navigation milestones. Before client demos, the product needs to feel more premium and trustworthy without changing any runtime mechanics.

## Decision

### 1. Next-action guidance derived from existing arrow data

The `ArrowPlacementData` array in `LoadedPackage.arrows` already contains the arrow `type` (`.turnLeft`, `.turnRight`, `.destination`, `.follow`) and `cumulativeDistance`. The ViewModel scans ahead of `userCumulativeDistance` to find the next non-follow arrow and surfaces it as a guidance card: "Continue straight", "Turn left ahead", "Turn left now" (within 3 m), "You're almost there" (near destination).

**Why:** No new data sources, no AR engine changes, no route recalculation. The information already exists in the computed route — this phase simply displays it to the user.

### 2. Haptics via UIKit feedback generators, gated by a shared singleton

Four haptic events are added: route start (medium impact on alignment lock), imminent turn (warning notification when a turn arrow is within 2 m), re-centering (light impact when tracking degrades to relocalizing), and arrival (success notification). All gated by `HapticManager.isEnabled`.

**Why:** Standard iOS feedback generators require no audio files, no permissions, and no new dependencies. The singleton pattern keeps haptic calls one-liners and makes future settings integration trivial.

### 3. Microcopy replaces technical labels with calmer product wording

All user-facing strings are updated in-place across the visitor flow. Examples: "Detecting entrance poster..." → "Scanning...", "Follow the arrows" → "Follow the path", "Navigation Data Unavailable" → "Unable to load navigation data". No localization framework is introduced (deferred).

**Why:** The current wording reflects the engineering view ("poster", "arrows", "pipeline"). Client-facing copy should describe the user's task, not the system's internals.

### 4. Lightweight theme namespace, not a design system

`VecturTheme` centralizes gradient, card background, and button styles as static properties and view modifiers. It does not impose a protocol, require migration of all views, or introduce a dependency.

**Why:** The existing inline styles are inconsistent across views (varying corner radii, padding, button shapes). A minimal namespace makes the polish pass consistent without a full design system migration.

### 5. Tracking confidence badge replaces raw quality strings

The top bar's raw ARKit tracking quality strings ("Move slower", "More visual detail needed", "Relocalizing") are replaced with user-friendly labels ("Hold steady", "Look around slowly", "Re-centering...") shown in a compact badge alongside the next-action card.

**Why:** Raw ARKit labels are developer-facing. Users need actionable, calming guidance — not technical diagnostics.

## What Is NOT Changed

- AR rendering engine (ARRouteRenderer, arrow placement, rolling lookahead, fade-behind)
- Package loading (BuildingPackageLoader, reviewed package format)
- State machine transitions (NavigationFlowModel flow states)
- Admin pipeline (tools/admin-api, draft jobs, room editing, export)
- Data models (PackageRoom, PackageNode, PackageEdge, etc.)
- Route computation (Dijkstra, edge costs, graph structure)
- Android codebase

## Consequences

**Positive:**
- Demo-ready visitor experience with proactive turn guidance
- Haptic feedback adds sensory confidence without audio
- Consistent button/card styling via shared theme
- Microcopy is calmer and less technical

**Negative:**
- Next-action scan runs on every pose sample (0.5 s interval), but the arrow array is small (~20 items for the demo house) so performance impact is negligible
- Microcopy is English-only; localization is deferred

**Deferred:**
- Localization / i18n framework
- Full design system with dark mode support
- Voice guidance / TTS
- Sound effects
- Custom 3D arrow models in AR
