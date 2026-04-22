# File Dossier: release-process.md

## Path
`docs\release\release-process.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Release Process

## Versioning

Single source of truth: `shared/core/.../config/AppVersion.kt`

```kotlin
object AppVersion {
    const val VERSION = "1.7.0-rc1"   // Semantic version
    const val BUILD_PHASE = "Phase 8"  // Dev phase
    const val BUILD_DATE = "2026-03-10"
}
```

## Version Surfaces

| Surface | Source | Example |
|---------|--------|---------|
| Shared KMP | `AppVersion.VERSION` | `1.7.0-rc1` |
| Android versionName | Gradle `versionName` | `1.7.0-rc1` |
| Android versionCode | Gradle `versionCode` | `17001` |
| iOS Marketing Version | Xcode target | `1.7.0` |
| iOS Build Number | Xcode target | `1` |
| Package manifest | Preprocessor `preprocessorVersion` | `1.7.0` |

## RC → Final Flow

```
main branch
  │
  ├─ tag v1.7.0-rc1 ← validation begins
  │    ├─ CI passes (tests + Android build)
  │    ├─ Local iOS build verified
  │    ├─ Demo smoke test passed
  │    ├─ Live marker test passed
  │    └─ Presenter walkthrough completed
  │
  ├─ fix if needed → tag v1.
```

## Status
Mapped (Pass 3 Normalization)
