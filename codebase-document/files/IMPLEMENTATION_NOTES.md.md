# File Dossier: IMPLEMENTATION_NOTES.md

## Path
IMPLEMENTATION_NOTES.md

## Type
docs

## Role
Implementation summary and developer guide.

## Imports / Includes
N/A

## Exports / Public Surface
N/A

## Main Symbols
N/A

## Important Logic by Line Range
- L3-64: Summary of completed implementation phases (Setup, AR, Cloud Anchors, Mapping, Persistence, etc.).
- L65-70: Remaining gaps.
- L80-97: Step-by-step run and test instructions.

## Uses
- Entire codebase.

## Used By
- Developers: For onboarding and local environment setup.

## Config / Constants / Protocol Details
- `ARCORE_API_KEY`: Required environment variable or `.env` entry.
- `ttlDays = 1`: Default Cloud Anchor TTL.

## Related Tests
- Instructions for running unit and device tests.

## Notes / Risks
- Explicitly mentions that `arrow.glb` placeholder exists.
- Notes the reliance on API keys for same-day demo.
