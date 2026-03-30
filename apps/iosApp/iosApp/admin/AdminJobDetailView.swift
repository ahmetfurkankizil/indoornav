import SwiftUI

/// Detail view for a single draft job — shows status, timestamps, artifacts, errors.
/// When the job succeeds, provides a "Review Draft" navigation link to the read-only review screen.
struct AdminJobDetailView: View {
    let jobId: String
    @StateObject private var viewModel = AdminJobDetailViewModel()

    var body: some View {
        List {
            if let job = viewModel.job {
                Section("Status") {
                    LabeledContent("Job ID", value: job.id)
                    LabeledContent("Filename", value: job.originalFilename)
                    LabeledContent("Status") {
                        HStack(spacing: 6) {
                            statusIcon(for: job.status)
                            Text(job.status)
                                .foregroundStyle(statusColor(for: job.status))
                        }
                    }
                    LabeledContent("Created", value: formatTimestamp(job.createdAt))
                    LabeledContent("Updated", value: formatTimestamp(job.updatedAt))
                }

                if job.status == "succeeded" {
                    Section("Draft Review") {
                        NavigationLink(destination: AdminDraftReviewView(jobId: job.id)) {
                            Label("Review Draft", systemImage: "doc.text.magnifyingglass")
                                .foregroundStyle(.blue)
                        }
                    }
                }

                if let error = job.errorMessage {
                    Section("Error") {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }

                if !job.artifacts.isEmpty {
                    Section("Artifacts (\(job.artifacts.count))") {
                        ForEach(job.artifacts, id: \.self) { artifact in
                            HStack {
                                Image(systemName: artifactIcon(artifact))
                                    .foregroundStyle(.secondary)
                                Text(artifact)
                                    .font(.subheadline)
                            }
                        }
                    }
                }
            } else if viewModel.isLoading {
                HStack {
                    ProgressView()
                    Text("Loading...")
                        .foregroundStyle(.secondary)
                }
            } else if let error = viewModel.errorMessage {
                Text(error)
                    .foregroundStyle(.red)
            }
        }
        .navigationTitle("Job Detail")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { viewModel.load(jobId: jobId) }) {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
        .onAppear {
            viewModel.load(jobId: jobId)
        }
    }

    private func statusIcon(for status: String) -> some View {
        Group {
            switch status {
            case "queued":
                Image(systemName: "clock").foregroundStyle(.gray)
            case "processing":
                ProgressView()
            case "succeeded":
                Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
            case "failed":
                Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
            default:
                Image(systemName: "questionmark.circle").foregroundStyle(.gray)
            }
        }
    }

    private func statusColor(for status: String) -> Color {
        switch status {
        case "succeeded": return .green
        case "failed": return .red
        case "processing": return .orange
        default: return .secondary
        }
    }

    private func artifactIcon(_ name: String) -> String {
        if name.hasSuffix(".svg") { return "doc.richtext" }
        if name.hasSuffix(".json") { return "doc.text" }
        return "doc"
    }

    private func formatTimestamp(_ iso: String) -> String {
        if iso.count > 19 {
            return String(iso.prefix(19)).replacingOccurrences(of: "T", with: " ")
        }
        return iso
    }
}

// MARK: - ViewModel

@MainActor
class AdminJobDetailViewModel: ObservableObject {
    @Published var job: DraftJobResponse?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let client = AdminAPIClient()
    private var pollTimer: Timer?

    func load(jobId: String) {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                let fetched = try await client.getJob(id: jobId)
                self.job = fetched
                self.isLoading = false

                // Auto-poll if still processing
                if fetched.status == "queued" || fetched.status == "processing" {
                    try? await Task.sleep(nanoseconds: 2_000_000_000)
                    load(jobId: jobId)
                }
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
}
