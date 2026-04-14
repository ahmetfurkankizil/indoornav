import Foundation

/// Decoded QR payload from an entrance marker.
///
/// Phase 4: The QR code encodes a small JSON object identifying the building
/// and entrance. The app validates it against the bundled reviewed package.
///
/// Payload format (JSON):
/// ```json
/// {
///   "type": "vecturai-entrance",
///   "buildingId": "19",
///   "entranceId": "marker-main-entrance",
///   "v": 1
/// }
/// ```
struct QRPayload: Codable, Equatable {
    let type: String
    let buildingId: String
    let entranceId: String
    let v: Int

    /// Expected `type` field value.
    static let expectedType = "vecturai-entrance"
    /// Current payload version.
    static let currentVersion = 1

    // MARK: - Parsing

    enum PayloadError: Error, CustomStringConvertible {
        case notJSON
        case wrongType(String)
        case unsupportedVersion(Int)
        case buildingMismatch(expected: String, got: String)
        case entranceNotFound(String)

        var description: String {
            switch self {
            case .notJSON:
                return "QR code does not contain a valid VecturAI payload"
            case .wrongType(let t):
                return "Unknown QR type: \(t)"
            case .unsupportedVersion(let v):
                return "Unsupported QR version: \(v)"
            case .buildingMismatch(let expected, let got):
                return "QR is for building \"\(got)\" but this app has \"\(expected)\""
            case .entranceNotFound(let id):
                return "Entrance \"\(id)\" not found in navigation package"
            }
        }
    }

    /// Parse a raw QR string into a validated payload.
    static func parse(_ raw: String) -> Result<QRPayload, PayloadError> {
        guard let data = raw.data(using: .utf8),
              let payload = try? JSONDecoder().decode(QRPayload.self, from: data) else {
            return .failure(.notJSON)
        }

        guard payload.type == expectedType else {
            return .failure(.wrongType(payload.type))
        }

        guard payload.v >= 1, payload.v <= currentVersion else {
            return .failure(.unsupportedVersion(payload.v))
        }

        return .success(payload)
    }

    /// Validate that the payload matches a loaded reviewed package config.
    func validate(against config: BuildingPackageLoader.ReviewedConfig) -> PayloadError? {
        // Check building id
        if config.manifest.buildingId != buildingId {
            return .buildingMismatch(expected: config.manifest.buildingId, got: buildingId)
        }

        // Check entrance exists
        let entranceExists = config.entranceMarkers.contains { $0.id == entranceId }
        if !entranceExists {
            return .entranceNotFound(entranceId)
        }

        return nil
    }
}
