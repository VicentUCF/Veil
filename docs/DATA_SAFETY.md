# Hoja de declaración Data Safety

Estado técnico comprobado el 13 de agosto de 2026. Esta hoja describe el binario actual de Veil; debe revisarse de nuevo si cambia el código, un proveedor o el formulario de Google Play.

## Respuestas generales

- La app recopila o comparte algún tipo de dato: **sí**. La ubicación aproximada cruza el límite del dispositivo para solicitar el tiempo.
- Todos los datos transmitidos fuera del dispositivo se cifran en tránsito: **sí**, mediante HTTPS con tráfico HTTP deshabilitado.
- El usuario puede solicitar la eliminación de datos: **no aplica a una cuenta remota de Veil**. Veil no tiene cuentas ni backend y no conserva remotamente estos datos. Los datos locales se borran desde Android al eliminar los datos de la app o desinstalarla.
- Compromiso con Families: no declarar salvo que la ficha se dirija realmente a menores y se complete la revisión específica correspondiente.

## Tipos de datos

### Ubicación aproximada

- Recopilada: **sí**.
- Compartida: declarar **sí** de forma conservadora, porque Open-Meteo recibe directamente la ubicación. Solo debe marcarse la excepción de proveedor de servicio o de acción iniciada por el usuario si el editor ha comprobado que los términos y el flujo publicado cumplen exactamente la definición vigente de Google Play.
- Procesamiento efímero: **sí para las coordenadas**. Se usan en memoria para la solicitud y no se guardan. La respuesta meteorológica se almacena temporalmente sin coordenadas.
- Obligatoria u opcional: **opcional**. El launcher funciona sin conceder ubicación.
- Finalidad: **funcionalidad de la aplicación**.
- Publicidad, analítica, personalización, gestión de cuenta o prevención del fraude: **no**.

## Datos que se procesan solo en el dispositivo

Según el comportamiento actual, no se declaran como recopilados porque no se transmiten fuera del dispositivo:

- aplicaciones instaladas;
- eventos del calendario;
- notas rápidas y preferencias;
- claves y paquetes de señales de notificación;
- FFT transitorio de la mezcla de audio;
- temporizadores y configuración del launcher.

El permiso `RECORD_AUDIO` no implica que Veil recopile grabaciones: el binario solo analiza localmente el espectro de salida mientras la app está visible, MEDIA está visible y hay reproducción. No almacena ni transmite audio.

## Steam y datos técnicos de conexión

GAME solicita rankings, noticias, metadatos e imágenes públicas de Steam. Veil no añade identificadores de usuario, cuenta o dispositivo. Steam y Open-Meteo reciben inevitablemente la IP y cabeceras técnicas mínimas de la conexión HTTPS. La política pública debe seguir explicándolo. Si una futura versión añade cuenta, telemetría, identificadores, búsquedas remotas o parámetros derivados del usuario, habrá que actualizar esta declaración antes de publicar.

## Evidencia del repositorio

- Política fuente: [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md).
- Destinos y validación HTTPS: `ExternalLinkPolicy`, `SteamGameRepository` y `WeatherRepository`.
- Exclusiones de backup: `backup_rules.xml` y `data_extraction_rules.xml`.
- Gate editorial y de firma: `verifyProductionRelease` en `app/build.gradle.kts`.

La definición oficial de Google considera “recopilación” cualquier transmisión fuera del dispositivo e indica que el procesamiento efímero debe incluirse en el formulario, aunque pueda no mostrarse públicamente en la sección Data Safety. La declaración final debe contrastarse con la [guía vigente de Google Play](https://support.google.com/googleplay/android-developer/answer/10787469).
