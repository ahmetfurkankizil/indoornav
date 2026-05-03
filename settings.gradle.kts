pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Vectura AI"

// ── App Modules ──────────────────────────────────────────
include(":apps:androidApp")
// Note: iOS app is built via Xcode, not Gradle directly.
// The iosApp directory contains the Xcode project files.

// ── Shared Modules ──────────────────────────────────────
include(":shared:core")
include(":shared:feature-search")
include(":shared:feature-routing")
include(":shared:feature-history")
include(":shared:feature-preview")
include(":shared:data-local")
include(":shared:data-remote")
include(":shared:designsystem")

// ── Tools ───────────────────────────────────────────────
include(":tools:nav-preprocessor")
include(":tools:admin-api")
