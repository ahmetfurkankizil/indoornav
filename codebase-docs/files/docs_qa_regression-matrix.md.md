# File Dossier: regression-matrix.md

## Path
`docs\qa\regression-matrix.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Regression Matrix

Demo-critical features and their test coverage.

## Matrix

| # | Feature | Unit Tests | Verification Script | Manual QA |
|---|---------|-----------|-------------------|-----------|
| 1 | Package loading | `PackageExporterTest`, `ContractBackwardCompatTest` | `verify-demo-package.sh` | Demo smoke |
| 2 | Room search | `RoomSearchTest` | — | Demo smoke |
| 3 | Route preview | `DijkstraRouteEngineTest`, `RouteToArrowMapperTest` | — | Demo smoke |
| 4 | Route-to-arrow mapping | `RouteToArrowMapperTest` | — | Demo smoke |
| 5 | Arrival/session completion | `ArrivalDetectorTest`, `NavigationSessionCoordinatorTest` | — | Live AR smoke |
| 6 | History persistence | `HistorySerializationTest`, `ContractBackwardCompatTest` | — | Demo smoke |
| 7 | Single-marker flow | `ProgressEstimatorTest`, `DemoCriticalRegressionTest` | `verify-all.sh` | Live AR smoke |
| 8 | Checkpoint-marker flow | `CorrectionCoordinatorTest`, `CheckpointMarkerValidationTest` | `verify-all.sh` | Check
```

## Status
Mapped (Pass 3 Normalization)
