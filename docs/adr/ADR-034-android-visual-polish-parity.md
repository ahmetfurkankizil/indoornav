# ADR-034: Android Visual Polish Parity

**Status:** Accepted
**Date:** 2026-05-03
**Deciders:** VecturAI team
**Relates to:** ADR-033 (Client-Facing Polish), ADR-027 (rolling lookahead AR guidance), ADR-012 (demo-first UX)

## Context

The iOS visitor flow received a Phase 11 polish pass with calmer microcopy, haptics, a consistent dark visual style, destination grouping, route ETA, and richer AR navigation overlays. Android already had the same core mechanics: reviewed package loading, QR validation on ARCore frames, entrance-poster alignment, rolling route arrows, next-action guidance, and arrival handling. Its Compose UI, however, still used repeated hardcoded colors, custom clickable rows for primary actions, duplicate dotted backgrounds, weak motion, and underused haptics.

## Decision

Android now uses a shared dark visitor design language in `shared/designsystem`: surface, border, text, accent, and gradient tokens; spacing and shape scales; Inter-backed Material typography; numeric and overline styles; and reusable buttons, cards, chips, badges, section headers, aurora background, gradient text, animated numbers, and tap behavior.

The Android visitor screens are migrated to those primitives rather than keeping screen-local button, card, and background implementations. The home, QR scan, entrance confirmation, destination selection, route preview, AR alignment, active navigation, arrival, and error overlays keep the existing state machines and ARCore ownership model while gaining motion, accessibility, haptics, and visual hierarchy.

Haptics remain Android-local through `AndroidHapticManager`, with a global enable gate and explicit interaction tiers for taps, selection, QR success/error, route start, imminent turns, tracking degradation, and arrival.

## Consequences

**Positive:**
- Android reaches client-demo visual parity with the iOS Phase 11 pass while using Android-native Compose primitives.
- The visitor flow now has one design-system path for CTAs, cards, chips, section labels, animated backgrounds, and number treatments.
- ARCore session ownership is unchanged: `ArCameraActivity` still owns the long-lived session and Compose overlays float above it.
- Accessibility improves through stronger text contrast, larger touch targets, roles on custom clickables, and clearer content descriptions.

**Negative:**
- The shared design system is now more opinionated toward the dark visitor flow.
- Decorative motion adds drawing work, so the aurora background is intensity-gated for reduce-motion and battery-saver contexts.

## Alternatives Considered

- **Leave Android app-local styles in place:** rejected because it preserved duplication and made parity drift likely.
- **Introduce a third-party animation/design framework:** rejected to keep the demo app small and dependency-light.
- **Change ARCore rendering/session lifecycle:** rejected because the existing Activity-owned ARCore model is stable and the polish work only needs overlay changes.

## Deferred

- Voice guidance / TTS
- Sound effects
- Full localization
- Custom 3D arrow models
- Android admin UI parity
