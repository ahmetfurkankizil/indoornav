# File Dossier: rc-checklist.md

## Path
`docs\release\rc-checklist.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Release Candidate Checklist

Version: ____  
Date: ____  
Validated by: ____

## 1. Automated Verification
- [ ] `scripts/verify-all.sh` passes
- [ ] CI workflow green (if pushed)

## 2. Build Verification
- [ ] Android debug APK builds (`make android-debug`)
- [ ] Android release APK builds (`make android-release`)
- [ ] iOS Xcode build succeeds for device
- [ ] iOS Xcode build succeeds for simulator

## 3. Demo Package
- [ ] `scripts/verify-demo-package.sh` passes
- [ ] `make preprocess` regenerates package without errors
- [ ] Package contains all required files

## 4. Demo Mode (Scripted)
- [ ] App launches in Demo config
- [ ] Sample package preloaded
- [ ] Room search returns results
- [ ] Route preview shows arrows
- [ ] Simulate Scan triggers alignment
- [ ] Progress advances with "Advance" button
- [ ] Arrival overlay appears at ~95%
- [ ] History records the visit

## 5. Live AR (Real Device + Marker)
- [ ] Marker printed at correct dimensions
- [ ] Camera detects marker wi
```

## Status
Mapped (Pass 3 Normalization)
