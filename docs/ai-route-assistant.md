# AI Route Assistant

## What it is

An LLM-powered, agentic route-selection assistant embedded in the iOS
destination-selection screen. The user can casually say (or type) what they
need — e.g. *"I'm about to pee myself"* — and Vectura interprets the request,
finds the best matching destination from the building catalog, asks for
confirmation and then drops the user back into the existing route preview /
AR-navigation flow.

No accounts, no profiles, no stored personal data. English-only for the
demo. Voice input is supported but text input always works as a fallback.

## High-level architecture

```
┌──────────────────────────────────────────┐
│  UI Layer                                │
│   DestinationSelectView                  │
│   └─ AiAssistantView (text + voice)      │
└──────────────────┬───────────────────────┘
                   │ AiRouteRequest
                   ▼
┌──────────────────────────────────────────┐
│  Agent Orchestration Layer               │
│   AiAssistantViewModel                   │
│     ↳ AiRouteAgent (protocol)            │
│        ├─ MockAiRouteAgent (default)     │
│        └─ BackendGptRouteAgent (opt.)    │
└──────────────────┬───────────────────────┘
                   │ tool calls
                   ▼
┌──────────────────────────────────────────┐
│  Deterministic Tool Layer                │
│   RouteAgentTools                        │
│    · searchPoiByName                     │
│    · searchPoiByCategory                 │
│    · resolveRoomIdentifier               │
│    · rankCandidateDestinations           │
│    · planRouteToDestination              │
│    · explainCandidate                    │
│    · createRouteSelectionResult          │
└──────────────────┬───────────────────────┘
                   │ uses real graph
                   ▼
┌──────────────────────────────────────────┐
│  Routing Layer                           │
│   BuildingPackageLoader                  │
│    · computeRoute(roomId)                │
│    · computeRoute(nodeId, label)  (new)  │
└──────────────────────────────────────────┘
```

The LLM (or mock) is only allowed to **interpret intent** and **pick among
tool-returned candidates**. It is never allowed to invent destinations, room
names, or coordinates. The actual route is always produced by the existing
deterministic Dijkstra route engine in `BuildingPackageLoader`.

## File map

### iOS — `apps/iosApp/iosApp/`

| File | Role |
| --- | --- |
| `ai/AiRouteModels.swift` | Request / result / candidate / intent / mode types |
| `ai/SemanticPoiCatalog.swift` | Loader for `semantic_pois.json` |
| `ai/RouteAgentTools.swift` | Deterministic tool layer (search, rank, plan) |
| `ai/AiRouteAgent.swift` | `AiRouteAgent` protocol |
| `ai/MockAiRouteAgent.swift` | Default offline implementation (prompt bank → rule-based fallback) |
| `ai/MockPromptBank.swift` | 30+ canned prompts/responses with Levenshtein-based fuzzy matching |
| `ai/AssistantConfirmationOverlay.swift` | "Found it!" full-screen confirmation popup |
| `ai/BackendGptRouteAgent.swift` | Calls the backend LLM proxy, falls back to mock |
| `ai/VoiceInputManager.swift` | `SFSpeechRecognizer` + microphone wrapper |
| `ai/AiAssistantViewModel.swift` | UI state machine and agent dispatcher |
| `ai/AiAssistantView.swift` | SwiftUI card embedded in `DestinationSelectView` |
| `ai/AiAssistantDemoChecks.swift` | In-process acceptance checks for the 6 demo cases |
| `reviewed-package/semantic_pois.json` | Companion semantic POI catalog (new) |
| `NavigationFlowModel.swift` | Added `selectAssistantDestination(...)` |
| `ar/BuildingPackageLoader.swift` | Added `computeRoute(destinationNodeId:destinationLabel:)` |
| `DestinationSelectView.swift` | Embeds `AiAssistantView` at the top of the list |
| `Info.plist` | Adds microphone + speech-recognition usage descriptions |
| `iosApp.xcodeproj/project.pbxproj` | Registers new files + the JSON resource |

### Backend (optional) — `tools/admin-api/src/main/kotlin/com/vecturai/tools/admin/`

| File | Role |
| --- | --- |
| `service/AiRouteIntentService.kt` | LLM proxy (Ollama / OpenAI-compatible) |
| `routes/MobileApiRoutes.kt` | Adds `POST /mobile/assistant/route-intent` |

## Data layer — semantic POI catalog

A new companion file `apps/iosApp/iosApp/reviewed-package/semantic_pois.json`
ships with the bundled reviewed package. It does **not** replace `rooms.json`
— `rooms.json` keeps driving the manual destination list. The semantic
catalog adds the metadata the agent needs:

```jsonc
{
  "version": 1,
  "buildingId": "19",
  "pois": [
    {
      "id": "poi-classroom-eaz04",
      "displayName": "EA-Z04 Classroom",
      "roomId": null,                 // agent-only POI
      "nodeId": "n21",                // MUST be valid in nav_graph.json
      "type": "classroom",
      "categories": ["classroom", "lecture", "course"],
      "aliases": ["ea-z04", "eaz04", "ea z04", "z04", "ea-z-04"],
      "products": [],
      "description": "Lecture room EA-Z04 …",
      "priority": 1.0,
      "isNavigable": true,
      "isRestricted": false,
      "demoOnly": true
    }
  ],
  "categorySynonyms": { "restroom": ["restroom", "toilet", "wc", "bathroom"] }
}
```

Design choices:

- Every POI is anchored to an existing nav-graph `nodeId`. The agent can
  never invent a destination.
- `roomId` is optional. When set, the assistant routes through
  `BuildingPackageLoader.computeRoute(config:destinationRoomId:)` — the
  exact same code path the manual destination list uses. This guarantees
  that anything the user can pick by hand is also routable by the
  assistant. When `roomId` is nil (e.g. EA-Z04, which is not part of the
  rooms list), a synthetic `PackageRoom` is created on the fly with the
  same `nodeId`.
- **rooms.json fallback.** `RouteAgentTools.searchPoiByCategory` and
  `searchPoiByName` also scan `config.rooms`, synthesising a POI for any
  room whose category matches the request (e.g. a `category: "toilet"`
  room is a valid match for `"restroom"` queries). This means the
  assistant keeps working even if `semantic_pois.json` is missing or
  hasn't been seeded for a remote v2 building. A baseline synonym map
  ships in `SemanticPoiCatalog` itself (`toilet → restroom`,
  `cafe → coffee`, `vertical_transport → elevator`, ...) so the mapping
  is always available.
- The catalog also extends to support future use cases mentioned in the
  spec — `products`, `categories` and `aliases` already cover museum / mall
  scenarios; we just don't expose them in this demo.

## Agent abstraction

```swift
protocol AiRouteAgent {
    func handleUserRequest(_ request: AiRouteRequest) async -> AiRouteAgentResult
}
```

`AiRouteAgentResult` carries `detectedIntent`, `assistantMessage`,
`candidateDestinations`, `selectedDestination`, `routeSummary`, `confidence`,
`requiresConfirmation`, optional `errorMessage` and a `toolTrace` (visible
when the in-card "Show dev trace" toggle is on).

### MockAiRouteAgent (default)

- **Step 0 — Prompt bank.** Consults `MockPromptBank` first. The bank
  ships with 30+ hand-curated entries covering the three required demo
  utterances **and** common slang phrasings. Each entry lists multiple
  canonical phrases, a conversational response template (with
  `{destination}` substitution) and how it resolves to the POI catalog
  (`.poi`, `.category`, `.genderedRestroom`).
- **Typo / extra-word tolerance.** The matcher tokenises the user input,
  scores each prompt phrase by per-token Levenshtein distance (≤1 for
  short tokens, ≤2 for longer ones) and forgives one extra word for free.
  Hyphens are stripped during distance comparison so `EA-Z04`, `EAZ04`
  and `EA Z04` all resolve to the same entry.
- **Anchor token rule.** Tokens that contain digits — `ea101`, `z04`,
  `102` — are treated as **required anchors**: they must match a user
  token exactly (distance 0) or the whole phrase scores 0. This stops
  *"I have a class in CS Lab"* from accidentally winning the
  *"I have a class in ea101"* entry just because 5 of 6 generic words
  overlap. Only the truly discriminating phrase (`cs lab`) survives.
- **roomId → displayName fallback.** Bank entries that target a
  specific room (`.roomId("fameo-cafe")`) first try an exact id lookup
  in `rooms.json`. If the active building uses different ids
  (e.g. UUIDs from a remote v2 package) the agent retries with a
  case-insensitive `displayName` search using the id as a hint
  (`"fameo-cafe"` → `"fameo cafe"` → matches *"Fameo Cafe"*). The
  assistant therefore stays robust against id renames between
  packages, as long as the human-readable label is preserved.
- **Step 1 — Rule-based fallback.** If no bank entry crosses the score
  threshold, the agent falls back to room-id extraction → restroom rules →
  coffee rules → generic category → fuzzy POI name search → unknown.
- Detects urgency (e.g. *"about to pee"*, *"emergency"*, *"gotta go"*) and
  switches the assistant message accordingly.
- Calls the same `RouteAgentTools` layer the GPT agent uses, so every
  selected destination is routed deterministically.

#### Prompt bank — what ships

| Group | Sample prompts | Resolves to |
| --- | --- | --- |
| Restroom (urgent slang) | "I'm about to pee myself", "I really need to pee" | nearest restroom |
| Restroom (neutral) | "where is the nearest restroom", "I need a bathroom", "WC please" | nearest restroom |
| Restroom (gendered) | "Men's WC", "Women's restroom", "Ladies room" | nearest men/women restroom |
| Coffee / Cafe | "I really need to drink a coffee", "I need coffee", "I'm hungry", "I want some tea" | nearest coffee POI |
| Cafe direct | "Fameo Cafe", "go to Fameo", "Cafe", "café", "kafe" | Fameo Cafe |
| Classrooms | "I have a class in EA-Z04", "EA101 Classroom", "EA 102", "ea-z04" | exact classroom |
| Labs | "Take me to CS Lab", "Computer lab", "ME Lab" | exact lab |
| Elevators | "Where are the elevators", "I need the lift", "I need to go up" | Elevators |
| Conversational | "hi", "thanks", "help", "I'm lost" | friendly text reply (no route) |

### BackendGptRouteAgent (optional)

- Posts the user request + the catalog vocabulary to
  `POST /mobile/assistant/route-intent` on the admin API.
- The backend forwards the request to Ollama (or any OpenAI-compatible
  endpoint via `OLLAMA_BASE_URL` / `OLLAMA_MODEL` env vars) with a strict
  system prompt that:
  - tells the model it is an indoor navigation assistant,
  - forbids inventing destinations, coordinates and personal-data use,
  - constrains the output to a small structured JSON schema.
- The LLM only returns `{intent, category|null, roomToken|null,
  assistantMessage, confidence}`. The on-device tool layer then runs the
  actual search and route.
- API keys never leave the backend.
- **Any failure falls back to `MockAiRouteAgent`** (network down, model
  timeout, parse error, empty response, ...). The user sees a friendly
  message, never a raw error.

### How to switch modes

1. Open Vectura → Scan QR → Choose destination screen.
2. Tap the `…` menu inside the *Ask Vectura* card.
3. Pick *Mock (offline)* or *Backend GPT*. Sticks for the lifetime of the
   destination screen.

## Voice input

`VoiceInputManager` wraps `SFSpeechRecognizer` with the `en-US` locale. It
requests speech + microphone permissions on first use. If either is denied,
the mic button shows a hint and the text input keeps working.

`Info.plist` carries the two required keys:
- `NSMicrophoneUsageDescription`
- `NSSpeechRecognitionUsageDescription`

When the user finishes speaking, the partial transcript is committed and
submitted as if it had been typed (with `inputMode: .voice` for the trace).

## Error and ambiguity handling

| Situation | Behaviour |
| --- | --- |
| No POI matches | Friendly message: *"I couldn't find a matching place in this building. Try a room name like EA-Z04 or something like coffee or restroom."* |
| Multiple matches | Up to 3 candidates rendered as alternative cards; user picks. |
| Route engine fails | *"I found the place, but I couldn't create a valid route to it yet."* |
| GPT down | Silent fallback to mock; trace shows `backendGpt:fallbackToMock`. |
| Ambiguous intent | Mock agent returns the highest-priority canonical category and asks for confirmation; user can cancel. |

## Demo acceptance tests

Run from Xcode debugger console once the app is launched:

```
po AiAssistantDemoChecks.run()
```

This executes six acceptance scenarios in process against an in-memory
fixture and prints a `✓` / `✗` summary plus a per-test detail line. The
tests are intentionally self-contained — they do **not** require the
bundled `semantic_pois.json` to be on disk.

| # | Input | Pass criteria |
| --- | --- | --- |
| 1 | "I'm about to pee myself." | intent=`categorySearch`, category=restroom/toilet/wc/bathroom, confirmation required |
| 2 | "I have a class in EA-Z04." | intent=`roomIdentifier`, displayName contains "EA-Z04" |
| 3 | "I really need to drink a coffee." | intent=`categorySearch`, selected=cafe/coffee |
| 4 | (voice) "im about to pee myself" | same as #1, regardless of missing apostrophe / period |
| 5 | "I want to see the dinosaur fossils." | intent in {unknown, ambiguous, freeText}, selected destination is nil |
| 6 | (GPT down) "I really need to drink a coffee." | falls back to mock and still selects Fameo Cafe |
| 7 | "Take me to Fameo Cafe" | bank entry `fameo-direct` selects Fameo Cafe |
| 8 | "I need a coffe" (one-letter typo) | bank fuzzy match still selects Fameo Cafe |
| 9 | "where is the nearest restroom please" (extra word) | bank match accepts the trailing "please" |
| 10 | "I'm hungry" | bank entry `hungry` routes to nearest cafe |
| 11 | "Men's WC" | gendered restroom resolution picks a Men's WC, never a Women's WC |
| 12 | "eaz04" (no separators) | hyphen-stripping Levenshtein resolves to EA-Z04 |
| 13 | "Take me to CS Lab" (lab POI not in fixture) | safe no-match — never invents a destination |
| 14 | "EA101 classroom" (rooms.json fallback) | resolves through rooms.json even when the POI catalog has no EA101 entry |
| 15 | "Cafe" (single word) | bank entry `cafe-bare` selects Fameo Cafe |
| 16 | "I am about to pee myself" (no contraction) | matches the urgent restroom entry the same as the apostrophe form |
| 17 | "WC" (single word) | resolves to the closest restroom |
| 18 | empty catalog + "I need a restroom" | rooms.json fallback still routes to the closest WC |
| 19 | empty catalog + "Fameo Cafe" | bank `.roomId` resolution routes via rooms.json |
| 20 | empty catalog + "WC" | single-word query resolves to a WC via rooms.json |
| 21 | empty catalog + "coffee" | single-word query resolves to Fameo Cafe via rooms.json |
| 22 | "I have a class in CS Lab" | anchor-token rule prevents accidentally matching `class-ea101`; routes to CS Lab |
| 23 | unit test of anchor-token scoring | digit-bearing prompts score 0 against unrelated user input |
| 24 | renamed room id (e.g. UUID) + "Fameo Cafe" | `.roomId` falls back to displayName lookup and still finds Fameo Cafe |

### Manual demo flow

1. Launch the app on iOS 17+ simulator or device.
2. Tap *Scan Entrance Code* → confirm entrance (use the bundled QR).
3. On the destination screen, the *Ask Vectura* card sits above the room
   list. Either tap one of the example chips or:
   - Type *"I have a class in EA-Z04."* (typos like *"i have class in eaz04"*
     also work) → tap the gradient send button or hit return.
   - A full-screen *Found it!* popup appears with the suggested destination,
     route summary, *Navigate now* (primary) and *Cancel* buttons.
   - Tap *Navigate now* → the app jumps **straight into AR navigation**,
     skipping the manual route preview.
4. Repeat with *"I'm about to pee myself."* and *"I really need to drink a
   coffee."* to validate the other two demo paths.
5. Tap the mic button to repeat any of the three sentences with voice
   input.

### Confirmation popup behaviour

- The popup is the only place where the user confirms the assistant's
  suggestion. The inline assistant card stays focused on input + idle hints.
- *Navigate now* selects the destination and starts AR navigation in a
  single step — no intermediate route-preview screen.
- *Cancel* (or tapping the dimmed background) dismisses the popup and
  returns the user to the assistant input so they can refine their query.
- If the agent returned alternatives, they appear underneath the suggested
  destination as tappable rows that also start AR navigation directly.

### Switching to GPT

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=gpt-oss:latest
./gradlew :tools:admin-api:run
```

Then in the *Ask Vectura* `…` menu pick *Backend GPT*. Without an active
LLM, the agent silently falls back to the mock — the demo is never blocked.

## Assumptions

- The reviewed package's `nav_graph.json` is the only source of physical
  truth. Every semantic POI is mapped to one of its existing node IDs (no
  invented coordinates).
- `EA-Z04` does not appear in the reviewed `rooms.json`; it is mapped to
  the `n21` graph node (the EA classroom area) for the demo. Adjust the
  POI's `nodeId` once a real EA-Z04 graph node exists.
- The admin API LLM proxy uses the same Ollama configuration that already
  drives `AiEdgeSuggester`. No new env vars introduced.
- Voice recognition uses `en-US`. Other locales require a small change in
  `VoiceInputManager.init()`.
- The iOS app does not ship a unit-test target; acceptance tests live in
  `AiAssistantDemoChecks.swift` and are exercised from the debugger.
