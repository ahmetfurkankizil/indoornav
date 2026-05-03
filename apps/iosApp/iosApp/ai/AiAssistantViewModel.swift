import Foundation
import SwiftUI

/// Drives the AI route assistant section embedded inside `DestinationSelectView`.
///
/// Keeps track of the current request, the agent result, and the user's
/// confirmation state. Every code path is null-safe: if the semantic POI
/// catalog or reviewed config can't be loaded the assistant degrades to a
/// disabled banner and the manual destination list keeps working.
@MainActor
final class AiAssistantViewModel: ObservableObject {

    // MARK: - Published state

    enum Status: Equatable {
        case idle
        case thinking
        case awaitingConfirmation(AiRouteAgentResult)
        case noMatch(String)
    }

    @Published var inputText: String = ""
    @Published private(set) var status: Status = .idle
    @Published var mode: AiRouteAgentMode = .mock
    @Published var lastInputMode: AiRouteRequest.InputMode = .text
    @Published var showDevTrace: Bool = false

    // MARK: - Internals

    private let flow: NavigationFlowModel
    private(set) var catalog: SemanticPoiCatalog
    private var tools: RouteAgentTools?
    private var lastResult: AiRouteAgentResult?
    /// Tracks the config the current `tools` was built against so we can
    /// rebuild when a new package (e.g. a remote v2 building) replaces it.
    private var toolsConfigSignature: Int?

    init(flow: NavigationFlowModel) {
        self.flow = flow

        switch SemanticPoiCatalog.loadFromBundle() {
        case .success(let c):
            self.catalog = c
            print("[AiAssistantViewModel] Loaded semantic catalog with \(c.pois.count) POIs")
        case .failure(let error):
            print("[AiAssistantViewModel] \(error.description) — assistant will fall back to rooms.json only")
            self.catalog = SemanticPoiCatalog()
        }

        rebuildToolsIfNeeded()
    }

    // MARK: - Capability gating

    var isAssistantAvailable: Bool {
        rebuildToolsIfNeeded()
        // `rooms.json` is the assistant's primary data source. The semantic
        // catalog is purely supplementary, so a missing/stale
        // `semantic_pois.json` no longer disables the assistant.
        return tools != nil && !(flow.reviewedConfig?.rooms.isEmpty ?? true)
    }

    // MARK: - Actions

    func submit(transcribed text: String? = nil, inputMode: AiRouteRequest.InputMode = .text) {
        rebuildToolsIfNeeded()
        let raw = (text ?? inputText).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return }
        guard let tools else {
            status = .noMatch("AI assistant is unavailable right now.")
            return
        }

        lastInputMode = inputMode
        status = .thinking

        let agent = makeAgent(tools: tools)
        let request = AiRouteRequest(rawText: raw, inputMode: inputMode, startNodeId: nil)

        print("[AiAssistantViewModel] submit raw=\"\(raw)\" mode=\(mode) inputMode=\(inputMode)")

        Task { [weak self] in
            let result = await agent.handleUserRequest(request)
            await MainActor.run {
                guard let self else { return }
                self.lastResult = result
                print("[AiAssistantViewModel] result intent=\(result.detectedIntent.rawValue) selected=\(result.selectedDestination?.displayName ?? "nil") trace=[\(result.toolTrace.joined(separator: " | "))] msg=\"\(result.assistantMessage)\"")
                if result.errorMessage != nil || result.selectedDestination == nil {
                    self.status = .noMatch(result.assistantMessage)
                } else {
                    self.status = .awaitingConfirmation(result)
                }
            }
        }
    }

    func acceptCurrentSuggestion() {
        guard case .awaitingConfirmation(let result) = status,
              let selected = result.selectedDestination else { return }
        flow.selectAssistantDestination(
            displayName: selected.displayName,
            nodeId: selected.nodeId,
            roomId: selected.roomId,
            category: selected.category
        )
        status = .idle
        inputText = ""
    }

    /// Confirm the current suggestion AND immediately start AR navigation,
    /// skipping the route-preview step. Wired to the "Navigate now" button
    /// in the assistant confirmation popup.
    func acceptAndStartNavigation() {
        guard case .awaitingConfirmation(let result) = status,
              let selected = result.selectedDestination else { return }
        flow.selectAssistantDestination(
            displayName: selected.displayName,
            nodeId: selected.nodeId,
            roomId: selected.roomId,
            category: selected.category
        )
        // selectAssistantDestination drops us in .routePreview; jump straight
        // to AR now that the user has already confirmed via the popup.
        flow.startNavigation()
        status = .idle
        inputText = ""
    }

    func chooseAlternativeCandidate(_ candidate: AiRouteCandidate) {
        flow.selectAssistantDestination(
            displayName: candidate.displayName,
            nodeId: candidate.nodeId,
            roomId: candidate.roomId,
            category: candidate.category
        )
        flow.startNavigation()
        status = .idle
        inputText = ""
    }

    func cancel() {
        status = .idle
    }

    func reset() {
        status = .idle
        inputText = ""
    }

    // MARK: - Helpers

    private func rebuildToolsIfNeeded() {
        guard let config = flow.reviewedConfig else {
            tools = nil
            toolsConfigSignature = nil
            return
        }
        // Rebuild whenever the config changes (e.g. a new remote v2 package
        // replaces the bundled one). The signature is cheap and stable.
        let signature = config.rooms.count &* 31 &+ config.nodes.count &* 7 &+ config.edges.count
        if tools == nil || toolsConfigSignature != signature {
            tools = RouteAgentTools(catalog: catalog, config: config)
            toolsConfigSignature = signature
            print("[AiAssistantViewModel] Built tools — rooms=\(config.rooms.count) nodes=\(config.nodes.count) catalogPois=\(catalog.pois.count)")
        }
    }

    private func makeAgent(tools: RouteAgentTools) -> AiRouteAgent {
        let mock = MockAiRouteAgent(tools: tools)
        switch mode {
        case .mock:
            return mock
        case .backendGpt:
            return BackendGptRouteAgent(
                tools: tools,
                fallback: mock,
                baseURL: NavigationFlowModel.adminAPIBaseURL
            )
        }
    }

    // MARK: - Convenience accessors for the view

    var assistantResult: AiRouteAgentResult? {
        if case .awaitingConfirmation(let r) = status { return r }
        return nil
    }

    var noMatchMessage: String? {
        if case .noMatch(let msg) = status { return msg }
        return nil
    }

    var isThinking: Bool {
        if case .thinking = status { return true }
        return false
    }
}
