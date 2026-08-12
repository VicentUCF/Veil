# Veil

Veil is an Android launcher inspired by the philosophy of Arch Linux and Qtile, translated for touch rather than copied from the desktop.

The repository contains the first functional launcher version: Android Home integration, real application discovery and launching, five context slots, and the quiet wallpaper-first Home UI.

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
