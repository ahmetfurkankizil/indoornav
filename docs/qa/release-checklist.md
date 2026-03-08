# Release Checklist

## Tests
- [ ] All tests pass (`make test-all`)
- [ ] ~123+ tests across all suites

## QA
- [ ] [Demo smoke checklist](demo-smoke-checklist.md) passed (scripted path)
- [ ] [Live AR smoke checklist](live-ar-smoke-checklist.md) passed (real device)
- [ ] Demo script rehearsed with both live and scripted paths

## Android
- [ ] Debug APK builds (`make android-debug`)
- [ ] Release APK builds (`make android-release`)
- [ ] Live AR works on physical device with marker
- [ ] Simulate Scan works on emulator/device

## iOS
- [ ] Xcode build for simulator succeeds
- [ ] Xcode build for device succeeds
- [ ] Live AR works on physical device with marker
- [ ] Simulate Scan works on simulator

## Docs
- [ ] README accurate (version, test count, features)
- [ ] ADRs numbered 1–18, no gaps
- [ ] Demo script current (live + scripted paths)
- [ ] Known limitations documented
- [ ] Marker guide printed and verified
