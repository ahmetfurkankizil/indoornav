# ADR-024: Presentation and Operator Handoff Strategy

**Status:** Accepted  
**Date:** 2026-03-10

## Context

VecturAI demos require coordinated effort: device setup, marker placement, app configuration, and live presentation. Without clear role definitions and documentation, demos are fragile and dependent on the original developer.

## Decision

Define two handoff roles:

1. **Operator** (technical): Handles device setup, builds, marker placement, package generation, and troubleshooting. Uses debug overlays and diagnostics. Has fallback actions when things go wrong.

2. **Presenter** (non-technical): Focuses on the demo narrative, user interaction, and value communication. Sees clean UI without engineering jargon. Knows what to say when things go wrong.

Supporting materials:
- Operator guide with setup checklists and fallback procedures
- Presenter guide with talking points and recovery scripts
- Presentation cheatsheet (30s / 2min / 5min demo scripts)
- Risks and fallbacks document for live demos

## Consequences

- Demos no longer require the developer to be present
- Presenters can handle common failure scenarios gracefully
- Knowledge transfer is documented, not tribal
