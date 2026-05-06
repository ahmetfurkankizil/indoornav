# Agent Context: Vectura AI Pages Branch

This file is for AI agents. It is a compact internal model of the repository and deployed site behavior. Use it before answering detailed questions or making changes.

## Repository Identity

- Repository path: `C:\Users\emirh\Desktop\pagesGit\vecturai`
- Git remote: `https://github.com/ahmetfurkankizil/vecturai.git`
- Current branch at time of inspection: `pages`
- Current HEAD at time of inspection: `d434569 Restore final-CTA and footer legibility over bright arrival frames`
- Other local branch: `main` at `cafa8c8`, containing only `LICENSE` and a minimal `README.md`.
- This branch is a GitHub Pages deployment branch. It contains static export output, not the editable Next.js source project.
- The site is served at: `https://ahmetfurkankizil.github.io/vecturai/`
- The configured public/base path in generated assets is `/vecturai`.

## Highest-Priority Rule

Do not treat this as the source app. `README.md` explicitly says the root files (`index.html`, `_next/`, `404.html`, assets, etc.) are produced by `next build` with `output: "export"` and copied from the source project's `out/` folder. Manual edits to generated files are fragile and likely to be overwritten by the next export.

If the task is a durable product/code change, the safest path is to find/edit the source Next.js repo, rebuild with:

- `output: "export"`
- `basePath: "/vecturai"`
- `trailingSlash: true`
- `images: { unoptimized: true, ... }`

Then replace this branch with the generated `out/` contents while preserving `LICENSE`, `.gitignore`, `.nojekyll`, and `README.md`.

The source project is not present in this checkout. `.gitignore` mentions `new-website/` as a convenience source-project folder that must not be committed to this branch, but that directory is not present.

## Repo Structure

Important root files:

- `README.md`: deployment-branch instructions and warning not to edit generated files by hand.
- `LICENSE`: MIT license, copyright 2025 Ahmet Furkan KIZIL.
- `.nojekyll`: required so GitHub Pages serves `_next/` without Jekyll filtering.
- `.gitignore`: ignores OS/editor files and `new-website/`.
- `index.html`: exported homepage shell and prerendered HTML.
- `404.html`, `404/index.html`, `_not-found/index.html`: exported Not Found pages.
- `index.txt`, `__next.*.txt`, `_not-found/__next.*.txt`: Next App Router/RSC static payloads for the page and not-found route.
- `_next/static/chunks/*.js`: Turbopack-generated JavaScript chunks.
- `_next/static/chunks/0pau.w3.hnvot.css`: compiled Tailwind/global CSS.
- `_next/static/HN05sFoS2BGZtyYFxtof-/*Manifest.js`: generated build/middleware/SSG manifests.
- `_next/static/media/*`: hashed font/icon/social-image assets.
- `videos/vectura-corridor-background.mp4`: corridor background video, ~9.66 MB, MP4 with `moov` atom near start.
- `videos/first_screen_phone_ss.png`: phone mockup screenshot, 1179x2556, ~976 KB.
- `vectura-logo.png`: 1254x1254 logo, ~381 KB.
- `icon.png`, `apple-icon.png`, `favicon.ico`: app icons.
- `opengraph-image.png`, `twitter-image.png`: 1200x630 social images.
- `rapor.pdf`: linked as "whitepaper", but content is a Turkish CS437/537 quiz-style software engineering study PDF, not a Vectura AI product whitepaper.
- `file.svg`, `globe.svg`, `next.svg`, `vercel.svg`, `window.svg`: stock Next.js starter SVGs, apparently unused by the exported page.

File inventory at inspection:

- 4 `.html`
- 14 `.js`
- 1 `.css`
- 13 `.txt`
- 10 `.png`
- 2 `.ico`
- 9 `.woff2`
- 5 `.svg`
- 1 `.mp4`
- 1 `.pdf`
- 1 `LICENSE`, 1 `README.md`, `.gitignore`, `.nojekyll`

No package files are present in this branch:

- No `package.json`
- No lockfile
- No `next.config.*`
- No TypeScript/React source files
- No tests
- No CI/deployment config files
- No migrations, API route source, models, schemas, or server services

## Generated App Stack

Inferred from compiled output:

- Framework: Next.js App Router static export.
- Bundler/runtime: Turbopack chunks, `globalThis.TURBOPACK`.
- React/React DOM: canary `19.3.0-canary-3f0b9e61-20260317` appears in runtime chunk.
- Styling: Tailwind-generated CSS plus custom global utilities.
- Client animation/libs in compiled chunks:
  - `gsap`
  - `ScrollTrigger`
  - `Lenis` for smooth scrolling
  - canvas-confetti implementation embedded in the arrival chunk
  - Next `Image` and `Link` client code

Main exported route model:

- Root layout wraps body in `SmoothScrollProvider`.
- Homepage renders:
  - `VideoBackdrop`
  - `JourneyOverlays`
  - `Navbar`
  - `ProgressRail`
  - content sections: `HeroSection`, `ProblemSection`, `SolutionSection`, `HowItWorksSection`, `BenefitsSection`, `ArrivalSection`
- Not-found route renders default Next 404 UI.

## Static Route/Manifest Model

`_buildManifest.js` contains only sorted pages:

- `/_app`
- `/_error`

`_clientMiddlewareManifest.js`:

- `self.__MIDDLEWARE_MATCHERS = []`

`_ssgManifest.js`:

- empty set

Implication: no middleware, no dynamic server routes, no API endpoints in this deployment branch.

## Metadata

Homepage metadata in `index.html`/RSC payload:

- Title: `Vectura AI - Indoor Navigation, Reimagined` (actual generated HTML uses Unicode punctuation in places)
- Description: hardware-free indoor navigation platform for AR wayfinding in hospitals, airports, campuses, and large venues.
- Application name: `Vectura AI`
- Theme color: `#04060d`
- OG/Twitter images point to `https://vectura.ai/vecturai/opengraph-image.png?...` and `https://vectura.ai/vecturai/twitter-image.png?...`
- Icons:
  - `/vecturai/icon.png?...`, 256x256
  - `/vecturai/apple-icon.png?...`, 180x180

Potential risk: deployed GitHub Pages URL is under `ahmetfurkankizil.github.io`, but metadata uses `https://vectura.ai/vecturai/...`. Verify whether `vectura.ai` is intentionally configured before changing metadata.

## Core Generated Modules

The important app logic lives in `_next/static/chunks/0u596_stgqx-d.js` and related chunks. Module numbers are Turbopack module IDs from the compiled output.

### Module `13003`: Content Constants

Purpose: central content/data model for the landing page.

Exports:

- `ARRIVAL`
- `BENEFITS`
- `FINAL_CTA`
- `HERO`
- `HOW_IT_WORKS`
- `NAV_ITEMS`
- `PROBLEM`
- `SOLUTION`
- `TEAM`
- `WAYPOINTS`

Important content:

- `HERO`
  - eyebrow: `Vectura AI - Indoor Navigation Platform`
  - title lines: `Indoor navigation,` / `reimagined.`
  - subtitle: hardware-free AR wayfinding, no beacons/retrofits.
  - primary CTA: `Explore the journey` -> `#problem`
  - secondary CTA: `See how it works` -> `#how-it-works`
  - micro-stats: hardware `0`, setup `Self-service`, privacy `On-device`
- `PROBLEM`
  - problem: large buildings confuse people
  - bullets: hardware-heavy, fragile UX, privacy gaps, closed admin
- `SOLUTION`
  - title: clear path drawn on the world
  - pillars: hardware-free, AR-native, privacy-first
- `HOW_IT_WORKS`
  - steps: map venue, define destinations, visitors scan, AR guides
- `BENEFITS`
  - items: zero hardware cost, days not quarters, accessible by default, admin autonomy, privacy by design, built to scale
- `ARRIVAL`
  - destination reached/team intro
- `TEAM`
  - Bugra Cayir: Frontend & Product
  - Ahmet Furkan Kizil: Computer Vision
  - Ahmet Zor: Backend & Mapping
  - Emirhan Kilic: AR & Interaction
  - Muhammed Umit Tavus: Platform & Infrastructure
  - Each member has initials, GitHub URL, LinkedIn URL, and blurb.
- `FINAL_CTA`
  - title: `Start your route with Vectura AI.`
  - primary: `Request a pilot`
  - secondary: `Read the whitepaper` -> `/rapor.pdf`
- `NAV_ITEMS`
  - `problem`, `solution`, `how-it-works`, `benefits`, `team`
- `WAYPOINTS`
  - `start`, `problem`, `solution`, `how-it-works`, `benefits`, `destination`

Risks:

- `WAYPOINTS` uses `destination`, but route state uses `arrival`; this is OK for the progress rail labels because progress is percentage-based, but it is a naming mismatch.
- Content constants are compiled. Edit source constants in the source project, not the minified bundle, when possible.

### Module `93209`: Asset Helper

Purpose: prefixes root-relative local paths with `/vecturai`.

Behavior:

- Returns unchanged for empty values, external URLs (`http`, `https`, protocol-relative), `mailto:`, `tel:`, `data:`, hashes, and non-root-relative paths.
- For paths starting with `/`, returns `/vecturai${path}`.

Hidden coupling:

- All local asset references assume the deployment base path `/vecturai`.
- Changing base path requires rebuilding from source and updating all RSC/static references.

### Module `45060`: `cn`

Purpose: local className joiner, equivalent to a small `clsx`-style helper.

Behavior:

- Accepts strings, numbers, arrays, objects.
- Includes object keys whose values are truthy.

### Module `24853` in `00fi27k48a0~v.js`: `SmoothScrollProvider`

Purpose: global smooth scrolling integration.

Dependencies:

- Lenis
- GSAP ticker
- ScrollTrigger

Behavior:

- If `prefers-reduced-motion: reduce`, skips Lenis and refreshes ScrollTrigger.
- Otherwise creates a Lenis instance:
  - duration `1.15`
  - easing `Math.min(1, 1.001 - 2 ** (-10 * t))`
  - `smoothWheel: true`
  - `wheelMultiplier: 1`
  - `touchMultiplier: 1.2`
- On Lenis `scroll`, calls `ScrollTrigger.update`.
- Registers `lenis.raf(1000 * gsapTickerTime)` with GSAP ticker.
- Disables GSAP lag smoothing with `gsap.ticker.lagSmoothing(0)`.
- Refreshes ScrollTrigger on resize.
- Cleanup removes ticker callback, destroys Lenis, removes resize listener.

Risks:

- Multiple scroll-driven components depend on ScrollTrigger refresh timing.
- Reduced-motion mode should remain a first-class path when changing animation behavior.

### Module `45678`: `Navbar`

Purpose: fixed top navigation.

Callers:

- Homepage RSC payload renders it inside `<main>`.

Dependencies:

- Next `Link`
- `NAV_ITEMS`
- `cn`
- `asset`

Behavior:

- Tracks `window.scrollY > 24` to toggle a blurred/dark header background and bottom border.
- Logo uses `/vectura-logo.png` via `asset`.
- Brand text: `Vectura.AI`.
- Desktop nav links map over `NAV_ITEMS`.
- Desktop CTA link goes to `#cta` with label `Start route`.
- Mobile nav is absent; on small screens only the brand remains.

Risks:

- No hamburger/mobile nav.
- Header CTA is a hash link, not the pilot modal trigger.

### Module `47120`: `VideoBackdrop`

Purpose: fixed full-window corridor video synchronized to scroll.

Dependencies:

- GSAP + ScrollTrigger
- `asset`

Behavior:

- Loads `/videos/vectura-corridor-background.mp4`.
- Fixed background at `z-0`, object-cover.
- Video is muted, playsInline, preloaded, picture-in-picture disabled.
- Pauses video and manually scrubs `currentTime` based on ScrollTrigger progress.
- Uses Safari-specific tolerance:
  - Safari: `0.033`
  - others: `0.05`
- Uses `requestAnimationFrame` scheduler to avoid setting `currentTime` too aggressively while seeking.
- On reduced motion, pins `currentTime = 0`.
- Adds one-time `touchstart` and `click` listeners to briefly `play()` then pause, likely to unlock media on mobile.
- On visibility change to visible, reschedules current frame.
- Adds radial tint overlays controlled by CSS vars:
  - `--tint-cyan`
  - `--tint-violet`
  - `--tint-warm`
- Adds top/bottom dark gradients, vignette, and base dark overlay.

Risks:

- Scroll-scrubbed video can be browser-sensitive, especially Safari/iOS and low-power devices.
- It depends on `document.body.scrollHeight - window.innerHeight`.
- If page height or section spacing changes, perceived video frame alignment changes.
- Manual edits to the video file should preserve fast-start MP4 behavior (`moov` atom near beginning).

### Module `49866`: `JourneyOverlays`

Purpose: non-interactive HUD overlays that react to section scroll position and total page progress.

Internal data:

- Route states: `start`, `problem`, `solution`, `mechanism`, `benefits`, `arrival`
- Status labels:
  - `00 - START`
  - `01 - PROBLEM`
  - `02 - SOLUTION`
  - `03 - MECHANISM`
  - `04 - BENEFITS`
  - `05 - DESTINATION`
- Location labels:
  - `Lobby - Floor 1`
  - `Hall B - Diagnostics`
  - `Gate 12 - Departures`
  - `Wing C - Floor 3`
  - `Atrium - Sky bridge`
  - `Vectura AI - You are here`
- Tint map drives body CSS vars.
- Venue cards for states:
  - problem -> Workplace / Office
  - solution -> Care zone / Hospital
  - mechanism -> Retail floor / Shopping mall
  - benefits -> Terminal / Airport

Behavior:

- On mount, initializes body CSS variables.
- Finds elements by `[data-route-state="..."]` and creates ScrollTriggers:
  - start: `top 65%`
  - end: `bottom 35%`
  - on enter/back updates HUD state.
- Uses a separate ScrollTrigger on `document.body` to update:
  - top progress bar `scaleX(progress)`
  - route percent text
  - remaining distance from `240 m` to `0 m`
  - ETA from `6:00` to `0:00`
  - turn-left prompt opacity around progress `0.43` to `0.60`
- All overlay content is `aria-hidden` and `pointer-events-none`.

Hidden coupling:

- Section wrappers must have matching `data-route-state` values:
  - Hero: `start`
  - Problem: `problem`
  - Solution: `solution`
  - HowItWorks: `mechanism`
  - Benefits: `benefits`
  - Arrival: `arrival`
- Adding/removing sections requires updating the state arrays and waypoint logic in source.

### Module `78868`: `ProgressRail`

Purpose: left-side route progress rail on large screens.

Dependencies:

- `WAYPOINTS`
- `cn`

Behavior:

- Tracks scroll progress with native `scroll` and `resize` listeners, throttled by `requestAnimationFrame`.
- Computes total scrollable height as `document.documentElement.scrollHeight - window.innerHeight`.
- Current waypoint index:
  - `Math.min(WAYPOINTS.length - 1, Math.floor(progress * WAYPOINTS.length + 0.05))`
- Renders only `lg:block`.
- Shows percent at bottom and waypoint dots/labels.

Risk:

- Progress rail state is based on full page progress, not actual section trigger points, so it can diverge from `JourneyOverlays` for unusual section heights.

### Module `43024`: `CTAButton`

Purpose: shared pill button/link component.

Props:

- `href`
- `onClick`
- `children`
- `variant`: `primary` or `ghost`
- `className`
- `ariaLabel`
- `type`
- `newTab`

Behavior:

- If `onClick` exists, renders a `<button>`.
- Otherwise renders Next `Link` with `href` or `#`.
- `newTab` adds `target="_blank"` and `rel="noopener noreferrer"`.
- Primary variant uses cyan gradient and `cta-glow`.
- Ghost variant uses border and transparent hover background.
- Uses arrow icon; external/new-tab variant uses diagonal arrow icon.

### Module `66853`: `HeroSection`

Purpose: first viewport hero.

Dependencies:

- Next `Image`
- GSAP
- content constants
- `CTAButton`
- `asset`

Behavior:

- Section id `top`, `data-route-state="start"`, min-height `100svh`.
- Entrance animations for eyebrow, title lines, subtitle, CTAs, stats, phone mockup.
- Reduced motion skips animation.
- Phone mockup:
  - hidden below `md`
  - aspect `9/19`
  - screenshot `/videos/first_screen_phone_ss.png`
  - Next image with `fill`, `priority`, object-top
- Hero copy has text shadow and a backdrop/masked radial dark wash behind left column.
- Scroll-to-begin affordance appears only on taller viewports.

### Module `38770`: `StoryPanel`

Purpose: reusable section wrapper with glass styling and corner marks.

Props:

- `id`
- `eyebrow`
- `className`
- `contentClassName`
- `children`

Behavior:

- Renders `<section id=...>` with max width.
- Inner `glass-panel` receives `data-story-panel`.
- Optional eyebrow.
- Adds decorative SVG corner marks at top-left, top-right, bottom-left, bottom-right.

### Module `43089`: `ProblemSection`

Purpose: problem narrative and four pain cards.

Dependencies:

- GSAP + ScrollTrigger
- `StoryPanel`
- `PROBLEM`

Behavior:

- Wrapper `data-route-state="problem"`.
- Animates panel and bullets on scroll unless reduced motion.
- Cards use `glass-card`.

### Module `36732`: `SolutionSection`

Purpose: solution narrative, three pillars, and route-arrow SVG cluster.

Dependencies:

- GSAP + ScrollTrigger
- `StoryPanel`
- `SOLUTION`

Behavior:

- Wrapper `data-route-state="solution"`.
- Animates panel, pillar cards, and arrow cluster.
- SVG cluster includes gradient path, route arrows, start/destination markers, and `Clarity engaged` label.

### Module `41013`: `HowItWorksSection`

Purpose: four-step mechanism section.

Dependencies:

- GSAP + ScrollTrigger
- `StoryPanel`
- `HOW_IT_WORKS`

Behavior:

- Wrapper `data-route-state="mechanism"`.
- Animates panel, step cards, and connector line.
- Steps are an ordered list.

### Module `24788`: `BenefitsSection`

Purpose: benefit cards.

Dependencies:

- GSAP + ScrollTrigger
- `StoryPanel`
- `BENEFITS`

Behavior:

- Wrapper `data-route-state="benefits"`.
- Animates panel and benefit cards.
- Icons cycle among checkmark, circle/target, and hexagon based on index modulo 3.

Note:

- Static DOM extraction observed some benefit cards represented as anchors in the generated HTML despite source-like JSX showing `article`; treat generated HTML as source of truth for deployed markup and source repo as source of truth for future source edits.

### Module `88261`: `ArrivalSection`, Pilot Modal, Team, Confetti

Purpose: final destination section, team grid, final CTA, footer, and modal form.

Dependencies:

- Next `Image`
- GSAP + ScrollTrigger
- embedded canvas-confetti library
- content constants
- `CTAButton`
- `cn`

Arrival behavior:

- Section id `team`, `data-route-state="arrival"`.
- Animates beacon, title/subtitle, team cards, final CTA.
- Confetti fires once when `[data-arrival-card]` enters at `top 70%`, unless reduced motion.
- Confetti bursts from left/right bottom and center.

Team behavior:

- Team cards display GitHub avatars derived from `member.github`:
  - Regex extracts GitHub username from `github.com/...`
  - Avatar URL: `https://github.com/${username}.png?size=200`
  - Next Image uses `unoptimized: true`
- If GitHub URL cannot be parsed, initials remain visible.
- Each card links to LinkedIn and GitHub with inline SVG icons.

Final CTA behavior:

- `id="cta"` and `data-final-cta`.
- Primary button opens local modal form.
- Secondary opens `/rapor.pdf` in a new tab via `CTAButton`.
- Latest commit specifically changed CTA and footer styles for contrast over bright video frames.

Footer behavior:

- Shows current year via `new Date().getFullYear()`.
- Text: `Vectura AI - Graduation Project` and `Indoor navigation, reimagined.`

Pilot modal data:

- Destination email displayed: `bugra.cayir@ug.bilkent.edu.tr`
- Initial state:
  - `fullName`
  - `email`
  - `organization`
  - `venueType`
  - `venueSize`
  - `message`
- Venue types:
  - Hospital
  - Airport
  - University campus
  - Shopping mall
  - Office
  - Museum
  - Conference center
  - Other
- Venue sizes:
  - Under 5,000 m2
  - 5,000 - 25,000 m2
  - 25,000 - 100,000 m2
  - 100,000+ m2
- Email regex: `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`

Pilot modal behavior:

- Opens as `role="dialog"` with `aria-modal="true"`.
- Locks body scroll by setting `document.body.style.overflow = "hidden"`, restoring previous value on close.
- Escape closes modal.
- Backdrop button closes modal.
- On open, resets form state/errors/status and focuses first input on next animation frame.
- Submit validation:
  - `fullName` required: `Please enter your name.`
  - `email` required: `We need an email to reach you.`
  - invalid email: `That doesn't look like a valid email.`
  - `venueType` required: `Pick a venue type.`
  - `venueSize` required: `Pick a venue size.`
  - First invalid field receives focus.
- If valid:
  - status becomes `submitting`
  - after 900 ms status becomes `success`
  - after 1800 ms modal closes
- There is no `fetch`, no `mailto`, no API call, no Formspree/EmailJS. It simulates successful email delivery only.

Major business risk:

- UI says "Email sent successfully" and "Your pilot request has been delivered", but no data leaves the browser. This is a likely product bug if real lead capture is expected.

## CSS Model

Compiled CSS: `_next/static/chunks/0pau.w3.hnvot.css`.

Fonts:

- `Space Grotesk` for display/body via CSS variable `--font-display`.
- `JetBrains Mono` for mono labels via `--font-mono`.
- Multiple WOFF2 subsets are in `_next/static/media`.

Root variables:

- `--bg-base: #04060d`
- `--bg-deep: #02030a`
- `--bg-panel: #0a10208c`
- `--bg-panel-strong: #0a1020c7`
- `--fg-primary: #eaf2ff`
- `--fg-muted: #8a99b8`
- `--fg-faint: #4d5a78`
- `--accent-cyan: #5cf2ff`
- `--accent-cyan-soft: #38b9ff`
- `--accent-violet: #8a6bff`
- `--accent-warm: #ffb15c`
- `--stroke-soft: #78aaff1f`
- `--stroke-mid: #78aaff38`
- `--stroke-strong: #8cc8ff73`
- glow vars for cyan/violet

Base styles:

- `html, body`: dark background, Space Grotesk, antialiased, optimized legibility.
- `body`: margin 0, overflow-x hidden.
- `html`: `scroll-behavior: auto`.
- `::selection`: cyan translucent.

Custom utility/classes:

- `.glass-panel`: 24px radius, border, blurred/saturated backdrop, dark gradient, inset highlight, large shadow, radial cyan wash via `:before`.
- `.glass-card`: 18px radius, softer border, blurred backdrop, hover lift/border/glow.
- `.eyebrow`: mono uppercase label with leading line.
- `.font-mono-tight`: uses `--font-mono`, letter spacing `.04em`.
- `.text-gradient-cyan`: white/cyan/violet gradient text.
- `.cta-glow`: inset highlight, cyan border glow, large cyan drop shadow.
- `.divider-line`: horizontal transparent-to-stroke gradient line.
- `.animate-pulse-soft`: pulse animation.

Global reduced-motion rule:

- `@media (prefers-reduced-motion: reduce)` forces very short transitions/animations and disables smooth scroll behavior.

Recent contrast fix:

- Latest commit changed final CTA wrapper to use `glass-panel`, made CTA body copy `fg-primary/85` with text shadow, and wrapped footer in a low-opacity dark pill for legibility over bright video frames.

## Data and Control Flow

### Initial Page Load

1. Browser requests `/vecturai/`.
2. GitHub Pages serves `index.html`.
3. HTML preloads fonts, logo, phone screenshot, CSS, scripts.
4. Static DOM already contains content for SEO/no-JS fallback.
5. Turbopack/Next runtime hydrates the App Router tree.
6. `SmoothScrollProvider` initializes Lenis and connects it to ScrollTrigger unless reduced motion is enabled.
7. `VideoBackdrop`, `JourneyOverlays`, `Navbar`, `ProgressRail`, and sections mount client effects.
8. ScrollTrigger instances bind to section wrappers and document body.

### Scroll-Driven Journey Flow

Inputs:

- User scroll
- Window resize
- Document visibility change
- Reduced-motion preference
- Video metadata loaded event

Flow:

1. Lenis smooths wheel/touch scroll and informs ScrollTrigger.
2. `VideoBackdrop` maps full-page scroll progress to video `currentTime`.
3. `JourneyOverlays` maps section visibility to HUD state and body tint variables.
4. `JourneyOverlays` maps full-page scroll progress to progress bar, distance, ETA, and turn-left prompt.
5. `ProgressRail` maps native scroll progress to a waypoint index and percent.
6. Section components trigger entrance animations as panels/cards enter viewport.
7. `ArrivalSection` fires confetti once when arrival card enters.

Outputs/side effects:

- DOM transforms/styles
- Body CSS variable updates
- Video `currentTime` mutation
- Body scroll lock in modal
- Event listeners on window/document
- Confetti canvas injected/removed by the embedded confetti lib

Error behavior:

- Most effects guard on missing refs.
- Video `currentTime` assignment is wrapped in try/catch.
- Media unlock `play()` promise failure is caught and ignored.

### Pilot Request Modal Flow

Inputs:

- Click primary final CTA button.
- Typed form fields.
- Submit button.
- Escape key/backdrop/close button.

Validation:

- Required: full name, valid email, venue type, venue size.
- Optional: organization, message.

Side effects:

- Scroll lock on body while open.
- Focus first invalid input.
- Simulated submit timeout.
- Success UI, then close timeout.

Outputs:

- Inline errors for invalid fields.
- "Email sent successfully" status for valid submission.

Critical caveat:

- No network request or persistence. This is pure front-end simulation.

### Whitepaper/PDF Flow

Input:

- Click `Read the whitepaper`.

Flow:

- Opens `/vecturai/rapor.pdf` in a new tab.

Actual PDF content:

- 7-page Turkish CS437/537 quiz-like practice exam and answer key about LOC/LLOC, Halstead, cyclomatic complexity, CK metrics, code smells, SOLID, GoF patterns.

Risk:

- The CTA label says product whitepaper, but the file is unrelated to Vectura AI. Treat this as suspicious unless user confirms it is intentional.

## External Systems

Runtime external requests likely include:

- GitHub avatar images:
  - `https://github.com/relixia.png?size=200`
  - `https://github.com/ahmetfurkankizil.png?size=200`
  - `https://github.com/AhmetZor.png?size=200`
  - `https://github.com/Emirhan-Kilic.png?size=200`
  - `https://github.com/tavus-umit.png?size=200`
- LinkedIn profile links.
- GitHub profile links.
- OG/Twitter metadata URLs on `https://vectura.ai/vecturai/...`.

No backend/API/database/auth/external lead-capture service is present.

## Edge Cases and Risks

### Generated Artifacts

- Manual edits to `index.html`, RSC `.txt`, chunk JS, or CSS can desynchronize generated references.
- Next static export uses hashed chunk/build IDs. Changing chunk names manually can break RSC payloads and preload references.
- Prefer source changes + rebuild.

### Base Path

- Assets and links are tightly coupled to `/vecturai`.
- Local testing must serve the root as if under `/vecturai`, or references will 404.
- All checked local `/vecturai/...` asset references existed at inspection.

### No Source/Test Harness

- There are no source files or tests in this branch.
- There is no `package.json`; `npm install`, `npm test`, `npm run build` cannot run here.
- Browser testing requires serving static files. Opening `index.html` directly may fail for absolute `/vecturai/...` asset paths.

### Browser/Animation

- Scroll-scrubbed video is sensitive to video metadata availability, duration, seek behavior, and browser media policies.
- Safari tolerance differs from other browsers.
- Reduced-motion path should be preserved.
- Multiple ScrollTriggers rely on body/page height; layout changes can alter all timing.
- Confetti uses canvas/worker/offscreen-canvas capabilities with fallbacks.

### Accessibility

- Main HUD overlays are `aria-hidden`, good for decorative UI.
- Modal has `role="dialog"` and `aria-modal`.
- Modal does not implement a full focus trap; Escape and backdrop close work, initial focus is set.
- Header lacks mobile navigation.
- Some decorative/interactive generated markup should be rechecked if source changes are made.

### Product/Business

- "Email sent successfully" is misleading because no email is sent.
- "Read the whitepaper" links to an unrelated CS quiz PDF.
- Privacy-first claims are marketing copy only in this static page; no real implementation exists in this repo.
- "Request a pilot" does not capture leads.

### Deployment

- `.nojekyll` must remain.
- `_next/` must remain available verbatim.
- GitHub Pages branch should keep root static files.
- Latest commit focused on CTA/footer contrast over a bright arrival video frame; avoid regressing this.

### Encoding

- The code/content includes non-ASCII names and punctuation in generated files and PDF:
  - Turkish characters in team names and LinkedIn URLs.
  - Unicode punctuation in marketing copy.
- Some PowerShell output may show mojibake depending on code page. Do not assume source text is wrong solely from terminal encoding artifacts.

## Testing and Verification Notes

Recommended local static server behavior:

- Serve repository root.
- Map requests beginning `/vecturai` to the repo root.
- Fall back unknown paths to `404.html`.
- Set reasonable MIME types for `.js`, `.css`, `.png`, `.svg`, `.ico`, `.mp4`, `.pdf`, `.woff2`.

Manual verification targets:

- `/vecturai/` loads without missing local assets.
- `/vecturai/rapor.pdf` opens.
- `/vecturai/_next/static/chunks/0pau.w3.hnvot.css` loads.
- `/vecturai/videos/vectura-corridor-background.mp4` loads and seeks.
- Navbar hash links scroll to sections.
- Progress rail and HUD update across scroll.
- Reduced-motion mode does not animate/scrub unexpectedly.
- Pilot modal validation:
  - empty submit shows name/email/type/size errors.
  - invalid email shows invalid email error.
  - valid fields show success, then modal closes.
- CTA/footer remain legible over arrival frames.
- 404 route displays default 404.

Tool notes from prior inspection:

- `rg` failed in this Windows environment with "Access denied"; use PowerShell recursion or other search tools if needed.
- Bundled Playwright package existed in the desktop runtime, but browser executable was not installed, so Playwright launch failed and suggested `npx playwright install`.
- Python `pypdf` was available through the Codex runtime and successfully extracted `rapor.pdf`.

## Safe Change Strategy

If user requests a content/design fix and source repo is unavailable:

1. Explain that this branch is generated static output and changes here are not durable across rebuilds.
2. If they still want a hotfix directly on pages branch, update all affected generated artifacts consistently:
   - `index.html`
   - `index.txt`
   - `__next.*.txt` payloads as needed
   - chunk JS/CSS only if unavoidable
   - mirrored `404`/`_not-found` payloads only when metadata/layout chunks changed
3. Avoid editing minified JS unless absolutely necessary.
4. Verify every `/vecturai/...` reference still resolves.
5. Prefer changing simple static HTML text only when hydration will not overwrite it. For React-hydrated content, HTML-only edits may be reverted after hydration; the compiled RSC/chunk source must also match.

If user requests a real feature:

- Do not invent APIs in this branch.
- Find the source Next.js project or ask for it.
- Implement in source, rebuild/export, then update pages branch.

If user requests lead capture:

- Need a real backend or form service. Current modal has no network layer.
- Likely source changes:
  - add submit endpoint/service integration
  - replace fake timeout with async call
  - add error state
  - preserve client-side validation
  - avoid exposing secrets in static frontend

If user requests replacing the PDF:

- Replace `rapor.pdf` with correct product whitepaper and ensure CTA copy/metadata are accurate.
- If filename changes, update `FINAL_CTA.secondary.href` in source and rebuild.

If user requests visual changes:

- Preserve base dark/cyan/violet/warm visual system.
- Preserve final CTA/footer legibility over bright video.
- Re-test mobile widths because hero phone is hidden under `md` and navbar has no mobile menu.

## Important File/Module Reference Map

- `README.md`: deployment instructions and source-of-truth warning.
- `index.html`: deployed homepage HTML.
- `404.html`, `404/index.html`, `_not-found/index.html`: deployed 404 HTML.
- `_next/static/chunks/0u596_stgqx-d.js`: app content/components/landing page/modal/confetti/asset helper.
- `_next/static/chunks/00fi27k48a0~v.js`: Lenis/SmoothScrollProvider and shared Next client runtime.
- `_next/static/chunks/0d3shmwh5_nmn.js`: Next App Router/layout runtime and `IconMark`.
- `_next/static/chunks/0dgq26a5_oy.a.js`, `10~x95jhs6ns3.js`, `1007mim.geoe4.js`, `16g2ek8bjolm~.js`, `16i90eio_ss7o.js`, `03~yq9q893hmn.js`, `turbopack-*.js`: Next/React runtime chunks.
- `_next/static/chunks/0pau.w3.hnvot.css`: compiled styling.
- `videos/vectura-corridor-background.mp4`: scroll-scrubbed video.
- `videos/first_screen_phone_ss.png`: hero phone image.
- `vectura-logo.png`: navbar logo.
- `rapor.pdf`: currently mislabeled "whitepaper" asset.

## Known Non-Code Facts from Inspection

- Worktree was clean before this `context.md` file was created.
- The `pages` branch is ahead/at `origin/pages` with no local modifications at inspection.
- `main` branch contains only license/readme and is not the app source.
- The latest commit author was `bugracayir <bugra.cayir@commencis.com>` and was co-authored by Cursor.
- The latest commit specifically addressed legibility over bright arrival frames; this is an important visual regression area.

