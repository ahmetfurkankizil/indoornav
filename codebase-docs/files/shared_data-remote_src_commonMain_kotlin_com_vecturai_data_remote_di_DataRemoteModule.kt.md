# File Dossier: DataRemoteModule.kt

## Path
`shared\data-remote\src\commonMain\kotlin\com\Vectura AI\data\remote\di\DataRemoteModule.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.data.remote.di

import com.Vectura AI.data.remote.KtorBuildingDataSource
import com.Vectura AI.data.remote.RemoteBuildingDataSource
import org.koin.dsl.module

val dataRemoteModule = module {
    single<RemoteBuildingDataSource> { KtorBuildingDataSource() }
}

```

## Status
Mapped (Pass 3 Normalization)
