package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.StorageUsagePolicy
import dev.vicent.veil.launcher.model.ConnectionType
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.util.Locale

private val DeviceDashboardTileHeight = 184.dp
private val ToolsSecondaryTileHeight = 116.dp
private val SettingsTileHeight = 150.dp

@Composable
internal fun ToolsWorkspace(
    systemStatus: SystemStatus,
    compact: Boolean,
    onVeilSettingsSelected: () -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeviceDashboardTile(systemStatus, onSettingsSelected)
        ResponsivePair(
            compact = compact,
            left = {
                val system = systemStatus
                CozyTile(
                    label = "Batería",
                    modifier = Modifier.fillMaxWidth().heightIn(min = ToolsSecondaryTileHeight),
                ) {
                    TileTitle(
                        system.batteryPercent
                            ?.let { "$it%${if (system.isCharging) " · cargando" else ""}" }
                            ?: "No disponible",
                    )
                    TileBody(
                        when {
                            system.batteryPercent == null -> "Android no ha publicado el estado"
                            system.isCharging -> "Conectado a la corriente"
                            else -> "Funcionando con batería"
                        },
                    )
                    TileAction("Abrir batería") { onSettingsSelected("battery") }
                }
            },
            right = {
                val connectionLabel = systemStatus.connectionType.presentationLabel
                CozyTile(
                    label = "Conectividad",
                    modifier = Modifier.fillMaxWidth().heightIn(min = ToolsSecondaryTileHeight),
                ) {
                    TileTitle(connectionLabel)
                    TileBody(
                        if (systemStatus.connectionType == ConnectionType.NONE) {
                            "No hay una red activa"
                        } else {
                            "Transporte activo"
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TileAction("Redes") { onSettingsSelected("network") }
                        TileAction("Bluetooth") { onSettingsSelected("bluetooth") }
                    }
                }
            },
        )
        SettingsPanel(onVeilSettingsSelected, onSettingsSelected)
    }
}

private val ConnectionType.presentationLabel: String
    get() = when (this) {
        ConnectionType.NONE -> "Sin conexión"
        ConnectionType.WIFI -> "Wi‑Fi"
        ConnectionType.CELLULAR -> "Datos móviles"
        ConnectionType.ETHERNET -> "Ethernet"
        ConnectionType.OTHER -> "Conectado"
    }

@Composable
private fun DeviceDashboardTile(
    system: SystemStatus,
    onSettingsSelected: (String) -> Unit,
) {
    val deviceName = listOfNotNull(system.deviceManufacturer, system.deviceModel)
        .distinct()
        .joinToString(" ")
        .ifBlank { "Dispositivo no disponible" }
    val androidLabel = system.androidVersion?.let { "Android $it" } ?: "Android no disponible"
    val patchLabel = system.securityPatch?.let { "Parche $it" } ?: "Parche no disponible"

    CozyTile(
        label = "Dispositivo",
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = DeviceDashboardTileHeight),
    ) {
        TileTitle(deviceName, prominent = true)
        TileBody("$androidLabel · $patchLabel")
        DeviceMetric(
            label = "Almacenamiento",
            availableBytes = system.storageAvailableBytes,
            totalBytes = system.storageTotalBytes,
        )
        DeviceMetric(
            label = "Memoria RAM",
            availableBytes = system.memoryAvailableBytes,
            totalBytes = system.memoryTotalBytes,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TileAction("Detalles") { onSettingsSelected("device_info") }
            TileAction("Almacenamiento") { onSettingsSelected("storage") }
        }
    }
}

@Composable
private fun DeviceMetric(label: String, availableBytes: Long, totalBytes: Long) {
    val palette = LocalVeilPalette.current
    val usedFraction = StorageUsagePolicy.usedFraction(availableBytes, totalBytes)
    val detail = if (usedFraction == null) {
        "NO DISPONIBLE"
    } else {
        val usedBytes = totalBytes - availableBytes.coerceIn(0L, totalBytes)
        "${formatCapacity(usedBytes)} / ${formatCapacity(totalBytes)}"
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        BasicText(label.uppercase(), style = workspaceMonoStyle(palette.contentMuted, 9))
        BasicText(detail, style = workspaceMonoStyle(palette.contentSecondary, 9))
    }
    if (usedFraction != null) SimpleProgress(usedFraction)
}

@Composable
private fun SettingsPanel(
    onVeilSettingsSelected: () -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    CozyTile(
        label = "Centro de control",
        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsTileHeight),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCell("Pantalla", "display", onSettingsSelected, Modifier.weight(1f))
            SettingsCell("Sonido", "sound", onSettingsSelected, Modifier.weight(1f))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            SettingsCell("Aplicaciones", "applications", onSettingsSelected, Modifier.weight(1f))
            SettingsCell("Seguridad", "security", onSettingsSelected, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TileAction("Ajustes de Veil", onVeilSettingsSelected)
            TileAction("Todos los ajustes") { onSettingsSelected("settings") }
        }
    }
}

@Composable
private fun SettingsCell(
    label: String,
    id: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = label.uppercase(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 9),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LocalVeilPalette.current.subtleFill)
            .clickable(role = Role.Button, onClickLabel = "Abrir $label") { onSelected(id) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

private fun formatCapacity(bytes: Long): String {
    if (bytes < 0L) return "No disponible"
    val gibibytes = bytes / 1_073_741_824.0
    return if (gibibytes >= 1.0) {
        String.format(Locale.getDefault(), "%.1f GB", gibibytes)
    } else {
        String.format(Locale.getDefault(), "%.0f MB", bytes / 1_048_576.0)
    }
}
