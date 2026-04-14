import SwiftUI
import UniformTypeIdentifiers

/// Admin screen for uploading GLB files and viewing draft jobs.
/// Dev-only — completely isolated from the visitor navigation flow.
struct AdminDraftJobsView: View {
    @StateObject private var viewModel = AdminDraftJobsViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List {
                // Upload section
                Section {
                    Text("Upload a Polycam .glb scan to generate a draft navigation package. The backend must be running locally.")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Button(action: { viewModel.showFilePicker = true }) {
                        Label("Select .glb File", systemImage: "doc.badge.plus")
                    }

                    if viewModel.isUploading {
                        HStack {
                            ProgressView()
                            Text("Uploading...")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }

                    if let error = viewModel.uploadError {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                } header: {
                    Text("Upload GLB")
                }

                // Jobs section
                Section {
                    if viewModel.jobs.isEmpty && !viewModel.isLoadingJobs {
                        Text("No draft jobs yet")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }

                    ForEach(viewModel.jobs) { job in
                        NavigationLink(destination: AdminJobDetailView(jobId: job.id)) {
                            AdminJobRow(job: job)
                        }
                    }

                    if viewModel.isLoadingJobs {
                        HStack {
                            ProgressView()
                            Text("Loading jobs...")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    HStack {
                        Text("Draft Jobs")
                        Spacer()
                        Button(action: { viewModel.loadJobs() }) {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                }
            }
            .navigationTitle("Admin Tools")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .fileImporter(
                isPresented: $viewModel.showFilePicker,
                allowedContentTypes: [UTType(filenameExtension: "glb") ?? .data],
                allowsMultipleSelection: false
            ) { result in
                viewModel.handleFileSelection(result)
            }
            .onAppear {
                viewModel.loadJobs()
            }
        }
    }
}

// MARK: - Job Row

private struct AdminJobRow: View {
    let job: DraftJobResponse

    var body: some View {
        HStack {
            statusIcon
            VStack(alignment: .leading, spacing: 2) {
                Text(job.originalFilename)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(job.status)
                    .font(.caption)
                    .foregroundStyle(statusColor)
            }
            Spacer()
            Text(formatDate(job.createdAt))
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
    }

    private var statusIcon: some View {
        Group {
            switch job.status {
            case "queued":
                Image(systemName: "clock")
                    .foregroundStyle(.gray)
            case "processing":
                ProgressView()
            case "succeeded":
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            case "failed":
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(.red)
            default:
                Image(systemName: "questionmark.circle")
                    .foregroundStyle(.gray)
            }
        }
    }

    private var statusColor: Color {
        switch job.status {
        case "succeeded": return .green
        case "failed": return .red
        case "processing": return .orange
        default: return .secondary
        }
    }

    private func formatDate(_ iso: String) -> String {
        // Simple truncation for dev display
        if iso.count > 19 {
            return String(iso.prefix(19)).replacingOccurrences(of: "T", with: " ")
        }
        return iso
    }
}

// MARK: - ViewModel

@MainActor
class AdminDraftJobsViewModel: ObservableObject {
    @Published var jobs: [DraftJobResponse] = []
    @Published var isLoadingJobs = false
    @Published var isUploading = false
    @Published var uploadError: String?
    @Published var showFilePicker = false

    private let client = AdminAPIClient()
    private var pollTask: Task<Void, Never>?

    deinit {
        pollTask?.cancel()
    }

    func loadJobs() {
        isLoadingJobs = true
        Task {
            do {
                let fetched = try await client.listJobs()
                self.jobs = fetched
                self.schedulePollingIfNeeded(fetched)
            } catch {
                print("[AdminDraftJobs] Failed to load jobs: \(error)")
            }
            self.isLoadingJobs = false
        }
    }

    /// Polls every 3 seconds while any job is still queued or processing,
    /// so the list reflects status updates without requiring manual refresh.
    private func schedulePollingIfNeeded(_ jobs: [DraftJobResponse]) {
        let hasActive = jobs.contains { $0.status == "queued" || $0.status == "processing" }
        if hasActive {
            pollTask?.cancel()
            pollTask = Task {
                try? await Task.sleep(nanoseconds: 3_000_000_000)
                guard !Task.isCancelled else { return }
                loadJobs()
            }
        } else {
            pollTask?.cancel()
            pollTask = nil
        }
    }

    func handleFileSelection(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            uploadFile(url: url)
        case .failure(let error):
            uploadError = "File selection failed: \(error.localizedDescription)"
        }
    }

    private func uploadFile(url: URL) {
        isUploading = true
        uploadError = nil

        // Start accessing security-scoped resource
        let accessing = url.startAccessingSecurityScopedResource()

        Task {
            defer {
                if accessing { url.stopAccessingSecurityScopedResource() }
                self.isUploading = false
            }
            do {
                let job = try await client.uploadGLB(fileURL: url)
                print("[AdminDraftJobs] Created job: \(job.id)")
                // Reload to show the new job
                loadJobs()
            } catch {
                self.uploadError = error.localizedDescription
            }
        }
    }
}
