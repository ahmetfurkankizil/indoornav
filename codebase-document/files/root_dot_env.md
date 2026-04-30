# File Dossier: .env / .env.example

## Path
.env / .env.example

## Type
config

## Role
Environment variable configuration for sensitive keys (e.g., ARCORE_API_KEY).

## Imports / Includes
N/A

## Exports / Public Surface
- `ARCORE_API_KEY`: Google Cloud Anchor API key.

## Main Symbols
N/A

## Important Logic by Line Range
N/A

## Uses
N/A

## Used By
- `app/build.gradle.kts`: Reads `ARCORE_API_KEY` for manifest placeholder injection.

## Config / Constants / Protocol Details
- Format: `KEY=VALUE`

## Related Tests
N/A

## Notes / Risks
- `.env` is excluded from git for security.
- `.env.example` provides the template for developers.
