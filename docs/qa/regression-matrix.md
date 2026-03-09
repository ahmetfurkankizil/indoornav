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
| 8 | Checkpoint-marker flow | `CorrectionCoordinatorTest`, `CheckpointMarkerValidationTest` | `verify-all.sh` | Checkpoint smoke |
| 9 | Live progress estimator | `ProgressEstimatorTest`, `ProgressContinuityAfterCorrectionTest` | `verify-all.sh` | Live AR smoke |
| 10 | Correction coordinator | `CorrectionCoordinatorTest`, `OffRouteDetectorTest` | `verify-all.sh` | Live AR smoke |

## Coverage Summary

- **Automated tests**: 160+ covering all 10 features
- **Verification scripts**: 3 scripts covering build, package, and iOS
- **Manual QA**: 4 checklists (demo smoke, live AR, checkpoint, release)

## Risk Levels

| Risk | Features | Mitigation |
|------|----------|------------|
| 🔴 High | Package loading, route preview, marker detection | Unit tests + demo smoke + CI |
| 🟡 Medium | Progress estimation, correction, arrival | Unit tests + live AR smoke |
| 🟢 Low | History, search, arrow mapping | Unit tests + demo smoke |

## Running Regression Tests

```bash
# All automated tests
make test-preprocessor

# Full verification (tests + build + package)
make verify-all

# Just the demo package
make verify-package
```
