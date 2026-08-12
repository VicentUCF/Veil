# Veil

Veil is an Android launcher inspired by the philosophy of Arch Linux and Qtile, translated for touch rather than copied from the desktop.

The repository contains the first functional launcher version: Android Home integration, real application discovery and launching, five context slots, a searchable application drawer, and the quiet wallpaper-first Home UI.

## Bootstrap verification

```bash
./gradlew assembleDebug
```

The product direction and implementation guardrails are documented in [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md).

## Configuration

The initial configuration lives in [`LauncherConfig.kt`](app/src/main/java/dev/vicent/veil/config/LauncherConfig.kt).

- Change the complete color system through `LauncherConfig.palette`.
- Change the context list through `LauncherConfig.contexts`.
- Add installed application package names to a context's `apps` list to configure it explicitly.
- When the HOME list is empty, Veil resolves the device's default phone, messaging, browser, music, and camera applications and fills any unavailable slot with another launchable app.

Veil displays the real system wallpaper through the Android window. The visual concept images are kept under [`docs/design`](docs/design) as references and are not bundled into the APK.

## Application drawer

- Swipe upward on Home to open the complete alphabetical application list.
- Press Home while Veil is already running to open the drawer directly.
- Press Home again while the drawer is open to return to the clean Home screen.
- Search by application label or package name; accents do not affect matching.
- Search also exposes direct links to common Android settings such as Wi-Fi, Bluetooth, display, sound, battery, security, and accessibility.
- Press the keyboard search action to open the first result, or use Back / `CERRAR` to return Home.
- Long-press an application on Home or in the drawer to open its action sheet: Open, App info, or Android's uninstall confirmation.
