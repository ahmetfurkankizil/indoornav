# Release Checklist

## Before Tagging
- [ ] All tests pass (`make test-all`)
- [ ] Demo smoke checklist passed on both platforms
- [ ] README version updated
- [ ] AppVersion constants updated
- [ ] No TODO/FIXME in critical path code
- [ ] Demo script rehearsed end-to-end

## Android
- [ ] Debug APK builds (`make android-debug`)
- [ ] Release APK builds (`make android-release`)
- [ ] Demo flow works on physical device
- [ ] ProGuard / R8 rules correct (if applicable)

## iOS
- [ ] Xcode project builds for simulator
- [ ] Xcode project builds for device
- [ ] Signing configured for TestFlight (if distributing)
- [ ] Demo flow works on physical device

## Documentation
- [ ] README accurate
- [ ] ADRs numbered sequentially
- [ ] Demo script current
- [ ] Known limitations documented
