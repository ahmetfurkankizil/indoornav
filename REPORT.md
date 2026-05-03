# Final Architecture and Design Details

Vectura AI is an end-to-end, local-first indoor navigation platform designed to guide users through complex building environments using precise Augmented Reality (AR) overlays. The core of its design is centered around a decoupled preprocessing pipeline and robust cross-platform shared logic. 

The architecture is divided into three primary phases: preprocessing, distribution, and runtime localization. In the preprocessing phase, 3D building scans (provided in GLB format) are ingested by a CLI-based tool (`tools/nav-preprocessor`) which extracts building geometry and structural metadata to generate optimized, navigable graphs in JSON format. For distribution, these navigation packages are served by a Ktor-based backend service (`tools/admin-api`) or bundled directly within the mobile applications.

During runtime localization, the mobile clients operate on a local-first principle. Instead of relying on constant cloud connectivity or external GPS coordinates, the system establishes a coordinate transformation between the device's local AR tracking space and the global building coordinate space. This is achieved by having the user scan a physical anchor (such as a unified QR/Image poster). Once aligned, the shared logic calculates the optimal route using Dijkstra's algorithm and projects the camera pose onto the navigation graph. The user is then guided by 3D AR overlays rendered natively in front of them. The platform separates the heavy business and routing logic into a shared module, while delegating the performance-critical rendering tasks to native AR engines (ARCore on Android, ARKit on iOS).

# Development/Implementation Details

The project is built around Kotlin Multiplatform (KMP), enabling maximum code sharing across backend services, CLI tools, and mobile clients, while preserving native performance for UI and AR rendering. The primary language is Kotlin (2.1.10), supplemented by Swift for native iOS implementations. The build system is managed via Gradle (8.11.1) with Kotlin DSL (`build.gradle.kts`) and version catalogs for strict dependency management. Dependency injection across the Kotlin modules is handled by Koin.

The repository is modularized into distinct subsystems with strict, acyclic dependency rules (`apps` → `shared/features` → `shared/core`). 
- The `shared/core` module is the foundation, containing domain models, the pathfinding engine, and the mathematical logic for AR alignment and coordinate transformations. 
- The `shared/designsystem` centralizes common Jetpack Compose UI components and branding to maintain consistency.
- The `apps/androidApp` encapsulates the Android user flow using Jetpack Compose. Its entry points involve `MainActivity` for the home state and `ArCameraActivity` for managing the long-lived ARCore session. It intelligently integrates ML Kit for on-device QR decoding directly within the ARCore camera frames, circumventing the need for a separate CameraX integration. 
- The `apps/iosApp` relies on Swift and ARKit for native AR visualization and visitor flow.
- On the backend, `tools/nav-preprocessor` acts as the computational workhorse for extracting occupancy grids and serializing graphs, while `tools/admin-api` handles draft ingestion, override persistence, and package export orchestration using Ktor and SqlDelight.

Development workflows emphasize testing and verification via comprehensive scripts (like `./scripts/verify-all.sh`) and standard Gradle commands (`./gradlew assembleDebug`, `./gradlew test`). A major implementation priority is mitigating critical technical risks such as AR coordinate drift, ensuring performant graph pathfinding on mobile hardware, and maintaining strict backward compatibility of the JSON schema between the preprocessor and deployed client applications.
