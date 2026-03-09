# ADR-020: Confidence-Scored Progress and Alignment Model

**Status:** Accepted  
**Date:** 2026-03-10

## Context

v1.5 exposes a binary `isLowConfidence` flag on progress updates. Operators and debug tools need richer, actionable confidence states to diagnose live-demo issues.

## Decision

Introduce explicit confidence enums and a composite `NavigationConfidenceState`:

- **AlignmentConfidence**: HIGH / MODERATE / LOW / NONE  
- **ProgressConfidence**: RELIABLE / ESTIMATED / DEGRADED  
- **OffRouteStatus**: ON_ROUTE / MINOR_DRIFT / LOW_CONFIDENCE / LIKELY_OFF_ROUTE / RECOVERY_RECOMMENDED

These combine into a `NavigationConfidenceState` that the UI and diagnostics panel consume. Each state maps to a user-facing `RecoveryRecommendation`.

## Consequences

- Debug panel shows actionable alignment quality
- UI can display differentiated guidance (e.g., "move toward route" vs "rescan marker")
- No auto-cancellation — recommendations are passive
- Thresholds are configurable but ship with sensible defaults
