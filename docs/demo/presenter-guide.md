# Presenter Guide

The **presenter** is the non-technical person who demonstrates the product to stakeholders. The **operator** handles setup and troubleshooting behind the scenes.

## Key Messages

1. **"We navigate you inside buildings using AR — no beacons, no infrastructure"**
2. **"Point at a marker, and arrows appear in the real world"**
3. **"It works on standard phones — no special hardware"**

## Demo Flow

### 1. Show the Building (30 seconds)
- "This is a 3D scan of our demo building"
- "We author navigation routes on this scan — rooms, waypoints, markers"
- Tap a building to show the room list

### 2. Search for a Room (15 seconds)
- "Let's navigate to the Kitchen"
- Type "Kitchen" or tap from the list
- "The system computes the shortest path"

### 3. Start Navigation (30 seconds)
- "Now we scan this marker to connect AR to the real building"
- Point phone at the entrance marker
- *(Or tap "Simulate Scan" if using demo mode)*
- "The arrows appear — showing the way in augmented reality"

### 4. Walk the Route (1–2 minutes)
- "As we walk, the arrows guide us around corners"
- "The progress bar shows how far we are"
- "The system estimates our position from phone movement"

### 5. Arrive (15 seconds)
- "And we've arrived at the Kitchen!"
- "The visit is saved in our history"

## Handling Problems

| What Happens | What to Say |
|-------------|-------------|
| Marker won't scan | "Let me adjust the angle — these work best with good lighting" |
| Arrows seem off | "I'll realign — this is expected on longer routes" (tap Rescan) |
| Progress stops | "Let me walk a bit more toward the corridor" |
| App seems frozen | "Let me restart — this prototype is still in development" |
| Checkpoint help | "Notice the arrows just corrected — we have markers along the route for precision" |

## Do's and Don'ts

✅ **Do:**
- Rehearse the flow at least once before presenting
- Keep the phone steady and at chest height
- Walk at a normal pace along the marked route
- Acknowledge this is a controlled demo environment

❌ **Don't:**
- Don't show debug overlays to non-technical audience
- Don't explain "VIO drift" or "polyline projection" — say "tracking"
- Don't rush — let the AR arrows be visible for a few seconds
- Don't walk off the route intentionally unless demonstrating recovery
