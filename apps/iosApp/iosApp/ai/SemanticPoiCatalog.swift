import Foundation

/// Semantic point-of-interest catalog used by the AI route-selection assistant.
///
/// Loaded from `semantic_pois.json` in the app bundle. Each POI is mapped to a
/// valid `nodeId` from the navigation graph so the deterministic route engine
/// can always plan a path; POIs are NEVER allowed to invent destinations.
///
/// The catalog is intentionally separate from `rooms.json`. Rooms.json drives
/// the manual destination list shown in the UI; this catalog adds richer
/// semantic metadata (categories, aliases, products) that the agent uses to
/// match informal natural-language requests.
struct SemanticPoiCatalog {

    // MARK: - Models

    struct Poi: Codable, Identifiable, Equatable {
        let id: String
        let displayName: String
        /// Optional link to a row in rooms.json. nil => agent-only POI.
        let roomId: String?
        /// REQUIRED. Must reference a valid node in nav_graph.json.
        let nodeId: String
        let buildingId: String?
        let floorId: String?
        let type: String
        let categories: [String]
        let aliases: [String]
        let products: [String]
        let description: String?
        let priority: Double?
        let isNavigable: Bool
        let isRestricted: Bool
        let demoOnly: Bool?
    }

    struct Catalog: Codable {
        let version: Int
        let buildingId: String?
        let notes: String?
        let pois: [Poi]
        let categorySynonyms: [String: [String]]?
    }

    // MARK: - State

    let pois: [Poi]
    let categorySynonyms: [String: [String]]

    /// Reverse lookup: any synonym (lower-cased) -> canonical category.
    let canonicalCategoryByAlias: [String: String]

    // MARK: - Loading

    enum LoadError: Error, CustomStringConvertible {
        case fileMissing
        case decodingFailed(Error)

        var description: String {
            switch self {
            case .fileMissing: return "semantic_pois.json not found in bundle"
            case .decodingFailed(let e): return "Failed to decode semantic_pois.json: \(e.localizedDescription)"
            }
        }
    }

    static func loadFromBundle() -> Result<SemanticPoiCatalog, LoadError> {
        guard let url = Bundle.main.url(forResource: "semantic_pois", withExtension: "json") else {
            return .failure(.fileMissing)
        }
        do {
            let data = try Data(contentsOf: url)
            let catalog = try JSONDecoder().decode(Catalog.self, from: data)
            return .success(SemanticPoiCatalog(catalog: catalog))
        } catch {
            return .failure(.decodingFailed(error))
        }
    }

    init(catalog: Catalog) {
        self.pois = catalog.pois
        // Always merge in baseline synonyms so the assistant can still map
        // common rooms.json categories ("toilet" → "restroom", "cafe" →
        // "coffee", ...) even when `semantic_pois.json` is missing or only
        // partially configured.
        var merged = SemanticPoiCatalog.baselineCategorySynonyms
        for (canonical, words) in catalog.categorySynonyms ?? [:] {
            let existing = merged[canonical] ?? []
            merged[canonical] = Array(Set(existing + words))
        }
        self.categorySynonyms = merged

        var alias: [String: String] = [:]
        for (canonical, words) in merged {
            alias[canonical.lowercased()] = canonical
            for w in words {
                alias[w.lowercased()] = canonical
            }
        }
        self.canonicalCategoryByAlias = alias
    }

    /// Empty catalog (used as a safe fallback when loading fails).
    init() {
        self.pois = []
        self.categorySynonyms = SemanticPoiCatalog.baselineCategorySynonyms
        var alias: [String: String] = [:]
        for (canonical, words) in SemanticPoiCatalog.baselineCategorySynonyms {
            alias[canonical.lowercased()] = canonical
            for w in words {
                alias[w.lowercased()] = canonical
            }
        }
        self.canonicalCategoryByAlias = alias
    }

    /// Hard-coded synonym fallback. These are always available so rooms.json
    /// categories can be matched against natural-language requests even when
    /// the semantic catalog file is missing or only partially populated.
    static let baselineCategorySynonyms: [String: [String]] = [
        "restroom": ["restroom", "toilet", "wc", "bathroom", "loo", "lavatory", "washroom"],
        "coffee":   ["coffee", "cafe", "cafeteria", "caffeine", "espresso", "latte", "vending_machine"],
        "elevator": ["elevator", "lift", "vertical_transport"],
        "classroom": ["classroom", "lecture", "lecture hall", "lecture room", "course", "class"],
        "lab":      ["lab", "laboratory"]
    ]

    // MARK: - Helpers

    func poi(byId id: String) -> Poi? { pois.first { $0.id == id } }

    func canonicalCategory(forAlias alias: String) -> String? {
        canonicalCategoryByAlias[alias.lowercased()]
    }
}
