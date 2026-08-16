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
| R-02 | resuelto | `LauncherScreen` recibía más de cuarenta callbacks y combinaba pager, gestos, overlays, permisos, navegación, ajustes y acciones de apps. `Long Parameter List` y `Data Clumps`. | La API usa cinco contratos acotados por responsabilidad y disclosures/acciones de app viven en `LauncherOverlays`; no se introdujo un bus global. |
| R-03 | resuelto | `LauncherController` agregaba once dependencias, iniciaba todos los observadores, resolvía apps, transformaba continuidad, navegaba, gestionaba ajustes y recibía `CoroutineScope` en varias operaciones. `Large Class`, `Middle Man` y `Divergent Change`. | Resolución, sincronización de fuentes y coordinación de navegación son clases separadas; el scope de lifecycle se inyecta una sola vez y el controlador queda como fachada de 269 líneas. |
| R-04 | resuelto | `LauncherUiState` era un snapshot global que llegaba completo a CURRENT, WORK, MEDIA y TOOLS. Esto contradecía el requisito local de composables con estados pequeños. | `WorkspaceDashboard` proyecta modelos inmutables específicos; cada workspace recibe únicamente clima/media, agenda/focus/notas, audio/proveedor o sistema según corresponda. |
| R-05 | pendiente | No hay tests de `LauncherController`, persistencia de preferencias/notas/focus ni regresiones Compose de navegación y workspaces. Las clases concretas y Android dificultan los fakes. | Introducir límites mínimos sustituibles donde aporten pruebas, añadir tests de estado/flujo y tests instrumentados para navegación crítica. |

### P1 — responsabilidades grandes y configuración personal

| ID | Estado | Evidencia y olor | Resolución prevista |
| --- | --- | --- | --- |
| R-06 | resuelto | `LauncherSettingsScreen.kt` contenía pantalla raíz, subpantalla de apariencia, picker, slider, filas y tres diálogos; además recibía más de veinte acciones. | Raíz, apariencia, picker y componentes tienen archivos de 174–347 líneas; el contrato usa un estado y cuatro grupos de acciones acotados. |
| R-07 | resuelto | `CurrentHome.kt` mezclaba layout, reloj/tiempo, dibujo del glifo meteorológico, reproductor, lista de apps y botón principal. | `CurrentHome` queda como composición de 253 líneas; estilo, reloj/clima y reproductor tienen archivos propios de 81–237 líneas. |
| R-08 | resuelto | `MainActivity` era composition root, host Compose, coordinador de permisos/roles y lanzador de todas las superficies Android. | Accesos/roles/alarmas y acciones externas viven en dos coordinadores concretos; la Activity queda en 334 líneas como host, lifecycle, composición manual y receptores. |
| R-09 | resuelto | `AmbientContinuityRepository` combinaba sesiones de media, notificaciones, preferencias de onboarding, acciones y lifecycle de callbacks. | El repositorio coordinador queda en 126 líneas; sesiones, mapper de notificaciones y onboarding son componentes separados y el mapper tiene tests de expiración. |
| R-10 | resuelto | `SteamGameRepository` combinaba HTTP, validación, parsing JSON, composición del feed, bitmap y caché; además fijaba región e idioma personales. | Repositorio (composición), cliente remoto, parser y caché son piezas separadas de 64–135 líneas; las URLs ya usan el fallback neutral de Steam y los tests del parser permanecen. |
| R-11 | resuelto | `WeatherRepository` combinaba proveedor de ubicación, cliente Open-Meteo, parser, caché y política de frescura. | Repositorio de 68 líneas, fuente de ubicación, cliente/parser y caché son piezas separadas; los tests de parsing y límites de respuesta siguen verdes. |
| R-12 | resuelto | `FocusTimerRepository` combinaba máquina de estado, SharedPreferences, AlarmManager, restauración de receiver, canal y notificación. | Coordinador, store y scheduler/alerter son clases separadas de 58–94 líneas; el receiver depende directamente del scheduler y los tests siguen verdes. |
| R-13 | resuelto | `WorkspaceDataPolicy` agrupaba agenda, Focus, almacenamiento, biblioteca GAME y visibilidad del dock: responsabilidades sin cohesión. | Dividido en políticas de agenda, focus, almacenamiento, biblioteca GAME y layout. |
| R-14 | resuelto | `LauncherSettings.kt` y `WorkspaceState.kt` agrupaban modelos de funcionalidades no relacionadas; `LauncherController.kt` también contenía los modelos de UI resuelta. | Cada familia tiene ahora un archivo reconocible y `LauncherUiState` salió del controlador. |
| R-15 | resuelto | `LauncherConfig` fijaba una selección personal de paquetes y un test exigía Google Authenticator en una posición concreta. | Los slots iniciales usan candidatos multiproveedor y fallbacks de categoría; GAME ya no nombra juegos personales y los overrides siguen siendo exactos. |
| R-16 | pendiente | Solo hay 6 recursos string frente a más de 200 textos/semánticas incrustados en Kotlin; también hay texto de presentación en repositorios/adaptadores. | Mover texto de usuario y accesibilidad a recursos, con formato/plurales cuando corresponda; dejar en código solo identificadores técnicos o copy deliberadamente no localizado. |
| R-17 | resuelto | `collectAsState()` mantenía la colección aunque la UI no estuviera en un estado de lifecycle activo. | La Activity usa ahora `collectAsStateWithLifecycle()`, según la guía oficial de Android. |
| R-18 | resuelto | `SystemStatus` almacenaba `connectionLabel` en español aunque ya contenía `ConnectionType`; eran dos fuentes para el mismo dato y la data layer decidía presentación. | El modelo conserva solo `ConnectionType`; la etiqueta se deriva en la capa de UI. |
| R-19 | resuelto | Refrescar accesos lanzaba calendario y tiempo y `onResume` llamaba además a `refreshVisibleData`; tampoco había coordinación explícita de peticiones duplicadas. | La lectura de permisos ya no refresca datos, el controlador cancela el trabajo anterior por fuente y calendar/weather serializan sus secciones críticas. |

### P2 — deuda localizada y calidad continua

| ID | Estado | Evidencia y olor | Resolución prevista |
| --- | --- | --- | --- |
| R-20 | resuelto | Código muerto confirmado: `CurrentWorkspace`; métodos de selección antiguos en `AppRepository`; `toggleDrawer` y `resetContextApps` sin consumidores de producción. | Eliminado junto con sus tests de APIs obsoletas; los tests de selección activa permanecen. |
| R-21 | pendiente | La descripción meteorológica está duplicada en CURRENT y dashboards; formatos de fecha/capacidad/duración y estilos se encuentran dispersos. | Extraer presentación específica compartida, evitando un archivo `Utils` genérico. |
| R-22 | pendiente | Dimensiones, límites y duraciones de UI aparecen como números repetidos; solo parte del movimiento está tokenizado. | Crear tokens pequeños por sistema visual cuando exista repetición o una regla de producto común. |
| R-23 | resuelto | `LauncherApp` llevaba un `Drawable` mutable dentro del estado Compose y el icono se rasterizaba en UI. | `AppIconLoader` rasteriza una sola vez fuera de Compose y el modelo expone un bitmap inmutable; la UI no consulta paquetes ni muta drawables. |
| R-24 | en curso | Lint registraba sugerencias KTX y un aviso de atributo API 24 con `minSdk 23`. | Se migraron URI, preferencias y bitmap a KTX y se documentó con `tools:targetApi` que la configuración de red se ignora deliberadamente solo en API 23; falta confirmar el informe final. |
| R-25 | pendiente | Nombres como `MaxNotes`, `PreferencesName` y `quickActionCount`, además de formato irregular en `Theme.kt`, no siguen la convención Kotlin del proyecto. | Normalizar constantes, visibilidad y formato al tocar cada área. |
| R-26 | resuelto | `README.md` mencionaba `SOCIAL`, describía una configuración antigua y presentaba el código fuente como personalización normal. | Actualizado para GAME, candidatos neutrales y overrides exactos desde Ajustes de Veil. |
| R-27 | pendiente | Las restricciones de versiones vulnerables se duplican entre Gradle raíz y `app`; el catálogo usa una versión RC de Kotlin. | Consolidar la política sin añadir infraestructura especulativa y validar una versión estable compatible antes de una release. |
| R-28 | pendiente | No existe un quality gate de CI versionado. | Añadir un workflow mínimo para tests, lint y assemble cuando la refactorización estructural esté estable. |
| R-29 | pendiente | Repositorios usan directamente `System.currentTimeMillis()` y APIs globales en puntos que necesitan pruebas temporales. | Introducir un proveedor de tiempo solo en las funcionalidades que lo necesiten para tests deterministas. |
| R-30 | pendiente | `BuildConfig` se consulta dentro de la pantalla de ajustes y `SettingsShortcut` mezcla copy/search con acciones Android. | Proyectar información editorial y shortcuts a modelos de UI desde el borde Android. |

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
