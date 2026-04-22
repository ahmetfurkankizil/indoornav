# File Dossier: ADR-004-entrance-marker-init.md

## Path
`docs\adr\ADR-004-entrance-marker-init.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-004: Entrance-Marker-Based Initialization

**Status:** Accepted  
**Date:** 2026-03-08

## Context

AR navigation requires knowing the user's position in the building's coordinate system. This is the "localization problem" — mapping from the AR device's world coordinates to the navigation graph coordinates.

Options considered:
1. **Compass + GPS** — unreliable indoors
2. **Cloud Anchors** — requires Google/Apple infrastructure, internet dependency, privacy concerns
3. **Visual place recognition** — ML-based, requires training data per building
4. **Entrance marker scanning** — controlled physical marker at known position

## Decision

Use **entrance marker scanning** as the sole localization method for MVP.

Each entrance has a physical marker that combines:
- A **QR code** containing the building ID, marker ID, and reference data
- A **visual reference image** that ARKit/ARCore can detect and track for 6-DoF pose estimation

When the user scans the marker:
1. QR data identifies
```

## Status
Mapped (Pass 3 Normalization)
