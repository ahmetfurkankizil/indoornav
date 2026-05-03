# ADR-025: iOS Truth-Path Navigation Flow with Explicit Destination Gating

**Status:** Accepted
**Date:** 2026-03-22
**Deciders:** Vectura AI iOS team
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

1. **AR cannot start without an explicit user-selected destination.** There is no default fallback.
2. **The flow is driven by a single state machine** (`NavigationFlowModel`) that is the source of truth for screen presentation.
3. **QR scanning is the entry point to navigation**, not a button that directly opens AR.
4. **Entrance confirmation** is shown as a quick bottom sheet after scan.
5. **Destination selection** provides a searchable list of rooms loaded from building config.
6. **Route preview** shows start, destination, and distance before AR begins.
7. **BuildingPackageLoader** is split: `loadConfig()` for data loading, `computeRoute()` for routing to a specific destination. No auto-selection.

### State Machine

| State | Description | Next |
|---|---|---|
| `home` | Dashboard with "Scan QR Code" button | `qrScan` |
| `qrScan` | Camera/simulated QR scanner | `entranceConfirmed` |
| `entranceConfirmed` | Bottom sheet: "Starting from Entrance A" | `destinationSelect` |
| `destinationSelect` | Search + room list | `routePreview` |
| `routePreview` | Start/destination/distance + "Start Navigation" | `arNavigation` |
| `arNavigation` | Full AR experience | `home` (on end) |

## Consequences

- No user can accidentally enter AR without a destination.
- The simulated QR scan path is clearly isolated as a temporary provider.
- Future phases can replace the simulated scanner with real AVCaptureSession QR detection without restructuring the flow.
- The flow model is testable independently of UI.

## Phase 1 Scope

- QR scanning is simulated (demo mode).
- Building data loaded from bundled draft JSON.
- Route preview is minimal (text-based, no map).
- AR internals unchanged except for removing fallback logic.
