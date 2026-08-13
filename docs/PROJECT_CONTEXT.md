# Veil — Android launcher bootstrap context

## Product statement

> Qtile philosophy translated to touch, not copied to Android.

Veil is a native Android launcher inspired by Arch Linux and Qtile. It must feel like a quiet operating-system layer in which the wallpaper is the primary visual content and interface elements appear only when useful.

Veil's defining interaction philosophy is **Ambient Continuity**: Home is the current context, not a dashboard. When Android exposes a trustworthy ongoing activity, Veil answers with one relevant thing that helps the user continue it. Nothing starts on Home; everything continues.

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
- Reveal layers progressively: Current, Context, Everything, Search.
- Model activities as purposeful context lenses—CURRENT, WORK, MEDIA, SOCIAL, and TOOLS—not traditional pages of icons.
- Keep 5–8 frequent actions on Home and make them available with one tap.
- Search is a future power tool, not the primary interaction model.
- Use an editorial, deliberately asymmetric layout with strong alignment and negative space.
- Typography has more visual importance than icons; do not imitate a terminal or use fake shell decoration.
- Color communicates state only. Centralize off-white, soft gray, muted blue, error, and success tokens.
- Motion must be short, discreet, and functional. Do not add decorative animation in v0.1.
- Configuration should remain declarative through ordinary Kotlin structures. Do not create a DSL in v0.1.
- Touch is primary; keyboard interaction is optional.
- Only one continuity surface may have visual priority at a time. If no trustworthy activity exists, the context falls back to its useful actions.

Veil is not a literal Qtile clone, a terminal-themed UI, a Niagara clone, a conventional Material launcher, a widget dashboard, or a desktop layout transplanted to mobile.

## v0.1 scope

### Launcher behavior

- Install as an Android application.
- Register as a real Home candidate.
- Allow Android to select it as the default launcher.
- Return to Veil through the Home action.
- Discover launchable applications installed on the device.
- Launch real applications.
- Open the complete application drawer with an upward gesture from Home.
- Open the drawer when Home is pressed while Veil is already running.
- Close the drawer when Home is pressed again while the drawer is open.
- Search installed applications and useful Android settings shortcuts.
- Open a contextual bottom sheet when an application is long-pressed.

### Home

- Fullscreen wallpaper.
- Extremely thin transparent top bar.
- Context indicators on the left and restrained system status on the right.
- An active context.
- One relevant continuity surface when Android publishes an ongoing activity.
- Five configured or automatically classified actions as the quiet fallback.
- A lower-left editorial application cluster rather than a centered grid.
- Small monochromatic icons, typographic labels, a vertical accent line, and comfortable touch targets.
- Tap-to-launch.

The five context lenses have fixed purposes:

- CURRENT ranks the most relevant activity across the system, then falls back to frequent actions.
- WORK falls back to productivity applications.
- MEDIA shows the active or recently paused media session, then falls back to media applications.
- SOCIAL exposes communication applications without reading or displaying conversations.
- TOOLS exposes direct Android settings actions.

Ambient Continuity v0.1 supports public Android signals only: active media sessions, navigation notifications, and ongoing or completed progress notifications. Navigation outranks playing media, which outranks active progress, paused media, and recently completed progress. Media paused for more than 30 minutes and completed progress older than 10 minutes expire.

Notification-listener access is explicit and optional. Veil explains the benefit before opening Android settings, remains a complete launcher when access is declined, and never persists or transmits notification content. Calls, conversations, email, alarms, social notifications, and unrelated notifications are excluded.

Do not use cards, Material surfaces, a conventional app grid, a traditional bottom dock, widgets, decorative gradients, or large Android-style icons. Home uses coherent activity glyphs; Everything preserves recognizable full-color application icons.

### Drawer and search

- Present all discovered launchable applications in an alphabetical typographic list.
- Keep search immediately available and tolerant of case and accents.
- Match application labels and package names.
- Include direct links to common Android settings categories without attempting to replace the system Settings app.
- Treat the drawer as a progressive layer over Home, not as the primary visual state.
- Provide Back and a visible close action as alternatives to gestures.
- A long press on an app from Home or the drawer opens the same contextual action sheet.
- Contextual app actions may open the app, system app details, or Android's uninstall confirmation.

## Explicit non-goals for v0.1

Do not implement Android widgets, weather, calendar access, smart-home features, notification badges, conversation reading, UsageStats, Accessibility-based inference, AI, wallpaper analysis, app prediction, cloud sync, accounts, backend services, databases, Room, analytics, plugins, icon packs, a replacement settings UI, a custom DSL, advanced cross-task animation systems, arbitrary overlays, folders, app hiding, or a gesture editor.

Do not promise continuity for internal app state such as a Kindle chapter unless that application publishes a compatible public Android session or notification. A third-party launcher cannot own or transform another application's cross-task transition.

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

A launchable app needs a package name, label, Android application category, and optional drawable icon. A launcher context needs an ID, label, fixed purpose, and a list of preferred application package names. Configuration should use normal Kotlin objects and lists.

Do not invent package names for configured apps. Resolve applications actually available on the device, tolerate missing configuration, and never crash because an application is absent or removed.

App discovery must be session-cached, run outside recomposition, avoid blocking the main thread, and handle packages without launcher activities. App launching is a separate Android-system responsibility and failure should remain quiet.

## Visual baseline

The Home composition is explainable as:

```text
Wallpaper
+ thin transparent context rail
+ one continuity surface OR five fallback actions
```

The wallpaper uses crop behavior and dominates the screen. A bundled development asset is acceptable initially; integration with the real system wallpaper comes later.

The top rail targets 32dp, remains transparent, has no full-width divider, and must not resemble a Material `TopAppBar`. It has no app title, hamburger menu, conventional toolbar, or oversized clock.

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
4. Build the wallpaper-first Home with its thin context rail, activity glyphs, lower-left fallback cluster, and context navigation.
5. Add Ambient Continuity for media, navigation, and progress with one relevant surface and optional notification access.
6. Add the application drawer: upward gesture, Home reentry, complete app list, search, Android settings shortcuts, and continuity-access entry.

## Definition of done for v0.1

- Project opens and syncs in Android Studio.
- `./gradlew assembleDebug` succeeds and produces an installable APK.
- The app runs on a physical Android device.
- Android recognizes and can select Veil as Home.
- Home returns correctly to Veil.
- Veil discovers real apps and resolves configured apps.
- Tapping an app launches it.
- Swiping upward and pressing Home on a running Veil instance open the drawer.
- Pressing Home again closes an open drawer.
- The drawer lists and filters real applications and opens Android settings shortcuts.
- Long-pressing an application opens actions for launch, system app details, and uninstall request.
- Missing configured apps never crash the launcher.
- Wallpaper remains visually dominant.
- The top rail is transparent and minimal, Home uses activity glyphs, and full application icons render correctly in Everything.
- CURRENT and MEDIA show at most one relevant continuity surface; WORK, SOCIAL, and TOOLS retain distinct purposes.
- Access can be accepted, declined, or revoked without breaking the launcher, and private notification categories never enter launcher state.
- There are no cards, conventional grids, widgets, UsageStats, persistence, or backend.
- The architecture remains small and easy to change.

## Code guardrails

- Use idiomatic Kotlin and Compose.
- Separate Android integration from UI.
- Avoid premature abstraction, giant composables, and one-line abstractions with no utility.
- Comment only non-obvious decisions.
- Use explicit names and deterministic behavior.
- Prefer real Android APIs and real app metadata.
- Test on a physical device as soon as a phase permits it.
- Keep Android handles such as `PendingIntent` and `MediaController` outside Compose state.
