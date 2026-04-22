# File Dossier: FeatureRoutingModule.kt

## Path
`shared\feature-routing\src\commonMain\kotlin\com\vecturai\feature\routing\di\FeatureRoutingModule.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.feature.routing.di

import com.vecturai.feature.routing.RouteNavigationUseCase
import org.koin.dsl.module

val featureRoutingModule = module {
    factory { RouteNavigationUseCase(
        buildingRepository = get(),
        routeEngine = get(),
        appStore = get(),
    ) }
}

```

## Status
Mapped (Pass 3 Normalization)
