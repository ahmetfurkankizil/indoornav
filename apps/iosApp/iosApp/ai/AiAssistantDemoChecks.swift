import Foundation

/// In-process demo / acceptance checklist for the AI route assistant.
///
/// The iOS app target does not currently ship a unit-test target, so this
/// file plays a double role:
///
/// 1. It is the executable spec for the six required demo cases (the three
///    spoken phrases, voice-equivalent, unknown utterance, GPT-down). The
///    asserts run a real `MockAiRouteAgent` against a real `RouteAgentTools`
///    layer wired to an in-memory copy of the reviewed package.
/// 2. It is the source of truth the docs (`docs/ai-route-assistant.md`)
///    reference. Whenever the data or rules change, run `runDemoChecklist()`
///    from the Xcode debugger console (`po AiAssistantDemoChecks.run()`).
///
/// The fixture below is a hand-trimmed copy of the bundled reviewed package
/// — kept tiny so the checklist is fully self-contained.
enum AiAssistantDemoChecks {

    struct CheckResult {
        let title: String
        let passed: Bool
        let detail: String
    }

    /// Public entry point. Returns the checklist + prints a human readable
    /// summary to the console. Safe to call on a background thread.
    @discardableResult
    static func run() -> [CheckResult] {
        let tools = makeTools()
        let mock = MockAiRouteAgent(tools: tools)
        var results: [CheckResult] = []

        results.append(check(
            title: "Test 1 · urgent restroom",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I'm about to pee myself.", inputMode: .text, startNodeId: nil))
                let category = r.selectedDestination?.category ?? ""
                let okIntent = r.detectedIntent == .categorySearch
                let okCat = ["restroom", "toilet", "wc", "bathroom"].contains(category)
                let okConfirm = r.requiresConfirmation
                return (okIntent && okCat && okConfirm,
                        "intent=\(r.detectedIntent.rawValue) category=\(category) confirm=\(r.requiresConfirmation) -> \(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 2 · room id EA-Z04",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I have a class in EA-Z04.", inputMode: .text, startNodeId: nil))
                let okIntent = r.detectedIntent == .roomIdentifier
                let okName = (r.selectedDestination?.displayName ?? "").contains("EA-Z04")
                return (okIntent && okName && r.requiresConfirmation,
                        "intent=\(r.detectedIntent.rawValue) -> \(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 3 · coffee request",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I really need to drink a coffee.", inputMode: .text, startNodeId: nil))
                let okIntent = r.detectedIntent == .categorySearch
                let okCat = (r.selectedDestination?.category ?? "") == "cafe" ||
                            (r.selectedDestination?.category ?? "") == "coffee"
                return (okIntent && okCat && r.requiresConfirmation,
                        "intent=\(r.detectedIntent.rawValue) -> \(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 4 · voice variant of test 1",
            run: {
                // Voice transcripts often drop the apostrophe and the period.
                let r = await mock.handleUserRequest(.init(rawText: "im about to pee myself", inputMode: .voice, startNodeId: nil))
                return (r.detectedIntent == .categorySearch && r.selectedDestination != nil,
                        "intent=\(r.detectedIntent.rawValue) -> \(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 5 · unknown ('dinosaur fossils')",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I want to see the dinosaur fossils.", inputMode: .text, startNodeId: nil))
                let safe = r.selectedDestination == nil || r.selectedDestination?.id == nil
                let intentSafe = r.detectedIntent == .unknown || r.detectedIntent == .ambiguous || r.detectedIntent == .freeText
                return (safe && intentSafe,
                        "intent=\(r.detectedIntent.rawValue) message=\(r.assistantMessage)")
            }
        ))

        results.append(check(
            title: "Test 6 · GPT down -> falls back to mock",
            run: {
                // Use a deliberately broken base URL so the network path always fails.
                let mockFallback = MockAiRouteAgent(tools: tools)
                let backend = BackendGptRouteAgent(
                    tools: tools,
                    fallback: mockFallback,
                    baseURL: "http://127.0.0.1:1",
                    timeoutSeconds: 1.0
                )
                let r = await backend.handleUserRequest(.init(rawText: "I really need to drink a coffee.", inputMode: .text, startNodeId: nil))
                let traceMentionsFallback = r.toolTrace.contains { $0.contains("backendGpt") || $0.contains("intent=coffee") }
                return (r.selectedDestination?.displayName == "Fameo Cafe" && traceMentionsFallback,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") trace=\(r.toolTrace.joined(separator: ","))")
            }
        ))

        // ─── Prompt-bank fuzzy / typo coverage ───────────────────────────

        results.append(check(
            title: "Test 7 · prompt bank · Fameo Cafe direct",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "Take me to Fameo Cafe", inputMode: .text, startNodeId: nil))
                let bankHit = r.toolTrace.contains { $0.hasPrefix("promptBank:matched=fameo-direct") }
                return (bankHit && r.selectedDestination?.displayName == "Fameo Cafe",
                        "selected=\(r.selectedDestination?.displayName ?? "nil") trace=\(r.toolTrace.joined(separator: ","))")
            }
        ))

        results.append(check(
            title: "Test 8 · typo · 'I need a coffe' (missing letter)",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I need a coffe", inputMode: .text, startNodeId: nil))
                return (r.selectedDestination?.displayName == "Fameo Cafe" && r.detectedIntent == .categorySearch,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") intent=\(r.detectedIntent.rawValue)")
            }
        ))

        results.append(check(
            title: "Test 9 · extra word · 'where is the nearest restroom please'",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "where is the nearest restroom please", inputMode: .text, startNodeId: nil))
                let category = r.selectedDestination?.category ?? ""
                return (r.detectedIntent == .categorySearch && ["restroom", "toilet", "wc", "bathroom"].contains(category),
                        "selected=\(r.selectedDestination?.displayName ?? "nil") category=\(category)")
            }
        ))

        results.append(check(
            title: "Test 10 · slang · 'I'm hungry'",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I'm hungry", inputMode: .text, startNodeId: nil))
                return (r.selectedDestination?.displayName == "Fameo Cafe",
                        "selected=\(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 11 · gendered restroom · 'Men's WC'",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "Men's WC", inputMode: .text, startNodeId: nil))
                let name = r.selectedDestination?.displayName ?? ""
                return (name.lowercased().contains("men") && !name.lowercased().contains("women"),
                        "selected=\(name)")
            }
        ))

        results.append(check(
            title: "Test 12 · typo · 'eaz04' (no separators)",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "eaz04", inputMode: .text, startNodeId: nil))
                return (r.selectedDestination?.displayName.contains("EA-Z04") == true,
                        "selected=\(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 13 · CS Lab direct",
            run: {
                // The fixture catalog used here doesn't include CS Lab POIs, so
                // we expect graceful no-match (still no false destination).
                let r = await mock.handleUserRequest(.init(rawText: "Take me to CS Lab", inputMode: .text, startNodeId: nil))
                let isSafe = (r.detectedIntent == .ambiguous) ||
                             (r.detectedIntent == .roomIdentifier && r.selectedDestination == nil) ||
                             (r.detectedIntent == .unknown)
                return (isSafe, "intent=\(r.detectedIntent.rawValue) selected=\(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        // ─── Issue #1, #2, #3 regression tests ───────────────────────────

        results.append(check(
            title: "Test 14 · EA101 routes through rooms.json (Issue #1)",
            run: {
                // EA101 isn't in the fixture's POI catalog — it must still be
                // resolvable via the rooms.json fallback in RouteAgentTools.
                let toolsWithEA101 = makeToolsWithExtraRoom(
                    .init(id: "ea101", displayName: "EA101 Classroom",
                          destinationNodeId: "n21",
                          category: "classroom", description: nil,
                          floorId: "1", floorName: "Ground")
                )
                let agent = MockAiRouteAgent(tools: toolsWithEA101)
                let r = await agent.handleUserRequest(.init(rawText: "EA101 classroom", inputMode: .text, startNodeId: nil))
                let routed = r.selectedDestination != nil &&
                             (r.routeSummary?.isEmpty == false) &&
                             !r.assistantMessage.contains("couldn't create a valid route")
                return (routed,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") summary=\(r.routeSummary ?? "nil") msg=\(r.assistantMessage)")
            }
        ))

        results.append(check(
            title: "Test 15 · single-word 'Cafe' resolves to a cafe (Issue #2)",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "Cafe", inputMode: .text, startNodeId: nil))
                let category = r.selectedDestination?.category ?? ""
                let okCat = ["cafe", "coffee"].contains(category)
                return (okCat && r.selectedDestination?.displayName == "Fameo Cafe",
                        "selected=\(r.selectedDestination?.displayName ?? "nil") cat=\(category)")
            }
        ))

        results.append(check(
            title: "Test 16 · 'I am about to pee myself' (no contraction) (Issue #3)",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "I am about to pee myself", inputMode: .text, startNodeId: nil))
                let category = r.selectedDestination?.category ?? ""
                let okCat = ["restroom", "toilet", "wc", "bathroom"].contains(category)
                let okIntent = r.detectedIntent == .categorySearch
                return (okIntent && okCat && r.selectedDestination != nil,
                        "intent=\(r.detectedIntent.rawValue) cat=\(category) -> \(r.selectedDestination?.displayName ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 17 · 'WC' alone resolves to a restroom (Issue #3)",
            run: {
                let r = await mock.handleUserRequest(.init(rawText: "WC", inputMode: .text, startNodeId: nil))
                let category = r.selectedDestination?.category ?? ""
                let okCat = ["restroom", "toilet", "wc", "bathroom"].contains(category)
                return (okCat && r.selectedDestination != nil,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") cat=\(category)")
            }
        ))

        results.append(check(
            title: "Test 18 · empty catalog falls back to rooms.json",
            run: {
                // Critical safety net: even with NO semantic POI catalog, the
                // assistant must still resolve restroom requests via rooms.json.
                let bareTools = RouteAgentTools(catalog: SemanticPoiCatalog(), config: makeConfig())
                let agent = MockAiRouteAgent(tools: bareTools)
                let r = await agent.handleUserRequest(.init(rawText: "I need a restroom", inputMode: .text, startNodeId: nil))
                let okCat = (r.selectedDestination?.category ?? "") == "toilet" ||
                            (r.selectedDestination?.category ?? "") == "restroom"
                return (r.selectedDestination != nil && okCat,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") cat=\(r.selectedDestination?.category ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 19 · bank .roomId resolves Fameo via rooms.json",
            run: {
                // Even with an EMPTY catalog, "Fameo Cafe" must resolve
                // because the bank entry now points to roomId="fameo-cafe"
                // directly.
                let bareTools = RouteAgentTools(catalog: SemanticPoiCatalog(), config: makeConfig())
                let agent = MockAiRouteAgent(tools: bareTools)
                let r = await agent.handleUserRequest(.init(rawText: "Fameo Cafe", inputMode: .text, startNodeId: nil))
                return (r.selectedDestination?.roomId == "fameo-cafe" &&
                        r.selectedDestination?.displayName == "Fameo Cafe",
                        "selected=\(r.selectedDestination?.displayName ?? "nil") roomId=\(r.selectedDestination?.roomId ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 20 · single-word 'WC' routes to a rooms.json restroom",
            run: {
                // Same behaviour as clicking 'Men's WC' / 'Women's WC' in
                // the manual destination list: the assistant must pick one
                // of those rows and return its rooms.json id.
                let bareTools = RouteAgentTools(catalog: SemanticPoiCatalog(), config: makeConfig())
                let agent = MockAiRouteAgent(tools: bareTools)
                let r = await agent.handleUserRequest(.init(rawText: "WC", inputMode: .text, startNodeId: nil))
                let roomId = r.selectedDestination?.roomId ?? ""
                let isWcRoom = roomId.hasSuffix("-wc")
                return (isWcRoom && r.selectedDestination != nil,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") roomId=\(roomId)")
            }
        ))

        results.append(check(
            title: "Test 21 · 'coffee' routes via rooms.json (no catalog)",
            run: {
                let bareTools = RouteAgentTools(catalog: SemanticPoiCatalog(), config: makeConfig())
                let agent = MockAiRouteAgent(tools: bareTools)
                let r = await agent.handleUserRequest(.init(rawText: "coffee", inputMode: .text, startNodeId: nil))
                return (r.selectedDestination?.roomId == "fameo-cafe",
                        "selected=\(r.selectedDestination?.displayName ?? "nil") roomId=\(r.selectedDestination?.roomId ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 22 · 'I have a class in CS Lab' MUST go to CS Lab, not EA101",
            run: {
                // Regression: the screenshot bug. Generic-coverage scoring used
                // to prefer "i have a class in ea101" because 5 of 6 generic
                // words overlap. With anchor-token rules the digit-bearing
                // "ea101" is now required, so the bank correctly picks the
                // CS Lab entry instead.
                let toolsWithLabs = makeToolsWithExtraRoom(
                    .init(id: "cs-lab", displayName: "CS Lab",
                          destinationNodeId: "n21",
                          category: "lab", description: nil,
                          floorId: "1", floorName: "Ground")
                )
                let agent = MockAiRouteAgent(tools: toolsWithLabs)
                let r = await agent.handleUserRequest(.init(rawText: "I have a class in CS Lab", inputMode: .text, startNodeId: nil))
                let goesToCsLab = (r.selectedDestination?.roomId == "cs-lab") ||
                                  (r.selectedDestination?.displayName.lowercased().contains("cs lab") == true)
                let notEa101 = (r.selectedDestination?.roomId != "ea101") &&
                               (r.selectedDestination?.displayName.contains("EA101") != true)
                return (goesToCsLab && notEa101,
                        "selected=\(r.selectedDestination?.displayName ?? "nil") roomId=\(r.selectedDestination?.roomId ?? "nil")")
            }
        ))

        results.append(check(
            title: "Test 23 · anchor token blocks wrong-room match",
            run: {
                // Plain unit test of the scoring rule: prompts with digit
                // tokens that don't appear in the user input must score 0.
                let scoreEa101 = MockPromptBank.scoreFor(input: "I want a coffee", phrase: "i have a class in ea101")
                let scoreCsLab = MockPromptBank.scoreFor(input: "I want a coffee", phrase: "cs lab")
                return (scoreEa101 == 0.0 && scoreCsLab >= 0,
                        "ea101=\(scoreEa101) cs-lab=\(scoreCsLab)")
            }
        ))

        results.append(check(
            title: "Test 24 · roomId fallback to displayName when id missing",
            run: {
                // Simulates a remote v2 building whose rooms have different
                // ids — the bank says .roomId("fameo-cafe") but the actual
                // room id is, say, "abc-123". The displayName fallback must
                // still pick the right room.
                var config = makeConfig()
                let renamed = BuildingPackageLoader.PackageRoom(
                    id: "weird-uuid-fameo", displayName: "Fameo Cafe",
                    destinationNodeId: "n14", category: "cafe",
                    description: nil, floorId: "1", floorName: "Ground"
                )
                let filteredRooms = config.rooms.filter { $0.id != "fameo-cafe" } + [renamed]
                config = .init(
                    manifest: config.manifest,
                    rooms: filteredRooms,
                    nodes: config.nodes,
                    edges: config.edges,
                    entranceMarkers: config.entranceMarkers,
                    routeRendering: config.routeRendering
                )
                let toolsRemote = RouteAgentTools(catalog: SemanticPoiCatalog(), config: config)
                let agent = MockAiRouteAgent(tools: toolsRemote)
                let r = await agent.handleUserRequest(.init(rawText: "Fameo Cafe", inputMode: .text, startNodeId: nil))
                return (r.selectedDestination?.displayName == "Fameo Cafe",
                        "selected=\(r.selectedDestination?.displayName ?? "nil") roomId=\(r.selectedDestination?.roomId ?? "nil")")
            }
        ))

        let summary = results.map { ($0.passed ? "✓" : "✗") + " " + $0.title + " — " + $0.detail }
        print("[AiAssistantDemoChecks]\n" + summary.joined(separator: "\n"))
        return results
    }

    /// Build a `RouteAgentTools` that mirrors the default fixture but with an
    /// extra room appended — used to verify the rooms.json fallback resolves
    /// rooms that aren't listed in the POI catalog.
    private static func makeToolsWithExtraRoom(_ extra: BuildingPackageLoader.PackageRoom) -> RouteAgentTools {
        var config = makeConfig()
        config = .init(
            manifest: config.manifest,
            rooms: config.rooms + [extra],
            nodes: config.nodes,
            edges: config.edges,
            entranceMarkers: config.entranceMarkers,
            routeRendering: config.routeRendering
        )
        return RouteAgentTools(catalog: makeCatalog(), config: config)
    }

    // MARK: - Async check wrapper

    private static func check(
        title: String,
        run: @escaping () async -> (Bool, String)
    ) -> CheckResult {
        let semaphore = DispatchSemaphore(value: 0)
        var captured: (Bool, String) = (false, "(no result)")
        Task {
            captured = await run()
            semaphore.signal()
        }
        // Bound the wait so misconfigured tests don't hang the demo build.
        _ = semaphore.wait(timeout: .now() + 5)
        return CheckResult(title: title, passed: captured.0, detail: captured.1)
    }

    // MARK: - In-memory fixture (mirrors the bundled reviewed package)

    private static func makeTools() -> RouteAgentTools {
        return RouteAgentTools(catalog: makeCatalog(), config: makeConfig())
    }

    private static func makeCatalog() -> SemanticPoiCatalog {
        let pois: [SemanticPoiCatalog.Poi] = [
            .init(id: "poi-restroom-east-men", displayName: "Men's WC (East)", roomId: "east-men-wc",
                  nodeId: "n11", buildingId: "19", floorId: "1", type: "restroom",
                  categories: ["restroom", "toilet", "wc", "bathroom"],
                  aliases: ["bathroom", "toilet", "pee", "restroom", "wc", "loo"],
                  products: [], description: "East men's restroom",
                  priority: 0.7, isNavigable: true, isRestricted: false, demoOnly: false),
            .init(id: "poi-restroom-east-women", displayName: "Women's WC (East)", roomId: "east-women-wc",
                  nodeId: "n12", buildingId: "19", floorId: "1", type: "restroom",
                  categories: ["restroom", "toilet", "wc", "bathroom"],
                  aliases: ["bathroom", "toilet", "pee", "restroom", "wc"],
                  products: [], description: "East women's restroom",
                  priority: 0.7, isNavigable: true, isRestricted: false, demoOnly: false),
            .init(id: "poi-coffee-fameo", displayName: "Fameo Cafe", roomId: "fameo-cafe",
                  nodeId: "n14", buildingId: "19", floorId: "1", type: "cafe",
                  categories: ["cafe", "coffee", "drink", "cafeteria"],
                  aliases: ["coffee", "cafe", "caffeine", "espresso", "latte", "drink"],
                  products: ["coffee"], description: "Fameo Cafe on the east corridor",
                  priority: 1.0, isNavigable: true, isRestricted: false, demoOnly: false),
            .init(id: "poi-classroom-eaz04", displayName: "EA-Z04 Classroom", roomId: nil,
                  nodeId: "n21", buildingId: "19", floorId: "1", type: "classroom",
                  categories: ["classroom", "lecture", "course"],
                  aliases: ["ea-z04", "eaz04", "ea z04", "z04"],
                  products: [], description: "Lecture room EA-Z04",
                  priority: 1.0, isNavigable: true, isRestricted: false, demoOnly: true),
        ]
        let synonyms: [String: [String]] = [
            "restroom": ["restroom", "toilet", "wc", "bathroom"],
            "coffee": ["coffee", "cafe", "cafeteria", "caffeine"],
            "classroom": ["classroom", "lecture", "course"],
        ]
        let catalog = SemanticPoiCatalog.Catalog(
            version: 1, buildingId: "19", notes: nil, pois: pois, categorySynonyms: synonyms
        )
        return SemanticPoiCatalog(catalog: catalog)
    }

    private static func makeConfig() -> BuildingPackageLoader.ReviewedConfig {
        // A small chain of nodes that connects the entrance to all fixture POIs.
        let nodes: [BuildingPackageLoader.PackageNode] = [
            .init(id: "n01", x: 0,   y: 0, z: 0,   type: "entrance",   label: "Entrance", floorId: "1"),
            .init(id: "n02", x: 11,  y: 0, z: 0,   type: "junction",   label: "West",     floorId: "1"),
            .init(id: "n03", x: 19,  y: 0, z: 0,   type: "junction",   label: "East",     floorId: "1"),
            .init(id: "n10", x: 19,  y: 0, z: 5,   type: "junction",   label: "EastLow",  floorId: "1"),
            .init(id: "n11", x: 22,  y: 0, z: 5,   type: "room_entry", label: "Men WC E", floorId: "1"),
            .init(id: "n12", x: 23,  y: 0, z: 5,   type: "room_entry", label: "Women WC E", floorId: "1"),
            .init(id: "n13", x: 31,  y: 0, z: 0,   type: "junction",   label: "CafeProj", floorId: "1"),
            .init(id: "n14", x: 31,  y: 0, z: 1,   type: "room_entry", label: "Fameo Cafe", floorId: "1"),
            .init(id: "n19", x: 51,  y: 0, z: 0,   type: "junction",   label: "EastFork", floorId: "1"),
            .init(id: "n21", x: 51,  y: 0, z: -2,  type: "room_entry", label: "EA 102",   floorId: "1"),
        ]
        let edges: [BuildingPackageLoader.PackageEdge] = [
            .init(id: "e01", from: "n01", to: "n02", cost: 11, bidirectional: true),
            .init(id: "e02", from: "n02", to: "n03", cost: 8,  bidirectional: true),
            .init(id: "e10", from: "n03", to: "n10", cost: 5,  bidirectional: true),
            .init(id: "e12", from: "n10", to: "n11", cost: 3,  bidirectional: true),
            .init(id: "e13", from: "n10", to: "n12", cost: 4,  bidirectional: true),
            .init(id: "e14", from: "n03", to: "n13", cost: 12, bidirectional: true),
            .init(id: "e15", from: "n13", to: "n14", cost: 1,  bidirectional: true),
            .init(id: "e20", from: "n13", to: "n19", cost: 20, bidirectional: true),
            .init(id: "e22", from: "n19", to: "n21", cost: 2,  bidirectional: true),
        ]
        let markers: [BuildingPackageLoader.PackageMarker] = [
            .init(id: "marker-entrance-a", displayName: "Entrance",
                  startNodeId: "n01", physicalWidthMeters: 0.21, physicalHeightMeters: 0.21,
                  position: .init(x: 0, y: 0, z: 0),
                  forwardBasis: "marker-z", rotationYDegrees: 0,
                  referenceImageName: "entrance_marker_main", notes: nil)
        ]
        let manifest = BuildingPackageLoader.Manifest(
            packageVersion: "1.0", buildingId: "19", buildingName: "Rectorate Building",
            floorId: "1", reviewStatus: "reviewed",
            files: .init(rooms: "rooms.json", navGraph: "nav_graph.json",
                         entranceMarkers: "entrance_markers.json", routeRendering: "route_rendering.json")
        )
        let rendering = BuildingPackageLoader.RouteRenderingConfig(
            arrowSpacingMeters: 1.5, lookaheadDistanceMeters: 8.0,
            destinationThresholdMeters: 1.5, turnMarkerThresholdDegrees: 30,
            arrowHeightOffsetMeters: 0.05
        )
        let rooms: [BuildingPackageLoader.PackageRoom] = [
            .init(id: "east-men-wc", displayName: "Men's WC (East)", destinationNodeId: "n11",
                  category: "toilet", description: nil, floorId: "1", floorName: "Ground"),
            .init(id: "east-women-wc", displayName: "Women's WC (East)", destinationNodeId: "n12",
                  category: "toilet", description: nil, floorId: "1", floorName: "Ground"),
            .init(id: "fameo-cafe", displayName: "Fameo Cafe", destinationNodeId: "n14",
                  category: "cafe", description: nil, floorId: "1", floorName: "Ground"),
        ]
        return .init(manifest: manifest, rooms: rooms, nodes: nodes, edges: edges,
                     entranceMarkers: markers, routeRendering: rendering)
    }
}
