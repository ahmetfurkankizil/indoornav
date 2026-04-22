# File Dossier: ci.yml

## Path
`.github/workflows/ci.yml`

## Type
Authored Config (GitHub Actions)

## Role
Defines the Continuous Integration (CI) pipeline for the project. Automates testing, building, and validation.

## Logic
- **on**: push/pull_request to `main`.
- **jobs**: `test-and-build`
  - Runs preprocessor tests.
  - Builds Android debug APK.
  - Verifies the demo package via script.

## Uses
- `gradlew`: For builds and tests.
- `scripts/verify-demo-package.sh`: For data validation.

## Used By
- Repository: Enforces quality on every PR.
