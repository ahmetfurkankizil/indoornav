# File Dossier: ADR-025-ios-truth-path-navigation-flow.md

## Path
`docs\adr\ADR-025-ios-truth-path-navigation-flow.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-025: iOS Truth-Path Navigation Flow with Explicit Destination Gating

**Status:** Accepted
**Date:** 2026-03-22
**Deciders:** VecturAI iOS team
**Relates to:** ADR-001, ADR-008, ADR-012

## Context

The previous iOS flow allowed AR navigation to launch directly from the home screen with a single button tap. The destination was auto-selected (first room in config or a hardcoded "Conference Room" fallback). This created several product-truth problems:

1. Users could enter AR without choosing where to go.
2. A default destination was silently chosen, which is incorrect product behavior.
3. The flow had no QR scan step, no entrance confirmation, and no route preview.
4. The code was coupled to "first room in config" as runtime truth.

## Decision

Introduce a mandatory sequential flow for iOS navigation:

```
home → qrScan → entranceConfirmed → destinationSelect → routePreview → arNavigation
```

### Rules

1. **AR cannot start without an explicit user-selected destination.** There 
```

## Status
Mapped (Pass 3 Normalization)
