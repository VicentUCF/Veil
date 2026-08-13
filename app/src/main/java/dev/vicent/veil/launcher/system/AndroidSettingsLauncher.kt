package dev.vicent.veil.launcher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import dev.vicent.veil.launcher.model.SettingsShortcut

class AndroidSettingsLauncher(private val context: Context) {
    val shortcuts = listOf(
        SettingsShortcut(
            id = "settings",
            label = "Ajustes",
            searchTerms = "configuracion preferencias sistema telefono settings",
            action = Settings.ACTION_SETTINGS,
        ),
        SettingsShortcut(
            id = "network",
            label = "Wi-Fi e Internet",
            searchTerms = "wifi red redes conexion conexiones datos movil internet",
            action = Settings.ACTION_WIRELESS_SETTINGS,
        ),
        SettingsShortcut(
            id = "bluetooth",
            label = "Bluetooth",
            searchTerms = "dispositivos vinculados conexion auriculares",
            action = Settings.ACTION_BLUETOOTH_SETTINGS,
        ),
        SettingsShortcut(
            id = "display",
            label = "Pantalla",
            searchTerms = "brillo fondo tema oscuro display",
            action = Settings.ACTION_DISPLAY_SETTINGS,
        ),
        SettingsShortcut(
            id = "sound",
            label = "Sonido",
            searchTerms = "audio volumen tono vibracion silencio",
            action = Settings.ACTION_SOUND_SETTINGS,
        ),
        SettingsShortcut(
            id = "applications",
            label = "Aplicaciones",
            searchTerms = "apps permisos predeterminadas desinstalar almacenamiento",
            action = Settings.ACTION_APPLICATION_SETTINGS,
        ),
        SettingsShortcut(
            id = "storage",
            label = "Almacenamiento",
            searchTerms = "almacenamiento espacio memoria archivos storage",
            action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        ),
        SettingsShortcut(
            id = "battery",
            label = "Batería",
            searchTerms = "bateria energia ahorro consumo",
            action = Settings.ACTION_BATTERY_SAVER_SETTINGS,
        ),
        SettingsShortcut(
            id = "security",
            label = "Seguridad",
            searchTerms = "privacidad bloqueo pantalla pin huella contrasena",
            action = Settings.ACTION_SECURITY_SETTINGS,
        ),
        SettingsShortcut(
            id = "device_info",
            label = "Información del dispositivo",
            searchTerms = "telefono dispositivo modelo android version parche informacion",
            action = Settings.ACTION_DEVICE_INFO_SETTINGS,
        ),
        SettingsShortcut(
            id = "language",
            label = "Idioma y teclado",
            searchTerms = "idiomas teclado entrada region locale",
            action = Settings.ACTION_LOCALE_SETTINGS,
        ),
        SettingsShortcut(
            id = "accessibility",
            label = "Accesibilidad",
            searchTerms = "lector pantalla tamaño texto contraste asistencia",
            action = Settings.ACTION_ACCESSIBILITY_SETTINGS,
        ),
    )

    fun launch(shortcut: SettingsShortcut): Boolean =
        start(Intent(shortcut.action)) || start(Intent(Settings.ACTION_SETTINGS))

    private fun start(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
