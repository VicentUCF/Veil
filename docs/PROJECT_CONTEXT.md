# Veil — Android launcher bootstrap context

## Product statement

> Qtile philosophy translated to touch, not copied to Android.

Veil is a native Android launcher inspired by Arch Linux and Qtile. It must feel like a quiet operating-system layer in which the wallpaper is the primary visual content and interface elements appear only when useful.

The v0.1 rule is:

> Wallpaper first. Five useful actions. One tap. Nothing else unless it earns its place.

## Decision hierarchy

When two solutions are possible, prioritize:

1. Reliability as an Android Home app.
2. One-tap usability.
3. Wallpaper visibility.
4. Visual silence.
5. Clear hierarchy.
6. Touch ergonomics.
7. Implementation simplicity.
8. Extensibility.
9. Decorative polish.

Do not overarchitect. The working priority is real behavior, then UX, design, architecture, and only later future features.

## Product and UX principles

- Wallpaper is content, not merely a background; keep roughly 70–90% of the screen visually free.
- Use a quiet interface with no attention-seeking decoration, permanent badges, or unnecessary motion.
- Reveal layers progressively: Home, Context, Drawer, Search.
- Model activities as contexts such as HOME, WORK, MEDIA, SOCIAL, and TOOLS, not traditional pages of icons.
- Keep 5–8 frequent actions on Home and make them available with one tap.
- Search is a future power tool, not the primary interaction model.
- Use an editorial, deliberately asymmetric layout with strong alignment and negative space.
- Typography has more visual importance than icons; do not imitate a terminal or use fake shell decoration.
- Color communicates state only. Centralize off-white, soft gray, muted blue, error, and success tokens.
- Motion must be short, discreet, and functional. Do not add decorative animation in v0.1.
- Configuration should remain declarative through ordinary Kotlin structures. Do not create a DSL in v0.1.
- Touch is primary; keyboard interaction is optional.
- Future dynamic modules are called Panels. Only one may have visual priority at a time, but Panels are out of scope for v0.1.

Veil is not a literal Qtile clone, a terminal-themed UI, a Niagara clone, a conventional Material launcher, a widget dashboard, or a desktop layout transplanted to mobile.

## v0.1 scope

### Launcher behavior

- Install as an Android application.
- Register as a real Home candidate.
- Allow Android to select it as the default launcher.
- Return to Veil through the Home action.
- Discover launchable applications installed on the device.
- Launch real applications.

### Home

- Fullscreen wallpaper.
- Extremely thin transparent top bar.
- Context indicators on the left and restrained system status on the right.
- An active context.
- Five to eight configured applications.
- A lower-left editorial application cluster rather than a centered grid.
- Small monochromatic icons, typographic labels, a vertical accent line, and comfortable touch targets.
- Tap-to-launch.

Do not use cards, Material surfaces, a conventional app grid, a traditional bottom dock, widgets, decorative gradients, or large Android-style icons.

## Explicit non-goals for v0.1

Do not implement Android widgets, Panels, weather, music controls, calendar, smart-home features, notification badges, NotificationListener, UsageStats, AI, wallpaper analysis, app prediction, cloud sync, accounts, backend services, databases, Room, analytics, plugins, icon packs, complete settings, a custom DSL, advanced animation systems, overlays, search, a complete drawer, folders, app hiding, or a gesture editor.

Future possibilities must not be blocked, but do not build speculative abstractions for them.

## Technology and dependencies

- Native Android.
- Kotlin.
- Jetpack Compose.
- Kotlin Coroutines.
- Flow or StateFlow only where they add value.
- Gradle Kotlin DSL and the project Gradle Wrapper.
- A single `app` module initially.
- No persistence initially; use DataStore later only if configuration persistence becomes necessary.

Dependency priority:

1. Android API.
2. AndroidX.
3. Compose.
4. A third-party library only for a demonstrated need.

Do not use Flutter, React Native, Capacitor, WebView, Redux/MVI frameworks, or a global Gradle installation as a project dependency.

## Architecture direction

Keep few abstractions with clear responsibilities. A likely shape, created only as implementation requires it, is:

```text
app/src/main/java/<package>/
├── MainActivity.kt
├── launcher/
│   ├── model/
│   ├── repository/
│   └── system/
├── config/
└── ui/
    ├── components/
    └── theme/
```

Do not create empty files merely to match this tree.

The Android integration boundary is:

```text
Android package APIs
        ↓
AppRepository
        ↓
Launcher models/state
        ↓
Compose UI
```

Composables must not query `PackageManager` directly.

## Initial data direction

A launchable app needs a package name, label, and optional drawable icon. A launcher context needs an ID, label, and a list of application package names. Configuration should use normal Kotlin objects and lists.

Do not invent package names for configured apps. Resolve applications actually available on the device, tolerate missing configuration, and never crash because an application is absent or removed.

App discovery must be session-cached, run outside recomposition, avoid blocking the main thread, and handle packages without launcher activities. App launching is a separate Android-system responsibility and failure should remain quiet.

## Visual baseline

The Home composition is explainable as:

```text
Wallpaper
+ thin system/context bar
+ 5–8 frequent actions
```

The wallpaper uses crop behavior and dominates the screen. A bundled development asset is acceptable initially; integration with the real system wallpaper comes later.

The top bar targets roughly 24–32dp, remains transparent, may have an extremely subtle divider, and must not resemble a Material `TopAppBar`. It has no app title, hamburger menu, conventional toolbar, or oversized clock.

Context indicators use small restrained geometries. Active state uses the accent token and inactive states use low contrast. Color alone must not be the only accessibility signal.

The app cluster sits toward the lower-left using constraints and responsive spacing rather than fixed pixel coordinates. Each row has a comfortable touch target despite its visually small content.

Use clean, technical, editorial, precise, restrained typography. Mono or mono-adjacent type may be used for status and small labels, but never to create terminal cosplay.

## Platform behavior

- Portrait-first, but never tied to one resolution.
- Use dp, Compose constraints, responsive spacing, and `WindowInsets`.
- Support edge-to-edge correctly around status bars, navigation bars, and display cutouts.
- Do not simulate system bars.
- Keep content descriptions, readable labels, sufficient contrast, and comfortable touch targets.
- Essential future gestures need non-gesture alternatives.

Performance is critical because Home opens constantly: no package queries during recomposition, no network or backend, small Home state, minimal dependencies, cached app metadata, and fast startup.

## Implementation phases

1. Validate the project: sync, build with `./gradlew assembleDebug`, run on a physical device, and remove template UI.
2. Make it a real launcher: manifest Home intent, default-launcher selection, and Home-button verification.
3. Add app discovery: models, repository, real launchable apps, configured resolution, and missing-app handling.
4. Build v0.1 Home: wallpaper, top bar, contexts, status, lower-left app cluster, accent, labels, icons, and tap-to-launch.
5. Scaffold contexts: HOME, WORK, MEDIA, SOCIAL, and TOOLS; add horizontal context swiping only if it remains simple.

Do not implement Drawer, Search, or Panels in these phases unless the scope is explicitly changed.

## Definition of done for v0.1

- Project opens and syncs in Android Studio.
- `./gradlew assembleDebug` succeeds and produces an installable APK.
- The app runs on a physical Android device.
- Android recognizes and can select Veil as Home.
- Home returns correctly to Veil.
- Veil discovers real apps and resolves configured apps.
- Tapping an app launches it.
- Missing configured apps never crash the launcher.
- Wallpaper remains visually dominant.
- The top bar is minimal and the apps use the lower-left editorial composition.
- There are no cards, conventional grids, widgets, Panels, or backend.
- The architecture remains small and easy to change.

## Code guardrails

- Use idiomatic Kotlin and Compose.
- Separate Android integration from UI.
- Avoid premature abstraction, giant composables, and one-line abstractions with no utility.
- Comment only non-obvious decisions.
- Use explicit names and deterministic behavior.
- Prefer real Android APIs and real app metadata.
- Test on a physical device as soon as a phase permits it.
- Preserve conceptual space for contexts and future Panels without implementing them early.
