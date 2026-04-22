# File Dossier: build.gradle.kts

## Path
`shared\designsystem\build.gradle.kts`

## Type
Build Configuration

## Role
Build Configuration for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
    }
}

android {
    namespace = "com.vecturai.designsystem"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = 
```

## Status
Mapped (Pass 3 Normalization)
