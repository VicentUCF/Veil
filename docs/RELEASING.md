# Publicar una versión estable de Veil

Veil `1.0` inicia la línea de actualizaciones estable con `versionCode 3`. Todas las APK posteriores deben conservar el mismo `applicationId` (`dev.vicent.veil`), usar un `versionCode` estrictamente superior y estar firmadas con la misma clave.

## Copia de seguridad obligatoria

Conserva fuera del repositorio:

- `veil-upload.jks`;
- `VEIL_UPLOAD_STORE_PASSWORD`;
- `VEIL_UPLOAD_KEY_ALIAS`;
- `VEIL_UPLOAD_KEY_PASSWORD`.

Si la clave se pierde, Android rechazará las actualizaciones directas y los usuarios tendrán que desinstalar Veil, perdiendo sus preferencias locales. Si la clave se filtra, un tercero podría firmar una actualización que Android aceptaría como legítima.

## Secretos de GitHub Actions

En `Settings → Secrets and variables → Actions`, crea estos secretos:

```text
VEIL_UPLOAD_KEYSTORE_BASE64
VEIL_UPLOAD_STORE_PASSWORD
VEIL_UPLOAD_KEY_ALIAS
VEIL_UPLOAD_KEY_PASSWORD
```

Obtén el valor Base64 sin modificar el archivo original:

```bash
base64 -w 0 veil-upload.jks
```

No pegues estos valores en archivos versionados, incidencias, pull requests o logs.

## Crear una actualización

1. Incrementa `versionCode` en `app/build.gradle.kts`.
2. Cambia `versionName` al número público deseado.
3. Ejecuta tests y lint.
4. Fusiona el cambio en `main`.
5. Crea y sube una etiqueta que coincida exactamente con `v<versionName>`.

Ejemplo para `1.1`:

```bash
git tag v1.1
git push origin v1.1
```

El workflow `Stable release` reconstruye la aplicación, verifica la firma y publica la APK. También puede ejecutarse manualmente para validar una release sin crear una etiqueta.
