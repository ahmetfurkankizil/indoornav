# File Dossier: build.gradle.kts

## Path
`shared\feature-routing\build.gradle.kts`

## Type
Build Configuration

## Role
Build Configuration for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
    }
}

android {
    namespace = "com.Vectura AI.feature.routing"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
}

```

## Status
Mapped (Pass 3 Normalization)
