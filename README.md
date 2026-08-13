# Veil

Veil is an Android launcher inspired by the philosophy of Arch Linux and Qtile, translated for touch rather than copied from the desktop.

The repository contains a functional launcher with Android Home integration, real application discovery and launching, five purposeful context lenses, Ambient Continuity, a searchable application drawer, and a quiet wallpaper-first Home UI.

## Ambient Continuity

CURRENT shows one relevant ongoing activity when Android exposes it: navigation, active or recently paused media, or progress. MEDIA specializes in the current media session. WORK, SOCIAL, and TOOLS remain focused lenses with automatically classified fallbacks.

Veil asks for Android notification-listener access contextually. This access is optional, content remains in memory, and Veil deliberately ignores calls, conversations, email, alarms, social notifications, and unrelated notifications. Without access, every launcher and drawer feature continues to work normally.

## Bootstrap verification

```bash
./gradlew assembleDebug
```

The product direction and implementation guardrails are documented in [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md).

## Configuration

The initial configuration lives in [`LauncherConfig.kt`](app/src/main/java/dev/vicent/veil/config/LauncherConfig.kt).

- Change the complete color system through `LauncherConfig.palette`.
- Change context labels and preferred packages through `LauncherConfig.contexts`; each context keeps its fixed product purpose.
- Add installed application package names to a context's `apps` list to configure it explicitly.
- Veil completes CURRENT with the available default phone, messaging, browser, music, and camera applications; it leaves a slot empty rather than filling it with an irrelevant app.
- WORK, MEDIA, and SOCIAL put configured installed packages first, then fill remaining positions from Android application categories.

Veil displays the real system wallpaper through the Android window. The visual concept images are kept under [`docs/design`](docs/design) as references and are not bundled into the APK.

## Application drawer

- Swipe upward on Home to open the complete alphabetical application list.
- Press Home while Veil is already running to open the drawer directly.
- Press Home again while the drawer is open to return to the clean Home screen.
- Search by application label or package name; accents do not affect matching.
- Search also exposes direct links to common Android settings such as Wi-Fi, Bluetooth, display, sound, battery, security, and accessibility.
- The SYSTEM section always includes an action to activate or review Ambient Continuity access.
- Press the keyboard search action to open the first result, or use Back / `CERRAR` to return Home.
- Long-press an application on Home or in the drawer to open its action sheet: Open, App info, or Android's uninstall confirmation.
