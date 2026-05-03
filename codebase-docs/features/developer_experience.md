# Feature: Developer Experience & Samples

## Overview
Vectura AI includes a robust set of developer tools, automated scripts, and sample data to ensure consistent development, testing, and demonstration of the system. This ecosystem supports the full lifecycle from raw GLB ingestion to mobile app deployment.

## Components

### Automated Verification (`scripts/`)
- **Unified Validation**: `verify-all.sh` orchestrates the entire test and build suite.
- **Data Validation**: `validate-reviewed-package.sh` and `verify-demo-package.sh` enforce schema compliance for navigation data.
- **Admin API Testing**: Scripts like `verify-admin-draft-api.sh` provide smoke tests for the orchestration backend.
- **Asset Generation**: `generate-entrance-poster.sh` automates the creation of QR-coded alignment posters.

### Reference Data (`sample/`)
- **`demo-building/`**: A complete end-to-end example of a navigation-ready building.
  - `draft/`: Output from the `nav-preprocessor` showing the initial state.
  - `package/`: Final GLB source asset.
- **`reviewed-house-package/`**: A finalized, manually reviewed navigation bundle (manifest, rooms, graph) used for regression testing and smoke tests.
- **`entrance-poster/`**: Sample physical asset for testing AR alignment via QR codes.

### Standards & ADRs (`docs/`)
- **Architecture Decision Records (ADRs)**: Located in `docs/adr/`, these document the "why" behind critical tech stack and design choices (e.g., KMP, Native AR, Entrance Markers).
- **Contract Schemas**: `docs/contracts/` contains JSON schemas that define the stable interface between the preprocessor tools and the mobile clients.
- **Operational Guides**: `docs/demo/` and `docs/qa/` provide instructions for presenters, operators, and QA testers.

## Lifecycle Support
1. **Bootstrap**: Developers use `docs/setup/` (if present) and `verify-all.sh` to ensure a working environment.
2. **Feature Development**: ADRs provide the context for new implementations.
3. **Data Integrity**: Scripts validate that tool outputs match contract schemas before they reach the apps.
4. **Regression Testing**: `reviewed-house-package` provides a stable baseline for UI and engine changes.

## Related Dossiers
- `scripts/verify-all.sh`
- `docs/adr/ADR-001-kmp-shared-ui-native-ar.md`
- `docs/contracts/manifest.schema.json`

## Risks & Maintenance
- **Sample Bitrot**: Sample data must be regenerated if the preprocessor logic or schema changes.
- **Script Fragility**: Bash scripts rely on local environment paths and tools (Gradle, Xcode).
