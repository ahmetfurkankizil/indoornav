# File Dossier: DataLocalModule.kt

## Path
`shared\data-local\src\commonMain\kotlin\com\VecturAI\data\local\di\DataLocalModule.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.data.local.di

import com.VecturAI.data.local.LocalCacheDataSource
import com.VecturAI.data.local.SqlDelightCacheDataSource
import org.koin.dsl.module

val dataLocalModule = module {
    single<LocalCacheDataSource> { SqlDelightCacheDataSource() }
}

```

## Status
Mapped (Pass 3 Normalization)
