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
- [ ] Camera detects marker within 5 seconds
- [ ] AR arrows appear aligned with physical space
- [ ] Walking forward advances progress
- [ ] Remaining distance decreases
- [ ] No backwards progress during forward walking
- [ ] Arrival detected near destination

## 6. Checkpoint Markers (if applicable)
- [ ] Checkpoint detected without restarting session
- [ ] Alignment correction applied (check debug)
- [ ] Progress continues forward after correction

## 7. Version Verification
- [ ] `AppVersion.VERSION` matches expected RC version
- [ ] Diagnostics panel shows correct version
- [ ] About/footer shows version in-app

## 8. Documentation
- [ ] README matches current state
- [ ] First-day setup tested by non-author
- [ ] Operator guide is current
- [ ] Presenter guide is current

## Result

- [ ] **PASS** — Promote to release
- [ ] **FAIL** — Fix and re-cut as rcN+1

Notes:
