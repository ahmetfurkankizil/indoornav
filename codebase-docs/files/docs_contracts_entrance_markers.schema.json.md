# File Dossier: entrance_markers.schema.json

## Path
`docs\contracts\entrance_markers.schema.json`

## Type
Data Contract / Sample Data

## Role
Data Contract / Sample Data for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Vectura AI Entrance Markers",
    "description": "Entrance marker definitions for AR world alignment and localization.",
    "type": "object",
    "required": [
        "buildingId",
        "markers"
    ],
    "properties": {
        "buildingId": {
            "type": "string"
        },
        "markers": {
            "type": "array",
            "items": {
                "type": "object",
                "required": [
                    "id",
                    "qrPayload",
                    "positionX",
                    "positionY",
                    "nearestNodeId"
                ],
                "properties": {
                    "id": {
                        "type": "string",
                        "description": "Unique marker identifier"
                    },
                    "qrPayload": {
                        "type": "string",
                        "description": "QR code con
```

## Status
Mapped (Pass 3 Normalization)
