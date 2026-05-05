# File Dossier: authoring_config.schema.json

## Path
`docs\contracts\authoring_config.schema.json`

## Type
Data Contract / Sample Data

## Role
Data Contract / Sample Data for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "VecturAI Authoring Config",
    "description": "Human-authored building annotation file. Input to the nav-preprocessor CLI.",
    "type": "object",
    "required": [
        "buildingId",
        "buildingName",
        "floorId",
        "asset",
        "entranceMarkers",
        "nodes",
        "edges",
        "rooms"
    ],
    "properties": {
        "buildingId": {
            "type": "string",
            "minLength": 1
        },
        "buildingName": {
            "type": "string",
            "minLength": 1
        },
        "floorId": {
            "type": "string",
            "default": "ground"
        },
        "asset": {
            "type": "object",
            "required": [
                "glbFile"
            ],
            "properties": {
                "glbFile": {
                    "type": "string",
                    "description": "Relative path to Polycam .glb file"
           
```

## Status
Mapped (Pass 3 Normalization)
