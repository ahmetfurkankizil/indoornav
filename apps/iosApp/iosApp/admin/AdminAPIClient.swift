import Foundation

/// Minimal HTTP client for the dev-only admin draft API.
/// Isolated from the visitor navigation package loader.
class AdminAPIClient {
    /// Base URL for the admin API. Default is localhost for simulator.
    /// Override for real device testing on LAN (e.g., "http://192.168.1.100:8080").
    static var baseURL: String = "http://172.20.10.4:8080"

    private let session = URLSession.shared

    // MARK: - Upload GLB

    func uploadGLB(fileURL: URL) async throws -> DraftJobResponse {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        let fileData = try Data(contentsOf: fileURL)
        let filename = fileURL.lastPathComponent

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(filename)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: application/octet-stream\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        let (data, response) = try await session.data(for: request)
        let httpResponse = response as! HTTPURLResponse

        if httpResponse.statusCode == 201 {
            return try JSONDecoder().decode(DraftJobResponse.self, from: data)
        } else {
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw AdminAPIError.uploadFailed(statusCode: httpResponse.statusCode, message: errorBody)
        }
    }

    // MARK: - List Jobs

    func listJobs() async throws -> [DraftJobResponse] {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs")!
        let (data, response) = try await session.data(from: url)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode([DraftJobResponse].self, from: data)
    }

    // MARK: - Get Job

    func getJob(id: String) async throws -> DraftJobResponse {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs/\(id)")!
        let (data, response) = try await session.data(from: url)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode(DraftJobResponse.self, from: data)
    }

    // MARK: - Get Artifacts

    func getArtifacts(jobId: String) async throws -> [String] {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/artifacts")!
        let (data, response) = try await session.data(from: url)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode([String].self, from: data)
    }

    // MARK: - Get Draft Summary

    func getDraftSummary(jobId: String) async throws -> DraftSummaryResponse {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/summary")!
        let (data, response) = try await session.data(from: url)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode(DraftSummaryResponse.self, from: data)
    }

    // MARK: - Get Artifact Content URL

    /// Returns the URL to fetch an artifact's raw content (SVG, JSON, etc.).
    /// Use this with WKWebView or URLSession for direct loading.
    func artifactContentURL(jobId: String, artifactName: String) -> URL {
        URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/artifacts/\(artifactName)/content")!
    }

    func getArtifactContent(jobId: String, artifactName: String) async throws -> Data {
        let url = artifactContentURL(jobId: jobId, artifactName: artifactName)
        let (data, response) = try await session.data(from: url)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return data
    }

    // MARK: - Patch Room

    func patchRoom(jobId: String, roomId: String, displayName: String?, category: String?, description: String?) async throws -> RoomOverrideResponse {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/rooms/\(roomId)")!
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        var body: [String: String] = [:]
        if let displayName { body["displayName"] = displayName }
        if let category { body["category"] = category }
        if let description { body["description"] = description }
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: request)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode(RoomOverrideResponse.self, from: data)
    }

    // MARK: - Export Reviewed Package

    func exportReviewedPackage(jobId: String) async throws -> ExportResultResponse {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/export-reviewed-package")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let (data, response) = try await session.data(for: request)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode(ExportResultResponse.self, from: data)
    }

    // MARK: - Get Reviewed Package Files

    func getExportedPackageFiles(jobId: String) async throws -> [String] {
        let url = URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/reviewed-package")!
        let (data, response) = try await session.data(from: url)
        let httpResponse = response as! HTTPURLResponse

        guard httpResponse.statusCode == 200 else {
            throw AdminAPIError.requestFailed(statusCode: httpResponse.statusCode)
        }

        return try JSONDecoder().decode([String].self, from: data)
    }

    func reviewedPackageFileContentURL(jobId: String, filename: String) -> URL {
        URL(string: "\(Self.baseURL)/admin/draft-jobs/\(jobId)/reviewed-package/\(filename)/content")!
    }
}

// MARK: - Models

struct DraftJobResponse: Codable, Identifiable {
    let id: String
    let originalFilename: String
    let createdAt: String
    let updatedAt: String
    let status: String
    let errorMessage: String?
    let artifacts: [String]
}

struct DraftSummaryResponse: Codable {
    let jobId: String
    let status: String
    let buildingId: String?
    let buildingName: String?
    let floorId: String?
    let artifactAvailability: ArtifactAvailabilityResponse
    let counts: GraphCountsResponse
    let rooms: [DraftRoomResponse]
    let generationMetadata: GenerationMetadataResponse?
    let geometryStats: GeometryStatsResponse?
    let warnings: [String]
}

struct ArtifactAvailabilityResponse: Codable {
    let hasOccupancyPreview: Bool
    let hasGraphPreview: Bool
    let hasAuthoringConfig: Bool
    let hasGenerationMetadata: Bool
    let hasGeometryStats: Bool
}

struct GraphCountsResponse: Codable {
    let nodes: Int
    let edges: Int
    let rooms: Int
    let entranceMarkers: Int
    let checkpointMarkers: Int?
}

struct DraftRoomResponse: Codable, Identifiable {
    let id: String
    let displayName: String
    let category: String?
    let destinationNodeId: String?
    let description: String?
}

struct GenerationMetadataResponse: Codable {
    let generatedBy: String?
    let timestamp: String?
    let confidence: String?
    let editRequired: Bool?
    let floorY: Double?
    let floorConfidence: Double?
}

struct GeometryStatsResponse: Codable {
    let totalVertices: Int?
    let meshCount: Int?
    let extentX: Double?
    let extentY: Double?
    let extentZ: Double?
    let occupancyGridWidth: Int?
    let occupancyGridHeight: Int?
    let occupancyGridCellSize: Double?
    let zoneCount: Int?
}

struct RoomOverrideResponse: Codable {
    let displayName: String?
    let category: String?
    let description: String?
    let updatedAt: String?
}

struct ExportResultResponse: Codable {
    let jobId: String
    let status: String
    let files: [String]
    let exportPath: String?
    let warnings: [String]
}

enum AdminAPIError: LocalizedError {
    case uploadFailed(statusCode: Int, message: String)
    case requestFailed(statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .uploadFailed(let code, let message):
            return "Upload failed (\(code)): \(message)"
        case .requestFailed(let code):
            return "Request failed with status \(code)"
        }
    }
}
