# File Dossier: markers.schema.json

## Path
`docs\contracts\markers.schema.json`

## Type
Data Contract / Sample Data

## Role
Data Contract / Sample Data for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "VecturAI Markers",
    "description": "Unified marker definitions for AR alignment. Supports entrance (session init) and checkpoint (mid-route correction) roles.",
    "type": "object",
    "required": [
        "buildingId",
        "entranceMarkers"
    ],
    "properties": {
        "buildingId": {
            "type": "string"
        },
        "entranceMarkers": {
            "type": "array",
            "minItems": 1,
            "description": "At least one entrance marker is required for session initialization.",
            "items": {
                "$ref": "#/definitions/marker"
            }
        },
        "checkpointMarkers": {
            "type": "array",
            "description": "Optional checkpoint markers for mid-route alignment correction.",
            "items": {
                "$ref": "#/definitions/marker"
            }
        }
    },
    "definitions": {
        "marker": {
        
```

## Status
Mapped (Pass 3 Normalization)
