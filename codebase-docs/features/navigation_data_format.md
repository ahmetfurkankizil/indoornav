# Feature: Indoor Navigation Data Format

- **Feature Name**: Indoor Navigation Data Format
- **Purpose**: Defines the JSON schema and coordinate system used to represent indoor environments, navigation graphs, and AR metadata.
- **Implemented In**:
    - `rectorate_main_floor_navigation.json` (Ground truth sample)
    - [TBD] shared logic classes for serialization.
- **Used By**:
    - `nav-preprocessor` (Tooling)
    - `shared/core` (Routing & Logic)
    - `apps/androidApp` & `apps/iosApp` (AR Rendering & Navigation)
- **Main Flow**:
    1. Preprocessor generates or a human creates a JSON file following the schema.
    2. Shared core loads and parses the JSON into domain objects.
    3. Routing engine uses nodes/edges for pathfinding.
    4. AR renderer uses entrance markers for world alignment and nodes for arrow placement.
- **Key Symbols**: N/A (Data format)
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `buildingId`, `nodes`, `edges`, `rooms`, `entranceMarkers`, `routeRendering`.
    - Coordinate system: building-local (`+X` right, `+Y` up, `+Z` down/forward depending on context, though `rectorate` sample notes `z` as down on 2D sketch).
- **Related Tests**: N/A (Data validation is done by preprocessor tools).
- **Related File Dossiers**:
    - [rectorate_main_floor_navigation.json](../files/rectorate_main_floor_navigation.json.md)
- **Risks / Notes**:
    - Schema consistency is critical between the preprocessor output and the app's loader.
    - Coordinate system orientation must be handled carefully between the 2D draft and 3D AR world.
