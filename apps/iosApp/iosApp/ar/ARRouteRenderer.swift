import Foundation
import RealityKit
import simd

/// Renders navigation arrows in the RealityKit scene.
///
/// Takes arrow placements in building-local coordinates, applies
/// the alignment transform, and creates/updates RealityKit entities.
///
/// Arrow geometry: simple colored boxes for v1.
/// Future: replace with proper 3D arrow models.
class ARRouteRenderer {
    
    /// Root anchor for all navigation entities
    private var routeAnchor: AnchorEntity?
    
    /// Currently rendered arrow entities, keyed by placement ID
    private var arrowEntities: [String: ModelEntity] = [:]
    
    // Alignment transform components
    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var rotationYRad: Double = 0.0
    
    /// Debug: number of arrows currently rendered
    var renderedArrowCount: Int { arrowEntities.count }
    
    /// Set the alignment transform (building-local → AR world).
    func setAlignmentTransform(
        offsetX: Double, offsetY: Double, offsetZ: Double,
        rotationYDeg: Double
    ) {
        self.offsetX = offsetX
        self.offsetY = offsetY
        self.offsetZ = offsetZ
        self.rotationYRad = rotationYDeg * .pi / 180.0
    }
    
    /// Render arrow placements into the AR scene.
    /// - Parameters:
    ///   - arView: The RealityKit ARView
    ///   - arrows: Arrow placements in building-local coords
    func renderRoute(in arView: ARView, arrows: [ArrowPlacementData]) {
        // Remove previous route
        clearRoute(from: arView)
        
        // Create root anchor at world origin
        let anchor = AnchorEntity(world: .zero)
        routeAnchor = anchor
        
        for arrow in arrows {
            let entity = createArrowEntity(for: arrow)
            
            // Transform building-local position to AR world
            let arPos = transformToAR(
                buildingX: arrow.positionX,
                buildingY: arrow.positionY,
                buildingZ: arrow.positionZ
            )
            entity.position = arPos
            
            // Rotate entity to face forward direction
            let arDir = transformDirectionToAR(
                dx: arrow.forwardDx,
                dy: arrow.forwardDy,
                dz: arrow.forwardDz
            )
            if simd_length(arDir) > 0.001 {
                let forward = simd_normalize(arDir)
                let angle = atan2(forward.x, forward.z)
                entity.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])
            }
            
            anchor.addChild(entity)
            arrowEntities[arrow.id] = entity
        }
        
        arView.scene.addAnchor(anchor)
        print("[RouteRenderer] Rendered \(arrows.count) arrows")
    }
    
    /// Remove all route entities from the scene.
    func clearRoute(from arView: ARView) {
        if let anchor = routeAnchor {
            arView.scene.removeAnchor(anchor)
        }
        routeAnchor = nil
        arrowEntities.removeAll()
    }
    
    // MARK: - Entity Creation
    
    private func createArrowEntity(for arrow: ArrowPlacementData) -> ModelEntity {
        let entity: ModelEntity
        
        switch arrow.type {
        case .follow:
            // Blue arrow box
            let mesh = MeshResource.generateBox(size: [0.15, 0.05, 0.3], cornerRadius: 0.02)
            let material = SimpleMaterial(color: .systemBlue, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])
            
        case .turnLeft, .turnRight:
            // Yellow turn marker (wider box)
            let mesh = MeshResource.generateBox(size: [0.25, 0.08, 0.25], cornerRadius: 0.02)
            let material = SimpleMaterial(color: .systemYellow, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])
            
        case .uTurn:
            // Orange U-turn marker
            let mesh = MeshResource.generateBox(size: [0.3, 0.1, 0.3], cornerRadius: 0.02)
            let material = SimpleMaterial(color: .systemOrange, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])
            
        case .destination:
            // Green destination sphere
            let mesh = MeshResource.generateSphere(radius: 0.15)
            let material = SimpleMaterial(color: .systemGreen, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])
        }
        
        entity.name = arrow.id
        return entity
    }
    
    // MARK: - Coordinate Transform
    
    /// Transform building-local point to AR world coordinates.
    private func transformToAR(buildingX: Double, buildingY: Double, buildingZ: Double) -> SIMD3<Float> {
        let cosR = cos(rotationYRad)
        let sinR = sin(rotationYRad)
        
        let rotatedX = buildingX * cosR + buildingZ * sinR
        let rotatedZ = -buildingX * sinR + buildingZ * cosR
        
        return SIMD3<Float>(
            Float(rotatedX + offsetX),
            Float(buildingY + offsetY),
            Float(rotatedZ + offsetZ)
        )
    }
    
    /// Transform direction vector (no translation).
    private func transformDirectionToAR(dx: Double, dy: Double, dz: Double) -> SIMD3<Float> {
        let cosR = cos(rotationYRad)
        let sinR = sin(rotationYRad)
        
        return SIMD3<Float>(
            Float(dx * cosR + dz * sinR),
            Float(dy),
            Float(-dx * sinR + dz * cosR)
        )
    }
}

// MARK: - Data Transfer Types

/// Swift-side arrow placement data (mirrors shared ArrowPlacement).
struct ArrowPlacementData {
    let id: String
    let positionX: Double
    let positionY: Double
    let positionZ: Double
    let forwardDx: Double
    let forwardDy: Double
    let forwardDz: Double
    let type: ArrowPlacementType
    let label: String?
}

enum ArrowPlacementType {
    case follow
    case turnLeft
    case turnRight
    case uTurn
    case destination
}
