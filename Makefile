.PHONY: help test-all test-preprocessor test-shared \
       android-debug android-release \
       ios-open preprocess clean

# ═══════════════════════════════════════════════
# VecturAI — Build Targets
# ═══════════════════════════════════════════════

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Tests ────────────────────────────────────

test-all: test-preprocessor ## Run all tests
	@echo "✅ All tests passed"

test-preprocessor: ## Run nav-preprocessor tests (~92 tests)
	./gradlew :tools:nav-preprocessor:test

test-shared: ## Run shared KMP module tests (when configured)
	./gradlew :shared:core:allTests

# ── Android ──────────────────────────────────

android-debug: ## Build Android debug APK
	./gradlew :apps:androidApp:assembleDebug

android-release: ## Build Android release APK
	./gradlew :apps:androidApp:assembleRelease

android-install: ## Install debug APK to connected device
	./gradlew :apps:androidApp:installDebug

# ── iOS ──────────────────────────────────────

ios-open: ## Open iOS project in Xcode
	open apps/iosApp/iosApp.xcodeproj

ios-framework: ## Build shared framework for iOS
	./gradlew :shared:core:linkDebugFrameworkIosArm64

# ── Preprocessor ─────────────────────────────

preprocess: ## Run nav-preprocessor on sample building
	./gradlew :tools:nav-preprocessor:run \
		--args="--input sample/demo-building/scan.glb \
		        --config sample/demo-building/authoring_config.json \
		        --output sample/demo-building/package/ \
		        --overwrite"

# ── Utilities ─────────────────────────────────

clean: ## Clean all build outputs
	./gradlew clean

tree: ## Show project structure (requires 'tree' command)
	tree -I 'build|.gradle|.idea|.git' --dirsfirst -L 3
