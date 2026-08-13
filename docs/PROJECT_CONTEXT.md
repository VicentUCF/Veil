# Veil — product and implementation context

## Product statement

> Qtile philosophy translated to touch, not copied to Android.

Veil is a native Android launcher that behaves as a quiet operating-system layer. Its defining idea is **Ambient Continuity**: Home remembers public Android activities and helps the user resume them. Nothing starts on Home; everything continues.

Veil uses five stable context lenses—CURRENT, WORK, MEDIA, SOCIAL and TOOLS. Each lens has its own **Cozy Workspace**: an asymmetric arrangement of dark translucent tiles inspired by Qtile windows with picom. A workspace may contain several useful surfaces, but exactly one surface owns visual priority.

## Decision hierarchy

Prioritize reliability as an Android Home app, one-tap usability, trustworthy data, wallpaper visibility, clear hierarchy, touch ergonomics, performance, and only then decorative polish. Do not present unavailable or inferred data as real.

## Interaction model

- The thin transparent top rail is aligned to the physical top edge, selects the five contexts and shows restrained system status.
- Horizontal swipes move between contexts.
- WORK, MEDIA, SOCIAL and TOOLS always show a fixed five-slot application dock. CURRENT deliberately hides it so the primary Home remains quiet.
- An upward swipe on Home opens Everything directly; Search remains part of Everything.
- Back closes Everything. Home closes Everything first and opens it on the next press.
- Tile geometry is stable. Missing data becomes an honest empty/permission/portal state instead of rearranging the screen.
- Motion is functional and remains within roughly 120–200 ms.

## Cozy Workspaces

All workspaces use a responsive two-column grid with 16 dp outer padding and 10 dp gaps. Below 360 dp, paired tiles stack without changing their semantic order. Dominant and secondary tiles use shared height tokens so adjacent pieces keep the same baseline even when their contents differ. Tiles use dark 70–82% opaque fills, one-pixel low-contrast borders and moderate 12 dp corners. There is no runtime blur, decorative gradient, invented third-party data or desktop window chrome.

- **CURRENT**: the highest-ranked ongoing activity is primary. The next calendar event and current weather support it. Without continuity, time/date and a quiet state become primary.
- **WORK**: today's agenda (up to three events) is primary and may include one compact, published work-progress state. Up to three local quick notes and a compact Pomodoro support it. It never repeats the dock applications.
- **MEDIA**: the active/recent media session is primary, including artwork, timeline and only supported transport controls. With no session, the same stable geometry becomes a library surface. Sound/output and collection context are secondary; applications remain in the dock.
- **SOCIAL**: the composition frames direct communication, communities, visual content and calls without duplicating application launchers. Veil never reads or renders conversation content, people or unread counts. A configured app may show the same binary active-notification indicator used by the other docks; the workspace itself does not render notification content or fill empty space with privacy notices.
- **TOOLS**: a device dashboard is primary, showing only public Android data: manufacturer/model, Android version and security patch, storage and memory. Battery and connectivity are secondary, followed by a full-width control centre with direct entries for display, sound, applications, security and all settings. Restricted controls open the relevant Android Settings surface rather than being simulated. Focus remains available from WORK.

Wallpaper remains perceptible around and through every tile. Dense does not mean equal: only one tile per workspace uses the accent and prominent type.

## Context dock and applications

Each context owns five stable quick-action slots. WORK, MEDIA, SOCIAL and TOOLS expose them continuously in a bottom dock; CURRENT keeps its configured set available to the model but does not render a dock. The dock has no floating trigger, expanded state or overlay. It uses recognizable real application icons in uniform 48 dp slots. Source defaults and deterministic category fallback provide the initial setup. Once the user customizes a context in Veil settings, its five exact positions are persisted: an empty or uninstalled slot remains empty and never moves or causes another slot to be refilled. There is no usage prediction or position reshuffling.

Applications shown in the dock must not be repeated as portal rows inside the same workspace. Workspace tiles add context—status, continuity, library, Focus or system controls—while the dock owns launching.

CURRENT app rows and the four contextual docks may show one restrained binary dot when Android exposes at least one active, badge-eligible notification for that package. The dot never contains a count or content, does not claim to represent unread state and remains synchronized with Android rather than being cleared locally when the app opens. Everything, workspace tiles and the top rail do not show these indicators.

Everything preserves the full alphabetical app list, search, settings shortcuts and app actions. Its SYSTEM section opens the essential Veil settings screen as well as Android settings. App discovery remains cached and outside Compose.

## Essential launcher settings

Veil provides a deliberately bounded internal settings screen, reachable from Everything and TOOLS. It controls the Veil accent and the readability of CURRENT's wallpaper-level foreground (light/dark text and line icons plus three fixed text weights). The chosen foreground tone selects a soft black/white wallpaper scrim behind every Veil surface; the user may disable it independently and adjust its bounded intensity. Settings delegates wallpaper selection to Android, reports and links to the permissions and special accesses already used by the launcher, reports the active HOME role and chooses the preferred app opened by MEDIA's empty state. Context apps are edited in place: holding an occupied app offers replace/remove actions and tapping an empty `+` slot opens the app picker. Changes apply immediately. The screen does not expose layout geometry, app-icon theming, font-family, folder, widget or gesture customization.

The accent may use the Veil coral, a short accessible preset palette or Android's wallpaper-derived dynamic color on Android 12 and later. Veil never reads or stores wallpaper imagery; Android owns wallpaper selection and rendering.

The preferred music provider affects only the action shown when MEDIA has no active session. Ambient Continuity continues to display the highest-ranked public media session from any compatible application.

## Real data and privacy boundaries

### Ambient Continuity

Notification-listener access is explicit and optional. Supported public signals are media sessions, navigation notifications and ongoing/completed progress notifications. Navigation outranks playing media, active progress, paused media and recently completed progress.

Media may expose title, artist, artwork, position, duration and supported transport controls through `MediaSession`. Navigation and progress use only their public ongoing notifications. Private notification categories—calls, messages, email, alarms and social—remain excluded from Ambient Continuity.

Notification indicators use a separate content-free projection of notification-listener events. Veil retains only notification keys and package names in memory, respects Android's channel badge setting where available and excludes its own notifications, ongoing/foreground services, media, navigation and progress. This signal is optional, is cleared when access is revoked or the listener disconnects, and never enters Ambient Continuity.

### Calendar

Calendar is optional and uses `CalendarContract.Instances` with `READ_CALENDAR`. Veil reads occurrences from all visible Android calendars—including locally synced Google Calendar events—from now through seven days ahead and keeps only event ID, title, start and end in memory. WORK can group those occurrences into a weekly summary; creating or editing an event delegates to the installed calendar application through public intents. It displays title and local time only: never calendar account, location, description, attendees or reminder contents. Google account setup and synchronization remain owned by Android and Google Calendar.

### Weather

Weather is optional. After a Veil disclosure, it requests foreground approximate location only and sends approximate coordinates to Open-Meteo over HTTPS. The free endpoint is valid for this personal, non-commercial build and requires visible attribution. Results are cached locally for 30 minutes, retained offline, and marked stale after two hours. No background or precise location is requested.

### Focus

Focus provides 25- and 50-minute presets plus custom durations from 5 to 180 minutes. Running, paused, completed, duration and end time are the only persisted fields. An exact `AlarmManager` alarm and a completion notification are requested in context after an explanation; denial degrades to an inexact alert without breaking the timer. Running alarms are restored after reboot and time changes.

### Quick notes

WORK stores up to three local quick notes for capture and recall. Each note has a short title—the only part rendered on the workspace—and explicitly uses either a multiline text body or a bounded checklist. The editor and other launcher modals use Veil's compact, dark Rofi/Alacritty-inspired surface rather than default Material dialogs. Notes remain ordered, are excluded from cloud backup and device transfer, and are never shared with another application or service.

### System status

Battery, charging, storage, memory, device identity, Android version, security patch and active transport come from Android system APIs. Missing values remain explicitly unavailable. Veil does not replace Settings and does not toggle restricted controls indirectly.

No data is transmitted except the disclosed Open-Meteo weather request. There are no analytics, accounts, cloud sync, backend services or databases.

## Technology and architecture

- Native Kotlin, Android APIs, AndroidX, Jetpack Compose, Coroutines and StateFlow.
- A single `app` module and no dependency-injection or state-management framework.
- Android integrations live in repositories/system adapters; composables receive small immutable states and callbacks.
- SharedPreferences is allowed only for Focus state, weather cache, the bounded WORK quick-note list, the one-bit notification-access onboarding acknowledgement and the bounded launcher preferences: accent, CURRENT text tone/weight, wallpaper-scrim state/intensity, preferred music provider and five nullable app-package slots per context. Do not add Room or broader configuration persistence.
- Keep Android handles such as `MediaController`, `PendingIntent`, cursors and listeners outside Compose state. A bounded bitmap is acceptable media display data.
- Prefer platform APIs. No runtime third-party dependency is currently required.

The main data direction is:

```text
Android APIs / Open-Meteo
        ↓
small repositories and system adapters
        ↓
LauncherController / LauncherUiState
        ↓
five explicit Compose workspaces
```

Do not introduce a generic widget engine or speculative plugin architecture. Each workspace remains an explicit product composition.

## Platform and quality requirements

- Veil is a real HOME candidate and must return correctly through the Home action.
- Portrait-first, responsive to width, edge-to-edge and safe around navigation bars/cutouts.
- Touch targets remain comfortable even when visual glyphs are small.
- Icon rasterization respects device density and adaptive drawables.
- No package, calendar, location or network query occurs during recomposition.
- Permissions may be accepted, denied or revoked without making Veil unusable.
- Weather/network failure and absent providers retain honest fallback states.
- Missing/uninstalled quick apps never crash the launcher.
- Focus survives process death and reboot and clearly reports when an external alert is not guaranteed.
- Unit tests cover deterministic slots, continuity ranking/policy, weather parsing and workspace data policies. Build and install validation must use the connected physical device when available.

## Explicit non-goals

Do not add Android widgets, conversation reading, notification counts or previews, a notification inbox, UsageStats, Accessibility inference, AI, wallpaper analysis, app prediction, cloud accounts, backend services, analytics, icon packs, arbitrary overlays, folders, app hiding, custom font families, editable grid geometry, theme export, manual launcher backups, smart-home controls, a gesture editor or a general user-customization system. The bounded accent/CURRENT-readability/wallpaper/access/app screen above is the only customization exception. The sole badge-like surface is the binary, content-free active-notification dot defined above.

Do not promise access to third-party internal state such as a Kindle chapter unless that application publishes a compatible Android session or notification. Veil cannot embed or transform another application's task as if it were a desktop window.

## Definition of done for Cozy Workspaces

- Five visibly distinct, stable workspaces render over the wallpaper and retain a single visual priority.
- Horizontal context navigation, top indicators, the four contextual docks, upward Everything gesture, Back and Home-layer behavior all work together.
- The configured personal quick apps resolve in fixed order with deterministic fallback and crisp real icons.
- Calendar, weather, Focus, system status and enhanced media use real data and degrade safely.
- Social never surfaces private notification or conversation information.
- The app compiles, unit tests pass, an installable debug APK is produced and core flows are exercised on the connected Xiaomi device.
