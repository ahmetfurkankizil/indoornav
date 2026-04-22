# File Dossier: FeaturePreviewModule.kt

## Path
`shared\feature-preview\src\commonMain\kotlin\com\vecturai\feature\preview\di\FeaturePreviewModule.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.feature.preview.di

import com.vecturai.feature.preview.RoutePreviewUseCase
import org.koin.dsl.module

val featurePreviewModule = module {
    factory { RoutePreviewUseCase(
        buildingRepository = get(),
        routeEngine = get(),
    ) }
}

```

## Status
Mapped (Pass 3 Normalization)
