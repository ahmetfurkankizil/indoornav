import Foundation
import UIKit
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
    private var arrowEntities: [String: Entity] = [:]

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
    private var fadeDistance: Double = 1.5
    private var arrowHeightOffset: Double = 0.05
    private var arrowFadeInProgress: [String: Float] = [:]

    private let fadeInFrames: Float = 10.0

    /// Number of currently visible (active + fading) arrows
    var renderedArrowCount: Int {
        arrowStates.values.filter { $0 != .hidden }.count
    }

    /// Configure rendering parameters from the reviewed package.
    func configureRendering(
        lookaheadDistanceMeters: Double,
        fadeDistanceMeters: Double = 1.5,
        arrowHeightOffsetMeters: Double = 0.05
    ) {
        self.lookaheadDistance = lookaheadDistanceMeters
        self.fadeDistance = fadeDistanceMeters
        self.arrowHeightOffset = arrowHeightOffsetMeters
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
        arrowFadeInProgress.removeAll()

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
                let angle = atan2(forward.x, forward.z) + .pi
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

            if newState != .hidden {
                let current = arrowFadeInProgress[arrow.id] ?? 0
                arrowFadeInProgress[arrow.id] = min(fadeInFrames, current + 1)
            } else if oldState != .hidden {
                arrowFadeInProgress.removeValue(forKey: arrow.id)
            }

            applyState(newState, to: entity, arrow: arrow, arrowDistance: dist, userDistance: userCumulativeDistance)
            arrowStates[arrow.id] = newState
        }
    }

    /// Update arrow visibility and cull guidance that is well behind the camera.
    func updateVisibility(
        userCumulativeDistance: Double,
        cameraWorldX: Float,
        cameraWorldZ: Float,
        cameraForwardX: Float,
        cameraForwardZ: Float
    ) {
        let aheadLimit = userCumulativeDistance + lookaheadDistance
        let behindLimit = userCumulativeDistance - fadeDistance

        for arrow in allArrows {
            guard let entity = arrowEntities[arrow.id] else { continue }
            let dist = arrow.cumulativeDistance
            let oldState = arrowStates[arrow.id] ?? .hidden

            var newState: ArrowState
            if dist >= userCumulativeDistance && dist <= aheadLimit {
                newState = .active
            } else if dist >= behindLimit && dist < userCumulativeDistance {
                newState = .fading
            } else {
                newState = .hidden
            }

            if newState != .hidden {
                let pos = transformToAR(
                    buildingX: arrow.positionX,
                    buildingY: arrow.positionY,
                    buildingZ: arrow.positionZ
                )
                let toArrowX = pos.x - cameraWorldX
                let toArrowZ = pos.z - cameraWorldZ
                let dot = toArrowX * cameraForwardX + toArrowZ * cameraForwardZ
                if dot < -0.5 {
                    newState = .hidden
                }
            }

            if newState != .hidden {
                let current = arrowFadeInProgress[arrow.id] ?? 0
                arrowFadeInProgress[arrow.id] = min(fadeInFrames, current + 1)
            } else if oldState != .hidden {
                arrowFadeInProgress.removeValue(forKey: arrow.id)
            }

            applyState(newState, to: entity, arrow: arrow, arrowDistance: dist, userDistance: userCumulativeDistance)
            arrowStates[arrow.id] = newState
        }
    }

    /// Hide all arrows (e.g. on arrival).
    func hideAllArrows() {
        for (id, entity) in arrowEntities {
            entity.scale = .zero
            arrowStates[id] = .hidden
        }
        arrowFadeInProgress.removeAll()
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
        arrowFadeInProgress.removeAll()
    }

    // MARK: - State Application

    private func applyState(_ state: ArrowState, to entity: Entity, arrow: ArrowPlacementData, arrowDistance: Double, userDistance: Double) {
        switch state {
        case .active:
            let fadeIn = (arrowFadeInProgress[arrow.id] ?? fadeInFrames) / fadeInFrames
            entity.scale = baseScale(for: arrow.type) * fadeIn
            setOpacity(entity, opacity: fadeIn)

        case .fading:
            let behind = userDistance - arrowDistance
            let t = max(0, min(1, behind / fadeDistance))
            let fadeIn = (arrowFadeInProgress[arrow.id] ?? fadeInFrames) / fadeInFrames
            let opacity = Float(1.0 - t * 0.8) * fadeIn
            let scale = Float(1.0 - t * 0.3) * fadeIn
            entity.scale = baseScale(for: arrow.type) * scale
            setOpacity(entity, opacity: opacity)

        case .hidden:
            entity.scale = .zero
        }
    }

    private func setOpacity(_ entity: Entity, opacity: Float) {
        if let model = entity as? ModelEntity,
           var material = model.model?.materials.first as? SimpleMaterial {
            let alpha = entity.name.hasSuffix("-shadow") ? opacity * 0.22 : opacity
            material.color.tint = material.color.tint.withAlphaComponent(CGFloat(alpha))
            model.model?.materials = [material]
        }
        for child in entity.children {
            setOpacity(child, opacity: opacity)
        }
    }

    // MARK: - Entity Creation

    private func createArrowEntity(for arrow: ArrowPlacementData) -> Entity {
        switch arrow.type {
        case .follow:
            return makeChevronArrow(name: arrow.id, color: UIColor(red: 0.086, green: 0.545, blue: 1.0, alpha: 1.0))

        case .turnLeft, .turnRight:
            return makeChevronArrow(name: arrow.id, color: UIColor(red: 0.98, green: 0.80, blue: 0.08, alpha: 1.0), isTurn: true)

        case .uTurn:
            return makeChevronArrow(name: arrow.id, color: .systemOrange, isTurn: true)

        case .destination:
            let mesh = MeshResource.generateSphere(radius: 0.15)
            let material = SimpleMaterial(color: .systemGreen, roughness: 0.3, isMetallic: false)
            let entity = ModelEntity(mesh: mesh, materials: [material])
            entity.name = arrow.id
            return entity
        }
    }

    private func makeChevronArrow(name: String, color: UIColor, isTurn: Bool = false) -> Entity {
        let root = Entity()
        root.name = name

        let core = makeChevronLineSet(
            name: "\(name)-core",
            color: color,
            width: 0.055,
            height: 0.018,
            length: isTurn ? 0.58 : 0.50,
            spread: isTurn ? 0.28 : 0.24
        )
        core.position = [0, 0.015, 0]
        root.addChild(core)

        let glow = makeChevronLineSet(
            name: "\(name)-glow",
            color: color.withAlphaComponent(0.32),
            width: 0.11,
            height: 0.01,
            length: isTurn ? 0.64 : 0.56,
            spread: isTurn ? 0.32 : 0.28
        )
        glow.position = [0, 0.006, 0]
        root.addChild(glow)

        return root
    }

    private func makeChevronLineSet(
        name: String,
        color: UIColor,
        width: Float,
        height: Float,
        length: Float,
        spread: Float
    ) -> Entity {
        let root = Entity()
        root.name = name
        let material = SimpleMaterial(color: color, roughness: 0.12, isMetallic: false)
        let segmentLength = sqrt(length * length + spread * spread)
        let mesh = MeshResource.generateBox(size: [width, height, segmentLength], cornerRadius: width * 0.45)

        let left = ModelEntity(mesh: mesh, materials: [material])
        left.position = [-spread / 2.0, 0, 0]
        left.orientation = simd_quatf(angle: -.pi / 4.0, axis: [0, 1, 0])
        root.addChild(left)

        let right = ModelEntity(mesh: mesh, materials: [material])
        right.position = [spread / 2.0, 0, 0]
        right.orientation = simd_quatf(angle: .pi / 4.0, axis: [0, 1, 0])
        root.addChild(right)

        return root
    }

    private func baseScale(for type: ArrowPlacementType) -> SIMD3<Float> {
        switch type {
        case .follow:
            return SIMD3<Float>(1.0, 1.0, 1.0)
        case .turnLeft, .turnRight:
            return SIMD3<Float>(1.18, 1.18, 1.18)
        case .uTurn:
            return SIMD3<Float>(1.25, 1.25, 1.25)
        case .destination:
            return SIMD3<Float>(1.0, 1.0, 1.0)
        }
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
