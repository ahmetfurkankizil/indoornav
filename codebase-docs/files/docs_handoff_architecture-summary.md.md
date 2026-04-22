# File Dossier: architecture-summary.md

## Path
`docs\handoff\architecture-summary.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Architecture Summary

Concise architecture overview for team handoff and technical review.

## System Diagram

```mermaid
graph TD
    subgraph "Authoring"
        A["Assisted Authoring<br/>(3D scan + config)"] --> B["Nav Preprocessor<br/>(validate + export)"]
        B --> C["Building Package<br/>(JSON + GLB)"]
    end

    subgraph "Shared KMP Core"
        C --> D["PackageLoader"]
        D --> E["BuildingRepository"]
        E --> F["RouteEngine<br/>(Dijkstra)"]
        F --> G["ArrowMapper"]
        G --> H["ProgressEstimator<br/>(polyline projection)"]
        H --> I["ArrivalDetector"]
        I --> J["SessionCoordinator"]
        J --> K["HistoryRepository"]
        
        L["CorrectionCoordinator"] --> H
        M["OffRouteDetector"] --> J
    end

    subgraph "Native AR"
        N["ARKit / ARCore"] --> O["MarkerDetector"]
        O --> P["ArNavigationCoordinator"]
        P --> H
        P --> L
        N --> Q["CameraPose @ 2Hz"]
        Q --> P
    end

    subgraph "U
```

## Status
Mapped (Pass 3 Normalization)
