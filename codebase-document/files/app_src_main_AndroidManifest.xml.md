# File Dossier: AndroidManifest.xml

## Path
app/src/main/AndroidManifest.xml

## Type
config

## Role
Android application manifest defining permissions, features, and components.

## Imports / Includes
N/A

## Exports / Public Surface
N/A

## Main Symbols
- `com.google.android.ar.API_KEY`: Placeholder for the ARCore API key.
- `MainActivity`: Entry point activity.

## Important Logic by Line Range
- L5-7: Required permissions (`CAMERA`, `INTERNET`, `HIGH_SAMPLING_RATE_SENSORS`).
- L9-11: Declares AR hardware requirement.
- L22-25: Signals to the OS that ARCore is "required" for this application.
- L30-40: `MainActivity` registration with `LAUNCHER` intent filter.

## Uses
- ARCore SDK

## Used By
- Android OS

## Config / Constants / Protocol Details
- Uses `${ARCORE_API_KEY}` which is typically injected via `build.gradle.kts` from a local properties file or environment variable.

## Related Tests
N/A

## Notes / Risks
- `HIGH_SAMPLING_RATE_SENSORS` is needed for low-latency AR tracking on newer Android versions.
