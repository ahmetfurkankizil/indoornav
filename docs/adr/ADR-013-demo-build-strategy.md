# ADR-013: Demo-Build and Release-Readiness Strategy

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

V1 uses three build profiles: **Dev**, **Demo**, **Release**. A root `Makefile` provides named targets for every build/test action. Demo builds preload the sample building package and enable simulate-scan by default.

## Consequences
- One-command builds for any team member
- Demo builds are deterministic and presentation-safe
- Release builds disable debug overlays
