# File Dossier: ADR-034-android-visual-polish-parity.md

## Path
`docs/adr/ADR-034-android-visual-polish-parity.md`

## Type
Authored Documentation (ADR)

## Role
Documents the design and implementation decisions for the Android UI/UX polish pass (Phase 12), ensuring visual and motion parity with the iOS Phase 11 experience.

## Context
VecturAI's Android app required a comprehensive polish pass to match the premium feel of the iOS version. This ADR records the shift to a centralized design system, enhanced haptics, and a shared dark visitor design language.

## Decisions
- Migration of ad-hoc styles to a shared design system (`shared/designsystem`).
- Adoption of a consistent dark theme for the visitor flow.
- Integration of `AndroidHapticManager` across all UI interactions.
- Preservation of the existing ARCore session ownership model in `ArCameraActivity`.
- Implementation of motion, accessibility improvements, and visual hierarchy.

## Consequences
- **Positive**: Visual parity with iOS, improved accessibility, and centralized design tokens.
- **Negative**: Increased complexity in the shared design system; decorative motion requires performance gating.

## Related ADRs
- `ADR-033`: Client-Facing Polish (iOS counterpart).
- `ADR-027`: Rolling lookahead AR guidance.
- `ADR-012`: Demo-first UX.
