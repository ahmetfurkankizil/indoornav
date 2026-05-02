# Android UI/UX Polish — Implementation Handoff

> **Audience:** an AI coding agent (or engineer) picking up this work cold, with no prior conversation context.
> **Goal:** bring the Android app to visual + motion parity with the iOS Phase 11 polish, with a richer brand identity.
> **Estimated effort:** ~7–9 engineer-days end-to-end. Phases are independently shippable.

---

## 0. Mission

The iOS app received a comprehensive client-facing polish pass in **Phase 11** (`apps/iosApp/iosApp/VecturTheme.swift`, `apps/iosApp/iosApp/HapticManager.swift`, [`docs/adr/ADR-033-client-facing-polish.md`](docs/adr/ADR-033-client-facing-polish.md)).
The Android app did **not** get a parallel pass. Its UI is functional but visually flat, inconsistent, and lacks motion. This document is the complete plan to close that gap and push Android slightly past iOS in expressiveness.

You should produce a **new ADR-034: Android visual polish parity** documenting the decisions before merging.

---

## 1. Context Handoff

### 1.1 Project at a glance

VecturAI is an AR indoor navigation app:
- **Shared logic:** Kotlin Multiplatform (`shared/core`, `shared/designsystem`, `shared/features`).
- **Android app:** `apps/androidApp/` — Jetpack Compose visitor flow + ARCore.
- **iOS app:** `apps/iosApp/` — SwiftUI + ARKit. Already polished (Phase 11).
- **Backend tools:** `tools/admin-api`, `tools/nav-preprocessor`.

**Current branch when this doc was written:** `demo-trail-enhance`.
**Project root:** `C:\Users\emirh\Desktop\bitirme\vecturai\` (Windows). Use forward slashes in code; the shell is bash.

### 1.2 The Android visitor flow

```
MainActivity (Home / PackageError)
    └─ tap "Scan Entrance Code"
       └─ ArCameraActivity (one ARCore session, GLSurfaceView underneath all overlays)
              ├─ QRScanScreen          (overlay above live camera)
              ├─ EntranceConfirmedSheet
              ├─ DestinationSelectScreen   (opaque overlay; AR session still resumed)
              ├─ RoutePreviewScreen        (opaque overlay)
              └─ ArNavigationScreen
                    ├─ AlignmentOverlay (pre-alignment)
                    ├─ ActiveNavigationOverlay
                    └─ ArrivalOverlay
```

`ArCameraActivity` owns one long-lived ARCore session, one `GLSurfaceView`, one GL camera texture, and one `UnifiedArRenderer` for the entire flow. **Do not break that ownership model.** Compose overlays float above the GLSurfaceView. QR scanning runs on ARCore frames via `ArFrameQrScanner` + ML Kit — there is no separate CameraX preview.

### 1.3 Pre-flight reading (read these before touching any code)

In this exact order:

1. [`CLAUDE.md`](CLAUDE.md) — project rules, phase history, build commands, things-not-to-do.
2. [`codebase-docs/AGENT.md`](codebase-docs/AGENT.md) — how to use the context pack.
3. [`codebase-docs/codebase-map.md`](codebase-docs/codebase-map.md) — top-level architecture.
4. [`codebase-docs/features/ar_navigation_android.md`](codebase-docs/features/ar_navigation_android.md) — Android AR pipeline.
5. [`codebase-docs/features/design_system.md`](codebase-docs/features/design_system.md) — shared design system overview.
6. [`docs/adr/ADR-033-client-facing-polish.md`](docs/adr/ADR-033-client-facing-polish.md) — the iOS pass you are mirroring.

### 1.4 The four files you will spend the most time in

```
apps/androidApp/src/main/kotlin/com/vecturai/android/MainActivity.kt
apps/androidApp/src/main/kotlin/com/vecturai/android/ui/AndroidNavigationApp.kt   (~1615 lines, biggest)
apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt
apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt
```

Plus the shared design system you will extend:
```
shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Color.kt
shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Theme.kt
shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Typography.kt
shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Components.kt   (you will add to this)
```

Plus the haptic singleton already present on Android but under-utilized:
```
apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidHapticManager.kt
```

### 1.5 Non-goals / boundaries

- **Do NOT touch `shared/core`** (graph + pathfinding domain). It is platform-agnostic and well-tested.
- **Do NOT change iOS code.** iOS is already polished.
- **Do NOT change ARCore session ownership / lifecycle** (`UnifiedArSession`, `UnifiedArRenderer`, `ArCameraActivity`).
- **Do NOT add new dependencies without justification.** You may add **only**: `androidx.compose.animation`, `androidx.compose.ui:ui-graphics`, `androidx.compose.foundation` (likely already present), and the Inter font via Compose Resources. No Lottie, no third-party motion libs, no design system frameworks.
- **Do NOT change the QR contract** (`{"type":"vecturai-entrance",...}`) or any data models.
- **Do NOT introduce backwards-compat shims**, feature flags, or "old vs new" toggles. Replace the old UI; do not parallel-build.
- **Do NOT add comments that explain WHAT the code does.** Only add a comment if WHY is non-obvious. Follow the rules in `CLAUDE.md`.

---

## 2. Diagnosis (evidence-based)

These are the concrete problems you must fix. Each has a file:line citation.

### 2.1 Theme is wrapped but ignored
`VecturaiTheme` is applied at the app root (`AndroidNavigationApp.kt:95`), but every Composable then hardcodes hex literals. Examples:

| Color literal | Occurrences | Should be |
|---|---|---|
| `Color(0xFF168BFF)` (primary blue) | 30+ across all 3 screens | `MaterialTheme.colorScheme.primary` |
| `Color(0xFF070D18)` (canvas bg) | every screen | `VecturaiColors.SurfaceCanvas` (new token) |
| `Color(0xFF151F31)` (card) | every card | `VecturaiColors.SurfaceCard` (new token) |
| `Color(0xFF233149)` (border) | every card | `VecturaiColors.BorderSubtle` (new token) |
| `Color(0xFF8A95A8)` / `0xFF6F7B8E` / `0xFF7F8A9D` | text muted, 3 different values | one `VecturaiColors.TextMuted` |

Migrate **all** literals to tokens or `MaterialTheme.colorScheme`.

### 2.2 No real buttons
Every primary CTA is a hand-rolled `Row + clickable`. No ripple. No press scale. No state layer. Examples:
- `AndroidNavigationApp.kt:339-365` — `WelcomePrimaryButton`
- `AndroidNavigationApp.kt:1033-1056` — "Start AR Navigation"
- `AndroidNavigationApp.kt:1473-1489` — "Choose Destination"
- `AndroidNavigationApp.kt:417-433` — "Try Again" in `PackageErrorScreen`

These all become a single new `VecturaiPrimaryButton`.

The back-arrow chip is also re-implemented at every screen (`AndroidNavigationApp.kt:489-504`, `:953-968`, `QRScanScreen.kt:88-104`). Becomes a single `IconChip`.

### 2.3 Three near-identical dot backgrounds
`DotGridBackground` (`AndroidNavigationApp.kt:298-316`), `ScanDotBackground` (`QRScanScreen.kt:250-268`), `ArrivalDotBackground` (`ArNavigationScreen.kt:640-658`) are copy-paste with cosmetic deltas. Collapse into one `AuroraBackground` composable.

### 2.4 Inconsistent radius and spacing scale
Corners observed: `11, 12, 13, 14, 16, 18, 20, 22, 28` dp. Padding observed: `7, 8, 10, 11, 12, 13, 14, 16, 18, 20, 22` dp. Pick a scale and migrate.

### 2.5 Typography overridden everywhere
`VecturaiTypography` exists but is essentially never used. Every `Text` sets `fontSize` + `fontWeight` + `lineHeight` + `color` inline. `FontWeight.ExtraBold` is used on ~80% of text, killing hierarchy.

### 2.6 No motion system
- `animateFloatAsState(targetValue = 1f)` at `AndroidNavigationApp.kt:1498` is a no-op (constant target).
- `AnimatedVisibility(visible = true, ...)` at `:1317` is also a no-op.
- The only real animation is the arrival spring at `ArNavigationScreen.kt:448-452`.

### 2.7 Zero brand expression
The "logo" on Home is `Icons.Default.LocationOn` in a flat blue square (`AndroidNavigationApp.kt:162-175`). No wordmark, no signature gradient, no decorative geometry.

### 2.8 No haptics on UI taps
`AndroidHapticManager` exists and is wired into AR alignment events, but no tap on Home, destination selection, route preview start, or back chip fires it.

### 2.9 QR scan screen is a frozen graphic
`QRScanScreen` shows a static `QrCodeScanner` icon in a box (`QRScanScreen.kt:140-145`) with no animation, no live camera underlay (the underlay exists — ARCore is running — but the screen background is opaque dots), and no "found it" success state.

### 2.10 Filter chips read weakly
`DestinationFilterChip` (`AndroidNavigationApp.kt:704-734`) uses the same blue fill as primary buttons and the same bordered surface style as cards. Selected vs. unselected is barely distinguishable from a glance.

### 2.11 Accessibility gaps
- 40 dp filter chips and 38 dp back chip violate the 48 dp min touch target.
- `Color(0xFF6F7B8E)` on `Color(0xFF151F31)` is ~3.4:1 contrast — below WCAG AA for body text.
- Inline `sp` ignores `fontScale`.
- Decorative dots/icons lack `contentDescription` semantics.

---

## 3. Target Design Language

### 3.1 Brand pillars

- **Premium, kinetic, "smart spatial".**
- Voltaic blue primary, electric cyan glow for live/active state, amber for warnings + arrival accent only.
- One signature gradient: **`#1D4ED8 → #2563EB → #06B6D4`** diagonal — used on hero CTAs, gradient text, and the active-route stripe. Nowhere else.
- Custom mark replaces the default location icon: a stylized arrow folding into a node.

### 3.2 Color tokens (additions to `VecturaiColors`)

Add these to [`Color.kt`](shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Color.kt):

```kotlin
// Surface scale (dark theme — visitor flow is always dark)
val SurfaceCanvas    = Color(0xFF070D18)
val SurfaceElevated  = Color(0xFF121A28)
val SurfaceCard      = Color(0xFF151F31)
val SurfaceOverlay   = Color(0xFF1B2436)

// Borders
val BorderSubtle     = Color(0xFF233149)
val BorderStrong     = Color(0xFF2B3952)

// Text
val TextPrimary      = Color(0xFFF1F5F9)
val TextSecondary    = Color(0xFFB6BFCE)
val TextMuted        = Color(0xFF8E99AE)   // bumped from 0xFF6F7B8E for WCAG AA
val TextDisabled     = Color(0xFF566173)

// Brand accents
val AccentCyan       = Color(0xFF22D3EE)
val AccentGreen      = Color(0xFF12C86A)
val AccentAmber      = Color(0xFFF59E0B)
val AccentRed        = Color(0xFFEF4444)

// Brand gradient stops
val GradientStart    = Color(0xFF1D4ED8)
val GradientMid      = Color(0xFF2563EB)
val GradientEnd      = Color(0xFF06B6D4)
```

Add a brush helper in the same file:

```kotlin
import androidx.compose.ui.graphics.Brush

object VecturaiBrush {
    val Primary: Brush
        @androidx.compose.runtime.Composable
        get() = Brush.linearGradient(
            colors = listOf(
                VecturaiColors.GradientStart,
                VecturaiColors.GradientMid,
                VecturaiColors.GradientEnd,
            ),
        )
}
```

### 3.3 Spacing scale

Create `Spacing.kt` in `shared/designsystem`:

```kotlin
object Spacing {
    val xxs = 4.dp
    val xs  = 8.dp
    val sm  = 12.dp
    val md  = 16.dp
    val lg  = 20.dp
    val xl  = 24.dp
    val xxl = 32.dp
}
```

Replace all ad-hoc dp values with these. Acceptable exceptions: 1.dp dividers, AR-physical-world offsets.

### 3.4 Shape scale

```kotlin
object VecturaiShapes {
    val Small  = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large  = RoundedCornerShape(20.dp)
    val XLarge = RoundedCornerShape(28.dp)
    val Pill   = RoundedCornerShape(50)
}
```

Migrate all corner literals. Keep the spec uniform: cards = Medium or Large; pills = Pill; hero panels = XLarge.

### 3.5 Typography

Extend `VecturaiTypography` and add a tabular-figures style for numerics (ETA, distance, counters):

```kotlin
val NumericDisplay = TextStyle(
    fontSize = 64.sp,
    fontWeight = FontWeight.ExtraBold,
    lineHeight = 64.sp,
    fontFeatureSettings = "tnum",
    letterSpacing = (-1).sp,
)
val NumericLarge = TextStyle(
    fontSize = 28.sp,
    fontWeight = FontWeight.ExtraBold,
    lineHeight = 32.sp,
    fontFeatureSettings = "tnum",
)
val Overline = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = 1.4.sp,
)
```

Load **Inter** (variable) via Compose Resources. Place font files at `shared/designsystem/src/commonMain/composeResources/font/inter_variable.ttf`. Set as default `fontFamily` in `VecturaiTypography`. Fall back to system if loading fails.

### 3.6 Motion vocabulary

- **Standard easing:** `FastOutSlowInEasing` (220 ms for screen transitions, 120 ms for taps).
- **Spring for arrival/success:** `spring(dampingRatio = 0.6f, stiffness = StiffnessMediumLow)`.
- **Tap pattern:** scale 1.0 → 0.97 → 1.0 over 120 ms with `interactionSource`.
- **Entry pattern:** 16 dp slide-up + fade, 220 ms.
- **Reduce-motion:** read `Settings.Global.TRANSITION_ANIMATION_SCALE`. If 0, disable non-essential motion (keep only state-changing animations like the success check).

### 3.7 Haptic vocabulary

Map every interaction to a tier:

| Interaction | Haptic |
|---|---|
| Primary button tap, list row tap | Light tick |
| Filter chip selection | Selection |
| QR detected, alignment locked | Success |
| Imminent turn (< 2 m), arrival | Success notification |
| Tracking degraded | Warning |
| Error (QR mismatch, package error) | Error |

`AndroidHapticManager` already supports these — wire them.

---

## 4. Phase A — Foundation

**Time budget:** 1 day. **Risk:** low. **Touches:** `shared/designsystem` only.

### A1. Extend the color system
- Add all tokens from §3.2 to [`Color.kt`](shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Color.kt).
- Add `VecturaiBrush` object in the same file (or a new `Brushes.kt`).

### A2. Make dark scheme the default for visitor flow
Edit [`Theme.kt`](shared/designsystem/src/commonMain/kotlin/com/vecturai/designsystem/Theme.kt):
- Set `darkTheme: Boolean = true` as default. The visitor app is always dark.
- Wire the new tokens into `darkColorScheme(...)` (`background = SurfaceCanvas`, `surface = SurfaceCard`, `surfaceVariant = SurfaceElevated`, `outline = BorderSubtle`, `onSurface = TextPrimary`, `onSurfaceVariant = TextMuted`).

### A3. Add Spacing + Shape objects
- Create `Spacing.kt` and `Shapes.kt` next to `Color.kt`.

### A4. Extend Typography + load Inter
- Add `NumericDisplay`, `NumericLarge`, `Overline` to `Typography.kt` (as separate vals, since `Material3 Typography` slots are fixed).
- Add Inter font file under `composeResources/font/`.
- Define `val InterFamily = FontFamily(Font(Res.font.inter_variable))` and set `fontFamily = InterFamily` on every style in `VecturaiTypography`.

### A5. Add ADR-034
Create [`docs/adr/ADR-034-android-visual-polish-parity.md`](docs/adr/ADR-034-android-visual-polish-parity.md) following the existing ADR style. Include: context, decision, consequences, alternatives considered.

### A6. Acceptance
- `./gradlew :shared:designsystem:build` passes.
- `./gradlew :apps:androidApp:assembleDebug` still builds (no consumer changes yet).
- Inter font renders on a smoke-test screen (run the app, see the system font swap).

---

## 5. Phase B — Component Library

**Time budget:** 2 days. **Risk:** low. **Touches:** `shared/designsystem/Components.kt`.

Add the components below. Each one is referenced by Phase C screens. **Build them all before starting C.**

### B1. `VecturaiPrimaryButton`
Real Material `Button` with brand gradient, ripple, press scale, leading-icon slot, loading state, disabled state, and built-in haptic.

```kotlin
@Composable
fun VecturaiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "pressScale",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(VecturaiShapes.Medium)
            .background(VecturaiBrush.Primary)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                role = Role.Button,
                enabled = enabled && !loading,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = Color.White,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(Spacing.sm))
                }
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                )
            }
        }
    }
}
```

### B2. `VecturaiSecondaryButton` / `VecturaiGhostButton`
Bordered + transparent variants. Same press behavior. No gradient.

### B3. `IconChip`
The recurring back-arrow chip:

```kotlin
@Composable
fun IconChip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) { /* 48.dp min touch target, surface elevated, border subtle, rounded medium */ }
```

Min size **48.dp** (was 38–44.dp; fix the touch-target violation).

### B4. `VecturaiCard`
The dark card preset:

```kotlin
@Composable
fun VecturaiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)
```

Surface: `SurfaceCard`, border: `BorderSubtle`, shape: `Large`, optional click w/ ripple + haptic.

### B5. `StatPill` / `CategoryBadge`
Replace the inline pill pattern:
```
Surface(shape = Pill, color = X, border = Y) { Text(...) }
```
Use it for: tracking confidence, "DEMO" tag, "Floor G", "Confirmed".

### B6. `VecturaiFilterChip`
Wraps M3 `FilterChip`. Selected state uses `VecturaiBrush.Primary` background and white text. Unselected uses `SurfaceElevated` + `BorderSubtle`. Min 48.dp height.

### B7. `SectionHeader`
Replaces every `SectionLabel` + count `Row`:
```kotlin
@Composable
fun SectionHeader(title: String, trailing: String? = null)
```
Uses `VecturaiTypography.Overline` style.

### B8. `AuroraBackground`
Single composable replacing all three dot canvases. Layers (back to front):
1. Solid `SurfaceCanvas` fill.
2. Two slow-drifting radial gradients (one cyan-tinted, one primary-tinted) using `infiniteTransition` + Lissajous-style offsets, 18s loop.
3. Dot grid overlay (the existing 22.dp spacing pattern).
4. Soft top + bottom vignette.

API:
```kotlin
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,    // 0f = static, 1f = full motion
    showDots: Boolean = true,
)
```

Pause animation when `LocalLifecycleOwner.current.lifecycle.currentState != RESUMED`.
Respect reduce-motion: when transition scale is 0, pass `intensity = 0f`.

**Performance budget:** < 1 ms/frame on a Pixel 4a. Profile with `Modifier.drawBehind` instead of nested Composables.

### B9. `Modifier.vecturaiTap()`
Centralizes the tap pattern (scale + haptic + ripple) so list rows can opt in:

```kotlin
fun Modifier.vecturaiTap(
    enabled: Boolean = true,
    haptic: HapticFeedbackType = HapticFeedbackType.LongPress,
    onClick: () -> Unit,
): Modifier = composed { /* interactionSource + scale + clickable */ }
```

### B10. `AnimatedNumber`
Tab-friendly number text that count-ups on appear and tweens on update:

```kotlin
@Composable
fun AnimatedNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = VecturaiTypography.NumericLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
)
```

### B11. `GradientText`
Helper to render text filled with `VecturaiBrush.Primary`:

```kotlin
@Composable
fun GradientText(text: String, style: TextStyle, modifier: Modifier = Modifier)
```

Use for "VecturAI" wordmark and the large ETA on Route Preview.

### B12. Acceptance for Phase B
- All components live in `shared/designsystem/Components.kt` (or one new file per component).
- A `ComponentGalleryPreview.kt` (Compose Preview) shows each in idle / pressed / disabled states.
- `./gradlew :shared:designsystem:build` passes.

---

## 6. Phase C — Per-Screen Redesigns

**Time budget:** 3–4 days. **Risk:** medium (visual regressions, ARCore overlay layering).

Tackle screens in this order so the highest-traffic ones land first: **Home → Destination Select → Route Preview → QR Scan → Entrance Confirmed → AR Active → Alignment → Arrival → Errors.**

For every screen, replace **all** raw `Color(0xFFxxx)`, `Color.White` (where it represents text), inline `fontSize`/`fontWeight`/`lineHeight`, and ad-hoc spacing with the tokens / typography styles / `Spacing.*` constants from Phase A. This is non-negotiable. After Phase C, **`grep -n "Color(0xFF" apps/androidApp/src/main/kotlin/com/vecturai/android/ui/` should return at most a handful of matches**, all in places where a literal is genuinely correct (e.g., a category accent color that is data-driven).

### C1. Home (`AndroidNavigationApp.HomeScreen`)
**File:** `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/AndroidNavigationApp.kt:116-295`

Changes:
- Replace `LocationOn`-in-a-blue-square logo with a **brand mark**: a custom `ImageVector` of an arrow folding into a node (build the path inline; ~30 lines of `path { … }`). Wrap in a gradient-stroked rounded square that has a subtle pulse (1.0 → 1.04 scale, 2 s loop, gated by reduce-motion).
- "VecturAI" → `GradientText` using brand brush.
- Subtitle → `MaterialTheme.typography.titleMedium`, color `TextMuted`.
- Replace the three static feature pills with a **rotating active-pill row**: at any moment one of "Live AR / Smart Routes / Indoor Maps" pulses cyan; cycle every 2 s. (Simple `LaunchedEffect` driving an index state.)
- Primary CTA → `VecturaiPrimaryButton(text = "Scan Entrance Code", leadingIcon = Icons.Default.QrCodeScanner, ...)`.
- Add a small "**Demo building: Building A**" status row below the hint.
- Settings gear → animate 30° rotation on tap before opening sheet (`animateFloatAsState`).
- Background → `AuroraBackground()`.

### C2. Destination Select (`DestinationSelectScreen`)
**File:** same file, lines `440-617`

Changes:
- **Sticky collapsing header**: as user scrolls, search field shrinks into a pill embedded in the top bar. Use `LazyColumn` + a remembered `firstVisibleItemScrollOffset` to drive a `Float` collapse fraction.
- **Skeleton state**: when `availableRooms.isEmpty()` and the package is still loading, show 6 shimmering placeholder rows (use `Modifier.background` with an animated brush).
- Filter chips → `VecturaiFilterChip`. Add a **sliding indicator underline** (a 3-dp tall gradient bar) that animates between selected chips.
- "Recently visited" → horizontal pager carousel (use `HorizontalPager` from Compose Foundation if already a dependency, else simple `LazyRow` with snap fling). Cards 72.dp tall, two visible at once.
- `DestinationRow`:
  - Add a 4-dp wide left-edge color bar in the row's category accent color (gives the list visual rhythm).
  - Walk-time pill uses `AnimatedNumber` for the minutes value.
  - Apply `Modifier.vecturaiTap()` (replaces raw `clickable`).
- Empty state: redraw using the brand mark in muted form, not a generic `Search` icon.
- Background → `AuroraBackground()`.

### C3. Route Preview (`RoutePreviewScreen`)
**File:** same file, lines `924-1276`

Changes:
- The summary card is the hero. Make the time number a **`GradientText` at `NumericDisplay` size (64 sp)** with `AnimatedNumber` count-up on first appear.
- Replace static "Steps" cards with a **vertical timeline**: continuous vertical line connects the 3 steps; dots animate in sequentially with 80 ms stagger. Use `Canvas` + `LaunchedEffect`.
- Add a **mini route preview strip**: 60-dp tall `Canvas` rendering the route polyline as an abstract diagram (origin dot → curved bezier → destination dot, with a dashed marker animating along the path). Pull coordinates from `session.routePackage`.
- "Floor G" badge → `StatPill`.
- Primary CTA → `VecturaiPrimaryButton(text = "Start AR Navigation", leadingIcon = Icons.Default.Place, ...)`.
- Background → `AuroraBackground()`.

### C4. QR Scan (`QRScanScreen`)
**File:** `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt`

Changes:
- **Show the live camera underlay.** The screen currently sits on top of an opaque dot background; the GLSurfaceView rendering live ARCore camera is behind it but covered. Make the screen background `Color.Transparent` so the camera shows through. Add a soft vignette gradient overlay so the reticle reads on bright scenes.
- Replace the static `QrCodeScanner` icon with an **animated scan reticle**:
  - 4 corner brackets that breathe (`scale 1.0 ↔ 1.04`, 1.6 s, `EaseInOutSine`).
  - A horizontal scan line sweeping top↔bottom (2 s, easing `EaseInOutSine`) drawn as a thin gradient line via `Canvas`.
  - On detection: brackets snap to `AccentGreen`, success ripple emits from center (concentric expanding circles), success haptic, then crossfade to the entrance-confirmed sheet.
- The status panel becomes a **glassmorphism card** when API ≥ 31 (`Modifier.graphicsLayer { renderEffect = BlurEffect(20f, 20f) }`); on older APIs, fall back to `SurfaceCard` + 92% opacity.
- Title: keep "Scanning..." but tighten the layout. Primary back button → `IconChip`.
- Error state: replace inline button with `VecturaiPrimaryButton(text = "Try Again")` + error haptic on first appearance.

### C5. Entrance Confirmed (`EntranceConfirmedSheet`)
**File:** `AndroidNavigationApp.kt:1316-1495`

Changes:
- Replace the static green check with an **animated check stroke**: draw a `Path` programmatically over 350 ms, ease-out. Wrap in a concentric ring that pulses outward once on appear.
- Add a **3-stop mini progress strip** at the top: "Entrance ✓ — Destination — Navigate". Recur this strip on the next two screens (Destination Select, Route Preview) for flow continuity.
- "Choose Destination" CTA → `VecturaiPrimaryButton`.
- "Confirmed" badge → `StatPill` variant.
- Wrap the entire sheet in `AnimatedContent` so phase transitions slide-up + fade properly (currently `AnimatedVisibility(visible = true)` is a no-op).

### C6. AR Active overlay (`ActiveNavigationOverlay`, `InstructionBanner`, `BottomHud`)
**File:** `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt:282-440`

Changes:
- **Top instruction banner:**
  - Glass blur surface (API ≥ 31) or 92% `SurfaceCard` fallback.
  - Turn glyph: 56-dp custom-stroked icon (not the 28-dp Material default).
  - Distance shown via `GradientText` + tabular figures.
  - Banner expands to ~88 dp height with stronger color when `nextActionDistance < 5 m` (urgency cue).
- **Compass strip:** add a thin (24-dp tall) horizontal compass at the top showing relative bearing to next waypoint. Tick marks slide as user yaw changes. Pull yaw from existing camera pose; **do not add new ARCore APIs**.
- **Progress arc:** wrap a circular progress arc around the bottom HUD ETA cluster. Value = `1 - remainingDistance / totalDistance`. Update at most every 200 ms to avoid jitter.
- **Tracking confidence chip:** animate color smoothly (`AccentGreen` steady → `AccentAmber` pulsing → `AccentRed`) instead of binary on/off. Fire `AndroidHapticManager.warning()` on first degradation per session.
- **End-route button:** change from tap to **swipe-to-confirm** ("← End route") to prevent accidental taps mid-walk. Use `Modifier.draggable` with a confirm threshold of 60% of width. Reset on release if not crossed.

### C7. Alignment overlay (`AlignmentOverlay`, pre-AR)
**File:** same file, lines `190-279`

Changes:
- Replace `CircularProgressIndicator` with a custom **scanning radar sweep**: a rotating gradient arc (`Canvas` + infinite rotation, 2 s/turn).
- Add small live counters: "Frames analyzed: N · Markers detected: M". Pull these from `ArMarkerDetector` candidate counts (already tracked).
- On timeout: keep categorized message + `VecturaiPrimaryButton("Retry") / VecturaiSecondaryButton("Cancel")`.
- Add an inline **mini illustration** (3 frames cycling: too-far / just-right / too-close) — use Canvas-drawn primitives, not bitmaps.

### C8. Arrival overlay (`ArrivalOverlay`)
**File:** same file, lines `442-637`

Changes:
- Reuse the **animated path-draw check** from C5 (consistency).
- Add a **subtle confetti burst** on appear: ~30 particles, cyan + amber, 1.5 s, `Canvas` + state list. Skip when reduce-motion is on.
- Add a "**Navigate somewhere else**" `VecturaiSecondaryButton` below "Done" that calls `flowModel.goBackToDestinationSelect()` instead of forcing Home.
- Stat cards: use `AnimatedNumber` for the count-ups.

### C9. Errors (`ConfigErrorOverlay`, `SessionErrorOverlay`, `PackageErrorScreen`)
**Files:** `ArNavigationScreen.kt:758-876`, `AndroidNavigationApp.kt:368-437`

Changes:
- Use `VecturaiCard` as the container.
- Primary CTA → `VecturaiPrimaryButton`. Secondary → `VecturaiSecondaryButton`.
- For `SessionErrorOverlay` when `isArCoreInstall = true`, also surface a deep link to the Play Store ARCore listing (`market://details?id=com.google.ar.core`). Currently the button labels itself "Install / Update ARCore" but the click handler is just `onRetry`.

### C10. Acceptance for Phase C
- Each screen renders identically in light + dark + RTL + `fontScale = 1.5` (use Compose preview annotations).
- No `Color(0xFFxxx)` literals remain in `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/*.kt` except where representing data-driven category colors.
- All primary CTAs use `VecturaiPrimaryButton`.
- `./gradlew :apps:androidApp:assembleDebug` passes.
- Manual run on a real ARCore device: full visitor flow (Home → QR → Entrance → Destination → Route → AR → Arrival) works end-to-end.

---

## 7. Phase D — Motion & Micro-interactions

**Time budget:** 1 day. **Risk:** low.

### D1. Screen transitions
Wrap the visitor-flow content in `AnimatedContent` keyed on the flow state. Use:
```kotlin
slideInVertically { it / 12 } + fadeIn() togetherWith slideOutVertically { -it / 12 } + fadeOut()
```
Duration 220 ms, easing `FastOutSlowInEasing`.

### D2. Tap pattern
Already centralized in `VecturaiPrimaryButton` and `Modifier.vecturaiTap()`. Audit and apply to every non-button clickable surface.

### D3. Number animations
Audit every screen for distance / ETA / counter values. Replace `Text("$n")` with `AnimatedNumber(n)` where the value can change while visible.

### D4. Reduce-motion respect
Add a utility:
```kotlin
@Composable
fun rememberReduceMotion(): Boolean { /* read TRANSITION_ANIMATION_SCALE */ }
```
Pipe through `AuroraBackground.intensity`, the brand-mark pulse, the confetti, and the radar sweep.

### D5. Acceptance
- Visual diff between flow phases is smooth — no abrupt swaps.
- With reduce-motion enabled in dev settings, decorative motion is suppressed; functional motion (success check, progress arc) still plays.

---

## 8. Phase E — Haptics

**Time budget:** 2 hours. **Risk:** low.

Wire `AndroidHapticManager` to:

| Site | File:Line | Tier |
|---|---|---|
| Primary button tap | `VecturaiPrimaryButton` (B1) | Light tick (built-in) |
| Filter chip selection | `VecturaiFilterChip` (B6) | Selection |
| Destination row tap | `DestinationRow` in `AndroidNavigationApp.kt` | Light tick |
| QR detected | `ArCameraFlowViewModel.confirmEntrance` call site | Success |
| Alignment locked | already in `AndroidArNavigationViewModel`; verify it fires | Success |
| Imminent turn (< 2 m) | `AndroidArNavigationViewModel` next-action change | Warning |
| Arrival | `ArrivalOverlay` LaunchedEffect | Success notification |
| QR mismatch error | `QRScanScreen` error state first-appear | Error |
| Tracking degraded | `InstructionBanner` low-confidence transition | Warning |

Add a global `HapticsEnabled` boolean (default true; flip off in screenshot/CI builds).

---

## 9. Phase F — Accessibility & QA

**Time budget:** 1 day. **Risk:** low.

### F1. Touch targets
Audit every `Modifier.size(...)` on a clickable element. Min interactive size = 48 dp. Existing violations: filter chips (40 dp → 48 dp), back chip in Route Preview (38 dp → 48 dp).

### F2. Roles
Add `Role.Button` to every `Modifier.clickable` on a non-Button. Most already have it; audit the remaining few.

### F3. Content descriptions
Every status-conveying icon needs a real description. Examples:
- Green dot at `AndroidNavigationApp.kt:519` ("origin status indicator: connected").
- Tracking icon already has `contentDescription = uiState.trackingStatusLabel` — keep.

### F4. Dynamic type
Replace inline `sp` with typography roles where possible. Numeric values keep explicit sizes (`NumericLarge`, `NumericDisplay`) but use `MaterialTheme.typography` for body / titles.

### F5. Contrast audit
Run a contrast check on every text-on-surface combo. The current `Color(0xFF6F7B8E)` on `Color(0xFF151F31)` is 3.4:1; replace with `TextMuted = 0xFF8E99AE` (4.7:1, AA pass).

### F6. Snapshot tests
Add Paparazzi (or Roborazzi if Paparazzi is unavailable on KMP):
```
apps/androidApp/src/test/kotlin/com/vecturai/android/ui/PaparazziSnapshotTests.kt
```
Snapshot each top-level screen × {idle, loading, error} × {LTR, RTL} × {fontScale 1.0, 1.5}.

### F7. Performance
- Run on a Pixel 4a-class device under `AuroraBackground`. Verify ≥ 55 fps in destination list and route preview.
- If frame drops occur, lower `AuroraBackground.intensity` automatically when battery saver is on.

### F8. Manual demo run
Re-run `./scripts/check-demo-readiness.sh` (the iOS-focused one) and verify nothing in the Android app's reviewed-package consumption broke.

---

## 10. Implementation rules (non-negotiable)

1. **Follow `CLAUDE.md`.** Especially: no premature abstraction, no comments explaining WHAT, no docstring-style `/* */` blocks, no backwards-compat shims.
2. **One concept per change.** Migrating colors is one PR/commit; rebuilding Home is another. Do not co-mingle a token migration with a screen redesign in the same diff.
3. **No new third-party dependencies** beyond Inter font and what's already in the catalog.
4. **Keep the ARCore session model intact.** Compose layers float above `GLSurfaceView`. If you find yourself wanting to change `UnifiedArSession` lifecycle, stop and re-read [`codebase-docs/features/ar_navigation_android.md`](codebase-docs/features/ar_navigation_android.md).
5. **No iOS changes.**
6. **Update the codebase-docs context pack** when files move or feature boundaries change. At minimum, edit:
   - `codebase-docs/features/ar_navigation_android.md`
   - `codebase-docs/features/design_system.md`
   - the dossiers under `codebase-docs/files/` for every file you significantly change.
7. **Update `CLAUDE.md`** with a new `## Phase 12 — Android UI Polish` section once the work lands, mirroring the Phase 11 entry style.
8. **Verify with the user before merging Phase C.** Visual changes are subjective; show before/after screenshots of Home + Destination Select before merging.

---

## 11. Verification checklist (run before declaring done)

```
# Build
./gradlew :shared:designsystem:build
./gradlew :apps:androidApp:assembleDebug

# Tests
./gradlew :shared:designsystem:test
./gradlew :apps:androidApp:test

# Static checks
grep -rn "Color(0xFF" apps/androidApp/src/main/kotlin/com/vecturai/android/ui/   # should be near-empty
grep -rn "fontSize = " apps/androidApp/src/main/kotlin/com/vecturai/android/ui/  # should be near-empty
grep -rn "fontWeight = FontWeight" apps/androidApp/src/main/kotlin/com/vecturai/android/ui/  # should be near-empty

# Smoke flow on device:
# - Home: aurora animates, brand mark pulses, gradient wordmark, gear rotates
# - Tap CTA: ripple + scale + light haptic
# - QR scan: live camera underlay visible, reticle breathes, scan line sweeps
# - Entrance confirmed: animated check, mini progress strip
# - Destination: filter chips have gradient selection + sliding indicator, rows count-up
# - Route preview: gradient ETA, animated polyline, vertical timeline
# - AR active: glass banner, 56-dp turn glyph, compass strip, progress arc, swipe-to-end
# - Arrival: animated check, confetti, "Navigate somewhere else" works
# - Reduce-motion: decorative motion suppressed, functional motion still plays
# - Battery saver: aurora intensity drops automatically
```

---

## 12. Suggested commit / PR shape

One PR per phase. Suggested titles:

1. `feat(android): design tokens, theme, typography (Phase A)`
2. `feat(android): shared component library (Phase B)`
3. `feat(android): redesign Home + brand mark (Phase C1)`
4. `feat(android): redesign Destination Select + skeleton + carousel (Phase C2)`
5. `feat(android): redesign Route Preview + animated timeline (Phase C3)`
6. `feat(android): live camera underlay + animated QR reticle (Phase C4)`
7. `feat(android): polish entrance / errors / animated check (Phase C5+C9)`
8. `feat(android): AR overlay glass banner + compass + progress arc (Phase C6)`
9. `feat(android): polish alignment + arrival overlays (Phase C7+C8)`
10. `feat(android): motion + reduce-motion (Phase D)`
11. `feat(android): wire haptics across UI (Phase E)`
12. `chore(android): a11y audit + Paparazzi snapshots (Phase F)`
13. `docs: ADR-034 Android visual polish parity, update CLAUDE.md`

Each commit message should mention the affected phase. Co-author lines per `CLAUDE.md`.

---

## 13. If you have to cut scope

If you have only **2 days**, ship: **A1–A4 + B1 + B4 + B8 + C1 + C4 + C5 + E**. That gives you tokens, the primary button, the card preset, the aurora background, redesigned Home + Destination Select + Route Preview, plus haptics. Every other screen will inherit the polish via the design tokens even without redesign. The remaining phases can land iteratively.

---

## 14. Open questions for the human (ask before starting)

1. Is **Inter** an acceptable brand font, or is there a preferred type face (e.g., Plus Jakarta, Geist, Manrope)?
2. The brand mark (arrow folding into a node) — should it be designed in Figma first, or can the agent author a reasonable vector inline and iterate?
3. Any existing brand color, gradient, or logo asset in design files that should override §3.1 / §3.2?
4. Acceptable to flip the visitor flow to **dark mode by default**, with no light variant for now? (iOS effectively is.)
5. Battery-saver fallback — disable aurora animation entirely, or just lower intensity?

If no answers are forthcoming within a reasonable wait, default: **Inter, dark-only, agent-authored brand mark, lower aurora intensity on battery saver**, and proceed.

---

*End of handoff. Start with `CLAUDE.md` § 0–1 of this doc, then Phase A.*
