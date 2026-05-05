# File Dossier: qr-payload.md

## Path
`docs\contracts\qr-payload.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# QR Payload Contract

**Phase:** 6
**Status:** Stable for single-building MVP

## Overview

The entrance poster contains a QR code encoding a small JSON payload identifying the building and entrance. The iOS app validates this payload against the bundled reviewed package before proceeding.

**Important:** The entrance poster serves as BOTH the QR code (scanned by AVCaptureSession) and the AR reference image (detected by ARKit for alignment). There is exactly one physical artifact. See [Entrance Poster Contract](#entrance-poster-contract) below.

## Payload Format

```json
{
  "type": "VecturAI-entrance",
  "buildingId": "house-demo-01",
  "entranceId": "marker-entrance-a",
  "v": 1
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `type` | string | yes | Always `"VecturAI-entrance"`. Rejects any other value. |
| `buildingId` | string | yes | Must match `buildingId` in `manifest.json`. |
| `entranceId` | string | yes | Must match an `id` in `entrance_markers.json`. |
```

## Status
Mapped (Pass 3 Normalization)
