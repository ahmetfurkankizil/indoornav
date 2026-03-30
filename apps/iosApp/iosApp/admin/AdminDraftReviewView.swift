import SwiftUI
import WebKit

// MARK: - Main Review View

/// Review screen for a succeeded draft job.
/// Supports read-only inspection, per-room metadata editing, and reviewed package export.
struct AdminDraftReviewView: View {
    let jobId: String

    @StateObject private var viewModel = AdminDraftReviewViewModel()
    @State private var selectedPreview: PreviewTab = .plan
    @State private var editingRoom: DraftRoomResponse?

    enum PreviewTab: String, CaseIterable {
        case plan = "Plan"
        case graph = "Graph"
    }

    var body: some View {
        List {
            if let summary = viewModel.summary {
                summarySection(summary)
                previewSection(summary)
                roomsSection(summary)
                exportSection
                if !summary.warnings.isEmpty {
                    warningsSection(summary.warnings)
                }
            } else if viewModel.isLoading {
                Section {
                    HStack {
                        ProgressView()
                        Text("Loading summary…").foregroundStyle(.secondary)
                    }
                }
            } else if let error = viewModel.errorMessage {
                Section("Error") {
                    Text(error).foregroundStyle(.red).font(.caption)
                }
            }
        }
        .navigationTitle("Draft Review")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { viewModel.load(jobId: jobId) }) {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(viewModel.isLoading)
            }
        }
        .sheet(item: $editingRoom) { room in
            RoomEditSheet(
                room: room,
                onSave: { displayName, category, description in
                    viewModel.patchRoom(
                        jobId: jobId,
                        roomId: room.id,
                        displayName: displayName,
                        category: category,
                        description: description
                    )
                }
            )
        }
        .onAppear { viewModel.load(jobId: jobId) }
    }

    // MARK: - Summary section

    @ViewBuilder
    private func summarySection(_ summary: DraftSummaryResponse) -> some View {
        Section("Building") {
            if let name = summary.buildingName {
                LabeledContent("Name", value: name)
            }
            if let id = summary.buildingId {
                LabeledContent("Building ID", value: id)
            }
            if let floor = summary.floorId {
                LabeledContent("Floor", value: floor)
            }
        }

        Section("Counts") {
            LabeledContent("Nodes", value: "\(summary.counts.nodes)")
            LabeledContent("Edges", value: "\(summary.counts.edges)")
            LabeledContent("Rooms", value: "\(summary.counts.rooms)")
            LabeledContent("Entrance Markers", value: "\(summary.counts.entranceMarkers)")
        }

        if let meta = summary.generationMetadata {
            Section("Generation") {
                if let ts = meta.timestamp {
                    LabeledContent("Timestamp", value: formatTimestamp(ts))
                }
                if let conf = meta.confidence {
                    LabeledContent("Confidence", value: conf)
                }
                if let edit = meta.editRequired {
                    LabeledContent("Edit Required", value: edit ? "Yes" : "No")
                }
                if let floorY = meta.floorY {
                    LabeledContent("Floor Y", value: String(format: "%.3f m", floorY))
                }
            }
        }

        if let geo = summary.geometryStats {
            Section("Geometry") {
                if let v = geo.totalVertices {
                    LabeledContent("Vertices", value: "\(v)")
                }
                if let m = geo.meshCount {
                    LabeledContent("Meshes", value: "\(m)")
                }
                if let x = geo.extentX, let z = geo.extentZ {
                    LabeledContent("Footprint", value: String(format: "%.1f × %.1f m", x, z))
                }
                if let w = geo.occupancyGridWidth, let h = geo.occupancyGridHeight {
                    LabeledContent("Grid", value: "\(w) × \(h) cells")
                }
                if let z = geo.zoneCount {
                    LabeledContent("Zones", value: "\(z)")
                }
            }
        }
    }

    // MARK: - Preview section

    @ViewBuilder
    private func previewSection(_ summary: DraftSummaryResponse) -> some View {
        Section("2D Previews") {
            Picker("Preview", selection: $selectedPreview) {
                ForEach(PreviewTab.allCases, id: \.self) { tab in
                    Text(tab.rawValue).tag(tab)
                }
            }
            .pickerStyle(.segmented)
            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))

            let client = AdminAPIClient()
            switch selectedPreview {
            case .plan:
                if summary.artifactAvailability.hasOccupancyPreview {
                    SVGPreviewCell(url: client.artifactContentURL(jobId: jobId, artifactName: "occupancy_debug.svg"))
                } else {
                    PreviewUnavailableCell(message: "Occupancy preview not available for this job.")
                }
            case .graph:
                if summary.artifactAvailability.hasGraphPreview {
                    SVGPreviewCell(url: client.artifactContentURL(jobId: jobId, artifactName: "draft_graph_debug.svg"))
                } else {
                    PreviewUnavailableCell(message: "Graph preview not available for this job.")
                }
            }
        }
    }

    // MARK: - Rooms section

    @ViewBuilder
    private func roomsSection(_ summary: DraftSummaryResponse) -> some View {
        Section("Room Candidates (\(summary.rooms.count))") {
            if summary.rooms.isEmpty {
                Text("No room candidates found in this draft.")
                    .foregroundStyle(.secondary)
                    .font(.subheadline)
            } else {
                ForEach(summary.rooms) { room in
                    Button {
                        editingRoom = room
                    } label: {
                        DraftRoomRow(room: room)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Export section

    @ViewBuilder
    private var exportSection: some View {
        Section("Reviewed Package") {
            if viewModel.isExporting {
                HStack {
                    ProgressView()
                    Text("Exporting…").foregroundStyle(.secondary)
                }
            } else if let result = viewModel.exportResult {
                if result.status == "succeeded" {
                    Label("Export succeeded", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                    ForEach(result.files, id: \.self) { filename in
                        HStack {
                            Image(systemName: "doc.text")
                                .foregroundStyle(.secondary)
                                .font(.caption)
                            Text(filename)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    if !result.warnings.isEmpty {
                        ForEach(result.warnings, id: \.self) { w in
                            Text(w)
                                .font(.caption2)
                                .foregroundStyle(.orange)
                        }
                    }
                } else {
                    Label("Export failed", systemImage: "xmark.circle.fill")
                        .foregroundStyle(.red)
                    if !result.warnings.isEmpty {
                        Text(result.warnings.joined(separator: "\n"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Button("Re-export Reviewed Package") {
                    viewModel.exportPackage(jobId: jobId)
                }
            } else {
                if let exportError = viewModel.exportError {
                    Text(exportError)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
                Button("Export Reviewed Package") {
                    viewModel.exportPackage(jobId: jobId)
                }
            }
        }
    }

    // MARK: - Warnings section

    @ViewBuilder
    private func warningsSection(_ warnings: [String]) -> some View {
        Section("Warnings (\(warnings.count))") {
            ForEach(warnings, id: \.self) { warning in
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundStyle(.orange)
                        .font(.caption)
                    Text(warning)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private func formatTimestamp(_ iso: String) -> String {
        guard iso.count > 19 else { return iso }
        return String(iso.prefix(19)).replacingOccurrences(of: "T", with: " ")
    }
}

// MARK: - Room Edit Sheet

private struct RoomEditSheet: View {
    let room: DraftRoomResponse
    let onSave: (String, String?, String?) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var displayName: String
    @State private var category: String
    @State private var description: String

    private static let categories = ["unknown", "room", "kitchen", "bedroom", "bathroom",
                                      "living", "corridor", "hallway", "entrance", "exit", "office", "other"]

    init(room: DraftRoomResponse, onSave: @escaping (String, String?, String?) -> Void) {
        self.room = room
        self.onSave = onSave
        _displayName = State(initialValue: room.displayName)
        _category = State(initialValue: room.category ?? "unknown")
        _description = State(initialValue: room.description ?? "")
    }

    var isSaveDisabled: Bool {
        displayName.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Identity") {
                    LabeledContent("Room ID", value: room.id)
                    if let nodeId = room.destinationNodeId {
                        LabeledContent("Node", value: nodeId)
                    }
                }

                Section("Display Name") {
                    TextField("Display name", text: $displayName)
                        .autocorrectionDisabled()
                }

                Section("Category") {
                    Picker("Category", selection: $category) {
                        ForEach(Self.categories, id: \.self) { cat in
                            Text(cat).tag(cat)
                        }
                    }
                }

                Section("Description") {
                    TextField("Description (optional)", text: $description, axis: .vertical)
                        .lineLimit(2...4)
                }
            }
            .navigationTitle("Edit Room")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let cat = category == "unknown" ? nil : category
                        let desc = description.isEmpty ? nil : description
                        onSave(displayName.trimmingCharacters(in: .whitespaces), cat, desc)
                        dismiss()
                    }
                    .disabled(isSaveDisabled)
                }
            }
        }
    }
}

// MARK: - SVG Preview Cell

/// Loads and renders an SVG from a URL using WKWebView.
/// Lightweight: no rasterisation, no third-party libraries.
private struct SVGPreviewCell: View {
    let url: URL

    var body: some View {
        SVGWebView(url: url)
            .frame(height: 260)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
    }
}

private struct SVGWebView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = true
        webView.scrollView.minimumZoomScale = 0.5
        webView.scrollView.maximumZoomScale = 4.0
        webView.backgroundColor = UIColor.secondarySystemBackground
        webView.isOpaque = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        webView.load(request)
    }
}

// MARK: - Preview Unavailable Cell

private struct PreviewUnavailableCell: View {
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "photo.slash")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 120)
        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
    }
}

// MARK: - Draft Room Row

/// A single room candidate row. Tap to open the edit sheet.
struct DraftRoomRow: View {
    let room: DraftRoomResponse

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(room.displayName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Spacer()
                if let cat = room.category {
                    Text(cat)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(categoryColor(cat).opacity(0.15))
                        .foregroundStyle(categoryColor(cat))
                        .clipShape(Capsule())
                }
                Image(systemName: "pencil")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            if let nodeId = room.destinationNodeId {
                Text("Node: \(nodeId)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if let desc = room.description, !desc.isEmpty {
                Text(desc)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 2)
    }

    private func categoryColor(_ category: String) -> Color {
        switch category.lowercased() {
        case "room": return .blue
        case "corridor", "hallway": return .green
        case "entrance", "exit": return .orange
        case "unknown": return .secondary
        default: return .purple
        }
    }
}

// MARK: - ViewModel

@MainActor
class AdminDraftReviewViewModel: ObservableObject {
    @Published var summary: DraftSummaryResponse?
    @Published var isLoading = false
    @Published var errorMessage: String?

    @Published var isExporting = false
    @Published var exportResult: ExportResultResponse?
    @Published var exportError: String?

    private let client = AdminAPIClient()

    func load(jobId: String) {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                let fetched = try await client.getDraftSummary(jobId: jobId)
                self.summary = fetched
                self.isLoading = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func patchRoom(jobId: String, roomId: String, displayName: String, category: String?, description: String?) {
        Task {
            do {
                _ = try await client.patchRoom(
                    jobId: jobId,
                    roomId: roomId,
                    displayName: displayName,
                    category: category,
                    description: description
                )
                // Reload summary so the room list reflects the override
                load(jobId: jobId)
            } catch {
                // Surface non-critical save errors without hiding the current summary
                errorMessage = "Failed to save room: \(error.localizedDescription)"
            }
        }
    }

    func exportPackage(jobId: String) {
        isExporting = true
        exportError = nil

        Task {
            do {
                let result = try await client.exportReviewedPackage(jobId: jobId)
                self.exportResult = result
                self.isExporting = false
            } catch {
                self.exportError = error.localizedDescription
                self.isExporting = false
            }
        }
    }
}
