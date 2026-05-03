# File Dossier: DataLocalModule.kt

## Path
`shared\data-local\src\commonMain\kotlin\com\Vectura AI\data\local\di\DataLocalModule.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.data.local.di

import com.Vectura AI.data.local.LocalCacheDataSource
import com.Vectura AI.data.local.SqlDelightCacheDataSource
import org.koin.dsl.module

val dataLocalModule = module {
    single<LocalCacheDataSource> { SqlDelightCacheDataSource() }
}

```

## Status
Mapped (Pass 3 Normalization)
