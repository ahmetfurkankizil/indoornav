import Foundation

/// Decoded QR payload from an entrance poster.
///
/// Two formats are supported:
/// - v1 (bundled package): {"type":"vecturai-entrance","buildingId":"...","entranceId":"...","v":1}
/// - v2 (remote package):  {"type":"vecturai-building","token":"...","v":2}
struct QRPayload: Codable, Equatable {
    let type: String
    let token: String?      // v2 only
    let buildingId: String? // v1 only
    let entranceId: String? // v1 only
    let v: Int

    enum Format: Equatable {
        case bundled(buildingId: String, entranceId: String)
        case remote(token: String)
    }

    var format: Format? {
        if type == "vecturai-building", let t = token { return .remote(token: t) }
        if type == "vecturai-entrance", let b = buildingId, let e = entranceId {
            return .bundled(buildingId: b, entranceId: e)
        }
        return nil
    }

    // MARK: - Errors

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

    // MARK: - Parsing

    static func parse(_ raw: String) -> Result<QRPayload, PayloadError> {
        guard let data = raw.data(using: .utf8),
              let payload = try? JSONDecoder().decode(QRPayload.self, from: data) else {
            return .failure(.notJSON)
        }
        guard payload.format != nil else {
            return .failure(.wrongType(payload.type))
        }
        guard payload.v >= 1 && payload.v <= 2 else {
            return .failure(.unsupportedVersion(payload.v))
        }
        return .success(payload)
    }

    /// Validate a v1 (bundled) payload against a loaded reviewed package.
    /// Always returns nil for v2 payloads (those are validated after remote fetch).
    func validate(against config: BuildingPackageLoader.ReviewedConfig) -> PayloadError? {
        guard case .bundled(let bid, let eid) = format else { return nil }
        if config.manifest.buildingId != bid {
            return .buildingMismatch(expected: config.manifest.buildingId, got: bid)
        }
        if !config.entranceMarkers.contains(where: { $0.id == eid }) {
            return .entranceNotFound(eid)
        }
        return nil
    }
}
