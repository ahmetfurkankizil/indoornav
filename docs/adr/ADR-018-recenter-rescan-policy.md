# ADR-018: Recenter/Rescan Policy

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

Users can manually trigger a "Rescan Marker" action at any time during AR navigation. If the entrance marker is re-observed (automatically or by user action), a new alignment is computed and the route is re-rendered. Progress is preserved at the higher of old and new estimates.

## States
- **Aligned** — normal navigation
- **Tracking Limited** — show warning, suggest rescan
- **Rescanning** — user-initiated, looking for marker
- **Re-aligned** — new alignment applied, route re-rendered

## Consequences
- User stays in control; no aggressive auto-cancellation
- Re-alignment corrects drift
- Progress never regresses from rescan
