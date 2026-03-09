# Demo Risks and Fallbacks

Known risks during live demonstrations and how to handle them.

## Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Marker won't scan | Medium | High | Adjust lighting/angle; use Simulate Scan |
| AR arrows misaligned | Medium | Medium | Rescan marker; checkpoint correction helps |
| Progress stalls or jumps | Low | Medium | Walk toward route corridor; rescan |
| App crash | Low | High | Relaunch app; pre-rehearse flow |
| Battery dies | Low | High | Keep charger handy; start at 80%+ |
| Device overheats | Low | Medium | Rest device; switch to demo mode |
| Wrong room selected | Low | Low | Cancel and re-select |
| Checkpoint not detected | Medium | Low | Single-marker flow still works fine |

## Fallback Ladder

If something goes wrong during a demo, escalate through these levels:

### Level 1: Quick Recovery (5 seconds)
- Adjust phone angle or distance from marker
- Tap "Rescan" button
- Walk back toward the route corridor

### Level 2: Mode Switch (15 seconds)
- Switch to Simulate Scan (bypass real marker)
- Use "Advance" button to control progress manually

### Level 3: Demo Mode (30 seconds)
- Cancel live navigation
- Start in Demo Mode (fully scripted)
- Reliable, deterministic path

### Level 4: Video Backup (immediate)
- Play pre-recorded screen recording of successful demo
- "Let me show you what this looks like in practice"

## What to Say When Things Go Wrong

| Situation | Say This |
|-----------|----------|
| Marker won't scan | "The lighting here isn't ideal — let me adjust" |
| Arrows look wrong | "I'll recalibrate — this is normal after walking a while" |
| App crashes | "Let me restart — the prototype is under active development" |
| Progress backward | "The tracking noticed some drift — it'll correct itself" |
| Nothing works | "Let me show you our recorded demo — same experience, just pre-captured" |

## Pre-Demo Checklist

- [ ] Rehearsed the full flow at least once today
- [ ] Verified marker prints are clean and properly sized
- [ ] Device battery > 80%
- [ ] Screen brightness at max
- [ ] Auto-lock disabled
- [ ] Video backup ready (screen recording of successful run)
- [ ] Backup device charged and ready (if available)
