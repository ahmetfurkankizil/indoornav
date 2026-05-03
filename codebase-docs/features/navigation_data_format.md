# Feature: Indoor Navigation Data Format

- **Feature Name**: Indoor Navigation Data Format
- **Purpose**: Defines the JSON schema and coordinate system used to represent indoor environments, navigation graphs, and AR metadata.
- **Implemented In**:
    - `rectorate_main_floor_navigation.json` (Ground truth sample)
    - [TBD] shared logic classes for serialization.
    - `apps/androidApp/src/main/assets/reviewed-package/` (Android bundled runtime package)
    - `apps/androidApp/src/main/kotlin/com/Vectura AI/android/data/AndroidReviewedPackageLoader.kt`
- **Used By**:
    - `nav-preprocessor` (Tooling)
    - `shared/core` (Routing & Logic)
    - `apps/androidApp` & `apps/iosApp` (AR Rendering & Navigation)
- **Main Flow**:
    1. Preprocessor/admin review exports the five-file reviewed package.
    2. Mobile package loaders parse `manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, and `route_rendering.json`.
    3. Routing uses nodes/edges for pathfinding.
    4. AR uses entrance markers for world alignment and route rendering config for arrow placement/lookahead.
- **Key Symbols**: N/A (Data format)
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `manifest.json`: `buildingId`, package metadata, and file names.
    - `rooms.json`: destination rooms with category/description.
    - `nav_graph.json`: nodes and weighted/bidirectional edges.
    - `entrance_markers.json`: marker id, start node, physical size, position, rotation, reference image name.
    - `route_rendering.json`: arrow spacing, lookahead, destination threshold, turn marker threshold, height offset.
    - Coordinate system: building-local (`+X` right, `+Y` up, `+Z` down/forward depending on context, though `rectorate` sample notes `z` as down on 2D sketch).
- **Related Tests**: N/A (Data validation is done by preprocessor tools).
- **Related File Dossiers**:
    - [rectorate_main_floor_navigation.json](../files/rectorate_main_floor_navigation.json.md)
    - [AndroidReviewedPackageLoader.kt](../files/apps_androidApp_src_main_kotlin_com_Vectura AI_android_data_AndroidReviewedPackageLoader.kt.md)
    - [Android manifest.json](../files/apps_androidApp_src_main_assets_reviewed-package_manifest.json.md)
    - [Android nav_graph.json](../files/apps_androidApp_src_main_assets_reviewed-package_nav_graph.json.md)
- **Risks / Notes**:
    - Schema consistency is critical between the preprocessor output and the app's loader.
    - Coordinate system orientation must be handled carefully between the 2D draft and 3D AR world.
    - Android and iOS reviewed package copies must stay synchronized for demo parity.
