# File Dossier: QRPayload.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/qr/QRPayload.kt`

## Type
Authored Source (QR Protocol)

## Role
Parses and validates the Android QR entrance payload contract.

## Imports / Includes
- `com.vecturai.android.data.AndroidReviewedPackageLoader`
- `kotlinx.serialization.Serializable`
- `kotlinx.serialization.json.Json`

## Exports / Public Surface
- `QRPayload`
- `PayloadError`
- `EXPECTED_TYPE`
- `CURRENT_VERSION`
- `parse(raw)`
- `validate(against)`

## Protocol
- JSON object with `type`, `buildingId`, `entranceId`, and `v`.
- Expected type: `vecturai-entrance`.
- Supported version: `1`.
- Building id and entrance id must match the bundled reviewed package.

## Uses
- Kotlinx Serialization JSON.
- `AndroidReviewedPackageLoader.ReviewedConfig`.

## Used By
- `AndroidNavigationFlowModel.kt`
- `QRScanScreen.kt` indirectly through the flow model.

## Related Tests
- None.
