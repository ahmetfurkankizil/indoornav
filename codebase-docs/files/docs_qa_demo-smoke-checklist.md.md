# File Dossier: demo-smoke-checklist.md

## Path
`docs\qa\demo-smoke-checklist.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Demo Smoke Test Checklist

Manual QA checklist for verifying the demo flow before presentation.

## Pre-flight
- [ ] App launches without crash
- [ ] Version string visible (Settings / Debug panel)
- [ ] Demo building package loaded (check diagnostics: "VecturAI Demo Office")

## Search Flow
- [ ] Search screen accessible from home
- [ ] Typing "con" shows "Conference Room" result
- [ ] Typing "kit" shows "Kitchen" result
- [ ] Selecting a room navigates to route preview

## Route Preview
- [ ] Destination name displayed correctly
- [ ] Route distance shown (should be ~10-13m for Conference Room)
- [ ] Step-by-step directions listed
- [ ] "Start AR" button visible and tappable

## AR Navigation
- [ ] AR screen opens
- [ ] "Point camera at entrance marker" instruction shown
- [ ] "Simulate Scan" button visible
- [ ] Tapping "Simulate Scan" triggers alignment
- [ ] "DEMO MODE" badge appears after simulate
- [ ] Route arrows rendered (colored boxes visible)
- [ ] "Follow the arrows" ins
```

## Status
Mapped (Pass 3 Normalization)
