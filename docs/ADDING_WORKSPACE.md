# Añadir una vista editorial a Veil

Las vistas de Veil son composiciones de producto compiladas con la aplicación. No son widgets ni configuraciones descargables.

## Contrato

1. Añade una identidad nueva y estable a `LauncherContextKind`. No reutilices ni renombres una identidad publicada.
2. Registra la vista en `LauncherConfig.workspaceCatalog` con título, descripción, capacidades, disponibilidad y cinco accesos semánticos predeterminados.
3. Define un estado inmutable pequeño en `WorkspaceUiState.kt`; no expongas repositorios, cursores ni handles Android a Compose.
4. Implementa un composable de workspace explícito y añádelo al `when` exhaustivo de `WorkspaceDashboard`.
5. Declara solo las capacidades que consume. `LauncherController` usa esas capacidades para activar calendario, tiempo, Steam y audio únicamente cuando corresponde.
6. Diseña estados reales para permiso denegado, datos ausentes, carga, error y caché obsoleta. La geometría debe permanecer estable y una sola superficie debe ser principal.
7. Añade strings, metadatos de catálogo y pruebas que comprueben identidad única, cinco slots, capacidades y política de fallback.

## Compatibilidad

- Una vista nueva aparece en el catálogo después de actualizar, pero no se activa automáticamente.
- Los docks se persisten por `LauncherContextKind`, no por posición.
- Para retirar una vista publicada, márcala `RETIRING` durante al menos una versión y mantenla renderizable. Una migración posterior debe sustituirla de forma determinista antes de eliminar su código.
- Añadir una capacidad o transmisión nueva exige revisar permisos, política de privacidad y Data Safety antes de publicar.
