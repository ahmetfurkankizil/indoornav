# File Dossier: verify-all.sh

## Metadata
- **Path**: `scripts/verify-all.sh`
- **Type**: Shell Script
- **Feature**: `infrastructure`
- **Status**: Mapped

## Role
Provides a unified entry point for local verification of the entire codebase. It orchestrates tests and builds across tools, shared logic, and the Android application to ensure repository integrity before commits or releases.

## Main Steps
1. **Preprocessor Tests** (L36): Runs `:tools:nav-preprocessor:test`.
2. **Android Build** (L39): Assembles the debug APK.
3. **Demo Package Verification** (L42): Validates the sample building data via `scripts/verify-demo-package.sh`.
4. **iOS Framework Build** (L45-50): Optionally builds the KMP framework for iOS if Xcode is detected.

## Important Logic
- **Exit on Failure** (L6): Uses `set -euo pipefail` for strict error handling.
- **Summary Reporting** (L53-63): Aggregates results and returns a non-zero exit code if any step fails.
- **Skipping iOS** (L45): Gracefully handles environments without macOS/Xcode.

## Uses
- `gradlew`: For builds and unit tests.
- `verify-demo-package.sh`: For data-level validation.

## Used By
- Developers: For pre-commit validation.
- CI/CD: The core logic matches the CI pipeline definition.

## Related Features
- `infrastructure`: Part of the build and validation system.

## Notes / Risks
- **Execution Time**: Running the full suite can be slow due to the Android build and framework linking.
- **Environment Dependency**: Requires Bash and Java/Gradle environments.
