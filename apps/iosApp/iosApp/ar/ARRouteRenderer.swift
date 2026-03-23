import Foundation
import RealityKit
import simd

/// Renders navigation arrows in the RealityKit scene with rolling
/// lookahead and fade-behind behavior.
///
/// Phase 3: Only a forward slice of arrows is shown. Passed arrows fade out.
/// Visibility is driven by the user's cumulative distance along the route.
class ARRouteRenderer {

    /// Arrow visibility state.
    enum ArrowState {
        case hidden      // too far ahead or fully faded
        case active      // within lookahead window
        case fading      // behind user, fading out
    }

    /// Root anchor for all navigation entities
    private var routeAnchor: AnchorEntity?

    /// All arrow entities, keyed by placement ID
    private var arrowEntities: [String: ModelEntity] = [:]

    /// All arrow placements (full route)
    private var allArrows: [ArrowPlacementData] = []

    /// Current visibility state per arrow id
    private var arrowStates: [String: ArrowState] = [:]

    // Alignment transform components
    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var rotationYRad: Double = 0.0

    // Rendering config from reviewed package
    private var lookaheadDistance: Double = 8.0
    private var fadeDistance: Double = 3.0

    /// Number of currently visible (active + fading) arrows
    var renderedArrowCount: Int {
        arrowStates.values.filter { $0 != .hidden }.count
    }

    /// Configure rendering parameters from the reviewed package.
    func configureRendering(
        lookaheadDistanceMeters: Double,
        fadeDistanceMeters: Double = 3.0
    ) {
        self.lookaheadDistance = lookaheadDistanceMeters
        self.fadeDistance = fadeDistanceMeters
    }

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

    /// Place all arrows into the AR scene (initially hidden).
    /// Call updateVisibility() afterward to show the initial forward slice.
    func placeAllArrows(in arView: ARView, arrows: [ArrowPlacementData]) {
        clearRoute(from: arView)

        self.allArrows = arrows
        let anchor = AnchorEntity(world: .zero)
        routeAnchor = anchor

        for arrow in arrows {
            let entity = createArrowEntity(for: arrow)

            let arPos = transformToAR(
                buildingX: arrow.positionX,
                buildingY: arrow.positionY,
                buildingZ: arrow.positionZ
            )
            entity.position = arPos

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

            // Start hidden
            entity.scale = .zero
            arrowStates[arrow.id] = .hidden

            anchor.addChild(entity)
            arrowEntities[arrow.id] = entity
        }

        arView.scene.addAnchor(anchor)
        print("[RouteRenderer] Placed \(arrows.count) arrows (all hidden initially)")
    }

    /// Update arrow visibility based on user's cumulative distance along the route.
    ///
    /// - Arrows within [userDistance, userDistance + lookahead] → active (full opacity)
    /// - Arrows within [userDistance - fadeDistance, userDistance] → fading
    /// - All other arrows → hidden
    func updateVisibility(userCumulativeDistance: Double) {
        let aheadLimit = userCumulativeDistance + lookaheadDistance
        let behindLimit = userCumulativeDistance - fadeDistance

        for arrow in allArrows {
            guard let entity = arrowEntities[arrow.id] else { continue }
            let dist = arrow.cumulativeDistance
            let oldState = arrowStates[arrow.id] ?? .hidden

            let newState: ArrowState
            if dist >= userCumulativeDistance && dist <= aheadLimit {
                newState = .active
            } else if dist >= behindLimit && dist < userCumulativeDistance {
                newState = .fading
            } else {
                newState = .hidden
            }

            // Only update entity if state changed
            if newState != oldState {
                applyState(newState, to: entity, arrowDistance: dist, userDistance: userCumulativeDistance)
                arrowStates[arrow.id] = newState
            } else if newState == .fading {
                // Update fade opacity continuously
                applyState(newState, to: entity, arrowDistance: dist, userDistance: userCumulativeDistance)
            }
        }
    }

    /// Hide all arrows (e.g. on arrival).
    func hideAllArrows() {
        for (id, entity) in arrowEntities {
            entity.scale = .zero
            arrowStates[id] = .hidden
        }
    }

    /// Remove all route entities from the scene.
    func clearRoute(from arView: ARView) {
        if let anchor = routeAnchor {
            arView.scene.removeAnchor(anchor)
        }
        routeAnchor = nil
        arrowEntities.removeAll()
        arrowStates.removeAll()
        allArrows.removeAll()
    }

    // MARK: - State Application

    private func applyState(_ state: ArrowState, to entity: ModelEntity, arrowDistance: Double, userDistance: Double) {
        switch state {
        case .active:
            entity.scale = SIMD3<Float>(1, 1, 1)
            setOpacity(entity, opacity: 1.0)

        case .fading:
            let behind = userDistance - arrowDistance
            let t = max(0, min(1, behind / fadeDistance))
            let opacity = Float(1.0 - t * 0.8) // fade from 1.0 down to 0.2
            let scale = Float(1.0 - t * 0.3)   // shrink slightly
            entity.scale = SIMD3<Float>(scale, scale, scale)
            setOpacity(entity, opacity: opacity)

        case .hidden:
            entity.scale = .zero
        }
    }

    private func setOpacity(_ entity: ModelEntity, opacity: Float) {
        guard var material = entity.model?.materials.first as? SimpleMaterial else { return }
        material.color.tint = material.color.tint.withAlphaComponent(CGFloat(opacity))
        entity.model?.materials = [material]
    }

    // MARK: - Entity Creation

    private func createArrowEntity(for arrow: ArrowPlacementData) -> ModelEntity {
        let entity: ModelEntity

        switch arrow.type {
        case .follow:
            let mesh = MeshResource.generateBox(size: [0.15, 0.05, 0.3], cornerRadius: 0.02)
            let material = SimpleMaterial(color: .systemBlue, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])

        case .turnLeft, .turnRight:
            let mesh = MeshResource.generateBox(size: [0.25, 0.08, 0.25], cornerRadius: 0.02)
            let material = SimpleMaterial(color: .systemYellow, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])

        case .uTurn:
            let mesh = MeshResource.generateBox(size: [0.3, 0.1, 0.3], cornerRadius: 0.02)
            let material = SimpleMaterial(color: .systemOrange, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])

        case .destination:
            let mesh = MeshResource.generateSphere(radius: 0.15)
            let material = SimpleMaterial(color: .systemGreen, roughness: 0.3, isMetallic: false)
            entity = ModelEntity(mesh: mesh, materials: [material])
        }

        entity.name = arrow.id
        return entity
    }

    // MARK: - Coordinate Transform

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
    /// Cumulative distance along the route polyline from the start to this arrow.
    let cumulativeDistance: Double
}

enum ArrowPlacementType {
    case follow
    case turnLeft
    case turnRight
    case uTurn
    case destination
}
