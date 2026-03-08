# ADR-011: Local-First Visit History

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

V1 persists visit history locally using an in-memory list with JSON serialization. No backend sync. History is a first-class user-facing feature for demo quality, not just debug logging.

## Consequences

- Instant read/write, no network dependency
- Data lost on app reinstall (acceptable for MVP)
- Future: add SqlDelight persistence + backend sync
