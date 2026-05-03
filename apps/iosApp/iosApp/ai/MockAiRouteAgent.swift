import Foundation

/// Always-available, GPT-free implementation of the route assistant.
///
/// The mock agent uses small deterministic intent rules that recognise the
/// three required demo utterances ("I'm about to pee myself", "I have a class
/// in EA-Z04", "I really need to drink a coffee") plus close paraphrases, and
/// then delegates to the same `RouteAgentTools` the GPT agent uses.
///
/// This means the mock NEVER bypasses the route planner with a fake response —
/// every selected destination is anchored to a real graph node and routed by
/// the existing deterministic engine.
final class MockAiRouteAgent: AiRouteAgent {

    private let tools: RouteAgentTools

    init(tools: RouteAgentTools) {
        self.tools = tools
    }

    // MARK: - Public API

    func handleUserRequest(_ request: AiRouteRequest) async -> AiRouteAgentResult {
        let raw = request.rawText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else {
            return AiRouteAgentResult.failure(message: "Tell Vectura where you want to go.")
        }
        let normalized = normalize(raw)
        var trace: [String] = ["normalize"]

        print("[MockAgent] handleUserRequest raw=\"\(raw)\" rooms=\(tools.config.rooms.count) catalogPois=\(tools.catalog.pois.count)")

        // 0) Hand-curated prompt bank (handles typos + extra words).
        if let match = MockPromptBank.bestMatch(for: raw) {
            print("[MockAgent] bank matched entry=\(match.entry.id) phrase=\"\(match.phrase)\" score=\(String(format: "%.2f", match.score))")
            trace.append("promptBank:matched=\(match.entry.id) score=\(String(format: "%.2f", match.score))")
            if let result = resolveBankMatch(match, trace: &trace) {
                return result
            }
            trace.append("promptBank:noResolution")
            print("[MockAgent] bank match for '\(match.entry.id)' could not resolve; falling through to rules")
        } else {
            print("[MockAgent] no bank match")
        }

        // 1) Hard room-id resolution always wins (e.g. "EA-Z04").
        if let token = tools.extractRoomToken(from: raw),
           let poi = tools.resolveRoomIdentifier(token) {
            trace.append("extractRoomToken=\(token)")
            trace.append("resolveRoomIdentifier=\(poi.id)")
            return finishWithSinglePoi(
                poi,
                intent: .roomIdentifier,
                message: "Got it, \(poi.displayName). I found the \(displayCategory(poi)). Should I take you there?",
                confidence: 0.95,
                trace: trace
            )
        }

        // 2) Restroom (matches the urgent demo case "pee myself").
        if matchesRestroom(normalized) {
            trace.append("intent=restroom")
            let candidates = tools.searchPoiByCategory("restroom")
            trace.append("searchPoiByCategory(restroom) -> \(candidates.count)")
            let ranked = tools.rankCandidateDestinations(candidates, intentCategory: "restroom")
            trace.append("rankCandidateDestinations -> \(ranked.count)")
            guard let top = ranked.first else {
                return AiRouteAgentResult.failure(
                    message: "I couldn't find a restroom in this building right now.",
                    intent: .categorySearch,
                    trace: trace
                )
            }
            let urgent = isUrgent(normalized)
            let message: String = urgent
                ? "Okay, sounds urgent. I found the nearest restroom for you. Want me to start the route?"
                : "I found the nearest restroom. Want me to start the route?"
            return tools.createRouteSelectionResult(
                intent: .categorySearch,
                message: message,
                candidates: Array(ranked.prefix(3)),
                selected: top,
                confidence: urgent ? 0.95 : 0.9,
                requiresConfirmation: true,
                toolTrace: trace
            )
        }

        // 3) Coffee / cafe.
        if matchesCoffee(normalized) {
            trace.append("intent=coffee")
            let candidates = tools.searchPoiByCategory("coffee")
            trace.append("searchPoiByCategory(coffee) -> \(candidates.count)")
            let ranked = tools.rankCandidateDestinations(candidates, intentCategory: "coffee")
            guard let top = ranked.first else {
                return AiRouteAgentResult.failure(
                    message: "I couldn't find a coffee spot in this building right now.",
                    intent: .categorySearch,
                    trace: trace
                )
            }
            return tools.createRouteSelectionResult(
                intent: .categorySearch,
                message: "I got you. The closest coffee spot looks like the best option. Want to go there?",
                candidates: Array(ranked.prefix(3)),
                selected: top,
                confidence: 0.92,
                requiresConfirmation: true,
                toolTrace: trace
            )
        }

        // 4) Generic category words ("classroom", "lab", "elevator", ...).
        if let canonical = detectCanonicalCategory(in: normalized) {
            trace.append("intent=category:\(canonical)")
            let candidates = tools.searchPoiByCategory(canonical)
            let ranked = tools.rankCandidateDestinations(candidates, intentCategory: canonical)
            if let top = ranked.first {
                let multi = ranked.count > 1
                let message = multi
                    ? "I found a few \(canonical) options. Want me to take you to \(top.displayName)?"
                    : "Sure. I'll take you to \(top.displayName) if that works."
                return tools.createRouteSelectionResult(
                    intent: .categorySearch,
                    message: message,
                    candidates: Array(ranked.prefix(3)),
                    selected: top,
                    confidence: 0.78,
                    requiresConfirmation: true,
                    toolTrace: trace
                )
            }
        }

        // 5) Free-text fallback: search aliases and display names.
        trace.append("intent=freeText")
        let nameHits = tools.searchPoiByName(raw)
        if !nameHits.isEmpty {
            let ranked = tools.rankCandidateDestinations(nameHits, intentCategory: nil)
            if let top = ranked.first {
                return tools.createRouteSelectionResult(
                    intent: .freeText,
                    message: "I think you mean \(top.displayName). Should I take you there?",
                    candidates: Array(ranked.prefix(3)),
                    selected: top,
                    confidence: 0.6,
                    requiresConfirmation: true,
                    toolTrace: trace
                )
            }
        }

        // 6) Nothing matched.
        trace.append("intent=unknown")
        return AiRouteAgentResult.failure(
            message: "I couldn't find a matching place in this building. Try a room name like EA-Z04 or something like coffee or restroom.",
            intent: .unknown,
            trace: trace
        )
    }

    // MARK: - Prompt bank resolution

    private func resolveBankMatch(
        _ match: MockPromptBank.Match,
        trace: inout [String]
    ) -> AiRouteAgentResult? {
        let entry = match.entry
        switch entry.resolution {
        case .roomId(let roomId):
            // Try exact id first. If the active package uses different ids
            // (e.g. a remote v2 building), fall back to a display-name
            // search using the id as a hint ("fameo-cafe" → "fameo cafe").
            let room = tools.findRoom(byId: roomId)
                ?? tools.findRoom(byDisplayName: hintFromRoomId(roomId))
            guard let room else {
                trace.append("promptBank:roomMissing=\(roomId)")
                print("[MockAgent] Bank entry '\(entry.id)' wanted room '\(roomId)' but rooms.json has no such id (hint='\(hintFromRoomId(roomId))')")
                return nil
            }
            return makeResultForRoom(
                room,
                intent: entry.intent,
                response: entry.response,
                urgent: entry.urgent,
                bankScore: match.score,
                trace: trace
            )

        case .roomDisplayName(let name):
            guard let room = tools.findRoom(byDisplayName: name) else {
                trace.append("promptBank:roomNameMissing=\(name)")
                print("[MockAgent] Bank entry '\(entry.id)' wanted room named '\(name)' but rooms.json has no match")
                return nil
            }
            return makeResultForRoom(
                room,
                intent: entry.intent,
                response: entry.response,
                urgent: entry.urgent,
                bankScore: match.score,
                trace: trace
            )

        case .category(let category):
            guard category != "__none" else {
                // Conversational entry without a navigable destination.
                trace.append("promptBank:conversational")
                return AiRouteAgentResult(
                    detectedIntent: entry.intent,
                    assistantMessage: entry.response,
                    candidateDestinations: [],
                    selectedDestination: nil,
                    routeSummary: nil,
                    confidence: bankConfidence(score: match.score, urgent: entry.urgent),
                    requiresConfirmation: false,
                    errorMessage: nil,
                    toolTrace: trace
                )
            }
            let candidates = tools.searchPoiByCategory(category)
            let ranked = tools.rankCandidateDestinations(candidates, intentCategory: category)
            print("[MockAgent] Bank entry '\(entry.id)' category=\(category) candidates=\(candidates.count) ranked=\(ranked.count)")
            guard let top = ranked.first else { return nil }
            return tools.createRouteSelectionResult(
                intent: entry.intent,
                message: substitute(entry.response, with: top.displayName),
                candidates: Array(ranked.prefix(3)),
                selected: top,
                confidence: bankConfidence(score: match.score, urgent: entry.urgent),
                requiresConfirmation: true,
                toolTrace: trace
            )

        case .genderedRestroom(let hint):
            let all = tools.searchPoiByCategory("restroom")
            let filtered: [SemanticPoiCatalog.Poi] = {
                switch hint {
                case .men:
                    return all.filter { matchesGenderHint($0, hint: ["men", "mens", "men's"]) }
                case .women:
                    return all.filter { matchesGenderHint($0, hint: ["women", "womens", "women's", "ladies"]) }
                case .any:
                    return all
                }
            }()
            let pool = filtered.isEmpty ? all : filtered
            let ranked = tools.rankCandidateDestinations(pool, intentCategory: "restroom")
            print("[MockAgent] Bank entry '\(entry.id)' gendered=\(hint.rawValue) candidates=\(all.count) filtered=\(filtered.count) ranked=\(ranked.count)")
            guard let top = ranked.first else { return nil }
            return tools.createRouteSelectionResult(
                intent: entry.intent,
                message: substitute(entry.response, with: top.displayName),
                candidates: Array(ranked.prefix(3)),
                selected: top,
                confidence: bankConfidence(score: match.score, urgent: entry.urgent),
                requiresConfirmation: true,
                toolTrace: trace
            )
        }
    }

    /// Convert a rooms.json id like "fameo-cafe" / "cs-lab" into a
    /// human-readable hint ("fameo cafe", "cs lab") suitable for a
    /// case-insensitive contains-match against actual `displayName`s. This
    /// is the fallback we use when the active package's room ids diverge
    /// from the bundled ones (e.g. UUIDs from a remote v2 building).
    private func hintFromRoomId(_ id: String) -> String {
        id.replacingOccurrences(of: "-", with: " ")
            .replacingOccurrences(of: "_", with: " ")
    }

    /// Build a result that points at a specific `PackageRoom` from
    /// `rooms.json`. Routes through the SAME `BuildingPackageLoader` call
    /// that the manual destination list uses.
    private func makeResultForRoom(
        _ room: BuildingPackageLoader.PackageRoom,
        intent: AiRouteIntent,
        response: String,
        urgent: Bool,
        bankScore: Double,
        trace: [String]
    ) -> AiRouteAgentResult? {
        let poi = tools.roomToSyntheticPoi(room)
        let ranked = tools.rankCandidateDestinations([poi], intentCategory: poi.categories.first)
        guard let candidate = ranked.first else {
            print("[MockAgent] Could not compute route to room '\(room.id)' (\(room.displayName))")
            return nil
        }
        return tools.createRouteSelectionResult(
            intent: intent,
            message: substitute(response, with: candidate.displayName),
            candidates: [candidate],
            selected: candidate,
            confidence: bankConfidence(score: bankScore, urgent: urgent),
            requiresConfirmation: true,
            toolTrace: trace
        )
    }

    private func matchesGenderHint(_ poi: SemanticPoiCatalog.Poi, hint words: [String]) -> Bool {
        // Use a token check instead of substring containment so "women"
        // doesn't accidentally satisfy a "men" hint.
        let haystack = (poi.displayName + " " + poi.aliases.joined(separator: " "))
            .lowercased()
            .replacingOccurrences(of: "'", with: "")
            .replacingOccurrences(of: "(", with: " ")
            .replacingOccurrences(of: ")", with: " ")
        let tokens = haystack.split(whereSeparator: { !$0.isLetter && !$0.isNumber }).map(String.init)
        let hintLowered = words.map { $0.replacingOccurrences(of: "'", with: "").lowercased() }
        return tokens.contains { token in hintLowered.contains(token) }
    }

    private func substitute(_ template: String, with destination: String) -> String {
        template.replacingOccurrences(of: "{destination}", with: destination)
    }

    private func bankConfidence(score: Double, urgent: Bool) -> Double {
        // Bank matches are high-trust by definition; map [0.5, 1.0] -> [0.85, 0.99].
        let clamped = max(0.5, min(1.0, score))
        let mapped = 0.85 + (clamped - 0.5) * (0.99 - 0.85) / 0.5
        return urgent ? min(0.99, mapped + 0.02) : mapped
    }

    // MARK: - Intent rules

    private func matchesRestroom(_ text: String) -> Bool {
        // Token-based check so "men" doesn't accidentally fire on words like
        // "menu" — the tokens are alphabetic chunks separated by spaces.
        let urgentTokens: Set<String> = ["pee", "peeing", "wee", "loo"]
        let neutralTokens: Set<String> = ["restroom", "toilet", "bathroom", "wc", "lavatory", "washroom"]
        let urgentPhrases = ["pee myself", "have to go", "gotta go", "bathroom emergency"]
        if urgentPhrases.contains(where: text.contains) { return true }
        let textTokens = text.split(whereSeparator: { !$0.isLetter && !$0.isNumber }).map(String.init)
        return textTokens.contains { urgentTokens.contains($0) || neutralTokens.contains($0) }
    }

    private func isUrgent(_ text: String) -> Bool {
        let urgent = ["pee myself", "about to pee", "pee", "urgent", "emergency", "gotta go", "have to go", "rush"]
        return urgent.contains(where: text.contains)
    }

    private func matchesCoffee(_ text: String) -> Bool {
        let phraseSignals = ["vending machine", "drink a coffee", "grab a coffee"]
        if phraseSignals.contains(where: text.contains) { return true }
        let tokens: Set<String> = [
            "coffee", "caffeine", "espresso", "latte", "cappuccino", "americano",
            "cafe", "cafeteria", "thirsty", "hungry", "snack", "drink", "tea"
        ]
        let textTokens = text.split(whereSeparator: { !$0.isLetter && !$0.isNumber }).map(String.init)
        return textTokens.contains { tokens.contains($0) }
    }

    private func detectCanonicalCategory(in text: String) -> String? {
        let candidates = ["classroom", "lab", "elevator", "lift", "office", "service", "cafe"]
        for word in candidates {
            if text.contains(word) {
                return tools.catalog.canonicalCategory(forAlias: word) ?? word
            }
        }
        return nil
    }

    private func displayCategory(_ poi: SemanticPoiCatalog.Poi) -> String {
        switch poi.type {
        case "classroom": return "classroom"
        case "lab": return "laboratory"
        case "cafe": return "cafe"
        case "restroom": return "restroom"
        case "service": return "service area"
        default: return "place"
        }
    }

    private func finishWithSinglePoi(
        _ poi: SemanticPoiCatalog.Poi,
        intent: AiRouteIntent,
        message: String,
        confidence: Double,
        trace: [String]
    ) -> AiRouteAgentResult {
        let ranked = tools.rankCandidateDestinations([poi], intentCategory: poi.categories.first)
        guard let candidate = ranked.first else {
            return AiRouteAgentResult.failure(
                message: "I found the place, but I couldn't create a valid route to it yet.",
                intent: intent,
                trace: trace
            )
        }
        return tools.createRouteSelectionResult(
            intent: intent,
            message: message,
            candidates: [candidate],
            selected: candidate,
            confidence: confidence,
            requiresConfirmation: true,
            toolTrace: trace
        )
    }

    private func normalize(_ s: String) -> String {
        s.lowercased()
            .replacingOccurrences(of: "'", with: "")
            .replacingOccurrences(of: "’", with: "")
            .replacingOccurrences(of: ",", with: " ")
            .replacingOccurrences(of: ".", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
