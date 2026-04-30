# File Dossier: INDOOR_AR_NAV_BUILD_PLAN.md

## Path
project-document/INDOOR_AR_NAV_BUILD_PLAN.md

## Type
docs

## Role
Comprehensive architectural blueprint and implementation guide for the project.

## Imports / Includes
N/A

## Exports / Public Surface
N/A

## Main Symbols
- `Core Concept`: Persistent localization via Cloud Anchors.
- `System Architecture`: Modular layout and state machines for Mapping/Navigation.
- `Data Model`: JSON schema for `MapGraph`, `MapNode`, and `MapEdge`.
- `Build Schedule`: 14-day implementation timeline.

## Important Logic by Line Range
- L33-47: Explanation of the persistent localization problem and the Cloud Anchor solution.
- L90-119: Recommended module layout and folder responsibilities.
- L142-169: Detailed data model definitions.
- L655-675: 14-day development schedule with critical milestones.

## Uses
- ARCore SDK documentation
- SceneView library documentation

## Used By
- Developers (Guidance)

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- Highlighted risk: Cloud Anchor resolution reliability depends heavily on environmental texture.
- Highlighted risk: 365-day TTL for free-tier Cloud Anchors.
