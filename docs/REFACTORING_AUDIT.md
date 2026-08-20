# Auditoría de mantenibilidad de Veil

Fecha de la auditoría inicial: 16 de agosto de 2026.

## Alcance y criterios

Esta auditoría cubre el módulo `app`, configuración Gradle, manifest y recursos,
tests y documentación. Mantiene los límites de `PROJECT_CONTEXT.md`: un único
módulo, sin framework de inyección, backend, analítica, persistencia nueva ni un
motor genérico de widgets.

Los criterios aplicados son:

- catálogo de olores y refactorizaciones de
  [Refactoring.Guru](https://refactoring.guru/refactoring/smells), en especial
  `Large Class`, `Long Method`, `Long Parameter List`, `Data Clumps`,
  `Divergent Change`, duplicación, código muerto y generalidad especulativa;
- separación de responsabilidades, flujo unidireccional, estado de UI y capa de
  datos de la [guía oficial de arquitectura de Android](https://developer.android.com/topic/architecture/recommendations);
- organización de archivos y nombres de las
  [convenciones oficiales de Kotlin](https://kotlinlang.org/docs/coding-conventions.html).

El tamaño por sí solo no decide una refactorización. Cada división propuesta
debe crear un límite de responsabilidad reconocible y conservar el comportamiento.

## Línea base

- 56 archivos Kotlin de producción y 11.897 líneas Kotlin de producción.
- 21 archivos de tests unitarios, 75 casos `@Test` y ningún test instrumentado.
- `./gradlew testDebugUnitTest lintDebug assembleDebug`: correcto.
- APK debug generado correctamente.
- Lint: 0 errores y 22 avisos.
- No había dispositivo Android conectado durante la auditoría inicial.
- El árbol de trabajo estaba limpio al comenzar.

Archivos que exceden unos pocos cientos de líneas:

| Archivo | Líneas iniciales |
| --- | ---: |
| `WorkspaceDashboards.kt` | 2.387 |
| `LauncherSettingsScreen.kt` | 968 |
| `CurrentHome.kt` | 810 |
| `LauncherScreen.kt` | 653 |
| `LauncherController.kt` | 630 |
| `AppDrawer.kt` | 535 |
| `MainActivity.kt` | 480 |
| `AmbientContinuityRepository.kt` | 454 |
| `SteamGameRepository.kt` | 393 |

## Hallazgos priorizados

Los estados son `pendiente`, `en curso`, `resuelto` o `descartado` con una
justificación. Las referencias `R-*` se resolverán en ese orden salvo que una
dependencia técnica exija adelantar una refactorización pequeña.

### P0 — límites que bloquean cambios seguros

| ID | Estado | Evidencia y olor | Resolución prevista |
| --- | --- | --- | --- |
| R-01 | resuelto | `WorkspaceDashboards.kt` contenía el enrutador y UI de WORK, MEDIA, GAME y TOOLS, además de diálogos, editores, formateadores y piezas compartidas. `Large Class` y `Divergent Change`. | El enrutador queda separado de cada workspace; agenda, notas, focus, reproductor y mezclador tienen archivos propios, y se eliminó `CurrentWorkspace`. |
| R-02 | resuelto | `LauncherScreen` recibía más de cuarenta callbacks y combinaba pager, gestos, overlays, permisos, navegación, ajustes y acciones de apps. `Long Parameter List` y `Data Clumps`. | La API usa cinco contratos acotados por responsabilidad; disclosures/acciones de app viven en `LauncherOverlays` y el wiring de Everything/Ajustes en `LauncherSurfaceLayers`. No se introdujo un bus global. |
| R-03 | resuelto | `LauncherController` agregaba once dependencias, iniciaba todos los observadores, resolvía apps, transformaba continuidad, navegaba, gestionaba ajustes y recibía `CoroutineScope` en varias operaciones. `Large Class`, `Middle Man` y `Divergent Change`. | Resolución, sincronización de fuentes y coordinación de navegación son clases separadas; el scope de lifecycle se inyecta una sola vez y el controlador queda como fachada de 269 líneas. |
| R-04 | resuelto | `LauncherUiState` era un snapshot global que llegaba completo a CURRENT, WORK, MEDIA y TOOLS. Esto contradecía el requisito local de composables con estados pequeños. | `WorkspaceDashboard` proyecta modelos inmutables específicos; cada workspace recibe únicamente clima/media, agenda/focus/notas, audio/proveedor o sistema según corresponda. |
| R-05 | resuelto | Faltaban tests de persistencia y regresiones Compose; el antiguo controlador monolítico también dificultaba sustituir dependencias. | Coordinadores/políticas puros tienen tests unitarios; se añadieron cuatro tests instrumentados de persistencia para preferencias/notas/focus y de navegación del header. Compilan y generan el APK de tests; la ejecución física queda condicionada a conectar un dispositivo. |

### P1 — responsabilidades grandes y configuración personal

| ID | Estado | Evidencia y olor | Resolución prevista |
| --- | --- | --- | --- |
| R-06 | resuelto | `LauncherSettingsScreen.kt` contenía pantalla raíz, subpantalla de apariencia, picker, slider, filas y tres diálogos; además recibía más de veinte acciones. | Raíz, apariencia, picker y componentes tienen archivos de 174–347 líneas; el contrato usa un estado y cuatro grupos de acciones acotados. |
| R-07 | resuelto | `CurrentHome.kt` mezclaba layout, reloj/tiempo, dibujo del glifo meteorológico, reproductor, lista de apps y botón principal. | `CurrentHome` queda como composición de 253 líneas; estilo, reloj/clima y reproductor tienen archivos propios de 81–237 líneas. |
| R-08 | resuelto | `MainActivity` era composition root, host Compose, coordinador de permisos/roles y lanzador de todas las superficies Android. | Accesos/roles/alarmas y acciones externas viven en dos coordinadores concretos; la Activity queda en 322 líneas como host, lifecycle, composición manual y receptor de paquetes. |
| R-09 | resuelto | `AmbientContinuityRepository` combinaba sesiones de media, notificaciones, preferencias de onboarding, acciones y lifecycle de callbacks. | El repositorio coordinador queda en 126 líneas; sesiones, mapper de notificaciones y onboarding son componentes separados y el mapper tiene tests de expiración. |
| R-10 | resuelto | `SteamGameRepository` combinaba HTTP, validación, parsing JSON, composición del feed, bitmap y caché; además fijaba región e idioma personales. | Repositorio (composición), cliente remoto, parser y caché son piezas separadas de 64–135 líneas; las URLs ya usan el fallback neutral de Steam y los tests del parser permanecen. |
| R-11 | resuelto | `WeatherRepository` combinaba proveedor de ubicación, cliente Open-Meteo, parser, caché y política de frescura. | Repositorio de 68 líneas, fuente de ubicación, cliente/parser y caché son piezas separadas; los tests de parsing y límites de respuesta siguen verdes. |
| R-12 | resuelto | `FocusTimerRepository` combinaba máquina de estado, SharedPreferences, AlarmManager, restauración de receiver, canal y notificación. | Coordinador, store y scheduler/alerter son clases separadas de 58–94 líneas; el receiver depende directamente del scheduler y los tests siguen verdes. |
| R-13 | resuelto | `WorkspaceDataPolicy` agrupaba agenda, Focus, almacenamiento, biblioteca GAME y visibilidad del dock: responsabilidades sin cohesión. | Dividido en políticas de agenda, focus, almacenamiento, biblioteca GAME y layout. |
| R-14 | resuelto | `LauncherSettings.kt` y `WorkspaceState.kt` agrupaban modelos de funcionalidades no relacionadas; `LauncherController.kt` también contenía los modelos de UI resuelta. | Cada familia tiene ahora un archivo reconocible y `LauncherUiState` salió del controlador. |
| R-15 | resuelto | `LauncherConfig` fijaba una selección personal de paquetes y un test exigía Google Authenticator en una posición concreta. | Los slots iniciales usan candidatos multiproveedor y fallbacks de categoría; GAME ya no nombra juegos personales y los overrides siguen siendo exactos. |
| R-16 | resuelto | Solo había 6 recursos string frente a más de 200 textos/semánticas incrustados en Kotlin; también había texto de presentación en adaptadores. | Todo el copy visible, semánticas, disclosures, notificaciones y fallbacks de adaptadores usan recursos con formatos/plurales. Los modelos de acento/contexto ya no almacenan etiquetas de UI; el barrido residual conserva solo ids, símbolos, formatos, claves, URLs y etiquetas internas. |
| R-17 | resuelto | `collectAsState()` mantenía la colección aunque la UI no estuviera en un estado de lifecycle activo. | La Activity usa ahora `collectAsStateWithLifecycle()`, según la guía oficial de Android. |
| R-18 | resuelto | `SystemStatus` almacenaba `connectionLabel` en español aunque ya contenía `ConnectionType`; eran dos fuentes para el mismo dato y la data layer decidía presentación. | El modelo conserva solo `ConnectionType`; la etiqueta se deriva en la capa de UI. |
| R-19 | resuelto | Refrescar accesos lanzaba calendario y tiempo y `onResume` llamaba además a `refreshVisibleData`; tampoco había coordinación explícita de peticiones duplicadas. | La lectura de permisos ya no refresca datos, el controlador cancela el trabajo anterior por fuente y calendar/weather serializan sus secciones críticas. |

### P2 — deuda localizada y calidad continua

| ID | Estado | Evidencia y olor | Resolución prevista |
| --- | --- | --- | --- |
| R-20 | resuelto | Código muerto confirmado: `CurrentWorkspace`; métodos de selección antiguos en `AppRepository`; `toggleDrawer` y `resetContextApps` sin consumidores de producción. | Eliminado junto con sus tests de APIs obsoletas; los tests de selección activa permanecen. |
| R-21 | resuelto | La descripción meteorológica estaba duplicada y el formato de duración compartido vivía accidentalmente en el router de dashboards. | La división eliminó el segundo mapper meteorológico y `WorkspacePresentation` alberga solo el formato compartido; fecha y capacidad permanecen locales porque expresan reglas distintas. |
| R-22 | resuelto | Alturas primarias/secundarias, breakpoint y separación de dashboard aparecían repetidos; solo parte del movimiento estaba tokenizado. | `WorkspaceLayoutTokens` expresa la geometría común y `VeilMotion` concentra las duraciones; medidas propias de un dibujo o control siguen encapsuladas localmente. |
| R-23 | resuelto | `LauncherApp` llevaba un `Drawable` mutable dentro del estado Compose y el icono se rasterizaba en UI. | `AppIconLoader` rasteriza una sola vez fuera de Compose y el modelo expone un bitmap inmutable; la UI no consulta paquetes ni muta drawables. |
| R-24 | resuelto | Lint registraba sugerencias KTX y un aviso de atributo API 24 con `minSdk 23`. | URI, preferencias y bitmap usan KTX; `tools:targetApi` documenta que la configuración de red se ignora deliberadamente solo en API 23. Informe actual: 0 errores y 0 avisos. |
| R-25 | resuelto | Había constantes como `MaxNotes` y `PreferencesName` y formato irregular en `Theme.kt`; `quickActionCount` sí es un nombre descriptivo convencional para una propiedad variable. | Constantes normalizadas a `UPPER_SNAKE_CASE`, formato corregido y propiedades no constantes conservadas con `lowerCamelCase`. |
| R-26 | resuelto | `README.md` mencionaba `SOCIAL`, describía una configuración antigua y presentaba el código fuente como personalización normal. | Actualizado para GAME, candidatos neutrales y overrides exactos desde Ajustes de Veil. |
| R-27 | resuelto | Las restricciones de versiones vulnerables se duplicaban entre Gradle raíz y `app`; el catálogo usaba Kotlin 2.4.20-RC. | Política de configuraciones centralizada en Gradle raíz (el classpath temprano conserva su frontera propia), Kotlin fijado a la estable 2.4.10 y locks/metadata regenerados; compilación validada. |
| R-28 | resuelto | La revisión inicial pasó por alto dos workflows versionados; el gate de seguridad ya ejecuta test, lint y assemble en push/PR. | Se conserva el gate existente y se corrige la auditoría: añadir otro workflow habría duplicado infraestructura. |
| R-29 | resuelto | Focus, clima, Steam, calendario y continuidad usaban directamente `System.currentTimeMillis()` en decisiones de frescura/expiración. | `TimeProvider` es inyectable en esas fronteras y `SystemTimeProvider` es el default de producción; políticas puras siguen recibiendo `nowMillis` explícito. |
| R-30 | resuelto | `BuildConfig` se consultaba dentro de la pantalla de ajustes y `SettingsShortcut` mezclaba copy/search con acciones Android. | La Activity proyecta `LauncherPublisherInfo`; la UI solo recibe datos y `AndroidSettingsLauncher` conserva internamente el mapa entre ids y acciones Android. |

## Aspectos que se conservarán

- módulo único `app` y composición manual de dependencias;
- repositorios por fuente/funcionalidad, StateFlow y flujo unidireccional;
- workspaces explícitos, no un motor genérico de widgets;
- SharedPreferences únicamente para los datos ya permitidos;
- fallbacks honestos y ausencia de consultas de Android/red durante recomposición;
- geometría, privacidad y alcance funcional definidos en `PROJECT_CONTEXT.md`.

## Estrategia de ejecución

1. Crear una red de seguridad: mantener la línea base verde y añadir tests antes
   de cambiar lógica con riesgo.
2. Aplicar primero extracciones mecánicas (`Extract Class`, `Move Method`) y
   eliminar código muerto confirmado.
3. Corregir configuración inicial/localización y contratos de UI.
4. Dividir controlador y repositorios una fuente cada vez.
5. Migrar textos y presentation models.
6. Cerrar cada bloque con tests, lint y APK; al final, ejercer flujos en un
   dispositivo físico cuando esté conectado.

No se mezclará una refactorización estructural con cambios visuales o nuevas
funcionalidades. Si un hallazgo exige alterar comportamiento, tendrá su test de
caracterización y una entrada explícita en este documento.

## Cierre de la pasada

Verificación final: 20 de agosto de 2026.

- 108 archivos Kotlin de producción y 13.156 líneas, repartidos por fronteras de
  UI, coordinación, políticas, modelos, sistema y fuentes de datos.
- Los dos archivos de más de 500 líneas restantes (`AppDrawer` y
  `GameWorkspace`) representan cada uno una sola pantalla/feature cohesionada;
  no se dividieron sólo por el contador de líneas.
- 83 tests unitarios verdes y 4 tests instrumentados compilados.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug` y
  `compileDebugAndroidTestKotlin`: correctos.
- Lint: 0 errores y 0 avisos; `git diff --check`: correcto.
- APK de aplicación y APK de tests instrumentados generados correctamente.
- `connectedDebugAndroidTest` llega a la fase de ejecución y no puede continuar
  porque ADB no tiene ningún dispositivo conectado; no se configuró un emulador
  administrado nuevo para no ampliar infraestructura fuera del alcance.
