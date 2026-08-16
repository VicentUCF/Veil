# Política de privacidad de Veil

Última actualización: 13 de agosto de 2026.

Veil es un launcher Android sin cuentas de usuario, publicidad, analítica, rastreadores ni backend propio.

## Datos procesados en el dispositivo

- Lista de aplicaciones instaladas y preferencias del launcher, para mostrar y configurar el inicio.
- Eventos próximos del calendario, solo con permiso y para mostrarlos en el workspace.
- Señales de notificaciones compatibles, solo con acceso explícito. Veil conserva en memoria claves y nombres de paquete necesarios para indicadores sin contenido; no muestra ni persiste el texto de las notificaciones.
- Espectro de salida de audio, solo con permiso de micrófono, mientras Veil está en primer plano, MEDIA está visible y hay reproducción. El FFT es transitorio y no se guarda ni transmite.
- Notas rápidas locales. Se excluyen de copias de seguridad y transferencias de dispositivo.
- Preferencias, temporizadores y cachés locales necesarios para el funcionamiento. El usuario puede eliminarlos borrando los datos de la aplicación o desinstalándola.

## Conexiones externas

- Open-Meteo recibe la ubicación aproximada solicitada en primer plano para devolver el tiempo local. Veil conserva el resultado hasta 30 minutos y puede mostrarlo como obsoleto hasta 2 horas.
- Servicios públicos de Steam reciben consultas para rankings, metadatos, noticias e imágenes solo mientras GAME es visible. Veil limita y almacena temporalmente esas respuestas.

Las conexiones usan HTTPS y se restringen a los dominios necesarios. Como en toda conexión de red, los proveedores reciben la dirección IP y datos técnicos básicos necesarios para responder. Sus propias políticas rigen ese tratamiento.

## Permisos y control

Los accesos a calendario, ubicación aproximada, notificaciones, audio, alarmas exactas y notificaciones de Focus son opcionales y revocables desde los ajustes de Android. Veil ofrece comportamiento degradado cuando no están disponibles.

## Compartición, venta y conservación

Veil no vende datos ni los comparte con fines publicitarios. No existe una cuenta remota de Veil. Los únicos destinatarios externos son Open-Meteo y Steam para las funciones descritas. Los datos locales permanecen hasta que el usuario los borra; las señales transitorias se eliminan al perder el acceso o finalizar la sesión relevante.

## Publicación y contacto

Antes de distribuir una versión de producción, el editor debe publicar este documento en una URL HTTPS activa e indicar un contacto de privacidad monitorizado. El build de release exige ambos valores mediante `VEIL_PRIVACY_POLICY_URL` y `VEIL_PRIVACY_CONTACT`, evitando publicar accidentalmente una ficha incompleta.

Los cambios materiales de esta política se reflejarán en la fecha de actualización y en la copia pública enlazada desde la ficha de la aplicación.
