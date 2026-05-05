# Presentation Cheatsheet

## 30-Second Demo

> "VecturAI gives you turn-by-turn AR navigation inside buildings — no beacons needed."

1. Show the app with a building loaded
2. Tap "Kitchen" → arrows appear
3. "You point your phone at a marker, and AR arrows guide you to your destination."

## 2-Minute Demo

1. **"This is a 3D scan of our demo building."** (Show building preview)
2. **"We author navigation routes on the scan."** (Show room list)
3. **"Search for Kitchen."** (Type and select)
4. **"Scan the entrance marker."** (Point camera or simulate)
5. **"AR arrows appear in the real world."** (Show arrows + walk)
6. **"Progress tracks your movement."** (Show progress bar)
7. **"You've arrived!"** (Show arrival overlay)
8. **"Visit saved to history."** (Show history)

## 5-Minute Demo

Everything in 2-minute, plus:

9. **Correction markers** — "We placed correction markers along the route. Watch the arrows adjust as we pass them."
10. **Recovery** — "If tracking degrades, the system suggests rescanning."
11. **Architecture** — "All navigation logic runs on-device. No server needed."
12. **Authoring** — "Routes are authored on a 3D scan — just like editing a map."
13. **Cross-platform** — "Same core logic on Android and iOS via Kotlin Multiplatform."

## Key Numbers

| Metric | Value |
|--------|-------|
| Route accuracy | ~1–2m on routes < 50m |
| Marker detection | 2–5 seconds |
| Correction bounds | Max 2m / 15° per checkpoint |
| Arrival threshold | 1.5m or 95% progress |
| Test coverage | 170+ automated tests |
| VIO drift | 1–2% of distance walked |
