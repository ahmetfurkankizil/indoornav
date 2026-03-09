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
  ├─ fix if needed → tag v1.7.0-rc2
  │
  └─ tag v1.7.0 ← RC promoted to release
       └─ Update AppVersion: remove "-rc1"
```

## Steps to Cut an RC

1. Update `AppVersion.kt`: version, phase, date
2. Run `scripts/verify-all.sh` — all must pass
3. Commit: `chore: bump version to v1.7.0-rc1`
4. Tag: `git tag v1.7.0-rc1`
5. Push: `git push origin main --tags`
6. Run RC checklist: `docs/release/rc-checklist.md`

## Steps to Promote RC to Release

1. Ensure RC checklist is fully passed
2. Update `AppVersion.kt`: remove `-rcN` suffix
3. Commit: `chore: release v1.7.0`
4. Tag: `git tag v1.7.0`
5. Push: `git push origin main --tags`
6. Write release notes from template
