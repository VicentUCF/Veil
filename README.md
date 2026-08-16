# Veil

## Release de producción

Las releases están cerradas por defecto: `assembleRelease` y `bundleRelease` fallan si no hay firma o datos editoriales de privacidad. Configura estas variables únicamente en el almacén de secretos del entorno de publicación:

```text
VEIL_UPLOAD_STORE_FILE=/ruta/absoluta/upload.jks
VEIL_UPLOAD_STORE_PASSWORD=...
VEIL_UPLOAD_KEY_ALIAS=...
VEIL_UPLOAD_KEY_PASSWORD=...
VEIL_PRIVACY_POLICY_URL=https://…
VEIL_PRIVACY_CONTACT=...
```

Después ejecuta `./gradlew clean test lint bundleRelease` y verifica el AAB/APK con las herramientas oficiales de Android. El gate comprueba también que la URL de privacidad responda públicamente con contenido HTML o texto, y la app incorpora el enlace y el contacto configurados. No guardes el keystore ni sus contraseñas en el repositorio. La política fuente está en [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md) y la hoja para Play Console en [`docs/DATA_SAFETY.md`](docs/DATA_SAFETY.md).

Veil is an Android launcher inspired by the philosophy of Arch Linux and Qtile, translated for touch rather than copied from the desktop.

The repository contains a functional launcher with Android Home integration, real application discovery and launching, five purposeful context lenses, Ambient Continuity, a searchable application drawer, and a quiet wallpaper-first Home UI.

## Ambient Continuity

CURRENT shows one relevant ongoing activity when Android exposes it: navigation, active or recently paused media, or progress. MEDIA specializes in the current media session. WORK, GAME, and TOOLS remain focused lenses with deterministic fallbacks.

Veil asks for Android notification-listener access contextually. This access is optional, content remains in memory, and Ambient Continuity deliberately ignores calls, conversations, email, alarms, social notifications, and unrelated notifications. Without access, every launcher and drawer feature continues to work normally.

The same optional access powers a separate privacy-preserving signal on CURRENT app rows and contextual docks. A small dot means Android currently exposes at least one relevant, badge-eligible notification for that app. It never shows content or a count, does not claim to measure unread messages, and disappears only when Android removes the last relevant notification.

## Bootstrap verification

```bash
./gradlew assembleDebug
```

The product direction and implementation guardrails are documented in [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md).

## Configuration

Veil ships vendor-neutral semantic defaults rather than a single developer's app list. Each initial slot contains alternatives from several providers; WORK, MEDIA and GAME can also use Android application categories as deterministic fallbacks. CURRENT and TOOLS leave a missing semantic slot empty instead of inserting an unrelated app.

Runtime preferences live in the internal **Ajustes de Veil** screen, available from Everything and TOOLS. Context apps are edited directly where they appear: hold an app to replace or remove it, or tap an empty `+` slot. Once a context is customized, its five exact package positions are persisted and Veil no longer refills or reorders them automatically.

The bounded product defaults and palette remain declared in [`LauncherConfig.kt`](app/src/main/java/dev/vicent/veil/config/LauncherConfig.kt); normal personalization does not require editing source code.

Veil displays the real system wallpaper through the Android window. The visual concept images are kept under [`docs/design`](docs/design) as references and are not bundled into the APK.

The settings screen also offers five accessible accent presets, Android 12+ dynamic color, a **Fuente de CURRENT** submenu with light/dark foreground, three fixed text weights and an optional intensity-adjustable contrast filter behind every view, the system wallpaper chooser, permission/special-access status, HOME-role selection and a preferred music provider.

## Application drawer

- Swipe upward on Home to open the complete alphabetical application list.
- Press Home while Veil is already running to open the drawer directly.
- Press Home again while the drawer is open to return to the clean Home screen.
- Search by application label or package name; accents do not affect matching.
- Search also exposes direct links to common Android settings such as Wi-Fi, Bluetooth, display, sound, battery, security, and accessibility.
- The SYSTEM section always includes an action to activate or review Ambient Continuity access.
- That action also explains and manages the content-free application notification signals.
- Press the keyboard search action to open the first result, or use Back / `CERRAR` to return Home.
- Long-press an application on Home or in the drawer to open its action sheet: Open, App info, or Android's uninstall confirmation.
