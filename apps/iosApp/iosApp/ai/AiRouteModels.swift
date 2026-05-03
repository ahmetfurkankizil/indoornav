import Foundation

/// Public data models exchanged between the AI route assistant UI, the agent
/// orchestration layer and the deterministic tool layer.
///
/// These types are intentionally framework-free (no SwiftUI / Combine imports)
/// so the same models can be reused by tests, mock agents and a future GPT
/// backend integration.

// MARK: - Intent

/// Coarse classification of what the user wants. The agent may produce more
/// than one tool call per request, but every result still carries one primary
/// intent so the UI can render an appropriate confirmation card.
enum AiRouteIntent: String, Codable, Equatable {
    case roomIdentifier   // e.g. "EA-Z04", "ea101"
    case categorySearch   // e.g. "restroom", "coffee"
    case freeText         // fuzzy fallback (alias / description match)
    case ambiguous        // multiple plausible categories
    case unknown          // could not be interpreted
}

// MARK: - Request

struct AiRouteRequest: Equatable {
    let rawText: String
    /// Source of the input — text typing or speech-to-text.
    let inputMode: InputMode
    /// Optional identifier of the start node. When nil the agent uses the
    /// validated entrance marker from the navigation flow.
    let startNodeId: String?

    enum InputMode: String, Codable, Equatable {
        case text
        case voice
    }
}

// MARK: - Result

/// Concrete destination candidate the assistant considered.
struct AiRouteCandidate: Equatable, Identifiable {
    let id: String                      // POI id
    let displayName: String
    let nodeId: String
    let roomId: String?
    let category: String?
    /// Estimated walking distance in metres for the deterministic route, if
    /// the route engine could compute it. nil => no route available.
    let routeDistanceMeters: Double?
    let routeStepCount: Int?
    /// Higher = more relevant.
    let relevanceScore: Double
    /// Subtitle shown to the user (e.g. "East corridor – 21 m").
    let summary: String
}

/// Final structured response returned by `AiRouteAgent.handleUserRequest(_:)`.
struct AiRouteAgentResult: Equatable {
    let detectedIntent: AiRouteIntent
    let assistantMessage: String
    let candidateDestinations: [AiRouteCandidate]
    let selectedDestination: AiRouteCandidate?
    let routeSummary: String?
    /// 0.0...1.0
    let confidence: Double
    let requiresConfirmation: Bool
    let errorMessage: String?
    /// Names of the deterministic tools the agent invoked (for debugging /
    /// the in-app dev trace).
    let toolTrace: [String]

    static func failure(message: String, intent: AiRouteIntent = .unknown, trace: [String] = []) -> AiRouteAgentResult {
        AiRouteAgentResult(
            detectedIntent: intent,
            assistantMessage: message,
            candidateDestinations: [],
            selectedDestination: nil,
            routeSummary: nil,
            confidence: 0.0,
            requiresConfirmation: false,
            errorMessage: message,
            toolTrace: trace
        )
    }
}

// MARK: - Agent Mode

/// Backing implementation that should be used for the AI assistant.
enum AiRouteAgentMode: String, Codable, CaseIterable, Identifiable {
    case mock       // always-on, fully deterministic intent rules
    case backendGpt // calls the admin-api LLM proxy, falls back to mock on failure

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .mock: return "Mock (offline)"
        case .backendGpt: return "Backend GPT"
        }
    }
}
