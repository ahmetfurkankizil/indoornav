import Foundation

/// Loader for the reviewed navigation package.
///
/// Phase 2: Loads from the reviewed multi-file package format
/// (manifest.json, rooms.json, nav_graph.json, entrance_markers.json, route_rendering.json).
///
/// The reviewed package is the ONLY runtime truth. The draft authoring_config.generated.json
/// is an offline authoring artifact only — it is never loaded at runtime.
struct BuildingPackageLoader {

    // MARK: - Data Models

    struct PackageNode: Codable {
        let id: String
        let x: Double
        let y: Double
        let z: Double
        let type: String
        let label: String?
        let floorId: String?
    }

    struct PackageEdge: Codable {
        let id: String
        let from: String
        let to: String
        let cost: Double
        let bidirectional: Bool
    }

    struct PackageMarker: Codable {
        let id: String
        let displayName: String?
        let startNodeId: String
        let physicalWidthMeters: Double
        let physicalHeightMeters: Double
        let position: Position3D
        let forwardBasis: String
        let rotationYDegrees: Double?
        let referenceImageName: String?
        let notes: String?
    }

    struct Position3D: Codable {
        let x: Double
        let y: Double
        let z: Double
    }

    struct PackageRoom: Codable {
        let id: String
        let displayName: String
        let destinationNodeId: String
        let category: String?
        let description: String?
        let floorId: String?
        let floorName: String?
    }

    struct RouteRenderingConfig: Codable {
        let arrowSpacingMeters: Double
        let lookaheadDistanceMeters: Double
        let destinationThresholdMeters: Double
        let turnMarkerThresholdDegrees: Double
        let arrowHeightOffsetMeters: Double
    }

    // MARK: - Unified (remote) package format

    struct UnifiedFloor: Codable {
        let floorId: String
        let floorNumber: Int
        let floorName: String
        let floorY: Double
        let nodes: [PackageNode]
        let edges: [PackageEdge]
    }

    struct CrossFloorConnection: Codable {
        let id: String
        let fromNodeId: String
        let toNodeId: String
        let type: String
        let bidirectional: Bool
    }

    struct UnifiedPackage: Codable {
        let buildingId: String
        let buildingName: String
        let version: Int
        let floors: [UnifiedFloor]
        let entranceMarkers: [PackageMarker]
        let buildingWidthMeters: Double?
        let routeRendering: RouteRenderingConfig
        let crossFloorConnections: [CrossFloorConnection]?
        let checksum: String?
    }

    // MARK: - Multi-file package containers

    struct Manifest: Codable {
        let packageVersion: String
        let buildingId: String
        let buildingName: String
        let floorId: String
        let reviewStatus: String
        let files: ManifestFiles
    }

    struct ManifestFiles: Codable {
        let rooms: String
        let navGraph: String
        let entranceMarkers: String
        let routeRendering: String
    }

    struct RoomsFile: Codable {
        let rooms: [PackageRoom]
    }

    struct NavGraphFile: Codable {
        let nodes: [PackageNode]
        let edges: [PackageEdge]
    }

    struct EntranceMarkersFile: Codable {
        let entranceMarkers: [PackageMarker]
    }

    /// Unified config assembled from multiple reviewed-package files.
    struct ReviewedConfig {
        let manifest: Manifest
        let rooms: [PackageRoom]
        let nodes: [PackageNode]
        let edges: [PackageEdge]
        let entranceMarkers: [PackageMarker]
        let routeRendering: RouteRenderingConfig
    }

    struct LoadedPackage {
        let config: ReviewedConfig
        let routeNodeIds: [String]
        let arrows: [ArrowPlacementData]
        let routePoints: [(Double, Double)]
        let totalDistance: Double
        let entranceMarker: PackageMarker?
        let destinationName: String
        /// Position of the destination door node (x, z) for arrival distance check.
        let destinationPosition: (Double, Double)
    }

    // MARK: - Package loading errors

    enum PackageError: Error, CustomStringConvertible {
        case manifestMissing
        case fileMissing(String)
        case decodingFailed(String, Error)
        case noEntranceMarker
        case noRooms

        var description: String {
            switch self {
            case .manifestMissing:
                return "Reviewed package manifest.json not found in bundle"
            case .fileMissing(let name):
                return "Reviewed package file missing: \(name)"
            case .decodingFailed(let name, let error):
                return "Failed to decode \(name): \(error.localizedDescription)"
            case .noEntranceMarker:
                return "No entrance markers defined in reviewed package"
            case .noRooms:
                return "No rooms defined in reviewed package"
            }
        }
    }

    // MARK: - Config Loading (reviewed package)

    /// Load the reviewed package from the app bundle.
    /// Returns the assembled config or a descriptive error.
    static func loadReviewedPackage() -> Result<ReviewedConfig, PackageError> {
        // Load manifest
        guard let manifestURL = Bundle.main.url(forResource: "manifest", withExtension: "json") else {
            return .failure(.manifestMissing)
        }
        let manifest: Manifest
        do {
            let data = try Data(contentsOf: manifestURL)
            manifest = try JSONDecoder().decode(Manifest.self, from: data)
        } catch {
            return .failure(.decodingFailed("manifest.json", error))
        }

        print("[PackageLoader] Loading reviewed package: \(manifest.buildingName) (v\(manifest.packageVersion))")

        // Load rooms
        let roomsFileName = manifest.files.rooms.replacingOccurrences(of: ".json", with: "")
        guard let roomsURL = Bundle.main.url(forResource: roomsFileName, withExtension: "json") else {
            return .failure(.fileMissing(manifest.files.rooms))
        }
        let roomsFile: RoomsFile
        do {
            let data = try Data(contentsOf: roomsURL)
            roomsFile = try JSONDecoder().decode(RoomsFile.self, from: data)
        } catch {
            return .failure(.decodingFailed(manifest.files.rooms, error))
        }

        if roomsFile.rooms.isEmpty {
            return .failure(.noRooms)
        }

        // Load nav graph
        let graphFileName = manifest.files.navGraph.replacingOccurrences(of: ".json", with: "")
        guard let graphURL = Bundle.main.url(forResource: graphFileName, withExtension: "json") else {
            return .failure(.fileMissing(manifest.files.navGraph))
        }
        let graphFile: NavGraphFile
        do {
            let data = try Data(contentsOf: graphURL)
            graphFile = try JSONDecoder().decode(NavGraphFile.self, from: data)
        } catch {
            return .failure(.decodingFailed(manifest.files.navGraph, error))
        }

        // Load entrance markers
        let markersFileName = manifest.files.entranceMarkers.replacingOccurrences(of: ".json", with: "")
        guard let markersURL = Bundle.main.url(forResource: markersFileName, withExtension: "json") else {
            return .failure(.fileMissing(manifest.files.entranceMarkers))
        }
        let markersFile: EntranceMarkersFile
        do {
            let data = try Data(contentsOf: markersURL)
            markersFile = try JSONDecoder().decode(EntranceMarkersFile.self, from: data)
        } catch {
            return .failure(.decodingFailed(manifest.files.entranceMarkers, error))
        }

        if markersFile.entranceMarkers.isEmpty {
            return .failure(.noEntranceMarker)
        }

        // Load route rendering config
        let renderingFileName = manifest.files.routeRendering.replacingOccurrences(of: ".json", with: "")
        guard let renderingURL = Bundle.main.url(forResource: renderingFileName, withExtension: "json") else {
            return .failure(.fileMissing(manifest.files.routeRendering))
        }
        let rendering: RouteRenderingConfig
        do {
            let data = try Data(contentsOf: renderingURL)
            rendering = try JSONDecoder().decode(RouteRenderingConfig.self, from: data)
        } catch {
            return .failure(.decodingFailed(manifest.files.routeRendering, error))
        }

        let config = ReviewedConfig(
            manifest: manifest,
            rooms: roomsFile.rooms,
            nodes: graphFile.nodes,
            edges: graphFile.edges,
            entranceMarkers: markersFile.entranceMarkers,
            routeRendering: rendering
        )

        print("[PackageLoader] Loaded: \(config.nodes.count) nodes, \(config.edges.count) edges, \(config.rooms.count) rooms")
        return .success(config)
    }

    // MARK: - Remote package parsing

    /// Parse a `UnifiedPackage` JSON string returned by the admin API mobile endpoint.
    /// Scales all coordinates from map units to real-world metres, tags nodes with their
    /// floor, and derives destination rows from navigable graph nodes.
    static func parseUnifiedPackage(_ jsonString: String) -> Result<ReviewedConfig, PackageError> {
        guard let data = jsonString.data(using: .utf8) else {
            return .failure(.decodingFailed("package", NSError(domain: "utf8", code: 0)))
        }
        let unified: UnifiedPackage
        do {
            unified = try JSONDecoder().decode(UnifiedPackage.self, from: data)
        } catch {
            return .failure(.decodingFailed("unified package", error))
        }

        let targetWidth = unified.buildingWidthMeters ?? 25.0
        let allRaw = unified.floors.flatMap { $0.nodes }
        let minX = allRaw.map(\.x).min() ?? 0.0
        let maxX = allRaw.map(\.x).max() ?? 1.0
        let mapUnitWidth = maxX - minX
        let scale = mapUnitWidth > 0.1 ? targetWidth / mapUnitWidth : 1.0

        let floorInfoMap = Dictionary(uniqueKeysWithValues: unified.floors.map { ($0.floorId, $0) })

        let allNodes: [PackageNode] = unified.floors.flatMap { floor in
            floor.nodes.map { n in
                PackageNode(id: n.id, x: n.x * scale, y: n.y * scale, z: n.z * scale,
                            type: n.type, label: n.label, floorId: floor.floorId)
            }
        }
        let nodeMap = Dictionary(uniqueKeysWithValues: allNodes.map { ($0.id, $0) })

        let intraEdges = unified.floors.flatMap { $0.edges }
        let crossEdges = (unified.crossFloorConnections ?? []).map { c -> PackageEdge in
            let fromFloor = nodeMap[c.fromNodeId]?.floorId.flatMap { floorInfoMap[$0] }
            let toFloor   = nodeMap[c.toNodeId]?.floorId.flatMap { floorInfoMap[$0] }
            let floorDiff: Double = (fromFloor != nil && toFloor != nil)
                ? Double(abs(fromFloor!.floorNumber - toFloor!.floorNumber)) : 1.0
            return PackageEdge(id: c.id, from: c.fromNodeId, to: c.toNodeId,
                               cost: floorDiff * 5.0, bidirectional: c.bidirectional)
        }
        let allEdges = intraEdges + crossEdges

        let rooms: [PackageRoom] = allNodes.filter(isRemoteDestinationNode).map { n in
            let floor = n.floorId.flatMap { floorInfoMap[$0] }
            let displayName = cleanLabel(n.label, fallback: "Room")
            return PackageRoom(id: n.id, displayName: displayName,
                               destinationNodeId: n.id,
                               category: inferredCategory(nodeType: n.type, label: displayName),
                               description: nil,
                               floorId: n.floorId, floorName: floor?.floorName)
        }

        if rooms.isEmpty { return .failure(.noRooms) }
        if unified.entranceMarkers.isEmpty {
            print("[PackageLoader] Warning: no entrance markers in remote package — AR alignment will be skipped")
        }

        let scaledMarkers = unified.entranceMarkers.map { m in
            PackageMarker(id: m.id, displayName: m.displayName, startNodeId: m.startNodeId,
                          physicalWidthMeters: m.physicalWidthMeters,
                          physicalHeightMeters: m.physicalHeightMeters,
                          position: Position3D(x: m.position.x * scale,
                                               y: m.position.y * scale,
                                               z: m.position.z * scale),
                          forwardBasis: m.forwardBasis, rotationYDegrees: m.rotationYDegrees,
                          referenceImageName: m.referenceImageName, notes: m.notes)
        }

        let manifest = Manifest(
            packageVersion: "\(unified.version)",
            buildingId: unified.buildingId,
            buildingName: unified.buildingName,
            floorId: unified.floors.first?.floorId ?? "0",
            reviewStatus: "published",
            files: ManifestFiles(rooms: "", navGraph: "", entranceMarkers: "", routeRendering: "")
        )

        print("[PackageLoader] Remote: \(allNodes.count) nodes, \(allEdges.count) edges, \(rooms.count) rooms, scale=\(String(format: "%.3f", scale))")

        return .success(ReviewedConfig(manifest: manifest, rooms: rooms, nodes: allNodes,
                                       edges: allEdges, entranceMarkers: scaledMarkers,
                                       routeRendering: unified.routeRendering))
    }

    // MARK: - Route Computation

    /// Compute a route to a specific room.
    /// - Parameter originNodeId: Optional override for the route start node. When nil,
    ///   a graph node with type == "entrance" is preferred over marker metadata.
    static func computeRoute(
        config: ReviewedConfig,
        destinationRoomId: String,
        originNodeId: String? = nil
    ) -> LoadedPackage? {
        guard let room = config.rooms.first(where: { $0.id == destinationRoomId }) else {
            print("[PackageLoader] Room \(destinationRoomId) not found in config")
            return nil
        }
        return computeRoute(
            config: config,
            destinationNodeId: room.destinationNodeId,
            destinationLabel: room.displayName,
            originNodeId: originNodeId
        )
    }

    /// Compute a route to a specific graph node.
    /// This variant lets the AI assistant route to semantic POIs that are not
    /// listed as rooms in `rooms.json` but still map to a valid nav-graph node.
    static func computeRoute(
        config: ReviewedConfig,
        destinationNodeId: String,
        destinationLabel: String,
        originNodeId: String? = nil
    ) -> LoadedPackage? {
        let nodeMap = Dictionary(uniqueKeysWithValues: config.nodes.map { ($0.id, $0) })
        var adjacency: [String: [(String, Double)]] = [:]
        for edge in config.edges {
            adjacency[edge.from, default: []].append((edge.to, edge.cost))
            if edge.bidirectional {
                adjacency[edge.to, default: []].append((edge.from, edge.cost))
            }
        }

        let entranceStart = defaultEntranceStartNodeId(config: config, nodeMap: nodeMap)
        let startNodeId = originNodeId ?? entranceStart

        guard nodeMap[startNodeId] != nil else {
            print("[PackageLoader] Start node \(startNodeId) not found in graph")
            return nil
        }

        guard nodeMap[destinationNodeId] != nil else {
            print("[PackageLoader] Destination node \(destinationNodeId) not found in graph")
            return nil
        }

        let destNodeId = destinationNodeId
        let destName = destinationLabel

        // Trivial case: entrance is the destination itself
        if startNodeId == destNodeId {
            guard let destNode = nodeMap[destNodeId] else { return nil }
            let destPos = (destNode.x, destNode.z)
            print("[PackageLoader] Route to \(destName): start == destination (0m)")
            return LoadedPackage(
                config: config, routeNodeIds: [startNodeId],
                arrows: [], routePoints: [destPos], totalDistance: 0,
                entranceMarker: config.entranceMarkers.first, destinationName: destName,
                destinationPosition: destPos
            )
        }

        let routeNodeIds = dijkstra(from: startNodeId, to: destNodeId, adjacency: adjacency)

        guard routeNodeIds.count >= 2 else {
            print("[PackageLoader] Could not find route from \(startNodeId) to \(destNodeId)")
            return nil
        }

        let routeNodes = routeNodeIds.compactMap { nodeMap[$0] }
        let spacing = config.routeRendering.arrowSpacingMeters
        let heightOffset = config.routeRendering.arrowHeightOffsetMeters
        let arrows = generateArrows(from: routeNodes, destinationLabel: destName, spacing: spacing, heightOffset: heightOffset)
        let points = routeNodes.map { ($0.x, $0.z) }
        let dist = totalDistance(points)

        let destNode = routeNodes.last!
        let destPos = (destNode.x, destNode.z)

        print("[PackageLoader] Route to \(destName): \(routeNodeIds.joined(separator: " -> ")) (\(String(format: "%.1f", dist))m)")

        return LoadedPackage(
            config: config, routeNodeIds: routeNodeIds,
            arrows: arrows, routePoints: points, totalDistance: dist,
            entranceMarker: config.entranceMarkers.first, destinationName: destName,
            destinationPosition: destPos
        )
    }

    private static func defaultEntranceStartNodeId(
        config: ReviewedConfig,
        nodeMap: [String: PackageNode]
    ) -> String {
        let entranceNodes = config.nodes.filter { $0.type.lowercased() == "entrance" }
        let marker = config.entranceMarkers.first

        if entranceNodes.count == 1 {
            return entranceNodes[0].id
        }

        if let marker, let closestEntrance = entranceNodes.min(by: {
            distanceSquared($0, to: marker.position) < distanceSquared($1, to: marker.position)
        }) {
            return closestEntrance.id
        }

        if let marker, nodeMap[marker.startNodeId] != nil {
            return marker.startNodeId
        }

        return config.nodes.first?.id ?? ""
    }

    private static func distanceSquared(_ node: PackageNode, to position: Position3D) -> Double {
        let dx = node.x - position.x
        let dz = node.z - position.z
        return dx * dx + dz * dz
    }

    private static func isRemoteDestinationNode(_ node: PackageNode) -> Bool {
        !isNavigationHelperNode(node)
    }

    static func isNavigationHelperNode(_ node: PackageNode) -> Bool {
        let rawType = node.type.lowercased()
        let normalizedType = normalizedHelperToken(rawType)
        let label = cleanLabel(node.label, fallback: "")

        switch normalizedType {
        case "corridor", "hallway", "junction", "entrance", "exit",
            "waypoint", "path", "turn", "turning_point", "turn_point",
            "turnpoint", "route_helper", "navigation_helper", "nav_helper",
            "helper", "corridor_projection", "projection", "connector", "fork":
            return true
        default:
            break
        }

        return isHelperNodeLabel(label)
    }

    private static func normalizedHelperToken(_ value: String) -> String {
        value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "-", with: "_")
            .replacingOccurrences(of: " ", with: "_")
    }

    private static func isHelperNodeLabel(_ label: String) -> Bool {
        let normalized = label.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return false }

        let helperFragments = [
            "turning point",
            "turn point",
            "turnpoint",
            "waypoint",
            "junction",
            "corridor projection",
            "projection",
            "corridor point",
            "route point",
            "navigation point",
            "helper",
            "fork",
            "landing"
        ]

        if helperFragments.contains(where: { normalized.contains($0) }) {
            return true
        }

        return normalized.hasPrefix("turn ") || normalized.hasPrefix("turn-")
    }

    private static func cleanLabel(_ label: String?, fallback: String) -> String {
        let trimmed = label?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? fallback : trimmed
    }

    private static func inferredCategory(nodeType: String, label: String) -> String {
        let rawType = nodeType.lowercased()
        let rawLabel = label.lowercased()

        switch rawType {
        case "classroom", "lecture", "lecture_room":
            return "classroom"
        case "lab", "laboratory":
            return "lab"
        case "cafe", "coffee", "cafeteria":
            return "cafe"
        case "vertical_transport", "elevator", "lift":
            return "vertical_transport"
        case "toilet", "restroom", "wc", "bathroom":
            return "toilet"
        default:
            break
        }

        if rawLabel.contains("wc") ||
            rawLabel.contains("toilet") ||
            rawLabel.contains("restroom") ||
            rawLabel.contains("bathroom") ||
            rawLabel.contains("men's") ||
            rawLabel.contains("women's") {
            return "toilet"
        }
        if rawLabel.contains("elevator") || rawLabel.contains("lift") {
            return "vertical_transport"
        }
        if rawLabel.contains("cafe") || rawLabel.contains("coffee") {
            return "cafe"
        }
        if rawLabel.contains("lab") || rawLabel.contains("laboratory") {
            return "lab"
        }
        if rawLabel.contains("class") || rawLabel.contains("lecture") {
            return "classroom"
        }

        return "other"
    }

    // MARK: - Dijkstra

    private static func dijkstra(from start: String, to end: String, adjacency: [String: [(String, Double)]]) -> [String] {
        var dist: [String: Double] = [start: 0]
        var prev: [String: String] = [:]
        var visited: Set<String> = []

        while true {
            guard let current = dist
                .filter({ !visited.contains($0.key) })
                .min(by: { $0.value < $1.value })?
                .key else { break }

            if current == end { break }
            visited.insert(current)

            for (neighbor, cost) in adjacency[current] ?? [] {
                let alt = dist[current]! + cost
                if alt < (dist[neighbor] ?? Double.greatestFiniteMagnitude) {
                    dist[neighbor] = alt
                    prev[neighbor] = current
                }
            }
        }

        var path: [String] = []
        var cur: String? = end
        while let c = cur {
            path.insert(c, at: 0)
            cur = prev[c]
        }
        return path.first == start ? path : []
    }

    // MARK: - Arrow generation

    private static func generateArrows(
        from nodes: [PackageNode],
        destinationLabel: String = "Destination",
        spacing: Double = 1.5,
        heightOffset: Double = 0.05
    ) -> [ArrowPlacementData] {
        guard nodes.count >= 2 else { return [] }
        var arrows: [ArrowPlacementData] = []
        var cumulativeDist: Double = 0.0

        for i in 0..<(nodes.count - 1) {
            let current = nodes[i]
            let next = nodes[i + 1]

            let dx = next.x - current.x
            let dz = next.z - current.z
            let segLen = sqrt(dx * dx + dz * dz)
            guard segLen > 0.01 else { continue }

            let dirX = dx / segLen
            let dirZ = dz / segLen

            let count = max(1, Int(segLen / spacing))

            for s in 0..<count {
                let t = Double(s) / Double(count)
                let ax = current.x + t * dx
                let az = current.z + t * dz
                let ay = current.y + heightOffset
                let arrowCumDist = cumulativeDist + t * segLen

                let type: ArrowPlacementType
                let label: String?
                if i == nodes.count - 2 && s == count - 1 {
                    type = .destination
                    label = destinationLabel
                } else if s == 0 && i > 0 {
                    let prevNode = nodes[i - 1]
                    let prevDx = current.x - prevNode.x
                    let prevDz = current.z - prevNode.z
                    let cross = prevDx * dirZ - prevDz * dirX
                    if abs(cross) > 0.3 {
                        // cross > 0 → clockwise in XZ plane (viewed from +Y) → RIGHT turn
                        // cross < 0 → counterclockwise → LEFT turn
                        type = cross > 0 ? .turnRight : .turnLeft
                        label = cross > 0 ? "Turn right" : "Turn left"
                    } else {
                        type = .follow
                        label = nil
                    }
                } else {
                    type = .follow
                    label = nil
                }

                arrows.append(ArrowPlacementData(
                    id: "a\(arrows.count)",
                    positionX: ax, positionY: ay, positionZ: az,
                    forwardDx: dirX, forwardDy: 0.0, forwardDz: dirZ,
                    type: type, label: label,
                    cumulativeDistance: arrowCumDist
                ))
            }

            cumulativeDist += segLen
        }

        return arrows
    }

    private static func totalDistance(_ points: [(Double, Double)]) -> Double {
        var d = 0.0
        for i in 1..<points.count {
            let dx = points[i].0 - points[i-1].0
            let dz = points[i].1 - points[i-1].1
            d += sqrt(dx*dx + dz*dz)
        }
        return d
    }
}
