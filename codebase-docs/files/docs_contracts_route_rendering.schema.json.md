# File Dossier: route_rendering.schema.json

## Path
`docs\contracts\route_rendering.schema.json`

## Type
Data Contract / Sample Data

## Role
Data Contract / Sample Data for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "VecturAI Route Rendering Configuration",
    "description": "Configuration for how navigation routes are rendered in AR and on the 2D map.",
    "type": "object",
    "required": [
        "buildingId"
    ],
    "properties": {
        "buildingId": {
            "type": "string"
        },
        "arArrows": {
            "type": "object",
            "description": "AR 3D arrow rendering configuration",
            "properties": {
                "modelAsset": {
                    "type": "string",
                    "description": "3D model filename for the navigation arrow",
                    "default": "arrow.glb"
                },
                "colorHex": {
                    "type": "string",
                    "description": "Arrow color as hex string",
                    "default": "#2563EB"
                },
                "spacingMeters": {
                    "type": "number",
         
```

## Status
Mapped (Pass 3 Normalization)
