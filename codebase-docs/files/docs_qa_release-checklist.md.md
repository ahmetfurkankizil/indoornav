# File Dossier: release-checklist.md

## Path
`docs\qa\release-checklist.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Release Checklist

## Tests
- [ ] All tests pass (`make test-preprocessor`, ~170 tests)
- [ ] Demo-critical regression tests pass
- [ ] Contract backward compatibility tests pass
- [ ] `make verify-all` passes

## QA
- [ ] [Demo smoke checklist](demo-smoke-checklist.md) passed (scripted path)
- [ ] [Live AR smoke checklist](live-ar-smoke-checklist.md) passed (real device)
- [ ] [Checkpoint marker checklist](checkpoint-marker-smoke-checklist.md) passed (if applicable)
- [ ] Demo script rehearsed with both live and scripted paths

## Android
- [ ] Debug APK builds (`make android-debug`)
- [ ] Release APK builds (`make android-release`)
- [ ] Live AR works on physical device with marker
- [ ] Simulate Scan works on emulator/device

## iOS
- [ ] `make verify-ios` passes (or Xcode manual build)
- [ ] Xcode build for simulator succeeds
- [ ] Xcode build for device succeeds
- [ ] Live AR works on physical device with marker
- [ ] Simulate Scan works on simulator

## CI
- [ ] GitHub Actions 
```

## Status
Mapped (Pass 3 Normalization)
