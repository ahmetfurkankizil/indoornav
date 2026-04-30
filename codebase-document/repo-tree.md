# Repository Tree - vecturDENEME

## Root
- .env (config)
- .env.example (config)
- .gitignore (config)
- AUDIT_REPORT.md (docs)
- build.gradle.kts (build)
- gradle.properties (build)
- gradlew (script)
- gradlew.bat (script)
- IMPLEMENTATION_NOTES.md (docs)
- local.properties (config)
- settings.gradle.kts (build)

## app/
- .gitignore (config)
- build.gradle.kts (build)
- proguard-rules.pro (config)
- src/
  - androidTest/ (test)
  - main/
    - AndroidManifest.xml (config)
    - assets/
      - arrow.glb (asset)
    - java/com/example/vecturai/
      - MainActivity.kt (source)
      - ar/ (feature: AR Rendering)
        - ArrowRenderer.kt
        - ArSessionConfig.kt
        - CloudAnchorAuthStatus.kt
        - CloudAnchorHelper.kt
        - PoseUtils.kt
      - graph/ (feature: Pathfinding)
        - MapGraph.kt
        - Pathfinder.kt
      - persistence/ (feature: Data Persistence)
        - GraphRepository.kt
      - ui/ (feature: UI Components)
        - ArAssetUtils.kt
        - ArPermission.kt
        - ModePickerScreen.kt
        - mapping/
          - MappingScreen.kt
          - MappingViewModel.kt
        - navigation/
          - DestinationPicker.kt
          - NavigationScreen.kt
          - NavigationViewModel.kt
        - theme/
          - Color.kt
          - Theme.kt
          - Type.kt
    - res/ (asset)
      - drawable/
      - mipmap-*/
      - values/
      - xml/
  - test/ (test)

## gradle/
- gradle-daemon-jvm.properties (build)
- libs.versions.toml (build)
- wrapper/
  - gradle-wrapper.jar (binary)
  - gradle-wrapper.properties (build)

## project-document/
- INDOOR_AR_NAV_BUILD_PLAN.md (docs)
