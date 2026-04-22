# File Dossier: nav_graph.json

## Path
`apps/androidApp/src/main/assets/reviewed-package/nav_graph.json`

## Type
Bundled Runtime Data (Reviewed Package Navigation Graph)

## Role
Defines Android graph nodes and weighted edges for route computation.

## Key Fields
- `nodes[].id`, `x`, `y`, `z`, `type`, `label`
- `edges[].id`, `from`, `to`, `cost`, `bidirectional`

## Used By
- `AndroidReviewedPackageLoader.kt`

## Notes
- Edge costs drive Android Dijkstra routing; node coordinates drive AR arrow placement.
