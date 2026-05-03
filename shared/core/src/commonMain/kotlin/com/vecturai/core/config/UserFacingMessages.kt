package com.Vectura AI.core.config

/**
 * User-facing messages for presentation-safe UI strings.
 *
 * Provides clear, non-technical language for all user-visible states.
 * Separate from debug logging. Use these in UI overlays and status displays.
 *
 * Two tiers:
 * - [user]: Clean, non-technical language for presenters and end users
 * - [debug]: Technical detail for operators and developers
 */
object UserFacingMessages {

    // ── Package Loading ─────────────────────────
    object PackageLoading {
        const val LOADING = "Loading building data…"
        const val LOADED = "Building loaded"
        const val NOT_FOUND = "Building data not available. Please check your connection."
        const val CORRUPT = "Building data could not be read. Try reloading."
        const val DEBUG_NOT_FOUND = "[Package] File not found at path"
        const val DEBUG_CORRUPT = "[Package] JSON parse failed"
    }

    // ── Marker Detection ────────────────────────
    object MarkerDetection {
        const val WAITING = "Point your camera at the entrance marker"
        const val SCANNING = "Scanning…"
        const val DETECTED = "Marker detected — starting navigation"
        const val NOT_DETECTED = "Marker not found. Make sure it's visible and well-lit."
        const val TRY_AGAIN = "Move closer and try again"
        const val SIMULATE_HINT = "Tap 'Simulate Scan' to skip marker detection"
        const val DEBUG_DETECTED = "[Marker] Anchor detected"
        const val DEBUG_CONFIDENCE = "[Marker] Confidence:"
    }

    // ── Checkpoint Markers ──────────────────────
    object Checkpoint {
        const val CORRECTING = "Adjusting alignment…"
        const val CORRECTED = "Alignment improved"
        const val NOT_AVAILABLE = "No checkpoint markers on this route"
        const val DEBUG_CORRECTION = "[Checkpoint] Correction applied"
        const val DEBUG_REJECTED = "[Checkpoint] Observation rejected"
    }

    // ── Navigation ──────────────────────────────
    object Navigation {
        const val NAVIGATING = "Follow the arrows"
        const val ARRIVING = "Almost there!"
        const val ARRIVED = "You've arrived!"
        const val ROUTE_NOT_FOUND = "No path found to this destination"
        const val SESSION_ENDED = "Navigation ended"
    }

    // ── Confidence & Recovery ───────────────────
    object Confidence {
        const val HIGH = "Tracking is good"
        const val MODERATE = "Tracking OK"
        const val LOW = "Tracking quality reduced"
        const val RECENTER_RECOMMENDED = "Try scanning the marker again for better accuracy"
        const val MOVE_TOWARD_ROUTE = "Move back toward the corridor"
        const val OFF_ROUTE = "You may be off the route. Head back toward the arrows."
        const val DEBUG_STALE_POSE = "[Tracking] Pose data stale"
        const val DEBUG_OFF_ROUTE = "[Tracking] Off-route distance:"
    }

    // ── History ──────────────────────────────────
    object History {
        const val SAVED = "Visit saved to history"
        const val EMPTY = "No recent visits"
        const val CLEARED = "History cleared"
        const val LOAD_FAILED = "Could not load visit history"
        const val DEBUG_LOAD_FAILED = "[History] Failed to parse stored history, starting fresh"
    }

    // ── Demo Mode ───────────────────────────────
    object DemoMode {
        const val LABEL = "Demo Mode"
        const val SIMULATED = "Simulated — not using real AR"
        const val ADVANCE_HINT = "Tap 'Advance' to move along the route"
    }

    // ── General ─────────────────────────────────
    object General {
        const val LOADING = "Loading…"
        const val ERROR = "Something went wrong. Please try again."
        const val RETRY = "Retry"
        const val CANCEL = "Cancel"
        const val OK = "OK"
    }
}
