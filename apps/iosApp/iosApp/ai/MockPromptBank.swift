import Foundation

/// Hand-crafted prompt → response bank used by `MockAiRouteAgent`.
///
/// Each entry lists the canonical phrasings of a single user intent (typed
/// or spoken), what the assistant should say back, and how the request maps
/// onto the semantic POI catalog. The bank is the FIRST thing the mock
/// agent consults so the demo is reliable for the prompts the team cares
/// about; rule-based intent detection in `MockAiRouteAgent` only kicks in
/// when no entry scores high enough.
///
/// Matching is fuzzy on purpose: short user typos (Levenshtein distance ≤ 1
/// for short tokens, ≤ 2 for longer ones) and one or two extra words still
/// match. The matcher returns a score in [0, 1] together with the best
/// entry; `MockAiRouteAgent` requires score ≥ 0.55 (lower for very short
/// prompts) before it commits to a bank-driven response.
struct MockPromptBank {

    // MARK: - Entry model

    /// How a prompt resolves onto a real destination.
    ///
    /// Every resolution ultimately maps onto a `PackageRoom` from
    /// `rooms.json` — the SAME data the manual destination list uses. This
    /// guarantees that any prompt the assistant accepts is also tappable in
    /// the manual list and routes through the exact same engine.
    enum Resolution: Equatable {
        /// Pick this exact room from `rooms.json` by id (e.g. "fameo-cafe",
        /// "ea101"). Preferred for prompts that name a specific place.
        case roomId(String)
        /// Pick the room whose `displayName` matches this string (case
        /// insensitive). Useful when the user names a place by its label
        /// rather than its id.
        case roomDisplayName(String)
        /// Run a category search via `searchPoiByCategory(_:)`. Falls back
        /// to a rooms.json scan when the catalog has no matches.
        case category(String)
        /// Run a category search and additionally bias by gender keywords.
        case genderedRestroom(GenderHint)

        enum GenderHint: String { case men, women, any }
    }

    struct Entry {
        let id: String
        /// Multiple phrasings to maximise recall.
        let phrases: [String]
        /// Conversational response. Use `{destination}` to inject the
        /// resolved POI's display name.
        let response: String
        /// Whether to flag the request as urgent (used to colour the UI).
        let urgent: Bool
        /// What to do once the entry matches.
        let resolution: Resolution
        /// Coarse intent classification surfaced to the UI.
        let intent: AiRouteIntent
    }

    // MARK: - Contents (32 prompts)

    static let entries: [Entry] = [

        // ─── Restroom — urgent slang ─────────────────────────────────────
        Entry(id: "restroom-urgent-pee", phrases: [
            "i'm about to pee myself",
            "im about to pee myself",
            "i am about to pee myself",
            "about to pee myself",
            "i'm gonna pee myself",
            "i am gonna pee myself",
            "i need to pee right now",
            "i really need to pee",
            "i need a wc right now",
            "i need a bathroom right now",
            "i need a restroom right now"
        ],
        response: "Hold on tight! The closest restroom is {destination}. Want me to start the route?",
        urgent: true,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-need-pee", phrases: [
            "i need to pee",
            "i have to pee",
            "gotta pee",
            "i wanna pee"
        ],
        response: "Got it. The closest restroom is {destination}. Should I start the route?",
        urgent: false,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-nearest", phrases: [
            "where is the nearest restroom",
            "nearest restroom",
            "closest restroom",
            "show me the nearest restroom",
            "find me the closest restroom"
        ],
        response: "The nearest restroom is {destination}. Want me to take you there?",
        urgent: false,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-bathroom", phrases: [
            "i need a bathroom",
            "i need to use the bathroom",
            "where is the bathroom",
            "find a bathroom",
            "take me to the bathroom"
        ],
        response: "I found {destination} for you. Want me to start the route?",
        urgent: false,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-toilet", phrases: [
            "where is the toilet",
            "where's the toilet",
            "i need a toilet",
            "take me to the toilet",
            "toilet please"
        ],
        response: "{destination} is closest. Should I start the route?",
        urgent: false,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-wc", phrases: [
            "where is the wc",
            "wc please",
            "find the wc",
            "nearest wc",
            "i need a wc"
        ],
        response: "{destination} is the closest WC. Want me to start the route?",
        urgent: false,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-restroom-please", phrases: [
            "restroom please",
            "restroom",
            "i need a restroom",
            "wc",
            "toilet",
            "bathroom"
        ],
        response: "I found {destination}. Want me to start the route?",
        urgent: false,
        resolution: .category("restroom"),
        intent: .categorySearch),

        Entry(id: "restroom-mens", phrases: [
            "men's wc",
            "mens wc",
            "men wc",
            "men's restroom",
            "men's bathroom",
            "men toilet",
            "men's toilet"
        ],
        response: "Heading to the closest men's restroom — {destination}. Start the route?",
        urgent: false,
        resolution: .genderedRestroom(.men),
        intent: .freeText),

        Entry(id: "restroom-womens", phrases: [
            "women's wc",
            "womens wc",
            "women wc",
            "ladies room",
            "women's restroom",
            "women's bathroom",
            "women toilet",
            "women's toilet"
        ],
        response: "Heading to the closest women's restroom — {destination}. Start the route?",
        urgent: false,
        resolution: .genderedRestroom(.women),
        intent: .freeText),

        // ─── Coffee / Cafe / Snacks ──────────────────────────────────────
        Entry(id: "coffee-need", phrases: [
            "i really need to drink a coffee",
            "i need a coffee",
            "i need coffee",
            "i want a coffee",
            "i'd love a coffee",
            "give me coffee"
        ],
        response: "I got you. The closest coffee spot is {destination}. Want to go there?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "coffee-where", phrases: [
            "where can i get coffee",
            "where's the coffee",
            "where is the coffee",
            "find me coffee",
            "coffee please"
        ],
        response: "{destination} is the closest coffee spot. Should I start the route?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "cafe-where", phrases: [
            "where's the cafe",
            "where is the cafe",
            "find a cafe",
            "take me to a cafe",
            "i need a cafe"
        ],
        response: "Sure thing — {destination} is the nearest cafe. Want me to start the route?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "cafe-bare", phrases: [
            "cafe",
            "the cafe",
            "any cafe",
            "café",
            "kafe"
        ],
        response: "{destination} is the closest cafe. Want me to take you there?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "coffee-bare", phrases: [
            "coffee",
            "a coffee",
            "some coffee"
        ],
        response: "{destination} is the closest coffee spot. Want me to start the route?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "fameo-direct", phrases: [
            "fameo cafe",
            "take me to fameo cafe",
            "go to fameo",
            "fameo",
            "fameo coffee"
        ],
        response: "Fameo Cafe coming right up. Want me to start the route?",
        urgent: false,
        resolution: .roomId("fameo-cafe"),
        intent: .roomIdentifier),

        Entry(id: "hungry", phrases: [
            "i am hungry",
            "i'm hungry",
            "im hungry",
            "i'm starving",
            "i could eat something",
            "i need a snack",
            "i want a snack"
        ],
        response: "{destination} has snacks and coffee. Want me to take you there?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "tea", phrases: [
            "i want some tea",
            "i need tea",
            "tea please",
            "where can i get tea"
        ],
        response: "{destination} serves tea too. Should I start the route?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        // ─── Classrooms ──────────────────────────────────────────────────
        Entry(id: "class-eaz04", phrases: [
            "i have a class in ea-z04",
            "i have class in ea-z04",
            "my class is in ea-z04",
            "take me to ea-z04",
            "where is ea-z04",
            "ea-z04",
            "eaz04",
            "ea z04"
        ],
        // EA-Z04 isn't a separate room in rooms.json — it sits at the same
        // node as EA 102, so we route through that room's id.
        response: "Got it, EA-Z04 maps to {destination}. Should I take you there?",
        urgent: false,
        resolution: .roomId("ea102"),
        intent: .roomIdentifier),

        Entry(id: "class-ea101", phrases: [
            "where is ea101 classroom",
            "where's ea101",
            "take me to ea101",
            "i have a class in ea101",
            "ea101 classroom",
            "ea101",
            "ea 101"
        ],
        response: "EA101 it is. Want me to start the route?",
        urgent: false,
        resolution: .roomId("ea101"),
        intent: .roomIdentifier),

        Entry(id: "class-ea102", phrases: [
            "where is ea 102 classroom",
            "where is ea102",
            "take me to ea 102",
            "i have a class in ea102",
            "ea 102 classroom",
            "ea102",
            "ea 102"
        ],
        response: "EA 102 it is. Want me to start the route?",
        urgent: false,
        resolution: .roomId("ea102"),
        intent: .roomIdentifier),

        Entry(id: "class-generic", phrases: [
            "i need a classroom",
            "find a classroom",
            "any free classroom",
            "where are the classrooms"
        ],
        response: "I found {destination}. Want me to take you there?",
        urgent: false,
        resolution: .category("classroom"),
        intent: .categorySearch),

        // ─── Labs ────────────────────────────────────────────────────────
        Entry(id: "lab-cs", phrases: [
            "take me to cs lab",
            "where is cs lab",
            "where's the cs lab",
            "computer lab",
            "computer science lab",
            "cs lab"
        ],
        response: "CS Lab it is. Want me to start the route?",
        urgent: false,
        resolution: .roomId("cs-lab"),
        intent: .roomIdentifier),

        Entry(id: "lab-me", phrases: [
            "take me to me lab",
            "where is me lab",
            "where's the me lab",
            "mechanical lab",
            "mechanical engineering lab",
            "me lab"
        ],
        response: "ME Lab it is. Want me to start the route?",
        urgent: false,
        resolution: .roomId("me-lab"),
        intent: .roomIdentifier),

        Entry(id: "lab-generic", phrases: [
            "where is the lab",
            "i need a lab",
            "find a lab",
            "take me to a lab"
        ],
        response: "I found {destination}. Want me to take you there?",
        urgent: false,
        resolution: .category("lab"),
        intent: .categorySearch),

        // ─── Elevators ───────────────────────────────────────────────────
        Entry(id: "elevator-nearest", phrases: [
            "where are the elevators",
            "where is the elevator",
            "take me to the elevator",
            "take me to the elevators",
            "elevators please",
            "elevator please",
            "i need the elevator",
            "i need an elevator",
            "i need the lift",
            "lift please",
            "where's the lift"
        ],
        response: "The elevator bank is at {destination}. Want me to take you there?",
        urgent: false,
        resolution: .roomId("elevators"),
        intent: .roomIdentifier),

        Entry(id: "going-up", phrases: [
            "i need to go up",
            "i want to go upstairs",
            "i want to go up a floor",
            "another floor please"
        ],
        response: "I'll get you to the elevators ({destination}). Start the route?",
        urgent: false,
        resolution: .roomId("elevators"),
        intent: .freeText),

        // ─── Misc helpers ────────────────────────────────────────────────
        Entry(id: "lost", phrases: [
            "i'm lost",
            "im lost",
            "i don't know where i am",
            "help me find my way"
        ],
        response: "No worries — try a place name like \"Fameo Cafe\" or \"EA-Z04\", or say \"nearest restroom\".",
        urgent: false,
        resolution: .category("__none"),
        intent: .ambiguous),

        Entry(id: "thirsty", phrases: [
            "i'm thirsty",
            "im thirsty",
            "i need a drink",
            "i want a drink"
        ],
        response: "{destination} can sort you out. Want me to start the route?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "snack", phrases: [
            "i want food",
            "where can i eat",
            "i need food",
            "take me to food"
        ],
        response: "{destination} has snacks. Want me to take you there?",
        urgent: false,
        resolution: .category("coffee"),
        intent: .categorySearch),

        Entry(id: "exit", phrases: [
            "where is the exit",
            "how do i get out",
            "take me to the entrance",
            "where's the entrance"
        ],
        response: "Head back the way you came in — the entrance is the start of every route.",
        urgent: false,
        resolution: .category("__none"),
        intent: .ambiguous),

        Entry(id: "go-home", phrases: [
            "go back",
            "cancel",
            "never mind",
            "nevermind",
            "forget it"
        ],
        response: "No problem — close this card whenever you're ready.",
        urgent: false,
        resolution: .category("__none"),
        intent: .ambiguous),

        Entry(id: "thanks", phrases: [
            "thanks",
            "thank you",
            "ty",
            "thx"
        ],
        response: "You're welcome! Tell me where to go whenever you're ready.",
        urgent: false,
        resolution: .category("__none"),
        intent: .ambiguous),

        Entry(id: "hello", phrases: [
            "hi",
            "hey",
            "hello",
            "hey vectura",
            "hi vectura",
            "yo"
        ],
        response: "Hey! Tell me where you want to go — try \"Fameo Cafe\" or \"nearest restroom\".",
        urgent: false,
        resolution: .category("__none"),
        intent: .ambiguous),

        Entry(id: "what-can-you-do", phrases: [
            "what can you do",
            "help",
            "how do you work",
            "what do you do"
        ],
        response: "Tell me a place (\"Fameo Cafe\") or a need (\"I need coffee\", \"nearest restroom\") and I'll route you there.",
        urgent: false,
        resolution: .category("__none"),
        intent: .ambiguous),
    ]

    // MARK: - Match

    /// Best-match outcome for an arbitrary user input.
    struct Match {
        let entry: Entry
        let score: Double
        let phrase: String
    }

    /// Test hook: score a single phrase against an arbitrary input the way
    /// `bestMatch(for:)` does internally. Used by the demo checks to assert
    /// the anchor-token rule.
    static func scoreFor(input: String, phrase: String) -> Double {
        let userTokens = tokens(normalize(input))
        return score(userTokens: userTokens, phrase: phrase)
    }

    /// Returns the best matching entry whose score crosses the dynamic
    /// acceptance threshold, or nil otherwise.
    static func bestMatch(for input: String) -> Match? {
        let normalized = normalize(input)
        guard !normalized.isEmpty else { return nil }
        let userTokens = tokens(normalized)

        var best: Match?
        for entry in entries {
            for phrase in entry.phrases {
                let score = score(userTokens: userTokens, phrase: phrase)
                if score > (best?.score ?? -1) {
                    best = Match(entry: entry, score: score, phrase: phrase)
                }
            }
        }
        guard let candidate = best else { return nil }
        let phraseTokenCount = tokens(normalize(candidate.phrase)).count
        // Lower the bar slightly for very short prompts (1-2 tokens) where
        // a single typo blows the percentage match.
        let threshold: Double = phraseTokenCount <= 2 ? 0.5 : 0.6
        return candidate.score >= threshold ? candidate : nil
    }

    // MARK: - Scoring

    /// Score in [0, 1]. 1.0 = every prompt token found, no extra junk.
    ///
    /// Tokens that contain digits ("ea101", "z04", "102") are treated as
    /// **required anchors**: they MUST match a user token exactly, otherwise
    /// the phrase is referring to a different place and the score collapses
    /// to 0. This is what stops "I have a class in CS Lab" from accidentally
    /// matching "I have a class in EA101" just because 5 of 6 generic words
    /// overlap.
    private static func score(userTokens: [String], phrase: String) -> Double {
        let promptTokens = tokens(normalize(phrase))
        guard !promptTokens.isEmpty else { return 0 }

        var matched: Int = 0
        var usedUserTokens = Set<Int>()
        for pToken in promptTokens {
            let isAnchor = isAnchorToken(pToken)
            // Anchors require an exact match — they're identifiers, not
            // adjectives.
            let allowed = isAnchor ? 0 : allowedDistance(for: pToken)
            var bestIdx: Int?
            var bestDistance = Int.max
            for (i, uToken) in userTokens.enumerated() where !usedUserTokens.contains(i) {
                let d = levenshtein(pToken, uToken)
                if d <= allowed && d < bestDistance {
                    bestDistance = d
                    bestIdx = i
                }
            }
            if let bestIdx {
                usedUserTokens.insert(bestIdx)
                matched += 1
            } else if isAnchor {
                // A required anchor is missing → this phrase is talking
                // about a different room / number than the user is.
                return 0
            }
        }

        let coverage = Double(matched) / Double(promptTokens.count)
        let extra = max(0, userTokens.count - promptTokens.count)
        // Forgive one extra token (allows "please", "now", filler words).
        let extraPenalty = min(0.25, Double(max(0, extra - 1)) * 0.08)
        return max(0, coverage - extraPenalty)
    }

    /// "Anchor" tokens are identifiers that pin a phrase to a specific place
    /// (room numbers like ea101, ea-z04, z04, 102). They must always match.
    private static func isAnchorToken(_ token: String) -> Bool {
        token.contains(where: { $0.isNumber })
    }

    private static func allowedDistance(for token: String) -> Int {
        switch token.count {
        case 0...3: return 0  // exact match required for tiny words ("ea", "wc")
        case 4...6: return 1
        default:    return 2
        }
    }

    // MARK: - Normalisation

    private static func normalize(_ s: String) -> String {
        let lower = s.lowercased()
        var out = ""
        for ch in lower {
            if ch.isLetter || ch.isNumber || ch.isWhitespace || ch == "-" || ch == "_" {
                out.append(ch)
            } else {
                out.append(" ")
            }
        }
        return out
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "  ", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func tokens(_ s: String) -> [String] {
        s.split(whereSeparator: { $0.isWhitespace })
            .map(String.init)
            .filter { !$0.isEmpty }
    }

    // MARK: - Levenshtein (iterative DP)

    static func levenshtein(_ a: String, _ b: String) -> Int {
        // Hyphenated tokens are treated as a single word, but for distance
        // we strip the hyphen so "ea-z04" and "eaz04" are distance 0.
        let s1 = Array(a.replacingOccurrences(of: "-", with: ""))
        let s2 = Array(b.replacingOccurrences(of: "-", with: ""))
        let m = s1.count
        let n = s2.count
        if m == 0 { return n }
        if n == 0 { return m }

        var prev = Array(0...n)
        var curr = Array(repeating: 0, count: n + 1)

        for i in 1...m {
            curr[0] = i
            for j in 1...n {
                let cost = s1[i - 1] == s2[j - 1] ? 0 : 1
                curr[j] = min(
                    prev[j] + 1,        // deletion
                    curr[j - 1] + 1,    // insertion
                    prev[j - 1] + cost  // substitution
                )
            }
            swap(&prev, &curr)
        }
        return prev[n]
    }
}
