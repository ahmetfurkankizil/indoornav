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
- [ ] "Follow the arrows" instruction shown
- [ ] Progress bar visible and at 0%

## Arrival Flow
- [ ] "Advance" button visible after alignment
- [ ] Tapping "Advance" increases progress bar
- [ ] At ~80% progress, instruction changes to "Approaching..."
- [ ] At ~95% progress, arrival overlay appears
- [ ] Checkmark and "You've arrived!" shown
- [ ] Destination name shown in arrival overlay
- [ ] "Done" button works

## History
- [ ] History screen shows the just-completed visit
- [ ] Visit card shows destination name, status, timestamp
- [ ] Demo/simulated sessions labeled accordingly
- [ ] Multiple visits accumulate correctly

## Known Limitations (v1)
- [ ] Arrival uses progress estimation, not camera position
- [ ] History is JSON-file-backed (no cloud sync)
- [ ] 3D arrows are placeholder geometry (boxes/spheres)
- [ ] Real marker detection requires physical printed marker
- [ ] VIO drift may cause arrow misalignment on long routes
