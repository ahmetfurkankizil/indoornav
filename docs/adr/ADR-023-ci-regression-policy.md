# ADR-023: CI and Regression Policy for Demo-Critical Features

**Status:** Accepted  
**Date:** 2026-03-10

## Context

With 160+ tests and increasing complexity, manual test runs are error-prone and slow. Demo-critical regressions (broken routing, progress estimation, marker alignment) are the highest-risk failures.

## Decision

1. **GitHub Actions CI** on push/PR to `main`:
   - JDK 21 setup
   - Preprocessor tests (160+ tests covering routing, progress, correction, validation)
   - Android debug build verification
   - Demo package integrity check
2. **iOS CI**: Local verification script (macOS runner costs not justified at current team size)
3. **Regression matrix**: 10 demo-critical features documented with coverage type (automated/manual)
4. **Integration-style regression test** covering full demo flow: load → search → route → arrows → progress → arrival → history

## Consequences

- Regressions caught before merge
- Android build breakage detected automatically
- iOS verified locally before releases
- Demo-critical paths have explicit, documented coverage
