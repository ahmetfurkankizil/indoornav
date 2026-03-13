import Foundation

/// Minimal loader for the generated authoring_config JSON.
/// Reads nodes and edges from the bundle and generates arrow placements
/// along the computed route from start → destination.
struct BuildingPackageLoader {
    
    struct PackageNode: Codable {
        let id: String
        let x: Double
        let y: Double
        let z: Double
        let type: String
        let label: String?
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
        let startNodeId: String
        let physicalWidthMeters: Double
        let physicalHeightMeters: Double
        let position: Position3D
        let forwardBasis: String
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
    }
    
    struct AuthoringConfig: Codable {
        let buildingId: String
        let buildingName: String
        let nodes: [PackageNode]
        let edges: [PackageEdge]
        let entranceMarkers: [PackageMarker]
        let rooms: [PackageRoom]
    }
    
    struct LoadedPackage {
        let config: AuthoringConfig
        /// Route from entrance node to first room's destination node
        let routeNodeIds: [String]
        /// Arrow placements along the route
        let arrows: [ArrowPlacementData]
        /// Route points for progress tracking (x, z)
        let routePoints: [(Double, Double)]
        /// Total route distance
        let totalDistance: Double
        /// Entrance marker config
        let entranceMarker: PackageMarker?
        /// Destination room name
        let destinationName: String
    }
    
    /// Load package from a JSON file name in the app bundle.
    static func loadFromBundle(fileName: String = "authoring_config.generated") -> LoadedPackage? {
        guard let url = Bundle.main.url(forResource: fileName, withExtension: "json") else {
            print("[PackageLoader] ❌ \(fileName).json not found in bundle")
            return nil
        }
        
        guard let data = try? Data(contentsOf: url),
              let config = try? JSONDecoder().decode(AuthoringConfig.self, from: data) else {
            print("[PackageLoader] ❌ Failed to decode \(fileName).json")
            return nil
        }
        
        print("[PackageLoader] ✓ Loaded \(config.buildingName): \(config.nodes.count) nodes, \(config.edges.count) edges")
        
        // Build adjacency map
        let nodeMap = Dictionary(uniqueKeysWithValues: config.nodes.map { ($0.id, $0) })
        var adjacency: [String: [(String, Double)]] = [:]
        for edge in config.edges {
            adjacency[edge.from, default: []].append((edge.to, edge.cost))
            if edge.bidirectional {
                adjacency[edge.to, default: []].append((edge.from, edge.cost))
            }
        }
        
        // Find start and destination
        let startNodeId = config.entranceMarkers.first?.startNodeId ?? config.nodes.first?.id ?? ""
        let destNodeId = config.rooms.first?.destinationNodeId ?? config.nodes.last?.id ?? ""
        let destName = config.rooms.first?.displayName ?? "Destination"
        
        // Dijkstra shortest path
        let routeNodeIds = dijkstra(from: startNodeId, to: destNodeId, adjacency: adjacency)
        
        guard routeNodeIds.count >= 2 else {
            print("[PackageLoader] ⚠ Could not find route from \(startNodeId) to \(destNodeId), using all nodes")
            let allNodes = config.nodes
            let arrows = generateArrows(from: allNodes)
            let points = allNodes.map { ($0.x, $0.z) }
            let dist = totalDistance(points)
            return LoadedPackage(
                config: config, routeNodeIds: allNodes.map { $0.id },
                arrows: arrows, routePoints: points, totalDistance: dist,
                entranceMarker: config.entranceMarkers.first, destinationName: destName
            )
        }
        
        let routeNodes = routeNodeIds.compactMap { nodeMap[$0] }
        let arrows = generateArrows(from: routeNodes)
        let points = routeNodes.map { ($0.x, $0.z) }
        let dist = totalDistance(points)
        
        print("[PackageLoader] ✓ Route: \(routeNodeIds.joined(separator: " → ")) (distance: \(String(format: "%.1f", dist))m)")
        
        return LoadedPackage(
            config: config, routeNodeIds: routeNodeIds,
            arrows: arrows, routePoints: points, totalDistance: dist,
            entranceMarker: config.entranceMarkers.first, destinationName: destName
        )
    }
    
    // MARK: - Dijkstra
    
    private static func dijkstra(from start: String, to end: String, adjacency: [String: [(String, Double)]]) -> [String] {
        var dist: [String: Double] = [start: 0]
        var prev: [String: String] = [:]
        var visited: Set<String> = []
        
        while true {
            // Pick unvisited with smallest distance
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
        
        // Reconstruct path
        var path: [String] = []
        var cur: String? = end
        while let c = cur {
            path.insert(c, at: 0)
            cur = prev[c]
        }
        return path.first == start ? path : []
    }
    
    // MARK: - Arrow generation
    
    private static func generateArrows(from nodes: [PackageNode]) -> [ArrowPlacementData] {
        guard nodes.count >= 2 else { return [] }
        var arrows: [ArrowPlacementData] = []
        
        for i in 0..<(nodes.count - 1) {
            let current = nodes[i]
            let next = nodes[i + 1]
            
            let dx = next.x - current.x
            let dz = next.z - current.z
            let segLen = sqrt(dx * dx + dz * dz)
            guard segLen > 0.01 else { continue }
            
            let dirX = dx / segLen
            let dirZ = dz / segLen
            
            // Place arrows every 1.5m along the segment
            let spacing = 1.5
            let count = max(1, Int(segLen / spacing))
            
            for s in 0..<count {
                let t = Double(s) / Double(count)
                let ax = current.x + t * dx
                let az = current.z + t * dz
                let ay = current.y + 0.05 // slightly above floor
                
                // Determine type
                let type: ArrowPlacementType
                let label: String?
                if i == nodes.count - 2 && s == count - 1 {
                    // Last arrow = destination
                    type = .destination
                    label = next.label ?? "Destination"
                } else if s == 0 && i > 0 {
                    // First arrow in a new segment → check if it's a turn
                    let prevNode = nodes[i - 1]
                    let prevDx = current.x - prevNode.x
                    let prevDz = current.z - prevNode.z
                    let cross = prevDx * dirZ - prevDz * dirX
                    if abs(cross) > 0.3 {
                        type = cross > 0 ? .turnLeft : .turnRight
                        label = cross > 0 ? "Turn left" : "Turn right"
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
                    type: type, label: label
                ))
            }
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
