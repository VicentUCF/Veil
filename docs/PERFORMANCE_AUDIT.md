# Auditoría de rendimiento de Veil

Fecha: 20 de agosto de 2026.

## Objetivo y límites

La pasada revisa el arranque, Everything, acciones, listas, actualizaciones de estado,
integraciones Android y coste de renderizado Compose. Conserva el alcance de
`PROJECT_CONTEXT.md`: un único módulo `app`, APIs Android/AndroidX/Compose, sin
analítica, backend, persistencia adicional ni nuevas abstracciones de producto.

No había dispositivo conectado por ADB durante la auditoría. Por ello los cambios se
validan con tests, lint y compilación, pero no se presentan tiempos de frame o arranque
inventados. La siguiente medición física debe realizarse en un build `release` o
profileable sobre el Xiaomi previsto.

## Hallazgos y correcciones

| Prioridad | Camino | Hallazgo | Corrección aplicada |
| --- | --- | --- | --- |
| P0 | Everything | Cada pulsación normalizaba etiqueta y paquete para todas las apps y recorría las asociaciones aprendidas por cada candidato. | Los candidatos conservan texto y palabras normalizados durante la vida del inventario; el aprendizaje se agrupa una vez por paquete antes del ranking. Se mantiene exactamente la política de relevancia. |
| P0 | Estado/Compose | Cualquier cambio del estado global recreaba los modelos de todos los workspaces; el espectro de audio podía provocar recomposición ajena a MEDIA. | Cada workspace memoriza su proyección usando solo sus dependencias. TopBar reutiliza la lista de definiciones y GAME reutiliza su biblioteca resuelta. |
| P0 | Focus | El repositorio leía SharedPreferences y disponibilidad de alarmas/notificaciones cada segundo aunque el temporizador estuviera inactivo o pausado. | El ticker existe únicamente mientras Focus está en estado `RUNNING`; las acciones siguen publicando el estado inmediatamente. |
| P0 | Audio | Los tres canales de volumen se consultaban cada 750 ms durante toda la vida de la Activity. | El sondeo se activa solo con Veil visible y MEDIA seleccionado, y se detiene completamente al cerrar cualquiera de esas dos puertas. |
| P1 | Continuidad multimedia | Play, pausa o cambio de metadatos volvía a consultar todas las sesiones por Binder y reconstruía todos los callbacks. | Los callbacks ahora reproyectan el estado de los controladores ya conocidos. Solo un cambio real del conjunto de sesiones vuelve a consultar Android y reconstruir callbacks. Las etiquetas de apps activas se cachean. |
| P1 | Iconos/arranque | La carga prioritaria del dock y el inventario completo podían rasterizar dos veces el mismo icono; el bitmap tenía 128 px fijos en cualquier densidad. | Caché acotada por componente durante el snapshot y tamaño de raster dependiente de densidad, limitado a 64–160 px. Un refresco completo invalida la caché para no mostrar iconos obsoletos. |
| P1 | Paquetes | Una instalación o desinstalación puede emitir varios broadcasts y provocar escaneos completos consecutivos. | Las solicitudes se conflan durante 250 ms y solo la última ejecuta el refresco. Una app eliminada desaparece del estado inmediatamente. |
| P1 | Listas GAME | El diálogo de biblioteca componía todas las filas aunque estuvieran fuera de pantalla y normalizaba cada app en cada tecla. | La biblioteca usa `LazyColumn` con claves estables e índice de texto reutilizable. |
| P2 | Pickers | El selector de apps volvía a normalizar y ordenar todo el inventario con cada cambio de consulta. | El orden base y el índice de búsqueda se calculan una vez por snapshot/objetivo; cada tecla solo filtra. |

## Impacto esperado

- Menos trabajo y asignaciones en el hilo principal al escribir en Everything.
- Cero sondeo periódico de Focus cuando no corre y cero sondeo de volumen fuera de
  MEDIA visible.
- Menos consultas Binder y reconstrucciones de callbacks al controlar multimedia.
- Menos rasterizaciones duplicadas en arranque y menor memoria de iconos en pantallas
  de densidad baja/media, manteniendo nitidez en densidades altas.
- Menos composición de filas fuera de pantalla y menos recomposición cruzada entre
  workspaces.

## Verificación realizada

- `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon`: correcto.
- Lint: sin errores ni avisos.
- APK debug generado en `app/build/outputs/apk/debug/app-debug.apk`.
- `git diff --check`: correcto.
- Validación física pendiente: no había dispositivo en `adb devices -l`.

## Medición física recomendada

Conectar el dispositivo objetivo y repetir, sin cambiar comportamiento:

1. Medir arranque frío y caliente con `am start -W` tras varias iteraciones.
2. Capturar Everything abriendo, escribiendo consultas cortas/largas y desplazando la
   lista con System Trace/JankStats o Perfetto.
3. Ejercer swipes entre los cinco contextos, abrir/cerrar ajustes y mantener MEDIA con
   reproducción activa.
4. Comparar frames lentos, tiempo de CPU, asignaciones y memoria de bitmaps en un build
   equivalente antes/después.

