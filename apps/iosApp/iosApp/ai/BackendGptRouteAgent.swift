import Foundation

/// GPT-backed route agent.
///
/// API keys are kept off the device: this client calls the existing admin API
/// (`/mobile/assistant/route-intent`) which proxies to the configured LLM
/// (Ollama / OpenAI-compatible). The LLM only returns a structured JSON
/// describing which deterministic tool to call; the actual route is then
/// produced by the on-device `RouteAgentTools` layer.
///
/// Any failure (network, parsing, no candidates, GPT timeout, ...) is caught
/// and silently delegated to the supplied mock agent so the demo never crashes
/// on a flaky network.
final class BackendGptRouteAgent: AiRouteAgent {

    private let tools: RouteAgentTools
    private let fallback: AiRouteAgent
    private let baseURL: String
    private let session: URLSession
    private let timeoutSeconds: TimeInterval

    init(
        tools: RouteAgentTools,
        fallback: AiRouteAgent,
        baseURL: String,
        timeoutSeconds: TimeInterval = 8.0,
        session: URLSession = .shared
    ) {
        self.tools = tools
        self.fallback = fallback
        self.baseURL = baseURL
        self.timeoutSeconds = timeoutSeconds
        self.session = session
    }

    // MARK: - AiRouteAgent

    func handleUserRequest(_ request: AiRouteRequest) async -> AiRouteAgentResult {
        var trace: [String] = ["backendGpt:start"]

        let intent = await fetchIntent(for: request, trace: &trace)
        guard let intent else {
            trace.append("backendGpt:fallbackToMock")
            let mockResult = await fallback.handleUserRequest(request)
            return mockResult.appending(trace: trace)
        }

        switch intent.intent {
        case .roomIdentifier:
            if let token = intent.roomToken,
               let poi = tools.resolveRoomIdentifier(token) {
                trace.append("tool:resolveRoomIdentifier=\(poi.id)")
                return executeSinglePoi(poi, intent: .roomIdentifier, message: intent.assistantMessage ?? defaultRoomMessage(poi), trace: trace)
            }
            trace.append("backendGpt:roomTokenUnresolved")
            return await fallback.handleUserRequest(request).appending(trace: trace)

        case .categorySearch:
            guard let category = intent.category, !category.isEmpty else {
                return await fallback.handleUserRequest(request).appending(trace: trace)
            }
            trace.append("tool:searchPoiByCategory=\(category)")
            let candidates = tools.searchPoiByCategory(category)
            let ranked = tools.rankCandidateDestinations(candidates, intentCategory: category)
            guard let top = ranked.first else {
                return AiRouteAgentResult.failure(
                    message: "I couldn't find a \(category) in this building right now.",
                    intent: .categorySearch,
                    trace: trace
                )
            }
            return tools.createRouteSelectionResult(
                intent: .categorySearch,
                message: intent.assistantMessage ?? defaultCategoryMessage(category),
                candidates: Array(ranked.prefix(3)),
                selected: top,
                confidence: max(0.5, min(intent.confidence ?? 0.85, 0.99)),
                requiresConfirmation: true,
                toolTrace: trace
            )

        case .freeText, .ambiguous, .unknown:
            return await fallback.handleUserRequest(request).appending(trace: trace)
        }
    }

    // MARK: - Networking

    private struct GptIntentResponse: Decodable {
        let intent: AiRouteIntent
        let category: String?
        let roomToken: String?
        let assistantMessage: String?
        let confidence: Double?
    }

    private func fetchIntent(for request: AiRouteRequest, trace: inout [String]) async -> GptIntentResponse? {
        guard let url = URL(string: "\(baseURL)/mobile/assistant/route-intent") else {
            trace.append("backendGpt:invalidUrl")
            return nil
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.timeoutInterval = timeoutSeconds

        let body: [String: Any] = [
            "rawText": request.rawText,
            "inputMode": request.inputMode.rawValue,
            "buildingId": tools.config.manifest.buildingId,
            "availableCategories": Array(Set(tools.catalog.pois.flatMap { $0.categories })),
            "availableRoomAliases": tools.catalog.pois.flatMap { $0.aliases }
        ]
        do {
            req.httpBody = try JSONSerialization.data(withJSONObject: body, options: [])
        } catch {
            trace.append("backendGpt:bodyEncodeFailed")
            return nil
        }

        do {
            let (data, response) = try await session.data(for: req)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                trace.append("backendGpt:httpError=\(((response as? HTTPURLResponse)?.statusCode).map(String.init) ?? "?")")
                return nil
            }
            let parsed = try JSONDecoder().decode(GptIntentResponse.self, from: data)
            trace.append("backendGpt:intent=\(parsed.intent.rawValue)")
            return parsed
        } catch {
            trace.append("backendGpt:networkError=\(error.localizedDescription)")
            return nil
        }
    }

    // MARK: - Helpers

    private func executeSinglePoi(
        _ poi: SemanticPoiCatalog.Poi,
        intent: AiRouteIntent,
        message: String,
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
            confidence: 0.94,
            requiresConfirmation: true,
            toolTrace: trace
        )
    }

    private func defaultRoomMessage(_ poi: SemanticPoiCatalog.Poi) -> String {
        "Got it, \(poi.displayName). I found the room. Should I take you there?"
    }

    private func defaultCategoryMessage(_ category: String) -> String {
        switch category.lowercased() {
        case "restroom", "toilet", "wc", "bathroom":
            return "I found the nearest restroom for you. Want me to start the route?"
        case "coffee", "cafe", "cafeteria":
            return "I got you. The closest coffee spot looks like the best option. Want to go there?"
        default:
            return "I found the closest \(category). Want me to start the route?"
        }
    }
}

private extension AiRouteAgentResult {
    func appending(trace extra: [String]) -> AiRouteAgentResult {
        AiRouteAgentResult(
            detectedIntent: detectedIntent,
            assistantMessage: assistantMessage,
            candidateDestinations: candidateDestinations,
            selectedDestination: selectedDestination,
            routeSummary: routeSummary,
            confidence: confidence,
            requiresConfirmation: requiresConfirmation,
            errorMessage: errorMessage,
            toolTrace: extra + toolTrace
        )
    }
}
