# File Dossier: build.gradle.kts

## Path
`tools\nav-preprocessor\build.gradle.kts`

## Type
Build Configuration

## Role
Build Configuration for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.Vectura AI.tools.preprocessor.MainKt")
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

```

## Status
Mapped (Pass 3 Normalization)
