import Foundation

/// Abstraction over the AI route-selection assistant.
///
/// Two backends are provided:
/// - `MockAiRouteAgent`: deterministic, always-on intent rules + the same
///   tool layer the GPT agent uses. This is what runs during the demo.
/// - `BackendGptRouteAgent`: calls an HTTP LLM proxy hosted on the admin API,
///   then re-uses the deterministic tool layer to execute tool calls and plan
///   the actual route. Falls back to the mock agent on any failure.
///
/// Both backends MUST go through the deterministic tool layer for routing —
/// the LLM is only allowed to interpret intent and pick among tool-returned
/// candidates.
protocol AiRouteAgent {
    func handleUserRequest(_ request: AiRouteRequest) async -> AiRouteAgentResult
}
