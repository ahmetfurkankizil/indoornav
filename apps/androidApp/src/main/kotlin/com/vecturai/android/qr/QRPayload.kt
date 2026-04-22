package com.vecturai.android.qr

import com.vecturai.android.data.AndroidReviewedPackageLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class QRPayload(
    val type: String,
    val buildingId: String,
    val entranceId: String,
    val v: Int,
) {
    fun validate(against: AndroidReviewedPackageLoader.ReviewedConfig): PayloadError? {
        if (against.manifest.buildingId != buildingId) {
            return PayloadError.BuildingMismatch(
                expected = against.manifest.buildingId,
                got = buildingId,
            )
        }
        if (against.entranceMarkers.none { it.id == entranceId }) {
            return PayloadError.EntranceNotFound(entranceId)
        }
        return null
    }

    sealed class PayloadError(
        override val message: String,
    ) : Exception(message) {
        data object NotJSON : PayloadError("QR code does not contain a valid VecturAI payload")
        data class WrongType(val actual: String) : PayloadError("Unknown QR type: $actual")
        data class UnsupportedVersion(val version: Int) : PayloadError("Unsupported QR version: $version")
        data class BuildingMismatch(val expected: String, val got: String) :
            PayloadError("QR is for building \"$got\" but this app has \"$expected\"")
        data class EntranceNotFound(val id: String) :
            PayloadError("Entrance \"$id\" not found in navigation package")
    }

    companion object {
        const val EXPECTED_TYPE = "vecturai-entrance"
        const val CURRENT_VERSION = 1

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

        fun parse(raw: String): Result<QRPayload> {
            val payload = try {
                json.decodeFromString<QRPayload>(raw)
            } catch (_: Throwable) {
                return Result.failure(PayloadError.NotJSON)
            }

            if (payload.type != EXPECTED_TYPE) {
                return Result.failure(PayloadError.WrongType(payload.type))
            }
            if (payload.v !in 1..CURRENT_VERSION) {
                return Result.failure(PayloadError.UnsupportedVersion(payload.v))
            }
            return Result.success(payload)
        }
    }
}
