# Codebase Map - vecturDENEME

## Repository Overview
- **Purpose:** Indoor AR Navigation application providing mapping and localization using Google Cloud Anchors.
- **Tech Stack:** Kotlin, Jetpack Compose, ARCore, SceneView, Coroutines, Kotlinx Serialization.
- **Entrypoints:** `MainActivity.kt` (Compose Navigation host).

## Runtime Architecture
- **Navigation Flow:** [UI_Components](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/UI_Components.md) manages screen transitions and permissions.
- **Core Operations:**
  - [Mapping](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Mapping.md): Creating persistent 3D graphs of indoor spaces.
  - [Navigation](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Navigation.md): Turn-by-turn guidance through mapped spaces.
- **Engine Layers:**
  - [Cloud_Anchor_Integration](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Cloud_Anchor_Integration.md) handles localization.
  - [AR_Rendering](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/AR_Rendering.md) manages visual guidance.
  - [Data_Persistence](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Data_Persistence.md) provides local JSON storage.
  - [Pathfinding](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Pathfinding.md) uses Dijkstra for shortest path computation.
- **Styling:**
  - [UI_Theme](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/UI_Theme.md) provides Material 3 coloring and typography.

## Folder Responsibilities
- `app/src/main/java/.../ar`: AR Rendering, Pose Math, and Cloud Anchor Logic.
- `app/src/main/java/.../graph`: Pathfinding Logic & Data Models.
- `app/src/main/java/.../persistence`: Local File Repository.
- `app/src/main/java/.../ui`: Jetpack Compose Screens, ViewModels, and Theme.
- `app/src/main/res`: Android Resources (XML/Assets).
- `gradle`: Build Configuration & Dependency Management.
- `project-document`: High-level implementation and design docs.

## Feature Index
- [Project_Infrastructure](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Project_Infrastructure.md)
- [System_Audit_Documentation](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/System_Audit_Documentation.md)
- [Pathfinding](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Pathfinding.md)
- [Data_Persistence](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Data_Persistence.md)
- [Cloud_Anchor_Integration](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Cloud_Anchor_Integration.md)
- [AR_Rendering](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/AR_Rendering.md)
- [Mapping](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Mapping.md)
- [Navigation](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/Navigation.md)
- [UI_Components](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/UI_Components.md)
- [UI_Theme](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/features/UI_Theme.md)

## Coverage Summary
- **Total Folders:** 18
- **Total Files:** 63
- **Status:** COMPLETE
- **Mapped:** 35 files.
- **Unresolved:** 0 files.

---
*This map is a living document and has been finalized as of the last documentation pass.*
