# Workplan - Codebase Mapping

## Current Status
- **Overall Completion**: 100%
- **Current Phase**: Documentation Finalized
- **Last Batch**: B10 (Mapping Pass 3: Normalization & Final Verification)

This plan outlines the deterministic batches for mapping the Vectura AI repository.

## Batch Status Model
- **pending**: Not started
- **in_progress**: Currently being mapped
- **completed**: Mapping finished, artifacts created

## Batches

### Batch 1: Root Configuration & Build System
- **batch_id**: B01
- **folders**: `/`, `gradle/`, `.github/`
- **estimated_complexity**: Low
- **dependency_notes**: Foundation for the entire project build.
- **status**: completed

### Batch 2: Shared Core & Domain Logic
- **batch_id**: B02
- **folders**: `shared/core/`
- **estimated_complexity**: High
- **dependency_notes**: Contains the primary domain models and navigation engines. Used by almost everything.
- **status**: completed

### Batch 3: Shared Features (Routing, Search, History, Preview)
- **batch_id**: B03
- **folders**: `shared/feature-*/`
- **estimated_complexity**: Medium
- **dependency_notes**: Business logic modules. Depend on Shared Core.
- **status**: completed

### Batch 4: Shared Data & Design System
- **batch_id**: B04
- **folders**: `shared/data-*/`, `shared/designsystem/`
- **estimated_complexity**: Medium
- **dependency_notes**: Persistence and UI components.
- **status**: completed

### Batch 5: Android Application
- **batch_id**: B05
- **folders**: `apps/androidApp/`
- **estimated_complexity**: Medium
- **dependency_notes**: Android-specific implementation and AR integration.
- **status**: completed

### Batch 6: iOS Application
- **batch_id**: B06
- **folders**: `apps/iosApp/`
- **estimated_complexity**: Medium
- **dependency_notes**: iOS-specific implementation (Swift/ARKit).
- **status**: completed

### Batch 7: Nav Preprocessor Tool (Logic & Extraction)
- **batch_id**: B07
- **folders**: `tools/nav-preprocessor/src/main/`
- **estimated_complexity**: High
- **dependency_notes**: Backend tool for processing GLB/navigation data.
- **status**: completed

### Batch 8: Nav Preprocessor Tool (Tests & Validation)
- **batch_id**: B08
- **folders**: `tools/nav-preprocessor/src/test/`
- **estimated_complexity**: Medium
- **dependency_notes**: Extensive test suite for the preprocessor.
- **status**: completed

### Batch 9: Admin API Tool
- **batch_id**: B09
- **folders**: `tools/admin-api/`
- **estimated_complexity**: Medium
- **dependency_notes**: Ktor-based backend for draft management.
- **status**: completed

### Batch 10: Samples, Scripts & Final Polish
- **batch_id**: B10
- **folders**: `sample/`, `scripts/`, `docs/`
- **estimated_complexity**: Low
- **status**: completed

