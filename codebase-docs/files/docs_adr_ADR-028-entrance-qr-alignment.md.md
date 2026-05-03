# File Dossier: ADR-028-entrance-qr-alignment.md

## Path
`docs\adr\ADR-028-entrance-qr-alignment.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-028: Entrance QR Establishes Initial iOS Alignment and Facing Before AR Guidance Begins

**Status:** Accepted
**Date:** 2026-03-23
**Deciders:** Vectura AI iOS team
**Relates to:** ADR-026, ADR-027, ADR-025

## Context

Phase 3 completed rolling-lookahead AR guidance, but the QR scan was still simulated and AR alignment was not grounded in any real marker detection. This meant:

1. The QR "scan" was a button press — no real payload, no validation.
2. AR alignment was invented at session start (offset = 0, rotation = 0), ignoring the entrance marker position in the reviewed package.
3. Navigation arrows appeared immediately on AR startup, before any real-world anchor was established.
4. There was no contract between QR payload, reviewed package entrance metadata, and AR world alignment.

## Decision

### QR payload is the entry gate

The QR code encodes a small JSON payload:

```json
{
  "type": "Vectura AI-entrance",
  "buildingId": "house-demo-01",
  "entranceId": "marker-entrance-a
```

## Status
Mapped (Pass 3 Normalization)
