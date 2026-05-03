import Foundation
import UIKit
import RealityKit
import Combine
import simd
import QuartzCore

// MARK: - Visual design tokens (Aurora Lane)
//
// The AR world layer uses a minimal "two parallel rails + compact glowing
// chevrons + beacon at destination" language. Everything is an UnlitMaterial
// so the neon surfaces survive ARKit's environment lighting.
// Tokens live here — the renderer reads them but never mutates them.

private enum VecturARPalette {
    /// Primary neon accent — electric aurora cyan.
    static let auroraCore  = UIColor(red: 0.247, green: 0.816, blue: 0.961, alpha: 1.0)   // #3FD0F5
    /// Slightly cooler shade — used for the wide halo bloom.
    static let auroraHalo  = UIColor(red: 0.184, green: 0.729, blue: 0.906, alpha: 1.0)   // #2FBAE7
    /// Warm-white arrival tint.
    static let arrivalGlow = UIColor(red: 0.88, green: 0.97, blue: 1.0, alpha: 1.0)
}

private enum VecturARGeometry {
    // Path plane offsets — everything hugs the floor.
    static let sideRailHeightOffset: Float = 0.015
    static let turnRingHeightOffset: Float = 0.010

    // Two parallel side rails define the corridor (no center stripe).
    static let sideRailHalfWidth:  Float = 0.34
    static let sideRailCoreWidth:  Float = 0.038
    static let sideRailCoreHeight: Float = 0.014
    static let sideRailHaloWidth:  Float = 0.105
    static let sideRailHaloHeight: Float = 0.006
    static let sideRailChunkLen:   Float = 0.48

    // Compact, minimal chevron arrows.
    static let chevronArmLength:  Float = 0.21
    static let chevronSpread:     Float = 0.18
    static let chevronCoreWidth:  Float = 0.034
    static let chevronMidWidth:   Float = 0.072
    static let chevronHaloWidth:  Float = 0.128
    static let chevronCoreHeight: Float = 0.022
    static let chevronMidHeight:  Float = 0.013
    static let chevronHaloHeight: Float = 0.006

    // Turn anticipation ring.
    static let turnRingRadius:    Float = 0.42
    static let turnRingThickness: Float = 0.022

    // Destination beacon.
    static let beaconRingRadius:    Float = 0.38
    static let beaconRingThickness: Float = 0.045
    static let beaconPillarHeight:  Float = 0.80
    static let beaconPillarWidth:   Float = 0.040
    static let beaconHaloRadius:    Float = 0.60
}

/// Layer-kind suffixes on child entity names let the opacity walker apply
/// per-layer alpha multipliers (halo < mid < core) from a single scalar
/// while keeping material colors untouched.
private enum VecturARLayer {
    static let halo   = "-halo"
    static let mid    = "-mid"
    static let core   = "-core"
    static let ring   = "-ring"
    static let pillar = "-pillar"

    /// Alpha multiplier a layer receives when its parent chain is fully
    /// opaque. Halo/mid values are intentionally low so the runway and
    /// chevrons read as glowing-but-matte surfaces rather than neon bloom.
    static func multiplier(for name: String) -> Float {
        if name.hasSuffix(halo)   { return 0.22 }
        if name.hasSuffix(mid)    { return 0.55 }
        if name.hasSuffix(core)   { return 1.00 }
        if name.hasSuffix(ring)   { return 0.85 }
        if name.hasSuffix(pillar) { return 0.70 }
        return 1.0
    }
}

// MARK: - Material helpers

private enum VecturARMaterial {
    /// Unlit emissive-looking material — survives AR environment lighting
    /// so neon surfaces keep reading as "light", not paint.
    static func unlit(color: UIColor, opacity: Float = 1.0) -> UnlitMaterial {
        var mat = UnlitMaterial()
        mat.color = .init(tint: color)
        mat.blending = .transparent(
            opacity: PhysicallyBasedMaterial.Opacity(floatLiteral: max(0, min(1, opacity)))
        )
        return mat
    }
}

// MARK: - Chevron factory (follow / turn)

/// Builds the layered chevron arrow used along the route.
///
/// Stack:
///   1. `-halo`  wide faint bloom
///   2. `-mid`   mid-width soft body
///   3. `-core`  crisp bright centerline (white)
///
/// Each arrow is a V composed of two rounded boxes rotated ±45°.
private enum ARChevronFactory {

    static func make(name: String, emphasis: Emphasis) -> Entity {
        let root = Entity()
        root.name = name

        let scale = emphasis.scale

        // Halo — widest, low alpha
        let halo = buildV(
            name: "\(name)\(VecturARLayer.halo)",
            width: VecturARGeometry.chevronHaloWidth * scale,
            height: VecturARGeometry.chevronHaloHeight,
            length: VecturARGeometry.chevronArmLength * (emphasis == .turn ? 1.15 : 1.0),
            spread: VecturARGeometry.chevronSpread   * (emphasis == .turn ? 1.15 : 1.0),
            color: VecturARPalette.auroraHalo
        )
        halo.position = [0, 0.004, 0]
        root.addChild(halo)

        // Mid — colored soft body
        let mid = buildV(
            name: "\(name)\(VecturARLayer.mid)",
            width: VecturARGeometry.chevronMidWidth * scale,
            height: VecturARGeometry.chevronMidHeight,
            length: VecturARGeometry.chevronArmLength * (emphasis == .turn ? 1.10 : 1.0),
            spread: VecturARGeometry.chevronSpread   * (emphasis == .turn ? 1.10 : 1.0),
            color: VecturARPalette.auroraCore
        )
        mid.position = [0, 0.010, 0]
        root.addChild(mid)

        // Core — crisp white centerline
        let core = buildV(
            name: "\(name)\(VecturARLayer.core)",
            width: VecturARGeometry.chevronCoreWidth * scale,
            height: VecturARGeometry.chevronCoreHeight,
            length: VecturARGeometry.chevronArmLength,
            spread: VecturARGeometry.chevronSpread,
            color: UIColor.white
        )
        core.position = [0, 0.018, 0]
        root.addChild(core)

        return root
    }

    /// Visual emphasis — follow arrows are unobtrusive, turn arrows read
    /// larger to match the extra attention the user needs.
    enum Emphasis {
        case follow
        case turn

        var scale: Float {
            switch self {
            case .follow: return 1.0
            case .turn:   return 1.25
            }
        }
    }

    private static func buildV(
        name: String,
        width: Float,
        height: Float,
        length: Float,
        spread: Float,
        color: UIColor
    ) -> Entity {
        let root = Entity()
        root.name = name

        let mat = VecturARMaterial.unlit(color: color)
        let segmentLength = sqrt(length * length + spread * spread)
        let mesh = MeshResource.generateBox(
            size: [width, height, segmentLength],
            cornerRadius: width * 0.48
        )

        let left = ModelEntity(mesh: mesh, materials: [mat])
        left.name = "\(name)-left"
        left.position = [-spread / 2.0, 0, 0]
        left.orientation = simd_quatf(angle: -.pi / 4.0, axis: [0, 1, 0])
        root.addChild(left)

        let right = ModelEntity(mesh: mesh, materials: [mat])
        right.name = "\(name)-right"
        right.position = [spread / 2.0, 0, 0]
        right.orientation = simd_quatf(angle: .pi / 4.0, axis: [0, 1, 0])
        root.addChild(right)

        return root
    }
}

// MARK: - Side rail factory (parallel lane line)

/// Thin lane line. Two of these placed at ±sideRailHalfWidth form the
/// complete path — there is no center stripe. Built at unit length
/// (`sideRailChunkLen`) and scaled along Z per chunk.
private enum ARSideRailFactory {

    static func make(name: String) -> Entity {
        let root = Entity()
        root.name = name

        // Halo — low alpha soft band
        let haloMesh = MeshResource.generateBox(
            size: [VecturARGeometry.sideRailHaloWidth,
                   VecturARGeometry.sideRailHaloHeight,
                   VecturARGeometry.sideRailChunkLen],
            cornerRadius: VecturARGeometry.sideRailHaloHeight * 0.5
        )
        let halo = ModelEntity(
            mesh: haloMesh,
            materials: [VecturARMaterial.unlit(color: VecturARPalette.auroraHalo)]
        )
        halo.name = "\(name)\(VecturARLayer.halo)"
        halo.position = [0, VecturARGeometry.sideRailHaloHeight * 0.5, 0]
        root.addChild(halo)

        // Core — crisp bright centerline of the rail
        let coreMesh = MeshResource.generateBox(
            size: [VecturARGeometry.sideRailCoreWidth,
                   VecturARGeometry.sideRailCoreHeight,
                   VecturARGeometry.sideRailChunkLen],
            cornerRadius: VecturARGeometry.sideRailCoreHeight * 0.5
        )
        let core = ModelEntity(
            mesh: coreMesh,
            materials: [VecturARMaterial.unlit(color: VecturARPalette.auroraCore)]
        )
        core.name = "\(name)\(VecturARLayer.core)"
        core.position = [0, VecturARGeometry.sideRailCoreHeight * 0.5 + 0.005, 0]
        root.addChild(core)

        return root
    }
}

// MARK: - Ring segment helper

/// Builds a single segment of a flat ring from a thin rounded box. Multiple
/// of these, rotated around Y, approximate an annulus without requiring
/// custom mesh generation at runtime.
private func makeRingSegment(
    name: String,
    radius: Float,
    thickness: Float,
    segAngle: Float,
    segHeight: Float,
    color: UIColor
) -> ModelEntity {
    let segLen = radius * segAngle * 1.06
    let mesh = MeshResource.generateBox(
        size: [thickness, segHeight, segLen],
        cornerRadius: thickness * 0.45
    )
    let entity = ModelEntity(
        mesh: mesh,
        materials: [VecturARMaterial.unlit(color: color)]
    )
    entity.name = name
    return entity
}

// MARK: - Turn anticipation ring

/// A glowing halo drawn on the floor at the turn pivot a few meters
/// before the turn chevron, so the user recognizes the maneuver before
/// they reach it. Pulses via scale in the renderer.
private enum ARTurnRingFactory {

    static func make(name: String, isRight: Bool) -> Entity {
        let root = Entity()
        root.name = name

        let segments = 28
        let segAngle = 2.0 * Float.pi / Float(segments)

        for i in 0..<segments {
            let angle = Float(i) * segAngle
            let seg = makeRingSegment(
                name: "\(name)\(VecturARLayer.ring)-\(i)",
                radius: VecturARGeometry.turnRingRadius,
                thickness: VecturARGeometry.turnRingThickness,
                segAngle: segAngle,
                segHeight: 0.009,
                color: VecturARPalette.auroraCore
            )
            seg.position = [
                VecturARGeometry.turnRingRadius * cos(angle),
                VecturARGeometry.turnRingHeightOffset,
                VecturARGeometry.turnRingRadius * sin(angle)
            ]
            seg.orientation = simd_quatf(angle: -angle, axis: [0, 1, 0])
            root.addChild(seg)
        }

        // Directional hint inside the ring showing the turn side.
        let chev = ARChevronFactory.make(name: "\(name)-hint", emphasis: .follow)
        let rot: Float = isRight ? -.pi / 2.0 : .pi / 2.0
        chev.orientation = simd_quatf(angle: rot, axis: [0, 1, 0])
        chev.position = [0, 0.02, 0]
        chev.scale = SIMD3<Float>(repeating: 0.62)
        root.addChild(chev)

        return root
    }
}

// MARK: - Destination beacon
//
// Multi-element beacon:
//   - wide floor halo ring (soft presence)
//   - crisp cyan ring (pinpoint)
//   - short vertical pillar of light (spatial anchor)
//
// On arrival the renderer swaps the color to `arrivalGlow` and scales up.
private enum ARDestinationBeaconFactory {

    static func make(name: String) -> Entity {
        let root = Entity()
        root.name = name

        // Floor halo (wide, soft)
        let segmentsHalo = 36
        let segAngleHalo = 2.0 * Float.pi / Float(segmentsHalo)
        for i in 0..<segmentsHalo {
            let angle = Float(i) * segAngleHalo
            let seg = makeRingSegment(
                name: "\(name)\(VecturARLayer.halo)-\(i)",
                radius: VecturARGeometry.beaconHaloRadius,
                thickness: 0.09,
                segAngle: segAngleHalo,
                segHeight: 0.009,
                color: VecturARPalette.auroraHalo
            )
            seg.position = [
                VecturARGeometry.beaconHaloRadius * cos(angle),
                0.008,
                VecturARGeometry.beaconHaloRadius * sin(angle)
            ]
            seg.orientation = simd_quatf(angle: -angle, axis: [0, 1, 0])
            root.addChild(seg)
        }

        // Pinpoint ring (crisp)
        let segmentsRing = 32
        let segAngleRing = 2.0 * Float.pi / Float(segmentsRing)
        for i in 0..<segmentsRing {
            let angle = Float(i) * segAngleRing
            let seg = makeRingSegment(
                name: "\(name)\(VecturARLayer.ring)-\(i)",
                radius: VecturARGeometry.beaconRingRadius,
                thickness: VecturARGeometry.beaconRingThickness,
                segAngle: segAngleRing,
                segHeight: 0.010,
                color: VecturARPalette.auroraCore
            )
            seg.position = [
                VecturARGeometry.beaconRingRadius * cos(angle),
                0.016,
                VecturARGeometry.beaconRingRadius * sin(angle)
            ]
            seg.orientation = simd_quatf(angle: -angle, axis: [0, 1, 0])
            root.addChild(seg)
        }

        // Vertical pillar (crisp white) + colored glow sleeve.
        let pillarMesh = MeshResource.generateBox(
            size: [VecturARGeometry.beaconPillarWidth,
                   VecturARGeometry.beaconPillarHeight,
                   VecturARGeometry.beaconPillarWidth],
            cornerRadius: VecturARGeometry.beaconPillarWidth * 0.5
        )
        let pillar = ModelEntity(
            mesh: pillarMesh,
            materials: [VecturARMaterial.unlit(color: UIColor.white)]
        )
        pillar.name = "\(name)\(VecturARLayer.pillar)"
        pillar.position = [0, VecturARGeometry.beaconPillarHeight * 0.5 + 0.02, 0]
        root.addChild(pillar)

        let sleeveMesh = MeshResource.generateBox(
            size: [VecturARGeometry.beaconPillarWidth * 2.4,
                   VecturARGeometry.beaconPillarHeight * 0.98,
                   VecturARGeometry.beaconPillarWidth * 2.4],
            cornerRadius: VecturARGeometry.beaconPillarWidth * 1.2
        )
        let sleeve = ModelEntity(
            mesh: sleeveMesh,
            materials: [VecturARMaterial.unlit(color: VecturARPalette.auroraCore, opacity: 0.7)]
        )
        sleeve.name = "\(name)\(VecturARLayer.halo)-pillar"
        sleeve.position = [0, VecturARGeometry.beaconPillarHeight * 0.5 + 0.02, 0]
        root.addChild(sleeve)

        return root
    }
}

// MARK: - AR Route Renderer
//
// Orchestrates placement of every guidance entity (side rails, chevron
// arrows, turn anticipation rings, destination beacon) and drives the
// rolling lookahead / fade-behind window based on the user's cumulative
// distance along the route.

class ARRouteRenderer {

    /// Visibility state for any guidance entity (arrow / rail chunk / ring).
    enum GuidanceState {
        case hidden      // too far ahead or fully faded
        case active      // within lookahead window
        case fading      // behind user, fading out
    }

    /// Root anchor for all navigation entities.
    private var routeAnchor: AnchorEntity?

    // Arrow placement entities
    private var arrowEntities: [String: Entity] = [:]
    private var allArrows: [ArrowPlacementData] = []
    private var arrowStates: [String: GuidanceState] = [:]
    private var arrowFadeInStart: [String: CFTimeInterval] = [:]

    // Side rail entities (two per subdivision — left + right).
    private var railEntities: [String: Entity] = [:]
    private var allRailChunks: [RailChunkData] = []
    private var railStates: [String: GuidanceState] = [:]
    private var railFadeInStart: [String: CFTimeInterval] = [:]

    // Turn anticipation rings (keyed by parent arrow id)
    private var turnRings: [String: Entity] = [:]

    // Destination beacon — distinguished for arrival updates.
    private weak var destinationEntity: Entity?

    // Alignment transform components
    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var rotationYRad: Double = 0.0

    // Rendering config from reviewed package
    private var lookaheadDistance: Double = 8.0
    private var fadeDistance: Double = 1.5
    private var arrowHeightOffset: Double = 0.05

    /// Wall-clock duration of the reveal animation for a newly visible
    /// entity. Driven by a per-frame scene subscription (not the 2 Hz
    /// pose timer) so growth reads as a smooth ramp rather than chunky
    /// 20%/step pop-ins that look like frame drops.
    private let fadeInDuration: CFTimeInterval = 0.55

    /// Per-frame animation ticker. Subscribed in `placeRoute` so fade-in
    /// interpolates at the display refresh rate.
    private var sceneUpdateSubscription: Cancellable?

    /// Most recent user-cumulative distance fed into `applyVisibility`,
    /// cached so the per-frame tick can re-apply `.fading` math without
    /// needing a fresh pose sample.
    private var lastUserCumulativeDistance: Double = 0.0

    /// Distance range (in meters along the route) within which a turn-ring
    /// should be rendered for its parent arrow.
    private let turnRingPreviewDistance: Double = 4.0

    /// Number of currently visible (active + fading) arrows.
    var renderedArrowCount: Int {
        arrowStates.values.filter { $0 != .hidden }.count
    }

    // MARK: - Configuration

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

    // MARK: - Placement

    /// Place all guidance (side rails + arrows + turn rings + destination
    /// beacon) into the AR scene. Everything starts hidden; call
    /// `updateVisibility(...)` after to reveal the forward slice.
    func placeRoute(
        in arView: ARView,
        arrows: [ArrowPlacementData],
        routePoints: [(Double, Double)]
    ) {
        clearRoute(from: arView)

        let anchor = AnchorEntity(world: .zero)
        routeAnchor = anchor

        placeSideRails(routePoints: routePoints, into: anchor)
        placeArrowEntities(arrows: arrows, into: anchor)
        placeTurnRings(arrows: arrows, into: anchor)

        arView.scene.addAnchor(anchor)

        // Drive the reveal animation at the display refresh rate so entities
        // ramp in smoothly between pose updates. Pose ticks only flip the
        // GuidanceState; this subscription interpolates scale/opacity.
        sceneUpdateSubscription?.cancel()
        sceneUpdateSubscription = arView.scene.subscribe(to: SceneEvents.Update.self) { [weak self] _ in
            self?.tickFadeInAnimations()
        }

        print("[RouteRenderer] Placed \(arrows.count) arrows and \(allRailChunks.count) rail chunks across \(routePoints.count) route points")
    }

    /// Backwards-compatible shim — places arrows only, with no side rails.
    /// Prefer `placeRoute(in:arrows:routePoints:)` for the full visual.
    func placeAllArrows(in arView: ARView, arrows: [ArrowPlacementData]) {
        placeRoute(in: arView, arrows: arrows, routePoints: [])
    }

    // MARK: - Sub-placements

    private func placeArrowEntities(arrows: [ArrowPlacementData], into anchor: AnchorEntity) {
        self.allArrows = arrows
        arrowFadeInStart.removeAll()

        for arrow in arrows {
            let entity = createArrowEntity(for: arrow)

            // Force every guidance entity onto the same floor plane. The
            // original `arrow.positionY` from the authoring pipeline is
            // ignored on purpose so arrows never hover at a different
            // height than the rails.
            let floorY = floorBuildingY(for: arrow.type)
            let arPos = transformToAR(
                buildingX: arrow.positionX,
                buildingY: floorY,
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

            // Turn chevrons get a subtle yaw toward the turn direction so
            // the V visibly leans into the maneuver instead of pointing
            // straight ahead like a follow arrow.
            switch arrow.type {
            case .turnLeft:
                entity.orientation = entity.orientation * simd_quatf(angle: .pi / 5, axis: [0, 1, 0])
            case .turnRight:
                entity.orientation = entity.orientation * simd_quatf(angle: -.pi / 5, axis: [0, 1, 0])
            case .uTurn:
                entity.orientation = entity.orientation * simd_quatf(angle: .pi, axis: [0, 1, 0])
            default:
                break
            }

            entity.scale = .zero
            arrowStates[arrow.id] = .hidden

            anchor.addChild(entity)
            arrowEntities[arrow.id] = entity
        }
    }

    /// Returns the building-local Y at which a given guidance type should
    /// sit. Everything hugs the floor plane.
    private func floorBuildingY(for type: ArrowPlacementType) -> Double {
        switch type {
        case .follow, .turnLeft, .turnRight, .uTurn:
            return arrowHeightOffset
        case .destination:
            return 0
        }
    }

    /// Place two parallel side rails along the route polyline. No center
    /// stripe — the two rails alone define the corridor edge.
    private func placeSideRails(routePoints: [(Double, Double)], into anchor: AnchorEntity) {
        railFadeInStart.removeAll()
        let chunks = buildRailChunks(routePoints: routePoints)
        self.allRailChunks = chunks

        for chunk in chunks {
            let entity = ARSideRailFactory.make(name: chunk.id)

            let arPos = transformToAR(
                buildingX: chunk.midX,
                buildingY: Double(VecturARGeometry.sideRailHeightOffset),
                buildingZ: chunk.midZ
            )
            entity.position = arPos

            let dirAR = transformDirectionToAR(dx: chunk.dirX, dy: 0, dz: chunk.dirZ)
            if simd_length(dirAR) > 0.001 {
                let forward = simd_normalize(dirAR)
                let angle = atan2(forward.x, forward.z)
                entity.orientation = simd_quatf(angle: angle, axis: [0, 1, 0])
            }

            entity.scale = .zero
            railStates[chunk.id] = .hidden

            anchor.addChild(entity)
            railEntities[chunk.id] = entity
        }
    }

    private func placeTurnRings(arrows: [ArrowPlacementData], into anchor: AnchorEntity) {
        for arrow in arrows {
            guard arrow.type == .turnLeft || arrow.type == .turnRight else { continue }
            let ringName = "turn-ring-\(arrow.id)"
            let isRight = (arrow.type == .turnRight)
            let ring = ARTurnRingFactory.make(name: ringName, isRight: isRight)
            ring.position = transformToAR(
                buildingX: arrow.positionX,
                buildingY: Double(VecturARGeometry.turnRingHeightOffset),
                buildingZ: arrow.positionZ
            )
            ring.scale = .zero
            anchor.addChild(ring)
            turnRings[arrow.id] = ring
        }
    }

    // MARK: - Data builders

    /// Subdivide each route polyline edge into ~`chunkLen`-meter chunks,
    /// emitting one left and one right rail per subdivision. They share
    /// `cumulativeMid` so the visibility system reveals both sides of
    /// the corridor together.
    private func buildRailChunks(routePoints: [(Double, Double)]) -> [RailChunkData] {
        guard routePoints.count >= 2 else { return [] }

        var chunks: [RailChunkData] = []
        var cumulative: Double = 0.0
        var counter = 0

        let chunkLenTarget = Double(VecturARGeometry.sideRailChunkLen)
        let railHalfWidth = Double(VecturARGeometry.sideRailHalfWidth)

        for i in 0..<(routePoints.count - 1) {
            let (ax, az) = routePoints[i]
            let (bx, bz) = routePoints[i + 1]
            let dx = bx - ax
            let dz = bz - az
            let segLen = sqrt(dx * dx + dz * dz)
            guard segLen > 0.01 else { continue }

            let dirX = dx / segLen
            let dirZ = dz / segLen

            // 2-D left perpendicular to (dirX, dirZ); right is its negation.
            let normalLX = -dirZ
            let normalLZ = dirX

            let count = max(1, Int(ceil(segLen / chunkLenTarget)))
            let chunkLen = segLen / Double(count)

            for k in 0..<count {
                let tMid = Double(k) * chunkLen + chunkLen * 0.5
                let cx = ax + dirX * tMid
                let cz = az + dirZ * tMid
                let cumMid = cumulative + tMid

                // Left side rail
                chunks.append(RailChunkData(
                    id: "rail-L-\(counter)",
                    midX: cx + normalLX * railHalfWidth,
                    midZ: cz + normalLZ * railHalfWidth,
                    dirX: dirX, dirZ: dirZ,
                    length: Float(chunkLen),
                    cumulativeMid: cumMid
                ))
                // Right side rail
                chunks.append(RailChunkData(
                    id: "rail-R-\(counter)",
                    midX: cx - normalLX * railHalfWidth,
                    midZ: cz - normalLZ * railHalfWidth,
                    dirX: dirX, dirZ: dirZ,
                    length: Float(chunkLen),
                    cumulativeMid: cumMid
                ))

                counter += 1
            }

            cumulative += segLen
        }

        return chunks
    }

    // MARK: - Visibility

    func updateVisibility(userCumulativeDistance: Double) {
        applyVisibility(userCumulativeDistance: userCumulativeDistance, cameraCullCheck: nil)
    }

    func updateVisibility(
        userCumulativeDistance: Double,
        cameraWorldX: Float,
        cameraWorldZ: Float,
        cameraForwardX: Float,
        cameraForwardZ: Float
    ) {
        applyVisibility(userCumulativeDistance: userCumulativeDistance) { entity in
            let pos = entity.position
            let toX = pos.x - cameraWorldX
            let toZ = pos.z - cameraWorldZ
            let dot = toX * cameraForwardX + toZ * cameraForwardZ
            return dot < -0.5
        }
    }

    private func applyVisibility(
        userCumulativeDistance: Double,
        cameraCullCheck: ((Entity) -> Bool)?
    ) {
        lastUserCumulativeDistance = userCumulativeDistance
        let aheadLimit = userCumulativeDistance + lookaheadDistance
        let behindLimit = userCumulativeDistance - fadeDistance

        // Arrows
        for arrow in allArrows {
            guard let entity = arrowEntities[arrow.id] else { continue }
            let dist = arrow.cumulativeDistance
            let oldState = arrowStates[arrow.id] ?? .hidden

            var newState = stateFor(
                cumulative: dist,
                userCumulative: userCumulativeDistance,
                aheadLimit: aheadLimit,
                behindLimit: behindLimit
            )
            if newState != .hidden, let cull = cameraCullCheck, cull(entity) {
                newState = .hidden
            }

            updateFadeInStart(id: arrow.id, newState: newState, oldState: oldState, starts: &arrowFadeInStart)
            applyArrowState(newState, to: entity, arrow: arrow, arrowDistance: dist, userDistance: userCumulativeDistance)
            arrowStates[arrow.id] = newState

            // Turn ring visibility follows the parent arrow but only
            // reveals within the anticipation distance so it feels like
            // pre-turn signage rather than ambient clutter.
            if let ring = turnRings[arrow.id] {
                applyTurnRingState(
                    ring: ring,
                    arrow: arrow,
                    parentState: newState,
                    userDistance: userCumulativeDistance
                )
            }
        }

        // Side rails (left + right)
        for chunk in allRailChunks {
            guard let entity = railEntities[chunk.id] else { continue }
            let oldState = railStates[chunk.id] ?? .hidden

            var newState = stateFor(
                cumulative: chunk.cumulativeMid,
                userCumulative: userCumulativeDistance,
                aheadLimit: aheadLimit,
                behindLimit: behindLimit
            )
            if newState != .hidden, let cull = cameraCullCheck, cull(entity) {
                newState = .hidden
            }

            updateFadeInStart(id: chunk.id, newState: newState, oldState: oldState, starts: &railFadeInStart)
            applyRailState(newState, to: entity, chunk: chunk, userDistance: userCumulativeDistance)
            railStates[chunk.id] = newState
        }
    }

    /// Per-frame animation tick — re-applies scale/opacity for entities
    /// whose reveal animation is still in flight so growth reads as a
    /// smooth ramp between the 2 Hz pose updates.
    private func tickFadeInAnimations() {
        let now = CACurrentMediaTime()
        let userDist = lastUserCumulativeDistance

        for arrow in allArrows {
            guard let startTime = arrowFadeInStart[arrow.id],
                  now - startTime < fadeInDuration,
                  let entity = arrowEntities[arrow.id] else { continue }
            let state = arrowStates[arrow.id] ?? .hidden
            guard state != .hidden else { continue }
            applyArrowState(state, to: entity, arrow: arrow, arrowDistance: arrow.cumulativeDistance, userDistance: userDist)
        }

        for chunk in allRailChunks {
            guard let startTime = railFadeInStart[chunk.id],
                  now - startTime < fadeInDuration,
                  let entity = railEntities[chunk.id] else { continue }
            let state = railStates[chunk.id] ?? .hidden
            guard state != .hidden else { continue }
            applyRailState(state, to: entity, chunk: chunk, userDistance: userDist)
        }
    }

    private func stateFor(
        cumulative: Double,
        userCumulative: Double,
        aheadLimit: Double,
        behindLimit: Double
    ) -> GuidanceState {
        if cumulative >= userCumulative && cumulative <= aheadLimit {
            return .active
        }
        if cumulative >= behindLimit && cumulative < userCumulative {
            return .fading
        }
        return .hidden
    }

    /// Bookkeeping for the per-entity reveal clock. We only stamp a start
    /// time on the hidden→visible edge so an entity that re-enters the
    /// frustum (e.g. after a camera cull) animates in fresh.
    private func updateFadeInStart(
        id: String,
        newState: GuidanceState,
        oldState: GuidanceState,
        starts: inout [String: CFTimeInterval]
    ) {
        if newState != .hidden {
            if oldState == .hidden || starts[id] == nil {
                starts[id] = CACurrentMediaTime()
            }
        } else if oldState != .hidden {
            starts.removeValue(forKey: id)
        }
    }

    /// Normalized 0…1 reveal progress for an entity, shaped with an
    /// ease-out cubic so growth is snappy up front and lands gently.
    private func fadeInProgress(startTime: CFTimeInterval?) -> Float {
        guard let startTime = startTime else { return 0 }
        let elapsed = CACurrentMediaTime() - startTime
        let linear = max(0.0, min(1.0, elapsed / fadeInDuration))
        let eased = 1.0 - pow(1.0 - linear, 3.0)
        return Float(eased)
    }

    // MARK: - State application

    private func applyArrowState(
        _ state: GuidanceState,
        to entity: Entity,
        arrow: ArrowPlacementData,
        arrowDistance: Double,
        userDistance: Double
    ) {
        switch state {
        case .active:
            let fadeIn = fadeInProgress(startTime: arrowFadeInStart[arrow.id])
            entity.scale = baseScale(for: arrow.type) * fadeIn
            setOpacity(entity, opacity: fadeIn)

        case .fading:
            let behind = userDistance - arrowDistance
            let t = max(0, min(1, behind / fadeDistance))
            let fadeIn = fadeInProgress(startTime: arrowFadeInStart[arrow.id])
            let opacity = Float(1.0 - t * 0.85) * fadeIn
            let scale = Float(1.0 - t * 0.2) * fadeIn
            entity.scale = baseScale(for: arrow.type) * scale
            setOpacity(entity, opacity: opacity)

        case .hidden:
            entity.scale = .zero
        }
    }

    private func applyRailState(
        _ state: GuidanceState,
        to entity: Entity,
        chunk: RailChunkData,
        userDistance: Double
    ) {
        // Rails are modeled at unit chunk length; scale along Z so short
        // remainder chunks at segment ends still land cleanly.
        let lengthScale = chunk.length / VecturARGeometry.sideRailChunkLen

        switch state {
        case .active:
            let fadeIn = fadeInProgress(startTime: railFadeInStart[chunk.id])
            entity.scale = SIMD3<Float>(fadeIn, fadeIn, lengthScale * fadeIn)
            setOpacity(entity, opacity: fadeIn)

        case .fading:
            let behind = userDistance - chunk.cumulativeMid
            let t = max(0, min(1, behind / fadeDistance))
            let fadeIn = fadeInProgress(startTime: railFadeInStart[chunk.id])
            let opacity = Float(1.0 - t * 0.85) * fadeIn
            let widthScale = Float(1.0 - t * 0.15) * fadeIn
            entity.scale = SIMD3<Float>(widthScale, widthScale, lengthScale * fadeIn)
            setOpacity(entity, opacity: opacity)

        case .hidden:
            entity.scale = .zero
        }
    }

    /// Turn ring: visible only when the parent arrow is within the
    /// anticipation window. Ring scales slightly up as the user gets
    /// closer — a premium "get ready to turn" cue.
    private func applyTurnRingState(
        ring: Entity,
        arrow: ArrowPlacementData,
        parentState: GuidanceState,
        userDistance: Double
    ) {
        guard parentState == .active else {
            ring.scale = .zero
            return
        }
        let ahead = arrow.cumulativeDistance - userDistance
        guard ahead >= 0 && ahead <= turnRingPreviewDistance else {
            ring.scale = .zero
            return
        }
        let t = 1.0 - (ahead / turnRingPreviewDistance)        // 0 far, 1 at turn
        let scale = Float(0.75 + 0.35 * t)
        ring.scale = SIMD3<Float>(scale, 1.0, scale)
        let opacity = Float(0.45 + 0.55 * t)
        setOpacity(ring, opacity: opacity)
    }

    /// Hide all guidance (e.g. on arrival). The destination beacon stays
    /// visible so the user still sees where they've arrived.
    func hideAllArrows() {
        for (id, entity) in arrowEntities {
            if let arrow = allArrows.first(where: { $0.id == id }), arrow.type == .destination {
                continue
            }
            entity.scale = .zero
            arrowStates[id] = .hidden
        }
        for (id, entity) in railEntities {
            entity.scale = .zero
            railStates[id] = .hidden
        }
        for (_, ring) in turnRings {
            ring.scale = .zero
        }

        arrowFadeInStart.removeAll()
        railFadeInStart.removeAll()

        // Swap the beacon tint to the warm arrival glow and scale it up.
        if let destination = destinationEntity {
            recolorEntityTree(destination, to: VecturARPalette.arrivalGlow)
            destination.scale = SIMD3<Float>(1.15, 1.15, 1.15)
        }
    }

    /// Remove all route entities from the scene.
    func clearRoute(from arView: ARView) {
        sceneUpdateSubscription?.cancel()
        sceneUpdateSubscription = nil

        if let anchor = routeAnchor {
            arView.scene.removeAnchor(anchor)
        }
        routeAnchor = nil

        arrowEntities.removeAll()
        arrowStates.removeAll()
        allArrows.removeAll()
        arrowFadeInStart.removeAll()

        railEntities.removeAll()
        railStates.removeAll()
        allRailChunks.removeAll()
        railFadeInStart.removeAll()

        turnRings.removeAll()
        destinationEntity = nil
        lastUserCumulativeDistance = 0.0
    }

    // MARK: - Opacity propagation

    /// Apply a normalized opacity to every glow surface in `entity`'s subtree.
    /// Per-layer multipliers (halo/mid/core/ring/pillar) come from
    /// `VecturARLayer.multiplier(for:)` so a single scalar cascades correctly.
    private func setOpacity(_ entity: Entity, opacity: Float, layerMultiplier: Float = 1.0) {
        var multiplier = layerMultiplier
        let name = entity.name
        let hit = VecturARLayer.multiplier(for: name)
        if hit != 1.0 {
            multiplier = hit
        }

        if let model = entity as? ModelEntity {
            let alpha = max(0, min(1, opacity * multiplier))
            applyAlpha(alpha, to: model)
        }
        for child in entity.children {
            setOpacity(child, opacity: opacity, layerMultiplier: multiplier)
        }
    }

    private func applyAlpha(_ alpha: Float, to model: ModelEntity) {
        guard var material = model.model?.materials.first as? UnlitMaterial else { return }
        material.blending = .transparent(opacity: PhysicallyBasedMaterial.Opacity(floatLiteral: alpha))
        model.model?.materials = [material]
    }

    /// Swap every unlit material in the subtree to a given tint, preserving
    /// the current opacity so transitions stay smooth.
    private func recolorEntityTree(_ entity: Entity, to color: UIColor) {
        if let model = entity as? ModelEntity,
           var material = model.model?.materials.first as? UnlitMaterial {
            material.color = .init(tint: color)
            model.model?.materials = [material]
        }
        for child in entity.children {
            recolorEntityTree(child, to: color)
        }
    }

    // MARK: - Arrow entity creation

    private func createArrowEntity(for arrow: ArrowPlacementData) -> Entity {
        switch arrow.type {
        case .follow:
            return ARChevronFactory.make(name: arrow.id, emphasis: .follow)

        case .turnLeft, .turnRight, .uTurn:
            return ARChevronFactory.make(name: arrow.id, emphasis: .turn)

        case .destination:
            let beacon = ARDestinationBeaconFactory.make(name: arrow.id)
            destinationEntity = beacon
            return beacon
        }
    }

    private func baseScale(for type: ArrowPlacementType) -> SIMD3<Float> {
        switch type {
        case .follow:
            return SIMD3<Float>(1.0, 1.0, 1.0)
        case .turnLeft, .turnRight:
            return SIMD3<Float>(1.15, 1.15, 1.15)
        case .uTurn:
            return SIMD3<Float>(1.25, 1.25, 1.25)
        case .destination:
            return SIMD3<Float>(1.0, 1.0, 1.0)
        }
    }

    // MARK: - Coordinate transform

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

// MARK: - Data transfer types

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

// MARK: - Internal chunk model

/// One subdivision of a side rail (left or right — identified by its `id`
/// prefix, e.g. `rail-L-…` / `rail-R-…`).
private struct RailChunkData {
    let id: String
    let midX: Double
    let midZ: Double
    let dirX: Double
    let dirZ: Double
    let length: Float
    let cumulativeMid: Double
}
