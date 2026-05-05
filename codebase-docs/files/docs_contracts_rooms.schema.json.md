# File Dossier: rooms.schema.json

## Path
`docs\contracts\rooms.schema.json`

## Type
Data Contract / Sample Data

## Role
Data Contract / Sample Data for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "VecturAI Rooms",
    "description": "Room/POI definitions for a building, used for search and navigation targets.",
    "type": "object",
    "required": [
        "buildingId",
        "rooms"
    ],
    "properties": {
        "buildingId": {
            "type": "string"
        },
        "rooms": {
            "type": "array",
            "items": {
                "type": "object",
                "required": [
                    "id",
                    "name",
                    "entryNodeIds"
                ],
                "properties": {
                    "id": {
                        "type": "string",
                        "description": "Unique room identifier"
                    },
                    "name": {
                        "type": "string",
                        "description": "Human-readable room name"
                    },
                    "description": {
           
```

## Status
Mapped (Pass 3 Normalization)
