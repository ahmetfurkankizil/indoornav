# Vectura AI Project Diagrams

This document contains the Mermaid.js diagram codes that model the architecture and flows of the Vectura AI platform. You can render these diagrams in any markdown viewer that supports Mermaid (like GitHub, GitLab, or the Notion app).

## 1. Architecture Diagram
This diagram illustrates the macro-architecture of Vectura AI, showing how the Kotlin Multiplatform shared core interacts with native clients, and how the preprocessing backend feeds navigation data to the system.

```mermaid
graph TD
    subgraph WebInterfaces["Web Interfaces"]
        ManagerPanel["Web Manager Panel<br>(Map Authoring, Overrides, Nodes)"]
        AdminDashboard["DB Admin Dashboard<br>(System & Database Mgmt)"]
    end

    subgraph Clients["Mobile Clients (Runtime)"]
        iOS["iOS App (Swift, ARKit)"]
        Android["Android App (Compose, ARCore, ML Kit)"]
    end

    subgraph KMPCore["Kotlin Multiplatform Core (shared/)"]
        Core["shared/core<br>(Domain, Dijkstra Routing, Math)"]
        DS["shared/designsystem<br>(Compose UI Components)"]
    end

    subgraph Backend["Backend Services & Pipeline"]
        API["Backend API (Ktor)"]
        DB[("Database (SqlDelight)")]
        Prep["Nav Preprocessor (CLI)"]
    end

    %% Web Connections
    ManagerPanel -->|"Map Configurations"| API
    AdminDashboard -->|"System Operations"| API
    API --> DB

    %% Mobile Dependencies
    iOS --> Core
    Android --> Core
    Android --> DS

    %% Data Flow
    GLB[("3D Building Scans (.glb)")] --> Prep
    Prep -- "Draft Artifacts (JSON, SVG)" --> API
    API -- "Reviewed Package (JSON)" --> iOS
    API -- "Reviewed Package (JSON)" --> Android

    %% Runtime anchoring
    PhysicalMarker["Entrance Poster<br>(Unified QR/Image)"] -. "Scanned via Camera" .-> Clients
```

## 2. Use-Case Diagram
This diagram shows the primary actors (Visitor and Manager) and the core interactions they have with the Vectura AI platform.

```plantuml
@startuml
left to right direction

actor "Visitor" as Visitor
actor "Manager" as Manager

package "Vectura AI Mobile App" {
  usecase "Scan Entrance Poster" as UC1
  usecase "Select Destination" as UC2
  usecase "View Route Preview" as UC3
  usecase "Follow AR Guidance" as UC4
}

package "Vectura AI Manager Panel" {
  usecase "Upload 3D Map (GLB)" as UC5
  usecase "Review Draft Navigation" as UC6
  usecase "Edit Room Details" as UC7
  usecase "Export Reviewed Package" as UC8
}

Visitor --> UC1
Visitor --> UC2
Visitor --> UC3
Visitor --> UC4

Manager --> UC5
Manager --> UC6
Manager --> UC7
Manager --> UC8

UC4 .> UC1 : <<requires>>
UC4 .> UC2 : <<requires>>
UC6 .> UC5 : <<requires>>
UC8 .> UC6 : <<requires>>
@enduml
```

## 3. Sequence Diagram (AR Navigation Flow)
This diagram maps out the runtime interactions when a visitor initiates navigation, highlighting the delegation of work between the UI, native AR engines, and the shared Kotlin routing engine.

```mermaid
sequenceDiagram
    actor Visitor
    participant App as Mobile App (iOS/Android)
    participant AR as Native AR Engine (ARKit/ARCore)
    participant Core as Shared Core (Routing Engine)

    Visitor->>App: Point camera at Entrance Poster
    App->>AR: Start AR Session & Image Tracking
    AR-->>App: Recognize Marker (Transform Matrix)
    App->>Core: Initialize Spatial Transform (Local AR -> Building Space)
    
    Visitor->>App: Select Destination Room
    App->>Core: Request Path (StartNode to EndNode)
    Core-->>App: Return Path (Nodes, Edges, Distances)
    
    App->>App: Display Route Preview & ETA
    Visitor->>App: Confirm Start Navigation
    
    loop Every Frame
        AR-->>App: Current Camera Pose
        App->>Core: Transform Pose to Building Coordinates
        Core-->>App: Next Actions & Arrow Placements
        App->>AR: Render 3D Arrows at Anchors
        App-->>Visitor: Show AR Guidance & Next Turn Text
    end
    
    App-->>Visitor: Trigger Haptic Arrival Feedback
```

## 4. Activity Diagram (Map Preprocessing & Manager Flow)
This details the manager workflow for ingesting a raw 3D scan and refining it into a fully deployable navigation package.

```mermaid
stateDiagram-v2
    [*] --> Upload_GLB
    Upload_GLB --> Queue_Job : Manager posts GLB to Manager API
    Queue_Job --> Process_Draft : Background coroutine launches
    
    state Process_Draft {
        [*] --> Extract_Geometry
        Extract_Geometry --> Generate_Grid
        Generate_Grid --> Serialize_Graph
        Serialize_Graph --> [*]
    }
    
    Process_Draft --> Draft_Failed : If Invalid Geometry
    Draft_Failed --> [*]
    
    Process_Draft --> Draft_Succeeded : Draft Artifacts Saved
    
    Draft_Succeeded --> Review_Draft : Manager reviews map SVG and Nodes
    Review_Draft --> Edit_Rooms : Manager overrides names/categories
    Edit_Rooms --> Export_Package : Manager triggers export
    Export_Package --> Package_Ready : 5 JSON files produced
    
    Package_Ready --> [*] : Ready to bundle into Mobile App
```

## 5. UML Class Diagram (Core Domain Models)
This diagram illustrates the core data structures used in the Vectura AI platform to represent buildings, floors, and the navigation graph (nodes and edges).

```plantuml
@startuml
skinparam classAttributeIconSize 0
skinparam linetype ortho
skinparam nodesep 40
skinparam ranksep 50

package "Web & Admin Domain" {
  class BuildingResponse {
    +id: String
    +managerId: String
    +name: String
    +qrToken: String
    +widthMeters: Double
    +status: BuildingStatus
    +createdAt: DateTime
    +updatedAt: DateTime
    +addFloor(floor: FloorResponse): void
  }

  enum BuildingStatus {
    DRAFT
    PUBLISHED
    ARCHIVED
  }

  class FloorResponse {
    +id: String
    +floorNumber: Int
    +floorName: String
    +uploadStatus: UploadStatus
    +mapFileType: String
    +boundsMinX: Double
    +boundsMaxX: Double
    +calculateArea(): Double
  }

  enum UploadStatus {
    PENDING
    PROCESSING
    COMPLETED
    FAILED
  }

  class DraftJob {
    +jobId: String
    +status: String
    +artifactCount: Int
    +startedAt: DateTime
    +completedAt: DateTime
  }
}

package "NavGraph Domain" {
  class NodeResponse {
    +id: String
    +label: String
    +nodeType: NodeType
    +canvasX: Double
    +canvasY: Double
    +worldX: Double
    +worldZ: Double
    +metadata: JSON
  }

  enum NodeType {
    ROOM
    CORRIDOR
    STAIRS
    ELEVATOR
    ENTRANCE
  }

  class EdgeResponse {
    +id: String
    +isBidirectional: Boolean
    +edgeType: EdgeType
    +distance: Double
    +calculateDistance(): Double
  }

  enum EdgeType {
    WALK
    RAMP
    STAIRS
    ESCALATOR
  }

  class Waypoint {
    +x: Double
    +y: Double
  }

  class FloorConnectionResponse {
    +id: String
    +connectionType: String
    +isBidirectional: Boolean
  }
}

package "AI Suggestions Domain" {
  class AiSuggestRequest {
    +floorPlanImageBase64: String
    +validate(): Boolean
  }
  
  class AiNodeInput {
    +id: String
    +label: String
    +canvasX: Double
    +canvasY: Double
  }

  class SuggestedEdge {
    +confidence: Double
  }
  
  class AiSuggestResponse {
    +processSuggestions(): void
  }
}

package "Preprocessor Engine" {
  class DraftPipeline {
    +execute(job: DraftJob): Result
    -extractGeometry(): void
  }
  class FloorPlaneEstimator {
    +estimatePlanes(mesh: Mesh): List<Plane>
  }
}

BuildingResponse --> BuildingStatus
FloorResponse --> UploadStatus
BuildingResponse "1" *-- "*" FloorResponse : contains >
FloorResponse "1" *-- "*" NodeResponse : contains >
FloorResponse "1" *-- "*" EdgeResponse : contains >
BuildingResponse "1" *-- "*" FloorConnectionResponse : contains >

NodeResponse --> NodeType
EdgeResponse --> EdgeType
EdgeResponse "1" *-- "*" Waypoint : contains >

EdgeResponse --> NodeResponse : fromNodeId
EdgeResponse --> NodeResponse : toNodeId
FloorConnectionResponse --> NodeResponse : fromNodeId
FloorConnectionResponse --> NodeResponse : toNodeId

AiSuggestRequest "1" *-- "*" AiNodeInput : includes >
AiSuggestResponse "1" *-- "*" SuggestedEdge : returns >
AiSuggestResponse "1" *-- "*" AiNodeInput : returns >
SuggestedEdge --> AiNodeInput : connects

DraftJob --> DraftPipeline : processed by
DraftPipeline --> FloorPlaneEstimator : utilizes
@enduml
```
