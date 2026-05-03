import Foundation

/// Deterministic tool layer the AI route agent can call.
///
/// Why "tools": every method is side-effect-free, accepts plain values and
/// returns plain values. Both the mock agent and the future GPT agent invoke
/// the same set of methods so route results are reproducible regardless of
/// which LLM (or no LLM) decides what to call.
///
/// CRITICAL CONTRACT: tools NEVER invent destinations or coordinates. Every
/// candidate they return is anchored to a real `nodeId` from the loaded
/// `BuildingPackageLoader.ReviewedConfig` graph and every route is produced by
/// the existing deterministic route engine.
struct RouteAgentTools {

    let catalog: SemanticPoiCatalog
    let config: BuildingPackageLoader.ReviewedConfig

    init(catalog: SemanticPoiCatalog, config: BuildingPackageLoader.ReviewedConfig) {
        self.catalog = catalog
        self.config = config
    }

    // MARK: - Direct rooms.json lookups
    //
    // These are the assistant's primary resolution path. `rooms.json` is the
    // SAME source of truth the manual destination list uses, so anything
    // tappable in the UI is also resolvable through these helpers (and
    // produces a route via the EXACT same `computeRoute(config:destinationRoomId:)`
    // call the manual list relies on).

    /// Look up a room by its `rooms.json` id (e.g. "fameo-cafe", "ea101").
    func findRoom(byId id: String) -> BuildingPackageLoader.PackageRoom? {
        let target = id.lowercased()
        return config.rooms.first { $0.id.lowercased() == target }
    }

    /// Look up a room whose `displayName` matches the query case-insensitively.
    /// Falls back to a contains-match if no exact match is found.
    func findRoom(byDisplayName name: String) -> BuildingPackageLoader.PackageRoom? {
        let normalized = normalize(name)
        if let exact = config.rooms.first(where: { normalize($0.displayName) == normalized }) {
            return exact
        }
        return config.rooms.first(where: { normalize($0.displayName).contains(normalized) })
    }

    // MARK: - searchPoiByName

    /// Fuzzy POI search by display name, alias or description. Case-insensitive.
    ///
    /// `rooms.json` is the PRIMARY source — anything in the manual destination
    /// list is included. The optional semantic catalog can add aliases (e.g.
    /// "fameo coffee" → Fameo Cafe), but is never required.
    func searchPoiByName(_ query: String) -> [SemanticPoiCatalog.Poi] {
        let q = normalize(query)
        guard !q.isEmpty else { return [] }

        var hits: [SemanticPoiCatalog.Poi] = []
        var seenRoomIds: Set<String> = []
        for room in config.rooms {
            if normalize(room.displayName).contains(q) || normalize(room.id).contains(q) ||
               (room.description.map { normalize($0).contains(q) } ?? false) {
                hits.append(roomToSyntheticPoi(room))
                seenRoomIds.insert(room.id)
            }
        }
        // Catalog aliases / descriptions can add extra matches not directly
        // reachable through the room name itself (e.g. "Fameo coffee").
        for poi in catalog.pois where !seenRoomIds.contains(poi.roomId ?? "") {
            let aliasMatch = poi.aliases.contains { normalize($0).contains(q) }
            let descMatch = poi.description.map { normalize($0).contains(q) } ?? false
            if normalize(poi.displayName).contains(q) || aliasMatch || descMatch {
                hits.append(poi)
            }
        }
        return hits
    }

    // MARK: - searchPoiByCategory

    /// Find every navigable destination matching the requested category.
    ///
    /// `rooms.json` is consulted FIRST — its `category` strings ("toilet",
    /// "cafe", "vertical_transport", "lab", "classroom") are mapped onto
    /// canonical keys ("restroom", "coffee", "elevator", ...) using the
    /// catalog's synonym table (which always ships with a baseline so this
    /// works even when `semantic_pois.json` is missing). Catalog POIs that
    /// don't already correspond to a known room are added afterwards.
    func searchPoiByCategory(_ category: String) -> [SemanticPoiCatalog.Poi] {
        let canonical = catalog.canonicalCategory(forAlias: category) ?? category.lowercased()
        let synonyms = Set((catalog.categorySynonyms[canonical] ?? []).map { $0.lowercased() } + [canonical])

        var hits: [SemanticPoiCatalog.Poi] = []
        var seenRoomIds: Set<String> = []
        for room in config.rooms {
            var matched = false
            // 1) Direct category match (with synonym mapping).
            if let roomCategory = room.category?.lowercased(), !roomCategory.isEmpty {
                let canonicalForRoom = catalog.canonicalCategory(forAlias: roomCategory) ?? roomCategory
                if synonyms.contains(roomCategory) || canonicalForRoom == canonical {
                    matched = true
                }
            }
            // 2) DisplayName-based fallback. Many remote / older packages
            //    use category strings that don't line up with our canonical
            //    keys; in that case the room's NAME usually contains the
            //    category word ("CS Lab", "Fameo Cafe", "Men's WC"). This
            //    keeps the assistant working even with non-standard data.
            if !matched {
                let nameLowered = room.displayName.lowercased()
                let nameTokens = nameLowered.split(whereSeparator: { !$0.isLetter && !$0.isNumber }).map(String.init)
                for synonym in synonyms where !synonym.isEmpty {
                    if nameTokens.contains(synonym) || nameLowered.contains(synonym) {
                        matched = true
                        break
                    }
                }
            }
            if matched {
                hits.append(roomToSyntheticPoi(room))
                seenRoomIds.insert(room.id)
            }
        }
        // Pull in catalog-only POIs (e.g. EA-Z04) that don't have a rooms.json
        // counterpart but still want to be discoverable by category.
        for poi in catalog.pois {
            guard poi.isNavigable, !poi.isRestricted else { continue }
            if let rid = poi.roomId, seenRoomIds.contains(rid) { continue }
            if poi.categories.contains(where: { synonyms.contains($0.lowercased()) }) {
                hits.append(poi)
            }
        }
        return hits
    }

    // MARK: - resolveRoomIdentifier

    /// Tries to extract and resolve a room identifier of the form `EA-Z04`,
    /// `ea101`, `EA 102`, `Z04`, etc. from free text. Returns the matching
    /// POI when one is found. `rooms.json` is checked first.
    func resolveRoomIdentifier(_ rawText: String) -> SemanticPoiCatalog.Poi? {
        guard let token = extractRoomToken(from: rawText) else { return nil }
        let normalized = normalize(token)
        // 1) Direct rooms.json hits (canonical destinations).
        for room in config.rooms {
            if normalize(room.id) == normalized { return roomToSyntheticPoi(room) }
            let nameTokens = normalize(room.displayName).split(separator: " ")
            if nameTokens.contains(Substring(normalized)) { return roomToSyntheticPoi(room) }
        }
        for room in config.rooms where normalize(room.displayName).contains(normalized) {
            return roomToSyntheticPoi(room)
        }
        // 2) Catalog aliases (e.g. EA-Z04) for cases not covered by rooms.json.
        for poi in catalog.pois {
            if poi.aliases.contains(where: { normalize($0) == normalized }) {
                return poi
            }
        }
        for poi in catalog.pois {
            let nameTokens = normalize(poi.displayName).split(separator: " ")
            if nameTokens.contains(Substring(normalized)) { return poi }
        }
        for poi in catalog.pois {
            if normalize(poi.displayName).contains(normalized) { return poi }
            if poi.aliases.contains(where: { normalize($0).contains(normalized) }) { return poi }
        }
        return nil
    }

    /// Pull the first token that looks like a room identifier (one or more
    /// letters followed by digits, possibly separated by `-`, `_`, ` ` or `Z`).
    func extractRoomToken(from text: String) -> String? {
        // Patterns covered: EA-Z04, EAZ04, EA Z04, EA101, EA-101, Z04, A1, B-203
        let pattern = #"\b([A-Za-z]{1,4}[\s\-_]?[A-Za-z]?[\s\-_]?\d{2,4})\b"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else { return nil }
        let nsText = text as NSString
        let range = NSRange(location: 0, length: nsText.length)
        guard let match = regex.firstMatch(in: text, options: [], range: range), match.numberOfRanges > 1 else {
            return nil
        }
        return nsText.substring(with: match.range(at: 1))
    }

    // MARK: - planRouteToDestination

    /// Calls the existing deterministic route engine. Never invents a path.
    ///
    /// When the POI links to a known `roomId` we route through
    /// `computeRoute(config:destinationRoomId:)` — the EXACT code path the
    /// manual destination list uses. This guarantees that anything routable
    /// from the room picker is also routable from the assistant.
    func planRouteToDestination(_ poi: SemanticPoiCatalog.Poi) -> BuildingPackageLoader.LoadedPackage? {
        if let roomId = poi.roomId,
           config.rooms.contains(where: { $0.id == roomId }) {
            return BuildingPackageLoader.computeRoute(
                config: config,
                destinationRoomId: roomId
            )
        }
        return BuildingPackageLoader.computeRoute(
            config: config,
            destinationNodeId: poi.nodeId,
            destinationLabel: poi.displayName
        )
    }

    // MARK: - Synthetic POI helper

    /// Build a `SemanticPoiCatalog.Poi` that mirrors a `PackageRoom`. Used as
    /// a fallback so the assistant can answer questions even when the
    /// semantic catalog hasn't been seeded for a building.
    func roomToSyntheticPoi(_ room: BuildingPackageLoader.PackageRoom) -> SemanticPoiCatalog.Poi {
        let rawCategory = room.category?.lowercased() ?? "place"
        let canonicalCategory = catalog.canonicalCategory(forAlias: rawCategory) ?? rawCategory
        // Build categories in deterministic order so `categories.first` is
        // always the canonical key. Subsequent entries pull in the room's
        // raw category and the catalog's sibling synonyms (e.g. "toilet"-
        // tagged rooms also match "wc" / "bathroom" / "restroom" queries).
        var orderedCategories: [String] = [canonicalCategory]
        var seen: Set<String> = [canonicalCategory]
        for extra in [rawCategory] + (catalog.categorySynonyms[canonicalCategory]?.map { $0.lowercased() } ?? []) {
            guard !seen.contains(extra), !extra.isEmpty else { continue }
            orderedCategories.append(extra)
            seen.insert(extra)
        }
        let aliases = Array(Set([
            room.displayName.lowercased(),
            room.id.lowercased(),
            rawCategory
        ])).filter { !$0.isEmpty }
        return SemanticPoiCatalog.Poi(
            id: "room-\(room.id)",
            displayName: room.displayName,
            roomId: room.id,
            nodeId: room.destinationNodeId,
            buildingId: nil,
            floorId: room.floorId,
            type: canonicalCategory,
            categories: orderedCategories,
            aliases: aliases,
            products: [],
            description: room.description,
            priority: 0.5,
            isNavigable: true,
            isRestricted: false,
            demoOnly: false
        )
    }

    // MARK: - rankCandidateDestinations

    /// Rank a candidate set: highest declared `priority` and category match
    /// first, shortest route distance second. Filters out non-navigable /
    /// restricted POIs and POIs with no computable route.
    func rankCandidateDestinations(
        _ candidates: [SemanticPoiCatalog.Poi],
        intentCategory: String?
    ) -> [AiRouteCandidate] {
        let canonicalIntent = intentCategory.flatMap { catalog.canonicalCategory(forAlias: $0) ?? $0.lowercased() }

        var scored: [(SemanticPoiCatalog.Poi, BuildingPackageLoader.LoadedPackage?, Double)] = []
        for poi in candidates {
            guard poi.isNavigable, !poi.isRestricted else {
                print("[RouteAgentTools] Skip non-navigable POI \(poi.id) (\(poi.displayName))")
                continue
            }
            let route = planRouteToDestination(poi)
            guard let r = route else {
                print("[RouteAgentTools] No route for \(poi.id) (\(poi.displayName)) roomId=\(poi.roomId ?? "nil") nodeId=\(poi.nodeId)")
                continue
            }

            let priority = poi.priority ?? 0.5
            // Distance-shorter-is-better (normalised softly so it doesn't dominate).
            let distancePenalty = min(r.totalDistance / 100.0, 1.0)
            // Category match boost.
            let categoryBoost: Double = {
                guard let canonical = canonicalIntent else { return 0.0 }
                return poi.categories.map { $0.lowercased() }.contains(canonical) ? 0.4 : 0.0
            }()
            let score = priority + categoryBoost - 0.4 * distancePenalty
            scored.append((poi, route, score))
        }

        scored.sort { lhs, rhs in
            if abs(lhs.2 - rhs.2) > 0.05 { return lhs.2 > rhs.2 }
            return (lhs.1?.totalDistance ?? .infinity) < (rhs.1?.totalDistance ?? .infinity)
        }

        return scored.map { (poi, route, score) in
            let distance = route?.totalDistance
            let stepCount = route.map { max(1, $0.arrows.filter { $0.type != .follow }.count) }
            let summary = explainCandidate(poi: poi, distanceMeters: distance, stepCount: stepCount)
            return AiRouteCandidate(
                id: poi.id,
                displayName: poi.displayName,
                nodeId: poi.nodeId,
                roomId: poi.roomId,
                category: poi.categories.first,
                routeDistanceMeters: distance,
                routeStepCount: stepCount,
                relevanceScore: score,
                summary: summary
            )
        }
    }

    // MARK: - explainCandidate

    /// Short user-facing subtitle for a candidate card.
    func explainCandidate(
        poi: SemanticPoiCatalog.Poi,
        distanceMeters: Double?,
        stepCount: Int?
    ) -> String {
        var parts: [String] = []
        if let d = distanceMeters {
            parts.append("\(Int(d.rounded())) m")
        }
        if let s = stepCount {
            parts.append("\(s) \(s == 1 ? "step" : "steps")")
        }
        if let desc = poi.description, !desc.isEmpty {
            parts.append(desc)
        }
        return parts.joined(separator: " · ")
    }

    // MARK: - createRouteSelectionResult

    /// Convenience builder used by every agent backend so the structure of the
    /// final response stays consistent.
    func createRouteSelectionResult(
        intent: AiRouteIntent,
        message: String,
        candidates: [AiRouteCandidate],
        selected: AiRouteCandidate?,
        confidence: Double,
        requiresConfirmation: Bool,
        toolTrace: [String]
    ) -> AiRouteAgentResult {
        let routeSummary: String? = {
            guard let s = selected, let d = s.routeDistanceMeters else { return nil }
            let steps = s.routeStepCount ?? 1
            return "\(Int(d.rounded())) m · \(steps) \(steps == 1 ? "step" : "steps")"
        }()
        return AiRouteAgentResult(
            detectedIntent: intent,
            assistantMessage: message,
            candidateDestinations: candidates,
            selectedDestination: selected,
            routeSummary: routeSummary,
            confidence: confidence,
            requiresConfirmation: requiresConfirmation,
            errorMessage: nil,
            toolTrace: toolTrace
        )
    }

    // MARK: - Internals

    private func normalize(_ s: String) -> String {
        s.lowercased()
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .replacingOccurrences(of: ".", with: " ")
            .replacingOccurrences(of: ",", with: " ")
            .replacingOccurrences(of: "  ", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
