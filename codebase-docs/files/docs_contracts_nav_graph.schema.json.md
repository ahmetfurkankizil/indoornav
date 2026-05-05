# File Dossier: nav_graph.schema.json

## Path
`docs\contracts\nav_graph.schema.json`

## Type
Data Contract / Sample Data

## Role
Data Contract / Sample Data for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "VecturAI Navigation Graph",
    "description": "Indoor navigation graph with nodes (waypoints) and edges (connections) for pathfinding.",
    "type": "object",
    "required": [
        "buildingId",
        "floorId",
        "nodes",
        "edges",
        "version"
    ],
    "properties": {
        "buildingId": {
            "type": "string"
        },
        "floorId": {
            "type": "string",
            "default": "ground"
        },
        "version": {
            "type": "integer",
            "minimum": 1
        },
        "nodes": {
            "type": "array",
            "items": {
                "type": "object",
                "required": [
                    "id",
                    "x",
                    "y"
                ],
                "properties": {
                    "id": {
                        "type": "string",
                        "description": "Unique node
```

## Status
Mapped (Pass 3 Normalization)
