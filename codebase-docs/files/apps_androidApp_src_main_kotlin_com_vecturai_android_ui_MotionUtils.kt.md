# File Dossier: MotionUtils.kt

## Path
`apps\androidApp\src\main\kotlin\com\vecturai\android\ui\MotionUtils.kt`

## Type
Authored Source

## Role
Provides Compose utilities for respecting system motion settings and battery saver mode in animations.

## Logic Overview
- `rememberReduceMotion()`: Reads `TRANSITION_ANIMATION_SCALE` to detect if the user has disabled animations.
- `rememberAuroraIntensity()`: Adjusts the background animation intensity based on motion settings and power save mode (0.35x intensity in power save, 0x when motion is reduced).

## Status
Mapped
