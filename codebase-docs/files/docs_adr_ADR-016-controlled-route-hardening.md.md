# File Dossier: ADR-016-controlled-route-hardening.md

## Path
`docs\adr\ADR-016-controlled-route-hardening.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-016: Real-World Controlled-Route Hardening Strategy

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

V1.5 targets believable progress on a **controlled single-floor demo route** (< 50m, well-lit, clear path). We do not chase full indoor localization. Instead:

1. Marker-based start alignment (unchanged)
2. Route-relative progress from VIO camera movement (new)
3. Recenter/rescan if drift becomes apparent
4. Monotonic progress guard to mask VIO jitter

## Assumptions
- Route is short (< 50m total)
- VIO drift ≤ 1–2% over route length
- User walks at normal pace on the expected path
- Single marker, single floor

## Consequences
- Progress is believable on controlled routes
- No claim of general indoor localization
- Drift may cause ~1m error at route end — acceptable for demo

```

## Status
Mapped (Pass 3 Normalization)
